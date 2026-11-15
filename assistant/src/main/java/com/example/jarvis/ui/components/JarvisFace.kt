package com.example.jarvis.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.dp
import com.example.jarvis.domain.model.AssistantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Núcleo holográfico estilo JARVIS (sin cabeza): reactor central, anillos HUD
 * y efectos de holograma que reaccionan al estado y al habla.
 */
@Composable
fun JarvisFace(
    state: AssistantState,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "jarvis-holo")

    val breathe by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respirar",
    )
    val reactorPulse by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "reactor",
    )
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
        ),
        label = "scan",
    )
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
        ),
        label = "orbit",
    )
    val speakWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = LinearEasing),
        ),
        label = "habla",
    )

    // Intensidad según estado
    val active = when (state) {
        AssistantState.LISTENING -> 1.05f
        AssistantState.THINKING -> 1.18f
        AssistantState.SPEAKING -> 1.25f
        AssistantState.ERROR -> 0.85f
        AssistantState.IDLE -> 0.95f
    }

    // Paleta holográfica (cian/blanco con acentos cálidos)
    val cyan = Color(0xFF37D7FF)
    val cyanBright = Color(0xFF9CF3FF)
    val blue = Color(0xFF1A6FA3)
    val white = Color(0xFFF4FCFF)
    val gold = Color(0xFFF2C94C)
    val steel = Color(0xFF86A6B8)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.50f
        val R = minOf(w, h) * 0.36f * breathe

        val activeGlow = when (state) {
            AssistantState.THINKING, AssistantState.SPEAKING -> 0.95f
            AssistantState.LISTENING -> 0.75f
            AssistantState.ERROR -> 0.40f
            else -> 0.58f
        }
        val wave = (sin(speakWave.toDouble()).toFloat() * 0.5f + 0.5f)
        val speakAmp = when (state) {
            AssistantState.SPEAKING -> 0.60f + 0.40f * wave
            AssistantState.LISTENING -> 0.30f + 0.20f * wave
            AssistantState.THINKING -> 0.28f
            else -> 0.14f
        }

        // Halo holográfico general (más intenso)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    cyanBright.copy(alpha = 0.28f * activeGlow),
                    cyan.copy(alpha = 0.16f * activeGlow),
                    blue.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = R * 2.8f,
            ),
            radius = R * 2.8f,
            center = Offset(cx, cy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(white.copy(alpha = 0.10f * activeGlow), Color.Transparent),
                center = Offset(cx, cy),
                radius = R * 1.4f,
            ),
            radius = R * 1.4f,
            center = Offset(cx, cy),
        )

        // Rejilla/proyección holográfica
        val gridAlpha = 0.10f + 0.08f * activeGlow
        val gridStep = R * 0.18f
        var gx = cx - R * 1.7f
        while (gx <= cx + R * 1.7f) {
            drawLine(cyan.copy(alpha = gridAlpha), Offset(gx, cy - R * 1.6f), Offset(gx, cy + R * 1.6f), 1.dp.toPx())
            gx += gridStep
        }
        var gy = cy - R * 1.6f
        while (gy <= cy + R * 1.6f) {
            drawLine(cyan.copy(alpha = gridAlpha), Offset(cx - R * 1.7f, gy), Offset(cx + R * 1.7f, gy), 1.dp.toPx())
            gy += gridStep
        }

        // Anillos exteriores (holograma)
        fun ring(radius: Float, width: Float, color: Color, start: Float, sweep: Float) {
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width, cap = StrokeCap.Round),
            )
        }
        ring(R * 1.72f, 1.4.dp.toPx(), cyan.copy(alpha = 0.22f), orbit * 0.9f, 320f)
        ring(R * 1.55f, 1.8.dp.toPx(), cyan.copy(alpha = 0.30f), orbit, 300f)
        ring(R * 1.55f, 1.8.dp.toPx(), cyanBright.copy(alpha = 0.22f), orbit + 180f, 140f)
        ring(R * 1.32f, 2.2.dp.toPx(), cyan.copy(alpha = 0.38f + 0.18f * speakAmp), -orbit * 0.8f, 260f)
        ring(R * 1.32f, 2.6.dp.toPx(), white.copy(alpha = 0.18f), -orbit * 0.8f + 40f, 70f)
        ring(R * 1.12f, 1.4.dp.toPx(), steel.copy(alpha = 0.35f), orbit * 0.6f, 360f)
        ring(R * 0.92f, 2.dp.toPx(), cyanBright.copy(alpha = 0.32f), -orbit * 1.1f + 20f, 220f)
        ring(R * 0.78f, 1.2.dp.toPx(), cyan.copy(alpha = 0.22f), orbit * 1.25f, 180f)

        // Segmentos/ticks tipo HUD
        for (i in 0 until 48) {
            val aDeg = i * 7.5f + orbit * 0.15f
            val a = (aDeg - 90f) * (PI / 180f).toFloat()
            val major = i % 4 == 0
            val r1 = R * (if (major) 0.98f else 1.03f)
            val r2 = R * 1.12f
            val col = when {
                major -> cyanBright.copy(alpha = 0.45f + 0.30f * activeGlow)
                else -> cyan.copy(alpha = 0.22f + 0.12f * activeGlow)
            }
            drawLine(
                col,
                Offset(cx + kotlin.math.cos(a) * r1, cy + kotlin.math.sin(a) * r1),
                Offset(cx + kotlin.math.cos(a) * r2, cy + kotlin.math.sin(a) * r2),
                (if (major) 2.2f else 1.2f).dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Arcos de datos (más holograma)
        drawArc(cyan.copy(alpha = 0.35f), orbit + 10f, 80f, false, Offset(cx - R * 1.62f, cy - R * 1.62f), Size(R * 3.24f, R * 3.24f), style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round))
        drawArc(gold.copy(alpha = 0.28f), -orbit * 0.9f + 200f, 50f, false, Offset(cx - R * 1.62f, cy - R * 1.62f), Size(R * 3.24f, R * 3.24f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawArc(cyanBright.copy(alpha = 0.28f), orbit * 1.3f + 120f, 65f, false, Offset(cx - R * 1.40f, cy - R * 1.40f), Size(R * 2.80f, R * 2.80f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawArc(white.copy(alpha = 0.18f), -orbit * 1.4f + 300f, 35f, false, Offset(cx - R * 1.22f, cy - R * 1.22f), Size(R * 2.44f, R * 2.44f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

        // Ecualizador orbital (reacciona al habla)
        val bars = 32
        for (i in 0 until bars) {
            val t = i / bars.toFloat()
            val ang = (t * 360f + orbit * 0.4f - 90f) * (PI / 180f).toFloat()
            val phase = speakWave + t * (2f * PI).toFloat() * 2f
            val v = (sin(phase.toDouble()).toFloat() * 0.5f + 0.5f)
            val level = if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) (0.30f + 0.70f * v) else (0.18f + 0.12f * ((i % 4) / 3f))
            val len = R * (0.12f + 0.30f * level * speakAmp / (if (state == AssistantState.IDLE) 0.14f else 1f).coerceAtLeast(0.35f))
            val rIn = R * 0.70f
            val rOut = rIn + len
            val alpha = 0.22f + 0.55f * level * activeGlow
            drawLine(
                cyanBright.copy(alpha = alpha),
                Offset(cx + kotlin.math.cos(ang) * rIn, cy + kotlin.math.sin(ang) * rIn),
                Offset(cx + kotlin.math.cos(ang) * rOut, cy + kotlin.math.sin(ang) * rOut),
                2.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Núcleo central (más intenso)
        val coreR = R * 0.36f * reactorPulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    white.copy(alpha = 0.20f * activeGlow),
                    cyanBright.copy(alpha = 0.70f * activeGlow),
                    cyan.copy(alpha = 0.28f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy), radius = coreR * 4.0f,
            ),
            radius = coreR * 4.0f, center = Offset(cx, cy),
        )
        drawCircle(cyan.copy(alpha = 0.75f), radius = coreR * 1.60f, center = Offset(cx, cy), style = Stroke(3.dp.toPx()))
        drawCircle(Color(0xFF071018), radius = coreR * 1.42f, center = Offset(cx, cy))
        drawCircle(cyanBright.copy(alpha = 0.55f), radius = coreR * 1.42f, center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
        for (i in 0 until 12) {
            val a = i * 30f - 90f + orbit * 0.3f
            drawArc(white.copy(alpha = 0.95f), a + 5f, 20f, false,
                Offset(cx - coreR, cy - coreR), Size(coreR * 2f, coreR * 2f),
                style = Stroke(coreR * 0.24f, cap = StrokeCap.Butt))
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, cyanBright.copy(alpha = 1f), cyan.copy(alpha = 0.30f)),
                center = Offset(cx, cy), radius = coreR * 0.62f,
            ),
            radius = coreR * 0.62f, center = Offset(cx, cy),
        )

        // Barrido holográfico
        val sweepY = cy - R * 1.7f + R * 3.4f * scan
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, cyanBright.copy(alpha = 0.14f), white.copy(alpha = 0.06f), Color.Transparent),
                startY = sweepY - R * 0.22f, endY = sweepY + R * 0.22f,
            ),
            topLeft = Offset(cx - R * 1.8f, sweepY - R * 0.22f),
            size = Size(R * 3.6f, R * 0.44f),
        )

        // Partículas/nodos holográficos
        val nodes = 12
        for (i in 0 until nodes) {
            val a = ((i * (360f / nodes) + orbit * (if (i % 2 == 0) 0.6f else -0.4f)) - 90f) * (PI / 180f).toFloat()
            val rr = R * (1.18f + 0.14f * sin((scan * 2f * PI + i).toDouble()).toFloat())
            val p = Offset(cx + kotlin.math.cos(a) * rr, cy + kotlin.math.sin(a) * rr)
            drawCircle(cyanBright.copy(alpha = 0.55f * activeGlow), radius = 3.5.dp.toPx(), center = p)
            drawCircle(cyan.copy(alpha = 0.30f), radius = 8.dp.toPx(), center = p, style = Stroke(1.2.dp.toPx()))
            drawLine(
                cyan.copy(alpha = 0.18f * activeGlow),
                Offset(cx + kotlin.math.cos(a) * R * 0.88f, cy + kotlin.math.sin(a) * R * 0.88f),
                p,
                1.dp.toPx(),
            )
        }
    }
}

