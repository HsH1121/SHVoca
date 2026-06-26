package com.baekseok.shvoca.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppFont = FontFamily.Default

val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold,   fontSize = 57.sp),
    displayMedium = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold,   fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold,   fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    headlineMedium= TextStyle(fontFamily = AppFont, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold,   fontSize = 24.sp),
    titleLarge    = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Bold,   fontSize = 22.sp),
    titleMedium   = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall    = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge     = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium    = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge    = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium   = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall    = TextStyle(fontFamily = AppFont, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
