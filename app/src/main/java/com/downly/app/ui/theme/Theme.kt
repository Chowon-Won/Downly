package com.downly.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object DColors {
    val Bg = Color(0xFF0D0B14)
    val Surface = Color(0xFF17141F)
    val SurfaceHigh = Color(0xFF211D2C)
    val Stroke = Color(0xFF2E2940)
    val Text = Color(0xFFF4F2FA)
    val TextDim = Color(0xFF9C97AE)
    val Coral = Color(0xFFFF2E5B)
    val Violet = Color(0xFF6C4DFF)
    val Mint = Color(0xFF2BD9A4)
    val Gold = Color(0xFFFFC14D)
    val Danger = Color(0xFFFF5470)

    val Brand = Brush.linearGradient(listOf(Coral, Violet))
    val Gold2 = Brush.linearGradient(listOf(Color(0xFFFFD36B), Color(0xFFFF8A3D)))
}

private val scheme = darkColorScheme(
    primary = DColors.Coral,
    onPrimary = Color.White,
    secondary = DColors.Violet,
    onSecondary = Color.White,
    tertiary = DColors.Mint,
    background = DColors.Bg,
    onBackground = DColors.Text,
    surface = DColors.Surface,
    onSurface = DColors.Text,
    surfaceVariant = DColors.SurfaceHigh,
    onSurfaceVariant = DColors.TextDim,
    surfaceContainer = DColors.Surface,
    surfaceContainerHigh = DColors.SurfaceHigh,
    outline = DColors.Stroke,
    outlineVariant = DColors.Stroke,
    error = DColors.Danger,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.6.sp),
)

@Composable
fun DownlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
