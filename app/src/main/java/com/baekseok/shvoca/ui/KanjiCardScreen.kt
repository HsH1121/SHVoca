package com.baekseok.shvoca.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baekseok.shvoca.data.KanjiDatabase
import com.baekseok.shvoca.data.KanjiWord
import com.baekseok.shvoca.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val indexOrderSaver = Saver<List<Int>, IntArray>(
    save = { it.toIntArray() },
    restore = { it.toList() }
)

@Composable
fun KanjiCardScreen(language: String, startIndex: Int = 0, shuffled: Boolean = false, onBack: () -> Unit) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { KanjiDatabase.getInstance(context.applicationContext) }
    val baseWords by db.kanjiDao().getByLanguage(language).collectAsState(initial = emptyList())

    var indexOrder by rememberSaveable(stateSaver = indexOrderSaver) {
        mutableStateOf(emptyList<Int>())
    }
    var pos by rememberSaveable { mutableStateOf(startIndex) }
    var flipped by rememberSaveable { mutableStateOf(false) }
    var backPos by remember { mutableStateOf(pos) }
    var pendingFlip by remember { mutableStateOf(false) }

    LaunchedEffect(baseWords.size) {
        if (baseWords.isNotEmpty() && indexOrder.isEmpty()) {
            indexOrder = if (shuffled) List(baseWords.size) { it }.shuffled()
                         else List(baseWords.size) { it }
        }
    }

    LaunchedEffect(pos) {
        if (pendingFlip) {
            delay(500L)
            pendingFlip = false
        }
        backPos = pos
    }

    if (baseWords.isEmpty() || indexOrder.isEmpty()) return

    val frontWord = baseWords[indexOrder[pos]]
    val frontNumber = indexOrder[pos] + 1
    val backWord = baseWords[indexOrder[backPos]]
    val backNumber = indexOrder[backPos] + 1

    fun move(delta: Int) {
        val next = pos + delta
        if (next in indexOrder.indices) {
            pendingFlip = flipped
            flipped = false
            pos = next
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
        Header(language = language, onBack = onBack)

        Spacer(Modifier.height(18.dp))
        ProgressBar(current = pos + 1, total = indexOrder.size)

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(pos) {
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragX < -60) move(1)
                            else if (dragX > 60) move(-1)
                            dragX = 0f
                        },
                        onHorizontalDrag = { _, amount -> dragX += amount }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            FlipCard(
                frontWord = frontWord,
                frontNumber = frontNumber,
                backWord = backWord,
                backNumber = backNumber,
                flipped = flipped,
                onClick = { flipped = !flipped },
                onSaveMemo = { memo ->
                    scope.launch { db.kanjiDao().setMemo(backWord.id, memo) }
                },
                onToggleBookmark = {
                    scope.launch { db.kanjiDao().setBookmarked(frontWord.id, !frontWord.bookmarked) }
                }
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { move(-1) },
                enabled = pos > 0,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("← 이전", color = Ink, fontWeight = FontWeight.Bold) }

            Button(
                onClick = { move(1) },
                enabled = pos < indexOrder.size - 1,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Paper)
            ) { Text("다음 →", fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
        ) {
            Chip(label = "셔플") {
                indexOrder = indexOrder.shuffled()
                pos = 0; backPos = 0; flipped = false
            }
            Chip(label = "원래순서") {
                indexOrder = List(baseWords.size) { it }
                pos = 0; backPos = 0; flipped = false
            }
        }
    }
}

@Composable
private fun Header(language: String, onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "<",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "단어카드",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "카드를 탭하면 뜻이 나와요.",
            color = Muted,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ProgressBar(current: Int, total: Int) {
    val fraction by animateFloatAsState(
        targetValue = current.toFloat() / total,
        animationSpec = tween(300),
        label = "progress"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Line)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Gold, Red)))
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "$current / $total",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FlipCard(
    frontWord: KanjiWord,
    frontNumber: Int,
    backWord: KanjiWord,
    backNumber: Int,
    flipped: Boolean,
    onClick: () -> Unit,
    onSaveMemo: (String) -> Unit,
    onToggleBookmark: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(500),
        label = "flip"
    )
    val showingBack = rotation > 90f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
            }
            .clip(RoundedCornerShape(18.dp))
            .background(CardBg)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!showingBack) {
            Text(
                "No.$frontNumber",
                color = Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
            )
            FrontFace(frontWord.kanji)
            Text(
                "탭하면 정답",
                color = Muted,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            )
            BookmarkButton(
                bookmarked = frontWord.bookmarked,
                onToggle = onToggleBookmark,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No.$backNumber",
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(18.dp)
                )
                BackFace(backWord, onSaveMemo)
                BookmarkButton(
                    bookmarked = backWord.bookmarked,
                    onToggle = onToggleBookmark,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FrontFace(kanji: String) {
    val size = when (kanji.length) {
        1, 2 -> 84.sp
        3 -> 64.sp
        4 -> 52.sp
        else -> 40.sp
    }
    Text(
        kanji,
        color = Ink,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun BackFace(word: KanjiWord, onSaveMemo: (String) -> Unit) {
    var memo by remember(word.id) { mutableStateOf(word.memo) }
    val dirty = memo != word.memo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(word.kanji, color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            if (word.furigana.isNotBlank()) {
                Text(
                    word.furigana,
                    color = Red,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Box(
                Modifier
                    .width(44.dp)
                    .height(2.dp)
                    .background(Line)
            )
            Text(
                word.meaning,
                color = Ink,
                fontSize = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                placeholder = { Text("메모를 입력하세요", color = Muted, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
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
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onSaveMemo(memo) },
                enabled = dirty,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = Paper,
                    disabledContainerColor = Line,
                    disabledContentColor = Muted
                )
            ) {
                Text("메모 저장", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BookmarkButton(bookmarked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        if (bookmarked) "★" else "☆",
        color = if (bookmarked) Gold else Muted,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onToggle() }
    )
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
