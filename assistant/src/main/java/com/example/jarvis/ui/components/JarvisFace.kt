package com.example.jarvis.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.jarvis.domain.model.AssistantState
import com.example.jarvis.ui.JarvisColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rostro androide de JARVIS, estilo holográfico/metálico.
 * Más detallado: contorno de cabeza, ojos con iris, nariz, boca/visor animado,
 * paneles faciales y reflejos que reaccionan al estado del asistente.
 */
@Composable
fun JarvisFace(
    state: AssistantState,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "jarvis-face")

    val breathe by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respirar",
    )
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "parpadeo",
    )
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
        ),
        label = "scan",
    )
    val mouthWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
        ),
        label = "boca",
    )
    val ringRot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
        ),
        label = "anillo",
    )

    val accent by animateColorAsState(
        targetValue = when (state) {
            AssistantState.LISTENING -> JarvisColors.Cyan
            AssistantState.THINKING -> JarvisColors.Primary
            AssistantState.SPEAKING -> JarvisColors.Ice
            AssistantState.ERROR -> JarvisColors.Red
            AssistantState.IDLE -> JarvisColors.Primary
        },
        animationSpec = tween(500),
        label = "accent",
    )
    val skinBase = Color(0xFF0B1C2A)
    val skinLight = Color(0xFF163247)
    val metal = Color(0xFF6FAEC8)

    Canvas(modifier.aspectRatio(1f)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val faceW = w * 0.52f * breathe
        val faceH = h * 0.70f * breathe
        val center = Offset(cx, cy)

        // Halo de fondo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.20f),
                    accent.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                center = center,
                radius = w * 0.48f,
            ),
            radius = w * 0.48f,
            center = center,
        )

        // Anillos HUD giratorios
        rotate(ringRot, pivot = center) {
            drawArc(
                color = accent.copy(alpha = 0.35f),
                startAngle = -30f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.44f, cy - h * 0.44f),
                size = Size(w * 0.88f, h * 0.88f),
                style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = accent.copy(alpha = 0.20f),
                startAngle = 150f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.44f, cy - h * 0.44f),
                size = Size(w * 0.88f, h * 0.88f),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        rotate(-ringRot * 0.7f, pivot = center) {
            drawArc(
                color = metal.copy(alpha = 0.18f),
                startAngle = 60f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(cx - w * 0.40f, cy - h * 0.40f),
                size = Size(w * 0.80f, h * 0.80f),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        val head = headPath(cx, cy, faceW, faceH)

        // Sombra/base de cabeza
        drawPath(
            path = head,
            brush = Brush.verticalGradient(
                colors = listOf(skinLight, skinBase, Color(0xFF07131D)),
                startY = cy - faceH / 2f,
                endY = cy + faceH / 2f,
            ),
        )
        // Borde metálico
        drawPath(
            path = head,
            color = accent.copy(alpha = 0.55f),
            style = Stroke(2.dp.toPx(), join = StrokeJoin.Round),
        )
        // Reflejo lateral
        drawPath(
            path = head,
            brush = Brush.horizontalGradient(
                colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Transparent),
                startX = cx - faceW / 2f,
                endX = cx + faceW / 2f,
            ),
        )

        // Paneles faciales internos
        clipPath(head) {
            // Línea central
            drawLine(
                color = accent.copy(alpha = 0.12f),
                start = Offset(cx, cy - faceH * 0.42f),
                end = Offset(cx, cy + faceH * 0.48f),
                strokeWidth = 1.dp.toPx(),
            )
            // Mejillas/paneles
            drawPanelLine(cx - faceW * 0.18f, cy - faceH * 0.05f, cx - faceW * 0.42f, cy + faceH * 0.10f, accent)
            drawPanelLine(cx + faceW * 0.18f, cy - faceH * 0.05f, cx + faceW * 0.42f, cy + faceH * 0.10f, accent)
            drawPanelLine(cx - faceW * 0.20f, cy + faceH * 0.18f, cx - faceW * 0.40f, cy + faceH * 0.30f, accent)
            drawPanelLine(cx + faceW * 0.20f, cy + faceH * 0.18f, cx + faceW * 0.40f, cy + faceH * 0.30f, accent)
            // Mandíbula
            drawLine(
                color = accent.copy(alpha = 0.10f),
                start = Offset(cx - faceW * 0.22f, cy + faceH * 0.28f),
                end = Offset(cx + faceW * 0.22f, cy + faceH * 0.28f),
                strokeWidth = 1.dp.toPx(),
            )

            // Barrido de escaneo
            val scanY = cy - faceH * 0.5f + faceH * scan
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        accent.copy(alpha = 0.10f),
                        accent.copy(alpha = 0.02f),
                        Color.Transparent,
                    ),
                    startY = scanY - faceH * 0.08f,
                    endY = scanY + faceH * 0.08f,
                ),
                topLeft = Offset(cx - faceW, scanY - faceH * 0.08f),
                size = Size(faceW * 2f, faceH * 0.16f),
            )
        }

        // Cejas / visor superior
        val browY = cy - faceH * 0.18f
        val browW = faceW * 0.28f
        drawLine(
            color = accent.copy(alpha = 0.45f),
            start = Offset(cx - faceW * 0.34f, browY - faceH * 0.03f),
            end = Offset(cx - faceW * 0.06f, browY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = accent.copy(alpha = 0.45f),
            start = Offset(cx + faceW * 0.34f, browY - faceH * 0.03f),
            end = Offset(cx + faceW * 0.06f, browY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Ojos
        val eyeY = cy - faceH * 0.08f
        val eyeOffsetX = faceW * 0.20f
        val eyeRx = faceW * 0.115f
        val eyeRy = faceH * 0.038f
        val eyeOpen = when (state) {
            AssistantState.IDLE -> 0.55f + 0.45f * blink
            AssistantState.LISTENING -> 0.75f + 0.25f * blink
            AssistantState.THINKING -> 0.85f
            AssistantState.SPEAKING -> 0.9f
            AssistantState.ERROR -> 1f
        }
        drawEye(cx - eyeOffsetX, eyeY, eyeRx, eyeRy, eyeOpen, accent, state)
        drawEye(cx + eyeOffsetX, eyeY, eyeRx, eyeRy, eyeOpen, accent, state)

        // Nariz (puente y punta sutil)
        val noseTop = cy - faceH * 0.02f
        val noseBottom = cy + faceH * 0.14f
        drawLine(
            color = metal.copy(alpha = 0.22f),
            start = Offset(cx, noseTop),
            end = Offset(cx - faceW * 0.03f, noseBottom),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = metal.copy(alpha = 0.18f),
            start = Offset(cx - faceW * 0.03f, noseBottom),
            end = Offset(cx + faceW * 0.04f, noseBottom + faceH * 0.01f),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
        // Fosas sutiles
        drawCircle(
            color = Color.Black.copy(alpha = 0.25f),
            radius = faceW * 0.012f,
            center = Offset(cx - faceW * 0.035f, noseBottom + faceH * 0.015f),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.20f),
            radius = faceW * 0.010f,
            center = Offset(cx + faceW * 0.015f, noseBottom + faceH * 0.018f),
        )

        // Boca / emisor de voz
        val mouthY = cy + faceH * 0.30f
        val mouthW = faceW * 0.26f
        drawMouth(cx, mouthY, mouthW, faceH, accent, state, mouthWave, blink)

        // Detalles "oreja"/laterales
        drawSideDetail(cx - faceW * 0.50f, cy - faceH * 0.02f, faceW, faceH, accent, left = true)
        drawSideDetail(cx + faceW * 0.50f, cy - faceH * 0.02f, faceW, faceH, accent, left = false)
    }
}

private fun headPath(cx: Float, cy: Float, w: Float, h: Float): Path {
    val left = cx - w / 2f
    val right = cx + w / 2f
    val top = cy - h / 2f
    val bottom = cy + h / 2f
    return Path().apply {
        moveTo(cx, top)
        // cráneo superior
        cubicTo(cx + w * 0.28f, top, right, top + h * 0.12f, right, cy - h * 0.05f)
        // mejilla derecha
        cubicTo(right, cy + h * 0.18f, right - w * 0.06f, cy + h * 0.36f, cx + w * 0.28f, bottom)
        // barbilla
        quadraticBezierTo(cx, bottom + h * 0.03f, cx - w * 0.28f, bottom)
        // mejilla izquierda
        cubicTo(left + w * 0.06f, cy + h * 0.36f, left, cy + h * 0.18f, left, cy - h * 0.05f)
        // cráneo superior izquierdo
        cubicTo(left, top + h * 0.12f, cx - w * 0.28f, top, cx, top)
        close()
    }
}

private fun DrawScope.drawPanelLine(x1: Float, y1: Float, x2: Float, y2: Float, accent: Color) {
    drawLine(
        color = accent.copy(alpha = 0.10f),
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawEye(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    open: Float,
    accent: Color,
    state: AssistantState,
) {
    val eyeH = (ry * 2f * open).coerceAtLeast(ry * 0.25f)
    val topLeft = Offset(cx - rx, cy - eyeH / 2f)
    val eyeSize = Size(rx * 2f, eyeH)
    val corner = CornerRadius(rx, rx)

    // Cuenca/glow
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(cx, cy),
            radius = rx * 1.6f,
        ),
        topLeft = Offset(cx - rx * 1.6f, cy - ry * 1.8f),
        size = Size(rx * 3.2f, ry * 3.6f),
        cornerRadius = CornerRadius(rx, rx),
    )

    // Blanco/esclerótica oscura metálica
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF102636), Color(0xFF08141E)),
            startY = topLeft.y,
            endY = topLeft.y + eyeH,
        ),
        topLeft = topLeft,
        size = eyeSize,
        cornerRadius = corner,
    )
    // Borde ojo
    drawRoundRect(
        color = accent.copy(alpha = 0.55f),
        topLeft = topLeft,
        size = eyeSize,
        cornerRadius = corner,
        style = Stroke(1.2.dp.toPx()),
    )

    if (open > 0.25f) {
        // Iris
        val irisR = ry * 0.85f * open.coerceAtMost(1f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    accent,
                    accent.copy(alpha = 0.35f),
                ),
                center = Offset(cx, cy),
                radius = irisR,
            ),
            radius = irisR,
            center = Offset(cx, cy),
        )
        // Pupila
        val pupilR = irisR * (if (state == AssistantState.LISTENING) 0.55f else 0.42f)
        drawCircle(color = Color(0xFF02070B), radius = pupilR, center = Offset(cx, cy))
        // Brillo
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = irisR * 0.22f,
            center = Offset(cx - irisR * 0.25f, cy - irisR * 0.28f),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = irisR * 0.10f,
            center = Offset(cx + irisR * 0.18f, cy + irisR * 0.15f),
        )
    }
}