private fun helmetPath(cx: Float, cy: Float, w: Float, h: Float): Path {
    val left = cx - w / 2f
    val right = cx + w / 2f
    val top = cy - h / 2f
    val bottom = cy + h / 2f
    return Path().apply {
        moveTo(cx, top + h * 0.03f)
        // frente/domo
        cubicTo(cx + w * 0.26f, top, right, top + h * 0.14f, right, cy - h * 0.08f)
        // lateral
        cubicTo(right, cy + h * 0.12f, right - w * 0.05f, cy + h * 0.30f, right - w * 0.14f, cy + h * 0.42f)
        // mandíbula angular
        lineTo(cx + w * 0.18f, bottom)
        lineTo(cx - w * 0.18f, bottom)
        lineTo(left + w * 0.14f, cy + h * 0.42f)
        cubicTo(left + w * 0.05f, cy + h * 0.30f, left, cy + h * 0.12f, left, cy - h * 0.08f)
        cubicTo(left, top + h * 0.14f, cx - w * 0.26f, top, cx, top + h * 0.03f)
        close()
    }
}

private fun DrawScope.drawFacePlate(cx: Float, cy: Float, w: Float, h: Float, gold: Color, goldLight: Color, goldDark: Color) {
    // Placa frontal dorada (cara de la máscara)
    val plate = Path().apply {
        moveTo(cx, cy - h * 0.30f)
        lineTo(cx + w * 0.34f, cy - h * 0.12f)
        lineTo(cx + w * 0.30f, cy + h * 0.10f)
        lineTo(cx + w * 0.20f, cy + h * 0.26f)
        lineTo(cx + w * 0.12f, cy + h * 0.44f)
        lineTo(cx - w * 0.12f, cy + h * 0.44f)
        lineTo(cx - w * 0.20f, cy + h * 0.26f)
        lineTo(cx - w * 0.30f, cy + h * 0.10f)
        lineTo(cx - w * 0.34f, cy - h * 0.12f)
        close()
    }
    drawPath(plate, Brush.verticalGradient(listOf(goldLight, gold, goldDark), cy - h * 0.30f, cy + h * 0.44f))
    // sombreado central y reflejo metálico
    drawPath(plate, Brush.horizontalGradient(
        listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent, Color.Black.copy(alpha = 0.10f)),
        cx - w * 0.34f, cx + w * 0.34f
    ))
    drawPath(plate, Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
        center = Offset(cx - w * 0.10f, cy - h * 0.18f),
        radius = w * 0.40f,
    ))
    // bisel sutil
    drawPath(plate, Color.White.copy(alpha = 0.08f), style = Stroke(1.dp.toPx()))
}

