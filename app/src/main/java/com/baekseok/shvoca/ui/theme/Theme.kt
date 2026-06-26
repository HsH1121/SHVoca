package com.baekseok.shvoca.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SHVOCATheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Paper,
            surface = CardBg,
            onBackground = Ink,
            onSurface = Ink,
            outline = Line
        ),
        typography = AppTypography,
        content = content
    )
}