private fun DrawScope.drawMouth(
    cx: Float,
    cy: Float,
    mouthW: Float,
    faceH: Float,
    accent: Color,
    state: AssistantState,
    wave: Float,
    blink: Float,
) {
    val baseH = faceH * 0.012f
    val amp = when (state) {
        AssistantState.SPEAKING -> 0.6f + 0.4f * (sin(wave.toDouble()).toFloat() * 0.5f + 0.5f)
        AssistantState.LISTENING -> 0.20f + 0.15f * blink
        AssistantState.THINKING -> 0.15f
        else -> 0.08f
    }

    // Ranura de boca / labios metálicos
    val h = baseH + faceH * 0.030f * amp
    drawRoundRect(
        color = Color(0xFF050D14),
        topLeft = Offset(cx - mouthW, cy - h),
        size = Size(mouthW * 2f, h * 2f),
        cornerRadius = CornerRadius(h, h),
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.35f),
        topLeft = Offset(cx - mouthW, cy - h),
        size = Size(mouthW * 2f, h * 2f),
        cornerRadius = CornerRadius(h, h),
        style = Stroke(1.dp.toPx()),
    )

    // Barras internas (voz)
    val bars = 7
    val gap = mouthW * 1.7f / bars
    for (i in 0 until bars) {
        val bx = cx - mouthW * 0.85f + i * gap + gap * 0.15f
        val bw = gap * 0.55f
        val phase = wave + i * 0.7f
        val bhFactor = when (state) {
            AssistantState.SPEAKING -> 0.25f + 0.75f * (sin(phase.toDouble()).toFloat() * 0.5f + 0.5f)
            AssistantState.LISTENING -> 0.2f + 0.2f * blink
            else -> 0.12f
        }
        val bh = (h * 1.4f * bhFactor).coerceAtLeast(1.dp.toPx())
        drawRoundRect(
            color = accent.copy(alpha = 0.35f + 0.55f * bhFactor),
            topLeft = Offset(bx, cy - bh / 2f),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(bw / 2f, bw / 2f),
        )
    }
}

private fun DrawScope.drawSideDetail(
    x: Float,
    y: Float,
    faceW: Float,
    faceH: Float,
    accent: Color,
    left: Boolean,
) {
    val dir = if (left) -1f else 1f
    val w = faceW * 0.05f
    val h = faceH * 0.18f
    drawRoundRect(
        color = accent.copy(alpha = 0.12f),
        topLeft = Offset(if (left) x - w else x, y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(w / 2f, w / 2f),
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.30f),
        topLeft = Offset(if (left) x - w else x, y - h / 2f),
        size = Size(w, h),
        cornerRadius = CornerRadius(w / 2f, w / 2f),
        style = Stroke(1.dp.toPx()),
    )
    // pequeño indicador
    drawCircle(
        color = accent.copy(alpha = 0.55f),
        radius = w * 0.18f,
        center = Offset(x + dir * w * 0.5f, y - h * 0.15f),
    )
}
