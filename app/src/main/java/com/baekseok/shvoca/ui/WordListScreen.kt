package com.baekseok.shvoca.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.baekseok.shvoca.R
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.baekseok.shvoca.data.KanjiDatabase
import com.baekseok.shvoca.data.KanjiWord
import com.baekseok.shvoca.data.VocabBook
import com.baekseok.shvoca.ui.theme.*
import kotlinx.coroutines.launch

private enum class HideMode { NONE, KANJI, MEANING }

private fun maskOf(text: String): String = "●●●"

private fun parseHideMode(value: String): HideMode =
    try { HideMode.valueOf(value) } catch (e: IllegalArgumentException) { HideMode.NONE }

@Composable
fun WordListScreen(
    language: String,
    onBack: () -> Unit,
    onWordSelected: (startIndex: Int, wordIds: List<Int>?) -> Unit,
    onShuffle: (wordIds: List<Int>?) -> Unit,
    onTest: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { KanjiDatabase.getInstance(context.applicationContext) }
    val words by db.kanjiDao().getByLanguage(language).collectAsState(initial = emptyList())
    val book: VocabBook? by db.kanjiDao().getBookByName(language).collectAsState(initial = null)

    var showBookmarkedOnly by remember { mutableStateOf(false) }
    val displayWords = remember(words, showBookmarkedOnly) {
        if (showBookmarkedOnly) words.withIndex().filter { it.value.bookmarked }
        else words.withIndex().toList()
    }

    var hideMode by remember { mutableStateOf(HideMode.NONE) }
    var revealedIds by remember(hideMode) { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(book?.hideMode) {
        book?.let { hideMode = parseHideMode(it.hideMode) }
    }

    fun selectHideMode(mode: HideMode) {
        hideMode = mode
        scope.launch { db.kanjiDao().setHideMode(language, mode.name) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<KanjiWord?>(null) }
    var deleteTarget by remember { mutableStateOf<KanjiWord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 32.dp, bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "뒤로",
                    tint = Ink,
                    modifier = Modifier.size(26.4.dp)
                )
            }
            Text(
                "단어 목록",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            if (book != null) {
                IconButton(onClick = { editMode = !editMode }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "편집 모드",
                        tint = if (editMode) Gold else Ink
                    )
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "단어 추가",
                        tint = Ink
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("${displayWords.size}개의 단어 존재", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))

        if (words.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterTab("전체 보기", selected = hideMode == HideMode.NONE) { selectHideMode(HideMode.NONE) }
                    FilterTab("원문 가리기", selected = hideMode == HideMode.KANJI) { selectHideMode(HideMode.KANJI) }
                    FilterTab("뜻 가리기", selected = hideMode == HideMode.MEANING) { selectHideMode(HideMode.MEANING) }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (showBookmarkedOnly) "★" else "☆",
                    color = if (showBookmarkedOnly) Gold else Muted,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showBookmarkedOnly = !showBookmarkedOnly }
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        if (words.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("단어가 없습니다", color = Muted, fontSize = 14.sp)
            }
        } else if (displayWords.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("북마크한 단어가 없어요.", color = Muted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(displayWords) { pos, (index, word) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onWordSelected(
                                    pos,
                                    if (showBookmarkedOnly) displayWords.map { it.value.id } else null
                                )
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            color = Muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(28.dp)
                        )
                        val kanjiHidden = hideMode == HideMode.KANJI && word.id !in revealedIds
                        val meaningHidden = hideMode == HideMode.MEANING && word.id !in revealedIds

                        Text(
                            if (kanjiHidden) maskOf(word.kanji) else word.kanji,
                            color = if (kanjiHidden) Muted else Ink,
                            fontSize = if (kanjiHidden) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (kanjiHidden) Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { revealedIds = revealedIds + word.id }
                                    else Modifier
                                )
                        )
                        Text(
                            if (meaningHidden) maskOf(word.meaning) else word.meaning,
                            color = Muted,
                            fontSize = if (meaningHidden) 15.sp else 13.sp,
                            fontWeight = if (meaningHidden) FontWeight.Bold else FontWeight.Normal,
                            modifier = if (meaningHidden) Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { revealedIds = revealedIds + word.id }
                            else Modifier
                        )
                        if (editMode) {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = "수정",
                                tint = Ink,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .size(18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { editTarget = word }
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = "삭제",
                                tint = Red,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .size(18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { deleteTarget = word }
                            )
                        } else {
                            Text(
                                if (word.bookmarked) "★" else "☆",
                                color = if (word.bookmarked) Gold else Muted,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .padding(start = 10.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        scope.launch { db.kanjiDao().setBookmarked(word.id, !word.bookmarked) }
                                    }
                            )
                        }
                    }
                    if (pos < displayWords.lastIndex) {
                        HorizontalDivider(color = Line, thickness = 0.5.dp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        onShuffle(if (showBookmarkedOnly) displayWords.map { it.value.id } else null)
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
                ) {
                    Text("셔플 학습하기", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onTest,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Paper)
                ) {
                    Text("테스트", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = CardBg,
            title = {
                Text("단어 삭제", color = Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            },
            text = {
                Text(
                    "'${target.kanji}' 단어를 삭제할까요?",
                    color = Muted,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { db.kanjiDao().deleteWord(target.id) }
                        deleteTarget = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = Paper)
                ) {
                    Text("삭제", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("취소", color = Muted)
                }
            }
        )
    }

    editTarget?.let { target ->
        AddWordDialog(
            languageType = book?.languageType ?: "일본어",
            initialKanji = target.kanji,
            initialFurigana = target.furigana,
            initialMeaning = target.meaning,
            initialVariant = target.variant,
            title = "단어 수정",
            confirmLabel = "수정",
            onDismiss = { editTarget = null },
            onConfirm = { kanji, furigana, meaning, variant ->
                scope.launch {
                    db.kanjiDao().updateWord(target.id, kanji, furigana, meaning, variant)
                }
                editTarget = null
            }
        )
    }

    if (showAddDialog && book != null) {
        AddWordDialog(
            languageType = book!!.languageType,
            onDismiss = { showAddDialog = false },
            onConfirm = { kanji, furigana, meaning, variant ->
                scope.launch {
                    db.kanjiDao().insertAll(
                        listOf(
                            KanjiWord(
                                language = language,
                                kanji = kanji,
                                furigana = furigana,
                                meaning = meaning,
                                variant = variant
                            )
                        )
                    )
                }
                showAddDialog = false
            }
        )
    }

}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Ink.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, if (selected) Ink else Line, RoundedCornerShape(99.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) Ink else Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── 단어 추가 다이얼로그 ────────────────────────────────────────────────────────
// 언어별 입력 항목/순서: 영어(영어·뜻) / 일본어(원문·후리가나·뜻) / 중국어(원문·발음·뜻) / 한자(원문·발음·뜻·간번체)

@Composable
private fun AddWordDialog(
    languageType: String,
    initialKanji: String = "",
    initialFurigana: String = "",
    initialMeaning: String = "",
    initialVariant: String = "",
    title: String = "단어 추가",
    confirmLabel: String = "추가",
    onDismiss: () -> Unit,
    onConfirm: (kanji: String, furigana: String, meaning: String, variant: String) -> Unit
) {
    var original by remember { mutableStateOf(initialKanji) }
    var readings by remember {
        mutableStateOf(
            if (languageType == "일본어")
                initialFurigana.split("|").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("") }
            else listOf(initialFurigana)
        )
    }
    var meaning by remember { mutableStateOf(initialMeaning) }
    var variant by remember { mutableStateOf(initialVariant) }

    val originalLabel = if (languageType == "영어") "영어" else "원문"
    val readingLabel = when (languageType) {
        "일본어" -> "후리가나"
        "중국어", "한자" -> "발음"
        else -> null
    }
    val showVariant = languageType == "한자"
    val isJapanese = languageType == "일본어"

    val readingValid = when {
        readingLabel == null -> true
        isJapanese -> readings.any { it.isNotBlank() }
        else -> readings.first().isNotBlank()
    }
    val canSave = original.isNotBlank() && meaning.isNotBlank() && readingValid &&
        (!showVariant || variant.isNotBlank())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(20.dp))

                LabeledField(originalLabel, original) { original = it }

                if (readingLabel != null) {
                    Spacer(Modifier.height(14.dp))
                    if (isJapanese) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(readingLabel, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "후리가나 추가",
                                tint = Ink,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { readings = readings + "" }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        readings.forEachIndexed { i, value ->
                            if (i > 0) Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = value,
                                    onValueChange = { new -> readings = readings.toMutableList().also { it[i] = new } },
                                    singleLine = true,
                                    placeholder = { Text("입력", color = Muted, fontSize = 14.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Gold,
                                        unfocusedBorderColor = Line,
                                        focusedTextColor = Ink,
                                        unfocusedTextColor = Ink,
                                        cursorColor = Gold,
                                        focusedContainerColor = Paper,
                                        unfocusedContainerColor = Paper
                                    )
                                )
                                if (readings.size > 1) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "×",
                                        color = Muted,
                                        fontSize = 18.sp,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { readings = readings.toMutableList().also { it.removeAt(i) } }
                                    )
                                }
                            }
                        }
                    } else {
                        LabeledField(readingLabel, readings.first()) { new -> readings = listOf(new) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                LabeledField("뜻", meaning) { meaning = it }

                if (showVariant) {
                    Spacer(Modifier.height(14.dp))
                    LabeledField("간체/번체", variant) { variant = it }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("취소", color = Muted)
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            val furigana = readings.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("|")
                            onConfirm(original.trim(), furigana, meaning.trim(), variant.trim())
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
                    ) {
                        Text(confirmLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onChange: (String) -> Unit) {
    Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text("입력", color = Muted, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = Line,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
            cursorColor = Gold,
            focusedContainerColor = Paper,
            unfocusedContainerColor = Paper
        )
    )
}