private fun DrawScope.drawMaskDetails(cx: Float, cy: Float, w: Float, h: Float, goldDark: Color, redDark: Color) {
    val line = goldDark.copy(alpha = 0.75f)
    val dark = redDark.copy(alpha = 0.6f)
    val fine = Color(0xFF3A2A08).copy(alpha = 0.35f)
    // Líneas angulares de la máscara
    drawLine(line, Offset(cx, cy - h * 0.28f), Offset(cx, cy - h * 0.02f), 1.6.dp.toPx())
    drawLine(line, Offset(cx, cy - h * 0.02f), Offset(cx - w * 0.10f, cy + h * 0.06f), 1.6.dp.toPx())
    drawLine(line, Offset(cx, cy - h * 0.02f), Offset(cx + w * 0.10f, cy + h * 0.06f), 1.6.dp.toPx())
    // pómulos angulares
    drawLine(line, Offset(cx - w * 0.10f, cy + h * 0.06f), Offset(cx - w * 0.28f, cy + h * 0.02f), 1.5.dp.toPx())
    drawLine(line, Offset(cx + w * 0.10f, cy + h * 0.06f), Offset(cx + w * 0.28f, cy + h * 0.02f), 1.5.dp.toPx())
    drawLine(line, Offset(cx - w * 0.10f, cy + h * 0.06f), Offset(cx - w * 0.18f, cy + h * 0.22f), 1.5.dp.toPx())
    drawLine(line, Offset(cx + w * 0.10f, cy + h * 0.06f), Offset(cx + w * 0.18f, cy + h * 0.22f), 1.5.dp.toPx())
    // frente / panel superior
    drawLine(fine, Offset(cx - w * 0.16f, cy - h * 0.20f), Offset(cx + w * 0.16f, cy - h * 0.20f), 1.dp.toPx())
    drawLine(fine, Offset(cx - w * 0.22f, cy - h * 0.14f), Offset(cx - w * 0.08f, cy - h * 0.14f), 1.dp.toPx())
    drawLine(fine, Offset(cx + w * 0.08f, cy - h * 0.14f), Offset(cx + w * 0.22f, cy - h * 0.14f), 1.dp.toPx())
    // contorno de mejillas rojo
    drawLine(dark, Offset(cx - w * 0.34f, cy - h * 0.12f), Offset(cx - w * 0.30f, cy + h * 0.10f), 1.2.dp.toPx())
    drawLine(dark, Offset(cx + w * 0.34f, cy - h * 0.12f), Offset(cx + w * 0.30f, cy + h * 0.10f), 1.2.dp.toPx())
    // línea de mandíbula
    drawLine(fine, Offset(cx - w * 0.18f, cy + h * 0.24f), Offset(cx + w * 0.18f, cy + h * 0.24f), 1.dp.toPx())
}

