package com.example.jarvis.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.example.jarvis.ui.JarvisColors

/**
 * Fondo holográfico: degradado profundo, rejilla tenue y un halo cian.
 *
 * Es estático a propósito: la animación se concentra en el núcleo, así el
 * teléfono no gasta batería repintando toda la pantalla a 60 fps.
 */
@Composable
fun HoloBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    JarvisColors.BackgroundTop,
                    JarvisColors.Background,
                    Color(0xFF02040A),
                ),
            ),
        )

        // Halo superior: da profundidad detrás del núcleo.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    JarvisColors.Primary.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                center = Offset(size.width * 0.5f, size.height * 0.30f),
                radius = size.width * 0.95f,
            ),
            radius = size.width * 0.95f,
            center = Offset(size.width * 0.5f, size.height * 0.30f),
        )

        drawGrid()

        // Línea de horizonte muy sutil.
        drawLine(
            color = JarvisColors.Outline.copy(alpha = 0.35f),
            start = Offset(0f, size.height * 0.62f),
            end = Offset(size.width, size.height * 0.62f),
            strokeWidth = 1f,
        )
    }
}

private fun DrawScope.drawGrid() {
    val step = 64.dp.toPx()
    val line = JarvisColors.Outline.copy(alpha = 0.16f)

    var x = 0f
    while (x <= size.width) {
        drawLine(color = line, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
        x += step
    }

    var y = 0f
    while (y <= size.height) {
        drawLine(color = line, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}
