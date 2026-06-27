package com.baekseok.shvoca.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.painterResource
import com.baekseok.shvoca.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baekseok.shvoca.data.KanjiDatabase
import com.baekseok.shvoca.data.KanjiWord
import com.baekseok.shvoca.ui.theme.*
import kotlinx.coroutines.launch

private enum class TestMode { Write, MC }
private enum class QType    { MC, Write }

private data class Question(
    val word: KanjiWord,
    val type: QType,
    val choices: List<String> = emptyList()
)

private fun makeMC(word: KanjiWord, allWords: List<KanjiWord>): Question {
    val wrong = allWords.filter { it.id != word.id }
        .shuffled()
        .take(minOf(5, allWords.size - 1))
        .map { it.meaning }
    return Question(word, QType.MC, (wrong + word.meaning).shuffled())
}

private fun buildQuestions(words: List<KanjiWord>, mode: TestMode): List<Question> = when (mode) {
    TestMode.Write -> words.shuffled().map { Question(it, QType.Write) }
    TestMode.MC    -> words.shuffled().map { makeMC(it, words) }
}

private val CorrectGreen = Color(0xFF2A5C45)
private val WrongRed     = Color(0xFF5C2A2A)

// ── 진입점 ────────────────────────────────────────────────────────────────────

@Composable
fun TestScreen(language: String, onBack: () -> Unit) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()
    val db       = remember { KanjiDatabase.getInstance(context.applicationContext) }
    val allWords by db.kanjiDao().getByLanguage(language).collectAsState(initial = emptyList())

    var bookmarkOnly by remember { mutableStateOf(false) }
    val testWords = remember(allWords, bookmarkOnly) {
        if (bookmarkOnly) allWords.filter { it.bookmarked } else allWords
    }

    var selectedMode by remember { mutableStateOf<TestMode?>(null) }
    var questions    by remember { mutableStateOf<List<Question>>(emptyList()) }
    var qIndex       by remember { mutableStateOf(0) }
    var correctCount by remember { mutableStateOf(0) }
    var wrongWords   by remember { mutableStateOf<List<KanjiWord>>(emptyList()) }
    var finished     by remember { mutableStateOf(false) }

    fun resetToModeSelect() {
        selectedMode = null
        questions    = emptyList()
        qIndex       = 0
        correctCount = 0
        wrongWords   = emptyList()
        finished     = false
    }

    val handleBack = { if (selectedMode != null) resetToModeSelect() else onBack() }
    BackHandler { handleBack() }

    LaunchedEffect(selectedMode, testWords.size) {
        val mode = selectedMode ?: return@LaunchedEffect
        if (testWords.size >= 2 && questions.isEmpty()) {
            questions = buildQuestions(testWords, mode)
        }
    }

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
            IconButton(onClick = { handleBack() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "뒤로",
                    tint = Ink,
                    modifier = Modifier.size(26.4.dp)
                )
            }
            Text(
                "테스트",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))

        when {
            selectedMode == null -> ModeSelector(
                bookmarkOnly = bookmarkOnly,
                onToggleBookmark = { bookmarkOnly = !bookmarkOnly },
                onSelect = { mode -> selectedMode = mode }
            )

            testWords.size < 2 ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("단어가 부족합니다.", color = Muted, fontSize = 14.sp)
                }

            questions.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("문제를 생성하는 중...", color = Muted, fontSize = 14.sp)
                }

            finished -> TestResult(
                total      = questions.size,
                correct    = correctCount,
                wrongWords = wrongWords,
                onRetry    = {
                    questions    = buildQuestions(testWords, selectedMode!!)
                    qIndex       = 0
                    correctCount = 0
                    wrongWords   = emptyList()
                    finished     = false
                },
                onBack = onBack
            )

            else -> {
                val q = questions[qIndex]

                val fraction by animateFloatAsState(
                    targetValue   = (qIndex + 1f) / questions.size,
                    animationSpec = tween(300),
                    label         = "testProgress"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f).height(5.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Line)
                    ) {
                        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Gold))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("${qIndex + 1} / ${questions.size}", color = Muted, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))

                key(qIndex) {
                    val advance: (Boolean) -> Unit = { isCorrect ->
                        if (isCorrect) correctCount++ else wrongWords = wrongWords + q.word
                        if (qIndex < questions.size - 1) qIndex++ else finished = true
                    }
                    val bookmarked = allWords.find { it.id == q.word.id }?.bookmarked ?: q.word.bookmarked
                    val toggleBookmark: () -> Unit = {
                        scope.launch { db.kanjiDao().setBookmarked(q.word.id, !bookmarked) }
                    }
                    if (q.type == QType.MC) MCQuestion(q, bookmarked, toggleBookmark, advance)
                    else WriteQuestion(q, bookmarked, toggleBookmark, advance)
                }
            }
        }
    }
}

// ── 모드 선택 ─────────────────────────────────────────────────────────────────

@Composable
private fun ModeSelector(
    bookmarkOnly: Boolean,
    onToggleBookmark: () -> Unit,
    onSelect: (TestMode) -> Unit
) {
    Column {
        Text("학습 방식을 선택하세요.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Row {
            TestFilterTab("북마크", selected = bookmarkOnly, onClick = onToggleBookmark)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard("후리가나 쓰기", "한자를 보고 읽는 법을 직접 입력")  { onSelect(TestMode.Write) }
            ModeCard("뜻 선택하기",  "한자를 보고 뜻을 고르는 6지 선다") { onSelect(TestMode.MC) }
        }
    }
}

