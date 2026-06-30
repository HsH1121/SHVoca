package com.baekseok.shvoca.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.painterResource
import com.baekseok.shvoca.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
fun KanjiCardScreen(language: String, startIndex: Int = 0, shuffled: Boolean = false, wordIds: List<Int>? = null, onBack: () -> Unit) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val db = remember { KanjiDatabase.getInstance(context.applicationContext) }
    val allWords by db.kanjiDao().getByLanguage(language).collectAsState(initial = emptyList())
    val baseWords = remember(allWords, wordIds) {
        if (wordIds == null) allWords
        else wordIds.mapNotNull { id -> allWords.find { it.id == id } }
    }

    var indexOrder by rememberSaveable(stateSaver = indexOrderSaver) {
        mutableStateOf(emptyList<Int>())
    }
    var pos by rememberSaveable { mutableStateOf(startIndex) }
    var flipped by rememberSaveable { mutableStateOf(false) }
    var cardPos by remember { mutableStateOf(pos) }
    val cardOffset = remember { Animatable(0f) }
    var prevCardPos by remember { mutableStateOf(pos) }
    val prevCardOffset = remember { Animatable(0f) }
    var showPrev by remember { mutableStateOf(false) }
    var pendingFlip by remember { mutableStateOf(false) }
    var moveDirection by remember { mutableStateOf(1) }
    var autoPlay by remember { mutableStateOf(false) }
    var intervalIndex by remember { mutableStateOf(3) }
    var speedIndex by remember { mutableStateOf(1) }
    val intervalValues = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f)
    val speedValues    = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f, 3.5f, 4.0f, 4.5f, 5.0f)

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
        val dir = moveDirection
        prevCardPos = cardPos
        prevCardOffset.snapTo(0f)
        showPrev = true
        cardOffset.snapTo(dir * screenWidthPx)
        cardPos = pos
        launch { prevCardOffset.animateTo(-dir * screenWidthPx, tween(300)) }
        cardOffset.animateTo(0f, tween(300))
        showPrev = false
    }

    LaunchedEffect(autoPlay, pos, intervalIndex) {
        if (!autoPlay) return@LaunchedEffect
        val d = (intervalValues[intervalIndex] * 1000).toLong()
        delay(d)
        flipped = true
        delay(d)
        if (pos < indexOrder.size - 1) {
            moveDirection = 1
            pendingFlip = true
            flipped = false
            pos += 1
        } else {
            autoPlay = false
            flipped = false
        }
    }

    if (baseWords.isEmpty() || indexOrder.isEmpty()) return

    fun move(delta: Int) {
        val next = pos + delta
        if (next in indexOrder.indices) {
            moveDirection = delta
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
            .padding(top = 32.dp, bottom = 36.dp)
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
            if (showPrev) {
                val prevWord = baseWords[indexOrder[prevCardPos]]
                val prevNumber = indexOrder[prevCardPos] + 1
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset(prevCardOffset.value.roundToInt(), 0) }
                ) {
                    FlipCard(
                        frontWord = prevWord,
                        frontNumber = prevNumber,
                        backWord = prevWord,
                        backNumber = prevNumber,
                        flipped = false,
                        onClick = {},
                        onSaveMemo = {},
                        onToggleBookmark = {}
                    )
                }
            }
            val word = baseWords[indexOrder[cardPos]]
            val number = indexOrder[cardPos] + 1
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(cardOffset.value.roundToInt(), 0) }
            ) {
                FlipCard(
                    frontWord = word,
                    frontNumber = number,
                    backWord = word,
                    backNumber = number,
                    flipped = flipped,
                    onClick = { flipped = !flipped },
                    onSaveMemo = { memo ->
                        scope.launch { db.kanjiDao().setMemo(word.id, memo) }
                    },
                    onToggleBookmark = {
                        scope.launch { db.kanjiDao().setBookmarked(word.id, !word.bookmarked) }
                    }
                )
            }
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 간격
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { intervalIndex = (intervalIndex + 1) % intervalValues.size },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("간격 ", color = Muted, fontSize = 13.sp)
                Text(
                    intervalValues[intervalIndex].let { if (it % 1f == 0f) "${it.toInt()}초" else "${it}초" },
                    color = Ink, fontSize = 13.sp
                )
            }

            // 재생/일시정지 버튼
            val spinTransition = rememberInfiniteTransition(label = "spin")
            val spinAngle by spinTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "spinAngle"
            )
            Box(modifier = Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(70.dp)) {
                    if (autoPlay) {
                        val stroke = 3.5.dp.toPx()
                        val inset = stroke / 2
                        rotate(spinAngle - 90f) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Transparent,
                                        0.30f to Gold.copy(alpha = 0.45f),
                                        0.68f to Gold,
                                        0.86f to Red,
                                        0.93f to Color.White.copy(alpha = 0.92f),
                                        1.00f to Color.Transparent
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 320f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Ink)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!autoPlay) flipped = false
                            autoPlay = !autoPlay
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (autoPlay) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = if (autoPlay) "일시 정지" else "재생",
                        tint = Paper,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 배율
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { speedIndex = (speedIndex + 1) % speedValues.size },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("배율 ", color = Muted, fontSize = 13.sp)
                Text(
                    speedValues[speedIndex].let { if (it % 1f == 0f) "x${it.toInt()}" else "x${it}" },
                    color = Ink, fontSize = 13.sp
                )
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
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "뒤로",
                    tint = Ink,
                    modifier = Modifier.size(26.4.dp)
                )
            }
            Text(
                "단어카드",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
        }
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
        Canvas(modifier = Modifier.weight(1f).height(6.dp)) {
            val r = size.height / 2f
            drawRoundRect(color = Line, size = size, cornerRadius = CornerRadius(r))
            if (fraction > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Gold, Red), endX = size.width),
                    size = Size(size.width * fraction, size.height),
                    cornerRadius = CornerRadius(r)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text("$current / $total", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                "탭하여 확인",
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
