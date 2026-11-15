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
import kotlin.math.sin

/**
 * Interfaz estilo JARVIS: cabeza androide sobre un núcleo/hub tecnológico.
 * Más "hub" que torso humano: núcleo reactor prominente, anillos HUD y base
 * de chasis, con reacción visual al estado (ojos, reactor, anillos).
 */
@Composable
fun JarvisFace(
    state: AssistantState,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "jarvis-armor")

    val breathe by transition.animateFloat(
        initialValue = 0.995f,
        targetValue = 1.005f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respirar",
    )
    val reactorPulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "reactor",
    )
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
        ),
        label = "scan",
    )
    val speakWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
        ),
        label = "habla",
    )

    // Intensidad según estado
    val active = when (state) {
        AssistantState.LISTENING -> 1.05f
        AssistantState.THINKING -> 1.15f
        AssistantState.SPEAKING -> 1.2f
        AssistantState.ERROR -> 0.9f
        AssistantState.IDLE -> 0.95f
    }
    val eyeGlow = when (state) {
        AssistantState.SPEAKING -> 0.85f + 0.15f * ((sin(speakWave.toDouble()).toFloat() * 0.5f) + 0.5f)
        AssistantState.THINKING -> 1f
        AssistantState.LISTENING -> 0.95f
        AssistantState.ERROR -> 0.5f
        AssistantState.IDLE -> 0.8f
    }

    // Paleta armadura
    val red = Color(0xFFB01018)
    val redBright = Color(0xFFD51C24)
    val redDark = Color(0xFF5E0A0E)
    val gold = Color(0xFFD4A017)
    val goldLight = Color(0xFFF2C94C)
    val goldDark = Color(0xFF8A6510)
    val white = Color(0xFFF8FCFF)
    val steel = Color(0xFFB8C4CC)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Proporciones: cabeza arriba y núcleo/hub debajo
        val scale = h * 0.98f * breathe
        val headH = scale * 0.34f
        val headW = headH * 0.78f
        val headCy = h * 0.22f
        val neckTop = headCy + headH * 0.46f
        val hubTop = neckTop + scale * 0.03f
        val hubBottom = h * 0.97f
        val hubW = scale * 0.70f
        val hubH = hubBottom - hubTop
        val hubCy = hubTop + hubH * 0.48f
        val hubR = minOf(hubW, hubH) * 0.46f

        // Fondo HUD sutil
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(white.copy(alpha = 0.06f), Color.Transparent),
                center = Offset(cx, hubCy),
                radius = w * 0.52f,
            ),
            radius = w * 0.52f,
            center = Offset(cx, hubCy),
        )

        // Núcleo/hub tecnológico
        drawHub(cx, hubTop, hubW, hubBottom, hubR, red, redBright, redDark, gold, goldLight, white, steel, reactorPulse * active, state, scan, speakWave)

        // Cuello/soporte hacia el hub
        drawNeck(cx, neckTop, headW, hubTop + hubH * 0.10f, red, redBright, redDark, gold, steel)

        // Casco
        val helmet = helmetPath(cx, headCy, headW, headH)
        // Base roja
        drawPath(helmet, Brush.linearGradient(
            colors = listOf(redBright, red, redDark),
            start = Offset(cx - headW / 2f, headCy - headH / 2f),
            end = Offset(cx + headW / 2f, headCy + headH / 2f),
        ))
        clipPath(helmet) {
            // Placa frontal dorada
            drawFacePlate(cx, headCy, headW, headH, gold, goldLight, goldDark)
            // Brillo metálico
            drawPath(helmet, Brush.verticalGradient(
                colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.18f)),
                startY = headCy - headH / 2f, endY = headCy + headH / 2f,
            ))
            // Reflejo
            drawPath(helmet, Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(cx - headW * 0.18f, headCy - headH * 0.20f),
                radius = headW * 0.55f,
            ))
            // Barrido sutil
            val scanY = headCy - headH * 0.5f + headH * scan
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, white.copy(alpha = 0.06f), Color.Transparent),
                    startY = scanY - headH * 0.05f, endY = scanY + headH * 0.05f,
                ),
                topLeft = Offset(cx - headW, scanY - headH * 0.05f),
                size = Size(headW * 2f, headH * 0.1f),
            )
        }
        // Contorno casco
        drawPath(helmet, Brush.verticalGradient(
            colors = listOf(goldLight.copy(alpha = 0.5f), goldDark.copy(alpha = 0.4f), redDark.copy(alpha = 0.5f)),
            startY = headCy - headH / 2f, endY = headCy + headH / 2f,
        ), style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))

        // Líneas angulares de máscara y ojos
        drawMaskDetails(cx, headCy, headW, headH, goldDark, redDark)
        drawEyeSlits(cx, headCy, headW, headH, white, eyeGlow)
        // Boca/mentón de máscara (animada al hablar)
        drawMouthPlate(cx, headCy, headW, headH, gold, goldDark, redDark, white, state, speakWave)
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