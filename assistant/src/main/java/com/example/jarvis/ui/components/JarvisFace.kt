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
import androidx.compose.ui.graphics.drawscope.drawOval
import androidx.compose.ui.unit.dp
import com.example.jarvis.domain.model.AssistantState
import kotlin.math.PI
import kotlin.math.sin

/**
 * Busto de armadura estilo Iron Man (hasta hombros/brazos superiores):
 * casco dorado y rojo, ranuras oculares blancas, cuello articulado rojo,
 * hombreras redondeadas, pecho con placas rojas y reactor blanco.
 * Reacciona al estado con brillo/pulso del reactor y ojos.
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

        // Proporciones: encuadre hasta hombros/brazos superiores
        val scale = h * 0.98f * breathe
        val headH = scale * 0.36f
        val headW = headH * 0.78f
        val headCy = h * 0.24f
        val neckTop = headCy + headH * 0.46f
        val shoulderY = neckTop + scale * 0.07f
        val chestBottom = h * 0.97f
        val chestW = scale * 0.62f

        // Fondo sutil
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(red.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(cx, h * 0.45f),
                radius = w * 0.55f,
            ),
            radius = w * 0.55f,
            center = Offset(cx, h * 0.45f),
        )

        // Brazos superiores (detrás de hombreras/pecho)
        drawUpperArm(cx - chestW * 0.62f, shoulderY + (chestBottom - shoulderY) * 0.18f, chestW, chestBottom - shoulderY, red, redBright, redDark, gold, left = true)
        drawUpperArm(cx + chestW * 0.62f, shoulderY + (chestBottom - shoulderY) * 0.18f, chestW, chestBottom - shoulderY, red, redBright, redDark, gold, left = false)

        // Pecho
        drawChest(cx, shoulderY, chestW, chestBottom, red, redBright, redDark, gold, goldLight, white, reactorPulse * active, state)

        // Hombreras (grandes, redondeadas, por delante de parte del pecho)
        drawPauldron(cx - chestW * 0.58f, shoulderY + (chestBottom - shoulderY) * 0.08f, chestW, chestBottom - shoulderY, red, redBright, redDark, gold, goldLight, steel, left = true)
        drawPauldron(cx + chestW * 0.58f, shoulderY + (chestBottom - shoulderY) * 0.08f, chestW, chestBottom - shoulderY, red, redBright, redDark, gold, goldLight, steel, left = false)

        // Cuello articulado rojo
        drawNeck(cx, neckTop, headW, shoulderY, red, redBright, redDark, gold, steel)

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

private fun DrawScope.drawChest(
    cx: Float, top: Float, chestW: Float, bottom: Float,
    red: Color, redBright: Color, redDark: Color, gold: Color, goldLight: Color, white: Color,
    reactorPulse: Float, state: AssistantState,
) {
    val h = bottom - top
    // Silueta más humana: clavículas estrechas, pectorales anchos, cintura recogida
    val collar = chestW * 0.42f
    val pec = chestW * 0.62f
    val waist = chestW * 0.38f
    val hip = chestW * 0.46f

    val path = Path().apply {
        moveTo(cx - collar, top)
        // hombro/pectoral izquierdo
        cubicTo(cx - chestW * 0.56f, top + h * 0.10f, cx - pec, top + h * 0.30f, cx - pec * 0.98f, top + h * 0.46f)
        // cintura izquierda
        cubicTo(cx - chestW * 0.56f, top + h * 0.64f, cx - waist, top + h * 0.78f, cx - hip, bottom)
        // base ligeramente curva
        quadraticTo(cx, bottom + h * 0.03f, cx + hip, bottom)
        // cintura derecha
        cubicTo(cx + waist, top + h * 0.78f, cx + chestW * 0.56f, top + h * 0.64f, cx + pec * 0.98f, top + h * 0.46f)
        // pectoral/hombro derecho
        cubicTo(cx + pec, top + h * 0.30f, cx + chestW * 0.56f, top + h * 0.10f, cx + collar, top)
        // clavícula curva
        quadraticTo(cx, top - h * 0.05f, cx - collar, top)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(redBright, red, redDark), top - h * 0.05f, bottom))
    // Placas curvas y sombreado anatómico
    clipPath(path) {
        drawPath(path, Brush.horizontalGradient(
            listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Black.copy(alpha = 0.18f)),
            cx - pec, cx + pec,
        ))
        // volumen pectoral
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(cx - chestW * 0.22f, top + h * 0.28f),
                radius = chestW * 0.28f,
            ),
            topLeft = Offset(cx - chestW * 0.50f, top + h * 0.05f),
            size = Size(chestW * 0.52f, h * 0.46f),
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                center = Offset(cx + chestW * 0.22f, top + h * 0.28f),
                radius = chestW * 0.28f,
            ),
            topLeft = Offset(cx - chestW * 0.02f, top + h * 0.05f),
            size = Size(chestW * 0.52f, h * 0.46f),
        )
        // líneas de placas siguiendo el torso
        val plateLine = redDark.copy(alpha = 0.45f)
        drawArc(plateLine, 210f, 120f, false,
            Offset(cx - chestW * 0.30f, top + h * 0.02f), Size(chestW * 0.60f, h * 0.38f),
            style = Stroke(1.5.dp.toPx()))
        drawArc(plateLine, 205f, 130f, false,
            Offset(cx - chestW * 0.24f, top + h * 0.18f), Size(chestW * 0.48f, h * 0.34f),
            style = Stroke(1.2.dp.toPx()))
        drawArc(redDark.copy(alpha = 0.30f), 20f, 140f, false,
            Offset(cx - chestW * 0.22f, top + h * 0.52f), Size(chestW * 0.44f, h * 0.30f),
            style = Stroke(1.dp.toPx()))
        drawLine(plateLine, Offset(cx, top - h * 0.02f), Offset(cx, top + h * 0.22f), 1.dp.toPx())
        drawLine(plateLine, Offset(cx, top + h * 0.58f), Offset(cx, bottom - h * 0.02f), 1.dp.toPx())
        // bordes dorados de clavícula/hombro
        drawLine(gold.copy(alpha = 0.35f), Offset(cx - collar, top), Offset(cx - pec * 0.72f, top + h * 0.22f), 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(gold.copy(alpha = 0.35f), Offset(cx + collar, top), Offset(cx + pec * 0.72f, top + h * 0.22f), 2.dp.toPx(), cap = StrokeCap.Round)
    }
    drawPath(path, redDark.copy(alpha = 0.55f), style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))

    // Reactor circular blanco central
    val coreCy = top + h * 0.46f
    val coreR = chestW * 0.12f * reactorPulse
    val glowAlpha = when (state) {
        AssistantState.THINKING, AssistantState.SPEAKING -> 0.45f
        AssistantState.ERROR -> 0.20f
        else -> 0.30f
    }
    // halo exterior
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(white.copy(alpha = glowAlpha), white.copy(alpha = glowAlpha * 0.25f), Color.Transparent),
            center = Offset(cx, coreCy), radius = coreR * 3f,
        ),
        radius = coreR * 3f, center = Offset(cx, coreCy),
    )
    // aro metálico
    drawCircle(gold.copy(alpha = 0.7f), radius = coreR * 1.35f, center = Offset(cx, coreCy), style = Stroke(2.5.dp.toPx()))
    drawCircle(Color(0xFF1A2530), radius = coreR * 1.25f, center = Offset(cx, coreCy))
    drawCircle(goldLight.copy(alpha = 0.5f), radius = coreR * 1.25f, center = Offset(cx, coreCy), style = Stroke(1.dp.toPx()))
    // segmentos internos
    for (i in 0 until 8) {
        val a = i * 45f - 90f
        drawArc(white.copy(alpha = 0.85f), a + 6f, 33f, false,
            Offset(cx - coreR, coreCy - coreR), Size(coreR * 2f, coreR * 2f),
            style = Stroke(coreR * 0.28f, cap = StrokeCap.Butt))
    }
    // núcleo blanco
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, white.copy(alpha = 0.9f), white.copy(alpha = 0.3f)),
            center = Offset(cx, coreCy), radius = coreR * 0.55f,
        ),
        radius = coreR * 0.55f, center = Offset(cx, coreCy),
    )
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