package com.example.jarvis.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object JarvisColors {
    val Green = Color(0xFF00FF41)
    val GreenDim = Color(0xFF00B020)
    val Cyan = Color(0xFF00E5FF)
    val Amber = Color(0xFFFFC107)
    val Red = Color(0xFFFF5252)
    val Background = Color(0xFF000000)
    val Panel = Color(0xFF03150A)
    val Outline = Color(0xFF124A26)
    val Muted = Color(0xFF7CFF9E)
}

private val mono = FontFamily.Monospace

private val jarvisTypography = Typography(
    bodyLarge = TextStyle(fontFamily = mono, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = mono, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = mono, fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = mono, fontSize = 13.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontFamily = mono, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = mono, fontSize = 10.sp),
    titleLarge = TextStyle(fontFamily = mono, fontSize = 18.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = mono, fontSize = 15.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontFamily = mono, fontSize = 13.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = JarvisColors.Green,
            onPrimary = Color.Black,
            secondary = JarvisColors.Cyan,
            onSecondary = Color.Black,
            background = JarvisColors.Background,
            onBackground = JarvisColors.Green,
            surface = JarvisColors.Panel,
            onSurface = JarvisColors.Green,
            surfaceVariant = JarvisColors.Panel,
            onSurfaceVariant = JarvisColors.Muted,
            outline = JarvisColors.Outline,
            outlineVariant = JarvisColors.Outline,
            error = JarvisColors.Red,
        ),
        typography = jarvisTypography,
        content = content,
    )
}