private fun DrawScope.drawEyeSlits(cx: Float, cy: Float, w: Float, h: Float, white: Color, glow: Float) {
    val eyeY = cy - h * 0.08f
    val slitW = w * 0.14f
    val slitH = h * 0.018f
    val gap = w * 0.07f

    fun slit(xCenter: Float) {
        val rect = Rect(xCenter - slitW / 2f, eyeY - slitH / 2f, xCenter + slitW / 2f, eyeY + slitH / 2f)
        // resplandor
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(white.copy(alpha = 0.55f * glow), white.copy(alpha = 0.12f * glow), Color.Transparent),
                center = Offset(xCenter, eyeY),
                radius = slitW * 0.8f,
            ),
            topLeft = Offset(rect.left - slitW * 0.3f, rect.top - slitH * 2f),
            size = Size(rect.width + slitW * 0.6f, rect.height + slitH * 4f),
            cornerRadius = CornerRadius(slitH, slitH),
        )
        // ranura blanca
        drawRoundRect(
            color = white.copy(alpha = 0.95f * glow),
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            cornerRadius = CornerRadius(slitH / 2f, slitH / 2f),
        )
        // núcleo brillante
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(rect.left + rect.width * 0.15f, rect.top + rect.height * 0.2f),
            size = Size(rect.width * 0.7f, rect.height * 0.6f),
            cornerRadius = CornerRadius(slitH / 3f, slitH / 3f),
        )
    }
    slit(cx - gap - slitW / 2f)
    slit(cx + gap + slitW / 2f)
}

