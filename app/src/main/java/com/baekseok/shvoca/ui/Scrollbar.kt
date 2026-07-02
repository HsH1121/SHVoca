package com.baekseok.shvoca.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baekseok.shvoca.ui.theme.Line

// 단어장 / 단어 목록 / 단어 확인 화면에서 공통으로 쓰는 오른쪽 끝 스크롤바.
fun Modifier.verticalScrollbar(
    state: LazyListState,
    width: Dp = 3.dp,
    color: Color = Line,
    minThumbHeight: Dp = 32.dp
): Modifier = drawWithContent {
    drawContent()

    val layoutInfo = state.layoutInfo
    val totalItemsCount = layoutInfo.totalItemsCount
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    if (totalItemsCount == 0 || visibleItemsInfo.isEmpty()) return@drawWithContent

    val avgItemHeight = visibleItemsInfo.sumOf { it.size } / visibleItemsInfo.size.toFloat()
    val estimatedTotalHeight = avgItemHeight * totalItemsCount
    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
    if (estimatedTotalHeight <= viewportHeight) return@drawWithContent

    val first = visibleItemsInfo.first()
    val scrollOffset = first.index * avgItemHeight - first.offset
    val scrollFraction = (scrollOffset / (estimatedTotalHeight - viewportHeight)).coerceIn(0f, 1f)

    val thumbHeight = ((viewportHeight / estimatedTotalHeight) * size.height)
        .coerceAtLeast(minThumbHeight.toPx())
        .coerceAtMost(size.height)
    val thumbOffsetY = scrollFraction * (size.height - thumbHeight)

    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - width.toPx(), thumbOffsetY),
        size = Size(width.toPx(), thumbHeight),
        cornerRadius = CornerRadius(width.toPx() / 2f, width.toPx() / 2f)
    )
}