@Composable
private fun TestFilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Gold.copy(alpha = 0.18f) else Color.Transparent)
            .border(1.dp, if (selected) Gold else Line, RoundedCornerShape(99.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) Gold else Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ModeCard(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

// ── 객관식 ────────────────────────────────────────────────────────────────────

@Composable
private fun MCQuestion(
    question: Question,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onNext: (Boolean) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    val answered  = selected != null
    val isCorrect = selected == question.word.meaning

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("뜻을 고르세요", color = Muted, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBg),
            contentAlignment = Alignment.Center
        ) {
            Text(question.word.kanji, color = Ink, fontSize = 52.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 36.dp))
            Text(
                if (bookmarked) "★" else "☆",
                color = if (bookmarked) Gold else Muted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleBookmark() }
            )
        }

        Spacer(Modifier.height(16.dp))

        val choices = question.choices
        for (row in 0 until 3) {
            if (row > 0) Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 2) {
                    val idx = row * 2 + col
                    if (idx < choices.size) {
                        val choice       = choices[idx]
                        val isThisRight  = choice == question.word.meaning
                        val isThisPicked = choice == selected
                        val bg = when {
                            !answered    -> CardBg
                            isThisRight  -> CorrectGreen
                            isThisPicked -> WrongRed
                            else         -> CardBg
                        }
                        val textColor = when {
                            !answered                   -> Ink
                            isThisRight || isThisPicked -> Color.White
                            else                        -> Muted
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .border(
                                    1.dp,
                                    when {
                                        answered && isThisRight  -> Color(0xFF4CAF50)
                                        answered && isThisPicked -> Red
                                        else                     -> Line
                                    },
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = !answered) { selected = choice }
                                .padding(vertical = 14.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(choice, color = textColor, fontSize = 13.sp,
                                textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (answered) {
            Spacer(Modifier.height(16.dp))
            Text(
                if (isCorrect) "정답!" else "오답",
                color = if (isCorrect) Color(0xFF4CAF50) else Red,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onNext(isCorrect) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
            ) { Text("다음 →", fontWeight = FontWeight.Bold) }
        }
    }
}

// ── 서답형 ────────────────────────────────────────────────────────────────────

@Composable
private fun WriteQuestion(
    question: Question,
    bookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onNext: (Boolean) -> Unit
) {
    var input    by remember { mutableStateOf("") }
    var answered by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val furiganaList = remember(question.word.furigana) {
        question.word.furigana.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }
    val isCorrect = input.trim() in furiganaList

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text("후리가나를 입력하세요", color = Muted, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardBg),
            contentAlignment = Alignment.Center
        ) {
            Text(question.word.kanji, color = Ink, fontSize = 52.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 36.dp))
            Text(
                if (bookmarked) "★" else "☆",
                color = if (bookmarked) Gold else Muted,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggleBookmark() }
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value         = input,
            onValueChange = { if (!answered) input = it },
            enabled       = !answered,
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = { Text("히라가나로 입력", color = Muted, fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (input.isNotBlank() && !answered) {
                    focusManager.clearFocus(); answered = true
                }
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Gold,
                unfocusedBorderColor    = Line,
                focusedTextColor        = Ink,
                unfocusedTextColor      = Ink,
                disabledTextColor       = Muted,
                cursorColor             = Gold,
                focusedContainerColor   = CardBg,
                unfocusedContainerColor = CardBg,
                disabledContainerColor  = CardBg
            )
        )

        Spacer(Modifier.height(12.dp))

        if (!answered) {
            Button(
                onClick  = { if (input.isNotBlank()) { focusManager.clearFocus(); answered = true } },
                enabled  = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
            ) { Text("확인", fontWeight = FontWeight.Bold) }
        } else {
            Text(
                if (isCorrect) "정답!" else "오답  ·  정답: ${furiganaList.joinToString(" / ")}",
                color = if (isCorrect) Color(0xFF4CAF50) else Red,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = { onNext(isCorrect) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
            ) { Text("다음 →", fontWeight = FontWeight.Bold) }
        }
    }
}

// ── 결과 ──────────────────────────────────────────────────────────────────────

@Composable
private fun TestResult(
    total: Int,
    correct: Int,
    wrongWords: List<KanjiWord>,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val percent = if (total > 0) correct * 100 / total else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("$correct / $total", color = Ink, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                percent >= 90 -> "완벽해요!"
                percent >= 70 -> "잘 했어요!"
                percent >= 50 -> "조금 더 연습해요."
                else          -> "열심히 복습해봐요."
            },
            color = Muted, fontSize = 16.sp
        )

        if (wrongWords.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text("틀린 단어 (${wrongWords.size}개)", color = Ink,
                fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            wrongWords.distinctBy { it.id }.forEach { word ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(word.kanji, color = Ink, fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.width(76.dp))
                    Text(
                        word.furigana.split("|").map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" / "),
                        color = Red, fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(word.meaning, color = Muted, fontSize = 13.sp)
                }
                HorizontalDivider(color = Line, thickness = 0.5.dp)
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick  = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
        ) { Text("다시 테스트하기", fontSize = 15.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick  = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Muted)
        ) { Text("목록으로", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
    }
}