private fun DrawScope.drawMouthPlate(
    cx: Float, cy: Float, w: Float, h: Float,
    gold: Color, goldDark: Color, redDark: Color, white: Color,
    state: AssistantState, speakWave: Float,
) {
    val top = cy + h * 0.18f
    val bottom = cy + h * 0.42f
    val mouth = Path().apply {
        moveTo(cx - w * 0.16f, top)
        lineTo(cx + w * 0.16f, top)
        lineTo(cx + w * 0.12f, bottom)
        lineTo(cx - w * 0.12f, bottom)
        close()
    }
    drawPath(mouth, Brush.verticalGradient(listOf(gold, goldDark), top, bottom))
    // volumen interior oscuro
    drawPath(mouth, Brush.verticalGradient(
        listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.25f)),
        top, bottom
    ))
    drawPath(mouth, redDark.copy(alpha = 0.45f), style = Stroke(1.dp.toPx()))

    val speaking = state == AssistantState.SPEAKING
    val listening = state == AssistantState.LISTENING
    val baseAmp = when (state) {
        AssistantState.SPEAKING -> 0.6f
        AssistantState.LISTENING -> 0.25f
        AssistantState.THINKING -> 0.15f
        else -> 0.05f
    }

    // ranuras animadas (se abren y brillan al hablar)
    val slots = 5
    for (i in 0 until slots) {
        val t = i / (slots - 1).toFloat()
        val y = top + (bottom - top) * (0.22f + t * 0.56f)
        val sw = w * (0.21f - t * 0.03f)
        val phase = speakWave + i * 0.9f
        val wave = (sin(phase.toDouble()).toFloat() * 0.5f + 0.5f)
        val amp = baseAmp * (0.4f + 0.6f * wave)
        val slotH = h * (0.010f + 0.018f * amp)
        val glow = if (speaking || listening) 0.25f + 0.55f * amp else 0.08f

        // luz interior de la ranura
        if (speaking || listening) {
            drawRoundRect(
                color = white.copy(alpha = glow * 0.35f),
                topLeft = Offset(cx - sw / 2f, y - slotH * 1.2f),
                size = Size(sw, slotH * 2.4f),
                cornerRadius = CornerRadius(slotH, slotH),
            )
        }
        // ranura
        drawRoundRect(
            color = Color(0xFF1A0A0A).copy(alpha = 0.75f),
            topLeft = Offset(cx - sw / 2f, y - slotH / 2f),
            size = Size(sw, slotH),
            cornerRadius = CornerRadius(slotH / 2f, slotH / 2f),
        )
        drawRoundRect(
            color = white.copy(alpha = glow),
            topLeft = Offset(cx - sw / 2f, y - slotH / 2f),
            size = Size(sw, slotH),
            cornerRadius = CornerRadius(slotH / 2f, slotH / 2f),
            style = Stroke(0.8.dp.toPx()),
        )
    }
    // línea central sutil
    drawLine(redDark.copy(alpha = 0.25f), Offset(cx, top + (bottom - top) * 0.12f), Offset(cx, bottom - (bottom - top) * 0.08f), 1.dp.toPx())
}

