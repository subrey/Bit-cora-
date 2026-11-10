package com.example.bitacora.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B4EFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E1FF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF9A5B00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE2B8),
    onSecondaryContainer = Color(0xFF311A00),
    tertiary = Color(0xFF1E6B4A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC4F1DA),
    onTertiaryContainer = Color(0xFF002114),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0EBE3),
    onSurfaceVariant = Color(0xFF5A554E),
    outline = Color(0xFF7C766D),
    outlineVariant = Color(0xFFDDD6CB),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC9BFFF),
    onPrimary = Color(0xFF2B1B6B),
    primaryContainer = Color(0xFF432E8F),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFFF5C06A),
    onSecondary = Color(0xFF3F2A00),
    secondaryContainer = Color(0xFF5C4100),
    onSecondaryContainer = Color(0xFFFFE2B8),
    tertiary = Color(0xFF8ED9B6),
    onTertiary = Color(0xFF003825),
    tertiaryContainer = Color(0xFF0A5138),
    onTertiaryContainer = Color(0xFFC4F1DA),
    background = Color(0xFF121014),
    onBackground = Color(0xFFE7E1E9),
    surface = Color(0xFF1C1A20),
    onSurface = Color(0xFFE7E1E9),
    surfaceVariant = Color(0xFF2A2730),
    onSurfaceVariant = Color(0xFFCBC5CF),
    outline = Color(0xFF958F99),
    outlineVariant = Color(0xFF3A3640),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun BitacoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BitacoraTypography,
        content = content,
    )
}
