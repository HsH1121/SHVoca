package com.baekseok.shvoca.ui

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.baekseok.shvoca.BuildConfig
import com.baekseok.shvoca.data.KanjiDatabase
import com.baekseok.shvoca.data.KanjiWord
import com.baekseok.shvoca.ui.theme.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private data class ParsedWord(val kanji: String, val furigana: String, val meaning: String)
private enum class Phase { Pick, Loading, Review }

private fun createImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").also { it.mkdirs() }
    val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "com.baekseok.shvoca.fileprovider", file)
}

private fun buildRecognizer(languageType: String): TextRecognizer = when (languageType) {
    "일본어" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    "중국어", "한자" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
}

private suspend fun runOcr(context: Context, uri: Uri, languageType: String): String {
    val stream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(stream)
    stream?.close()
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = buildRecognizer(languageType)
    return suspendCancellableCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { cont.resume(it.text) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
}

private fun buildPrompt(languageType: String, ocrText: String): String = when (languageType) {
    "일본어" -> """
        아래는 일본어 교재에서 OCR로 추출한 텍스트입니다.
        단어 목록을 JSON 배열로 파싱하세요. 반드시 JSON 배열만 응답하세요.
        형식: [{"kanji":"漢字","furigana":["ふりがな1","ふりがな2"],"meaning":"한국어 뜻1, 뜻2"}]
        - kanji: 원문 단어(한자 또는 가나)
        - furigana: 히라가나 읽기 배열(복수 읽기 가능), 없으면 []
        - meaning: 한국어 뜻, 여러 뜻은 쉼표로 구분(예: "가다, 오르다, 나아지다")
        - 단어가 아닌 내용(페이지 번호, 챕터 제목 등) 제외
        텍스트: $ocrText
    """.trimIndent()
    "중국어" -> """
        아래는 중국어 교재에서 OCR로 추출한 텍스트입니다.
        단어 목록을 JSON 배열로 파싱하세요. 반드시 JSON 배열만 응답하세요.
        형식: [{"kanji":"汉字","furigana":"pīnyīn","meaning":"한국어 뜻1, 뜻2"}]
        - kanji: 중국어 단어(한자)
        - furigana: 병음(pinyin), 없으면 ""
        - meaning: 한국어 뜻, 여러 뜻은 쉼표로 구분(예: "가다, 이동하다")
        - 단어가 아닌 내용 제외
        텍스트: $ocrText
    """.trimIndent()
    "한자" -> """
        아래는 한자 교재에서 OCR로 추출한 텍스트입니다.
        한자 목록을 JSON 배열로 파싱하세요. 반드시 JSON 배열만 응답하세요.
        형식: [{"kanji":"漢字","furigana":"음","meaning":"뜻1, 뜻2"}]
        - kanji: 한자
        - furigana: 한국어 음(독음), 없으면 ""
        - meaning: 한자의 뜻(훈), 여러 훈은 쉼표로 구분(예: "즐거울, 음악, 좋아할")
        - 단어가 아닌 내용 제외
        텍스트: $ocrText
    """.trimIndent()
    else -> """
        아래는 영어 교재에서 OCR로 추출한 텍스트입니다.
        단어 목록을 JSON 배열로 파싱하세요. 반드시 JSON 배열만 응답하세요.
        형식: [{"kanji":"word","furigana":"pronunciation","meaning":"한국어 뜻1, 뜻2"}]
        - kanji: 영어 단어
        - furigana: 발음기호 또는 발음 표기, 없으면 ""
        - meaning: 한국어 뜻, 여러 뜻은 쉼표로 구분(예: "은행, 강둑, 기울다")
        - 단어가 아닌 내용 제외
        텍스트: $ocrText
    """.trimIndent()
}

private suspend fun parseWithGemini(languageType: String, ocrText: String): List<ParsedWord> {
    val model = GenerativeModel(modelName = "gemini-2.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
    val response = model.generateContent(buildPrompt(languageType, ocrText))
    val raw = response.text?.trim() ?: return emptyList()
    val json = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val furigana = when (val f = obj.opt("furigana")) {
                is JSONArray -> (0 until f.length())
                    .mapNotNull { f.optString(it).takeIf { s -> s.isNotBlank() } }
                    .joinToString("|")
                else -> obj.optString("furigana")
            }
            ParsedWord(
                kanji    = obj.optString("kanji"),
                furigana = furigana,
                meaning  = obj.optString("meaning")
            )
        }.filter { it.kanji.isNotBlank() || it.meaning.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }
}

// 상태와 로직을 담은 콘텐츠 — BottomSheet와 전체화면 모두에서 재사용
@Composable
fun PhotoWordAddContent(language: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val db      = remember { KanjiDatabase.getInstance(context.applicationContext) }
    val book    by db.kanjiDao().getBookByName(language).collectAsState(initial = null)
    val langType = book?.languageType ?: "영어"

    var phase       by remember { mutableStateOf<Phase>(Phase.Pick) }
    var cameraUri   by remember { mutableStateOf<Uri?>(null) }
    var editWords   by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var selected    by remember { mutableStateOf<List<Boolean>>(emptyList()) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    fun processUri(uri: Uri) {
        phase = Phase.Loading
        scope.launch {
            try {
                val ocrText = runOcr(context, uri, langType)
                if (ocrText.isBlank()) { errorMsg = "텍스트를 인식하지 못했습니다."; phase = Phase.Pick; return@launch }
                val words = parseWithGemini(langType, ocrText)
                if (words.isEmpty()) { errorMsg = "단어를 파싱하지 못했습니다."; phase = Phase.Pick; return@launch }
                editWords = words.map { Triple(it.kanji, it.furigana, it.meaning) }
                selected  = List(words.size) { true }
                phase     = Phase.Review
            } catch (e: Exception) {
                errorMsg = e.message ?: "오류가 발생했습니다."
                phase = Phase.Pick
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processUri(it) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { processUri(it) }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    when (phase) {
        Phase.Pick    -> PickPhase(
            errorMsg  = errorMsg,
            onGallery = { errorMsg = null; galleryLauncher.launch("image/*") },
            onCamera  = { errorMsg = null; cameraPermission.launch(Manifest.permission.CAMERA) }
        )
        Phase.Loading -> LoadingPhase()
        Phase.Review  -> ReviewPhase(
            languageType = langType,
            editWords    = editWords,
            selected     = selected,
            onWordChange = { idx, t -> editWords = editWords.toMutableList().also { it[idx] = t } },
            onToggle     = { idx -> selected = selected.toMutableList().also { it[idx] = !it[idx] } },
            onAdd = {
                scope.launch {
                    val toInsert = editWords.zip(selected)
                        .filter { (_, sel) -> sel }
                        .map { (w, _) -> KanjiWord(language = language, kanji = w.first, furigana = w.second, meaning = w.third) }
                    db.kanjiDao().insertAll(toInsert)
                    onDismiss()
                }
            },
            onBack = { phase = Phase.Pick }
        )
    }
}

@Composable
private fun PickPhase(errorMsg: String?, onGallery: () -> Unit, onCamera: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (errorMsg != null) {
            Text(errorMsg, color = Red, fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onGallery,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper),
                modifier = Modifier.weight(1f).height(80.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("갤러리", fontSize = 13.sp)
                }
            }
            OutlinedButton(
                onClick = onCamera,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                modifier = Modifier.weight(1f).height(80.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("카메라", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun LoadingPhase() {
    Box(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Gold)
            Spacer(Modifier.height(16.dp))
            Text("단어 인식 중...", color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ReviewPhase(
    languageType: String,
    editWords: List<Triple<String, String, String>>,
    selected: List<Boolean>,
    onWordChange: (Int, Triple<String, String, String>) -> Unit,
    onToggle: (Int) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    val selectedCount = selected.count { it }
    val (label1, label2) = when (languageType) {
        "일본어" -> "단어" to "후리가나"
        "중국어" -> "단어" to "병음"
        "한자"   -> "한자" to "음"
        else     -> "단어" to "발음"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("${editWords.size}개 단어 인식됨 · ${selectedCount}개 선택", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(editWords) { idx, word ->
                ParsedWordCard(
                    languageType = languageType,
                    word = word, selected = selected[idx],
                    label1 = label1, label2 = label2,
                    onToggle = { onToggle(idx) },
                    onChange = { onWordChange(idx, it) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text("다시 찍기") }
            Button(
                onClick = onAdd, enabled = selectedCount > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Paper),
                modifier = Modifier.weight(1f).height(52.dp)
            ) { Text("${selectedCount}개 추가") }
        }
    }
}

@Composable
private fun ParsedWordCard(
    languageType: String,
    word: Triple<String, String, String>,
    selected: Boolean,
    label1: String,
    label2: String,
    onToggle: () -> Unit,
    onChange: (Triple<String, String, String>) -> Unit
) {
    val isJapanese = languageType == "일본어"
    var readings by remember {
        mutableStateOf(
            if (isJapanese)
                word.second.split("|").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("") }
            else listOf(word.second)
        )
    }

    fun notifyReadings(newReadings: List<String>) {
        readings = newReadings
        onChange(word.copy(second = newReadings.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("|")))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Paper else Line.copy(alpha = 0.3f))
            .border(1.dp, if (selected) Line else Line.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected, onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = Gold, uncheckedColor = Muted)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            WordField(label1, word.first) { onChange(word.copy(first = it)) }
            if (isJapanese) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label2, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "후리가나 추가",
                        tint = Ink,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { notifyReadings(readings + "") }
                    )
                }
                readings.forEachIndexed { i, value ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { new ->
                                notifyReadings(readings.toMutableList().also { it[i] = new })
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Gold, unfocusedBorderColor = Line,
                                focusedTextColor = Ink, unfocusedTextColor = Ink,
                                cursorColor = Gold,
                                focusedContainerColor = Paper, unfocusedContainerColor = Paper
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                            modifier = Modifier.weight(1f)
                        )
                        if (readings.size > 1) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "×", color = Muted, fontSize = 18.sp,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { notifyReadings(readings.toMutableList().also { it.removeAt(i) }) }
                            )
                        }
                    }
                }
            } else {
                WordField(label2, word.second) { onChange(word.copy(second = it)) }
            }
            WordField("뜻", word.third) { onChange(word.copy(third = it)) }
        }
    }
}

@Composable
private fun WordField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold, unfocusedBorderColor = Line,
            focusedTextColor = Ink, unfocusedTextColor = Ink,
            cursorColor = Gold,
            focusedContainerColor = Paper, unfocusedContainerColor = Paper
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        modifier = Modifier.fillMaxWidth()
    )
}