private fun DrawScope.drawNeck(cx: Float, top: Float, headW: Float, shoulderY: Float, red: Color, redBright: Color, redDark: Color, gold: Color, steel: Color) {
    val neckW = headW * 0.30f
    val bottom = shoulderY + (shoulderY - top) * 0.1f
    val path = Path().apply {
        moveTo(cx - neckW, top)
        lineTo(cx - neckW * 1.35f, bottom)
        lineTo(cx + neckW * 1.35f, bottom)
        lineTo(cx + neckW, top)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(redBright, red, redDark), top, bottom))
    // segmentos articulados superpuestos
    val segments = 4
    for (i in 0 until segments) {
        val y0 = top + (bottom - top) * (i / segments.toFloat())
        val y1 = top + (bottom - top) * ((i + 1) / segments.toFloat()) + (bottom - top) * 0.04f
        val ww = neckW * (1f + 0.09f * i)
        val seg = Path().apply {
            moveTo(cx - ww, y0)
            lineTo(cx - ww * 1.08f, y1)
            lineTo(cx + ww * 1.08f, y1)
            lineTo(cx + ww, y0)
            close()
        }
        drawPath(seg, redBright.copy(alpha = 0.18f))
        drawLine(gold.copy(alpha = 0.25f), Offset(cx - ww, y0), Offset(cx + ww, y0), 1.2.dp.toPx())
        drawLine(redDark.copy(alpha = 0.5f), Offset(cx - ww * 1.02f, y1 - (bottom - top) * 0.04f), Offset(cx + ww * 1.02f, y1 - (bottom - top) * 0.04f), 1.dp.toPx())
    }
    // articulaciones laterales
    drawCircle(steel.copy(alpha = 0.5f), radius = neckW * 0.08f, center = Offset(cx - neckW * 1.15f, (top + bottom) / 2f))
    drawCircle(steel.copy(alpha = 0.5f), radius = neckW * 0.08f, center = Offset(cx + neckW * 1.15f, (top + bottom) / 2f))
    drawCircle(gold.copy(alpha = 0.4f), radius = neckW * 0.04f, center = Offset(cx - neckW * 1.15f, (top + bottom) / 2f))
    drawCircle(gold.copy(alpha = 0.4f), radius = neckW * 0.04f, center = Offset(cx + neckW * 1.15f, (top + bottom) / 2f))
}

