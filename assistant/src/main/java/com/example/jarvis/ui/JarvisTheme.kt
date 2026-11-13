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

/**
 * Paleta holográfica de JARVIS AI.
 *
 * Azul eléctrico y cian sobre azul casi negro. Los nombres antiguos
 * (`Green`, `GreenDim`) se conservan como alias para las pantallas que ya
 * existían: así toda la app cambia de identidad sin reescribir cada línea.
 */
object JarvisColors {
    // Fondos
    val Background = Color(0xFF04070F)
    val BackgroundTop = Color(0xFF071322)
    val Panel = Color(0xFF081A2C)
    val PanelHigh = Color(0xFF0B2439)

    // Acentos
    val Primary = Color(0xFF3BA7FF)
    val PrimaryDim = Color(0xFF1B6E9E)
    val Cyan = Color(0xFF22E3F5)
    val Ice = Color(0xFFBDEFFF)

    // Texto
    val Text = Color(0xFFD3ECFA)
    val Muted = Color(0xFF7B9BB3)
    val Outline = Color(0xFF16425F)

    // Avisos
    val Amber = Color(0xFFF2C14E)
    val Red = Color(0xFFFF5D7A)

    /** Alias heredados de la etapa Matrix. */
    val Green = Primary
    val GreenDim = PrimaryDim
}

private val sans = FontFamily.SansSerif
private val mono = FontFamily.Monospace

private val jarvisTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        letterSpacing = 6.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 2.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = sans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.8.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = sans,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = sans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = sans,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = mono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = mono,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = mono,
        fontSize = 9.sp,
        letterSpacing = 1.1.sp,
    ),
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = JarvisColors.Primary,
            onPrimary = JarvisColors.Background,
            secondary = JarvisColors.Cyan,
            onSecondary = JarvisColors.Background,
            tertiary = JarvisColors.Ice,
            background = JarvisColors.Background,
            onBackground = JarvisColors.Text,
            surface = JarvisColors.Panel,
            onSurface = JarvisColors.Text,
            surfaceVariant = JarvisColors.PanelHigh,
            onSurfaceVariant = JarvisColors.Muted,
            outline = JarvisColors.Outline,
            outlineVariant = JarvisColors.Outline,
            error = JarvisColors.Red,
        ),
        typography = jarvisTypography,
        content = content,
    )
}
