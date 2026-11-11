package com.example.jarvis.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private class RainColumn(val seed: Int) {
    var y = 0f
    val speed: Float
    val length: Int

    init {
        val random = Random(seed)
        speed = 28f + random.nextFloat() * 46f
        length = 6 + random.nextInt(8)
        y = random.nextFloat() * 1200f
    }
}

/**
 * Lluvia de caracteres estilo Matrix dibujada con el canvas nativo de Android.
 */
@Composable
fun MatrixRain(modifier: Modifier = Modifier, brightness: Float = 1f) {
    val density = LocalDensity.current
    val textSizePx = with(density) { 16.sp.toPx() }
    val cellHeight = textSizePx * 1.3f
    val cellWidth = textSizePx * 0.78f

    val glyphs = remember {
        listOf(
            "ｱ", "ｲ", "ｳ", "ｴ", "ｵ", "ｶ", "ｷ", "ｸ", "ｹ", "ｺ", "ｻ", "ｼ", "ｽ", "ｾ", "ｿ",
            "ﾀ", "ﾁ", "ﾂ", "ﾃ", "ﾄ", "ﾅ", "ﾆ", "ﾇ", "ﾈ", "ﾉ", "ﾊ", "ﾋ", "ﾌ", "ﾍ", "ﾎ",
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F",
        )
    }

    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.LEFT
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val columns = remember { mutableStateListOf<RainColumn>() }
    var frameNanos by remember { mutableStateOf(0L) }

    LaunchedEffect(canvasSize.width, canvasSize.height) {
        if (canvasSize.width > 0) {
            columns.clear()
            val count = (canvasSize.width / cellWidth).toInt().coerceIn(10, 40)
            for (i in 0 until count) {
                columns.add(RainColumn(i * 7919 + 13))
            }
        }
    }

    LaunchedEffect(Unit) {
        // 24 fps en lugar de 60: deja el hilo principal libre para el audio.
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (now - last >= 42_000_000L) {
                    last = now
                    frameNanos = now
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
    ) {
        paint.textSize = textSizePx
        val seconds = frameNanos / 1_000_000_000f
        val totalHeight = size.height + cellHeight * 30f
        columns.forEachIndexed { index, column ->
            val y = (column.y + seconds * column.speed) % totalHeight
            for (i in column.length downTo 0) {
                val charY = y - i * cellHeight
                if (charY < -cellHeight || charY > size.height) continue
                val alpha = ((1f - i.toFloat() / column.length) * 200f * brightness)
                    .toInt()
                    .coerceIn(0, 255)
                paint.color = android.graphics.Color.argb(alpha, 0, 255, 65)
                val glyph = glyphs[(index + i + (frameNanos / 120_000_000L).toInt()) % glyphs.size]
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        glyph,
                        index * cellWidth + 2f,
                        charY,
                        paint,
                    )
                }
            }
        }
    }
}