private fun DrawScope.drawHub(
    cx: Float, top: Float, hubW: Float, bottom: Float, hubR: Float,
    red: Color, redBright: Color, redDark: Color, gold: Color, goldLight: Color, white: Color, steel: Color,
    reactorPulse: Float, state: AssistantState, scan: Float, speakWave: Float,
) {
    val h = bottom - top
    val cy = top + h * 0.48f
    val baseR = hubR * 0.98f
    val activeGlow = when (state) {
        AssistantState.THINKING, AssistantState.SPEAKING -> 0.55f
        AssistantState.LISTENING -> 0.45f
        AssistantState.ERROR -> 0.25f
        else -> 0.35f
    }

    // Base del hub (chasis circular)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(redBright.copy(alpha = 0.55f), redDark.copy(alpha = 0.9f), Color(0xFF1A0608)),
            center = Offset(cx, cy),
            radius = baseR,
        ),
        radius = baseR,
        center = Offset(cx, cy),
    )
    drawCircle(redDark.copy(alpha = 0.7f), radius = baseR, center = Offset(cx, cy), style = Stroke(2.dp.toPx()))

    // Anillos HUD giratorios/sutiles
    val rot = scan * 360f
    drawArc(
        color = white.copy(alpha = 0.18f * activeGlow + 0.08f),
        startAngle = rot,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = Offset(cx - baseR * 0.88f, cy - baseR * 0.88f),
        size = Size(baseR * 1.76f, baseR * 1.76f),
        style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
        color = goldLight.copy(alpha = 0.20f),
        startAngle = -rot * 0.7f + 40f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - baseR * 0.74f, cy - baseR * 0.74f),
        size = Size(baseR * 1.48f, baseR * 1.48f),
        style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
        color = white.copy(alpha = 0.12f),
        startAngle = rot * 0.5f + 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(cx - baseR * 1.02f, cy - baseR * 1.02f),
        size = Size(baseR * 2.04f, baseR * 2.04f),
        style = Stroke(1.dp.toPx(), cap = StrokeCap.Round),
    )

    // Marcas de interfaz
    for (i in 0 until 12) {
        val a = (i * 30f - 90f) * (PI / 180f).toFloat()
        val inner = baseR * 0.82f
        val outer = baseR * (if (i % 3 == 0) 0.95f else 0.90f)
        val c = if (i % 3 == 0) gold.copy(alpha = 0.35f) else steel.copy(alpha = 0.20f)
        drawLine(
            c,
            Offset(cx + kotlin.math.cos(a) * inner, cy + kotlin.math.sin(a) * inner),
            Offset(cx + kotlin.math.cos(a) * outer, cy + kotlin.math.sin(a) * outer),
            (if (i % 3 == 0) 2f else 1f).dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Núcleo reactor prominente
    val coreR = hubR * 0.28f * reactorPulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(white.copy(alpha = activeGlow), white.copy(alpha = activeGlow * 0.20f), Color.Transparent),
            center = Offset(cx, cy), radius = coreR * 3.2f,
        ),
        radius = coreR * 3.2f, center = Offset(cx, cy),
    )
    drawCircle(gold.copy(alpha = 0.75f), radius = coreR * 1.45f, center = Offset(cx, cy), style = Stroke(3.dp.toPx()))
    drawCircle(Color(0xFF0E151C), radius = coreR * 1.32f, center = Offset(cx, cy))
    drawCircle(goldLight.copy(alpha = 0.55f), radius = coreR * 1.32f, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
    for (i in 0 until 10) {
        val a = i * 36f - 90f + rot * 0.2f
        drawArc(white.copy(alpha = 0.85f), a + 5f, 26f, false,
            Offset(cx - coreR, cy - coreR), Size(coreR * 2f, coreR * 2f),
            style = Stroke(coreR * 0.26f, cap = StrokeCap.Butt))
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, white.copy(alpha = 0.95f), white.copy(alpha = 0.25f)),
            center = Offset(cx, cy), radius = coreR * 0.58f,
        ),
        radius = coreR * 0.58f, center = Offset(cx, cy),
    )

    // Barrido HUD sobre el hub
    val sweepY = top + h * 0.08f + h * 0.84f * scan
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, white.copy(alpha = 0.05f), Color.Transparent),
            startY = sweepY - h * 0.04f, endY = sweepY + h * 0.04f,
        ),
        topLeft = Offset(cx - hubW / 2f, sweepY - h * 0.04f),
        size = Size(hubW, h * 0.08f),
    )

    // Indicadores laterales tipo hub (reaccionan al habla)
    val wave = (sin(speakWave.toDouble()).toFloat() * 0.5f + 0.5f)
    val bars = 4
    for (side in listOf(-1f, 1f)) {
        for (i in 0 until bars) {
            val t = i / (bars - 1).toFloat()
            val bx = cx + side * (hubR * 0.62f + i * hubR * 0.08f)
            val bh = h * (0.05f + 0.10f * (if (state == AssistantState.SPEAKING) (0.4f + 0.6f * ((wave + t) % 1f)) else 0.25f))
            val by = cy - bh / 2f
            val bc = if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) white.copy(alpha = 0.35f) else steel.copy(alpha = 0.20f)
            drawRoundRect(bc, topLeft = Offset(bx - 2.dp.toPx(), by), size = Size(4.dp.toPx(), bh), cornerRadius = CornerRadius(2.dp.toPx()))
        }
    }
}

