package com.example.bitacora.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
)

@Composable
fun HandwritingDialog(
    onDismiss: () -> Unit,
    onSave: (Bitmap) -> Unit,
) {
    var strokes by remember { mutableStateOf(listOf<DrawStroke>()) }
    var currentPoints by remember { mutableStateOf(listOf<Offset>()) }
    var currentColor by remember { mutableStateOf(Color(0xFF1C1B1F)) }
    var currentWidth by remember { mutableFloatStateOf(6f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val palette = listOf(
        Color(0xFF1C1B1F),
        Color(0xFF1565C0),
        Color(0xFFC62828),
        Color(0xFF2E7D32),
        Color(0xFF6A1B9A),
    )
    val widths = listOf(4f, 8f, 14f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFDFBF7),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancelar", tint = Color(0xFF1C1B1F))
                    }
                    Text(
                        text = "Escribir a mano",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1C1B1F),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { if (strokes.isNotEmpty()) strokes = strokes.dropLast(1) },
                        enabled = strokes.isNotEmpty(),
                    ) {
                        Icon(Icons.Rounded.Undo, contentDescription = "Deshacer", tint = Color(0xFF1C1B1F))
                    }
                    TextButton(onClick = { strokes = emptyList(); currentPoints = emptyList() }) {
                        Text("Limpiar", color = Color(0xFF6B4EFF))
                    }
                    FilledIconButton(
                        onClick = {
                            val all = if (currentPoints.size > 1) {
                                strokes + DrawStroke(currentPoints, currentColor, currentWidth)
                            } else strokes
                            if (all.isNotEmpty() && canvasSize.width > 0 && canvasSize.height > 0) {
                                onSave(renderBitmap(all, canvasSize))
                            }
                        },
                        enabled = strokes.isNotEmpty() || currentPoints.size > 1,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Guardar")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    palette.forEach { c ->
                        val selected = c == currentColor
                        Box(
                            modifier = Modifier
                                .size(if (selected) 32.dp else 26.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (selected) Modifier.border(2.dp, Color(0xFF6B4EFF), CircleShape)
                                    else Modifier
                                )
                                .clickable { currentColor = c },
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                    widths.forEach { w ->
                        val selected = w == currentWidth
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .then(
                                    if (selected) Modifier.background(Color(0x1A6B4EFF))
                                    else Modifier
                                )
                                .clickable { currentWidth = w },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((w + 6).dp.coerceAtMost(24.dp))
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C1B1F)),
                            )
                        }
                    }
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(currentColor, currentWidth) {
                            detectDragGestures(
                                onDragStart = { offset -> currentPoints = listOf(offset) },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints = currentPoints + change.position
                                },
                                onDragEnd = {
                                    if (currentPoints.size > 1) {
                                        strokes = strokes + DrawStroke(currentPoints, currentColor, currentWidth)
                                    }
                                    currentPoints = emptyList()
                                },
                                onDragCancel = { currentPoints = emptyList() },
                            )
                        },
                ) {
                    strokes.forEach { stroke ->
                        drawStrokePath(stroke.points, stroke.color, stroke.width)
                    }
                    if (currentPoints.size > 1) {
                        drawStrokePath(currentPoints, currentColor, currentWidth)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStrokePath(
    points: List<Offset>,
    color: Color,
    width: Float,
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            quadraticTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
        }
        val last = points.last()
        lineTo(last.x, last.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun renderBitmap(strokes: List<DrawStroke>, size: IntSize): Bitmap {
    val w = size.width.coerceAtLeast(1)
    val h = size.height.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    strokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = stroke.width
            color = stroke.color.toArgb()
        }
        val path = android.graphics.Path().apply {
            val pts = stroke.points
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]
                val curr = pts[i]
                quadTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
            }
            val last = pts.last()
            lineTo(last.x, last.y)
        }
        canvas.drawPath(path, paint)
    }
    return bitmap
}