private fun DrawScope.drawPauldron(
    cx: Float, cy: Float, chestW: Float, torsoH: Float,
    red: Color, redBright: Color, redDark: Color, gold: Color, goldLight: Color, steel: Color,
    left: Boolean,
) {
    val dir = if (left) -1f else 1f
    val w = chestW * 0.34f
    val h = torsoH * 0.30f
    val path = Path().apply {
        moveTo(cx, cy - h * 0.55f)
        cubicTo(cx + dir * w * 0.55f, cy - h * 0.60f, cx + dir * w, cy - h * 0.20f, cx + dir * w * 0.95f, cy + h * 0.25f)
        cubicTo(cx + dir * w * 0.70f, cy + h * 0.60f, cx + dir * w * 0.20f, cy + h * 0.55f, cx - dir * w * 0.05f, cy + h * 0.20f)
        cubicTo(cx - dir * w * 0.25f, cy - h * 0.05f, cx - dir * w * 0.15f, cy - h * 0.40f, cx, cy - h * 0.55f)
        close()
    }
    drawPath(path, Brush.radialGradient(
        colors = listOf(redBright, red, redDark),
        center = Offset(cx + dir * w * 0.25f, cy - h * 0.1f),
        radius = w * 0.9f,
    ))
    // brillo superior
    drawPath(path, Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
        startY = cy - h * 0.6f, endY = cy,
    ))
    // capas/placas
    clipPath(path) {
        drawArc(redDark.copy(alpha = 0.35f), if (left) 210f else -30f, 120f, false,
            Offset(cx - w * 0.2f, cy - h * 0.45f), Size(w * 1.0f, h * 0.8f),
            style = Stroke(1.5.dp.toPx()))
        drawArc(redDark.copy(alpha = 0.30f), if (left) 220f else -20f, 100f, false,
            Offset(cx - w * 0.05f, cy - h * 0.25f), Size(w * 0.85f, h * 0.7f),
            style = Stroke(1.2.dp.toPx()))
    }
    drawPath(path, gold.copy(alpha = 0.25f), style = Stroke(1.5.dp.toPx()))
    // detalle/articulación visible
    val jointX = cx + dir * w * 0.55f
    val jointY = cy + h * 0.15f
    drawCircle(steel.copy(alpha = 0.55f), radius = w * 0.10f, center = Offset(jointX, jointY))
    drawCircle(goldLight.copy(alpha = 0.45f), radius = w * 0.05f, center = Offset(jointX, jointY))
    drawCircle(redDark.copy(alpha = 0.5f), radius = w * 0.10f, center = Offset(jointX, jointY), style = Stroke(1.dp.toPx()))
    // remache
    drawCircle(gold.copy(alpha = 0.5f), radius = w * 0.04f, center = Offset(cx + dir * w * 0.20f, cy - h * 0.25f))
}

private fun DrawScope.drawUpperArm(
    cx: Float, top: Float, chestW: Float, torsoH: Float,
    red: Color, redBright: Color, redDark: Color, gold: Color,
    left: Boolean,
) {
    val dir = if (left) -1f else 1f
    val w = chestW * 0.18f
    val h = torsoH * 0.42f
    val path = Path().apply {
        moveTo(cx - w * 0.2f, top)
        cubicTo(cx + dir * w * 0.8f, top + h * 0.1f, cx + dir * w, top + h * 0.55f, cx + dir * w * 0.6f, top + h)
        lineTo(cx - dir * w * 0.1f, top + h)
        cubicTo(cx - dir * w * 0.3f, top + h * 0.55f, cx - dir * w * 0.4f, top + h * 0.15f, cx - w * 0.2f, top)
        close()
    }
    drawPath(path, Brush.linearGradient(listOf(redBright, red, redDark), Offset(cx, top), Offset(cx + dir * w, top + h)))
    // placas y detalle dorado
    clipPath(path) {
        drawLine(redDark.copy(alpha = 0.4f), Offset(cx - w * 0.1f, top + h * 0.35f), Offset(cx + dir * w * 0.7f, top + h * 0.30f), 1.5.dp.toPx())
        drawLine(redDark.copy(alpha = 0.35f), Offset(cx - w * 0.15f, top + h * 0.60f), Offset(cx + dir * w * 0.5f, top + h * 0.62f), 1.2.dp.toPx())
        drawLine(gold.copy(alpha = 0.30f), Offset(cx + dir * w * 0.1f, top + h * 0.15f), Offset(cx + dir * w * 0.2f, top + h * 0.45f), 2.dp.toPx(), cap = StrokeCap.Round)
    }
    drawPath(path, redDark.copy(alpha = 0.5f), style = Stroke(1.2.dp.toPx()))
    // articulación codo/bíceps
    val jointY = top + h * 0.55f
    drawCircle(gold.copy(alpha = 0.35f), radius = w * 0.18f, center = Offset(cx + dir * w * 0.25f, jointY), style = Stroke(1.5.dp.toPx()))
    drawCircle(Color(0xFF2A2A2A), radius = w * 0.10f, center = Offset(cx + dir * w * 0.25f, jointY))
}