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
 * Rostro androide de JARVIS con proporciones humanas y textura metálica.
 * Reacciona al estado del asistente con luz, parpadeo y animación de voz.
 */
@Composable
fun JarvisFace(
    state: AssistantState,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "jarvis-face")

    val breathe by transition.animateFloat(
        initialValue = 0.992f,
        targetValue = 1.008f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respirar",
    )
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(4600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "parpadeo",
    )
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
        ),
        label = "scan",
    )
    val mouthWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
        ),
        label = "boca",
    )
    val ringRot by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
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

    // Paleta metálica
    val metalDark = Color(0xFF08131C)
    val metalMid = Color(0xFF122433)
    val metalLight = Color(0xFF1E3A4D)
    val steel = Color(0xFF8FB6C8)

    Canvas(modifier.aspectRatio(1f)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.50f
        // Proporciones humanas aproximadas: ancho cara ~0.72 de alto cabeza
        val faceH = h * 0.72f * breathe
        val faceW = faceH * 0.72f

        // Halo ambiental
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.04f), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.48f,
            ),
            radius = w * 0.48f,
            center = Offset(cx, cy),
        )

        // Anillos HUD
        rotate(ringRot, pivot = Offset(cx, cy)) {
            drawArc(accent.copy(alpha = 0.30f), -20f, 60f, false,
                Offset(cx - w * 0.45f, cy - h * 0.45f), Size(w * 0.90f, h * 0.90f),
                style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
            drawArc(accent.copy(alpha = 0.16f), 160f, 45f, false,
                Offset(cx - w * 0.45f, cy - h * 0.45f), Size(w * 0.90f, h * 0.90f),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
        }
        rotate(-ringRot * 0.6f, pivot = Offset(cx, cy)) {
            drawArc(steel.copy(alpha = 0.14f), 40f, 120f, false,
                Offset(cx - w * 0.41f, cy - h * 0.41f), Size(w * 0.82f, h * 0.82f),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
        }

        val head = headPath(cx, cy, faceW, faceH)

        // Base metálica de la cabeza
        drawPath(
            path = head,
            brush = Brush.linearGradient(
                colors = listOf(metalLight, metalMid, metalDark, metalMid),
                start = Offset(cx - faceW * 0.5f, cy - faceH * 0.5f),
                end = Offset(cx + faceW * 0.5f, cy + faceH * 0.5f),
            ),
        )
        // Sombreado vertical para volumen
        drawPath(
            path = head,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.25f),
                ),
                startY = cy - faceH / 2f,
                endY = cy + faceH / 2f,
            ),
        )
        // Brillo especular (mejilla/luz)
        drawPath(
            path = head,
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(cx - faceW * 0.18f, cy - faceH * 0.15f),
                radius = faceW * 0.55f,
            ),
        )

        // Textura metálica (líneas de cepillado + paneles) recortada al rostro
        clipPath(head) {
            drawMetalTexture(cx, cy, faceW, faceH, accent, steel)
            drawFaceStructure(cx, cy, faceW, faceH, accent, steel)
            // Escaneo
            val scanY = cy - faceH * 0.52f + faceH * scan
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.12f), accent.copy(alpha = 0.02f), Color.Transparent),
                    startY = scanY - faceH * 0.06f,
                    endY = scanY + faceH * 0.06f,
                ),
                topLeft = Offset(cx - faceW, scanY - faceH * 0.06f),
                size = Size(faceW * 2f, faceH * 0.12f),
            )
        }

        // Contorno exterior
        drawPath(
            path = head,
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.65f), accent.copy(alpha = 0.25f), accent.copy(alpha = 0.45f)),
                startY = cy - faceH / 2f,
                endY = cy + faceH / 2f,
            ),
            style = Stroke(2.dp.toPx(), join = StrokeJoin.Round),
        )

        // Cuello metálico (debajo de mandíbula)
        drawNeck(cx, cy, faceW, faceH, metalMid, metalDark, accent, steel)

        // Rasgos faciales con proporciones humanas
        drawFacialFeatures(cx, cy, faceW, faceH, accent, steel, state, blink, mouthWave)

        // Indicadores laterales
        drawSidePlate(cx - faceW * 0.52f, cy - faceH * 0.05f, faceW, faceH, accent, steel, true)
        drawSidePlate(cx + faceW * 0.52f, cy - faceH * 0.05f, faceW, faceH, accent, steel, false)
    }
}

private fun headPath(cx: Float, cy: Float, w: Float, h: Float): Path {
    val left = cx - w / 2f
    val right = cx + w / 2f
    val top = cy - h / 2f
    val bottom = cy + h / 2f
    // Proporciones: frente más amplia, pómulos, mandíbula angulada, barbilla redondeada
    return Path().apply {
        moveTo(cx, top + h * 0.02f)
        // frente derecha
        cubicTo(cx + w * 0.22f, top, right - w * 0.02f, top + h * 0.10f, right, cy - h * 0.12f)
        // sien/pómulo derecho
        cubicTo(right + w * 0.01f, cy + h * 0.02f, right - w * 0.02f, cy + h * 0.18f, right - w * 0.08f, cy + h * 0.30f)
        // mandíbula derecha
        cubicTo(right - w * 0.14f, cy + h * 0.42f, cx + w * 0.20f, bottom - h * 0.02f, cx, bottom)
        // mandíbula izquierda
        cubicTo(cx - w * 0.20f, bottom - h * 0.02f, left + w * 0.14f, cy + h * 0.42f, left + w * 0.08f, cy + h * 0.30f)
        // pómulo/sien izquierda
        cubicTo(left + w * 0.02f, cy + h * 0.18f, left - w * 0.01f, cy + h * 0.02f, left, cy - h * 0.12f)
        // frente izquierda
        cubicTo(left + w * 0.02f, top + h * 0.10f, cx - w * 0.22f, top, cx, top + h * 0.02f)
        close()
    }
}

private fun DrawScope.drawMetalTexture(cx: Float, cy: Float, faceW: Float, faceH: Float, accent: Color, steel: Color) {
    val left = cx - faceW
    val right = cx + faceW
    val top = cy - faceH
    val bottom = cy + faceH
    // Cepillado horizontal fino
    var y = top
    var i = 0
    while (y < bottom) {
        val alpha = if (i % 3 == 0) 0.05f else 0.025f
        drawLine(
            color = steel.copy(alpha = alpha),
            start = Offset(left, y),
            end = Offset(right, y),
            strokeWidth = 1f,
        )
        y += faceH * 0.018f
        i++
    }
    // Bandas de brillo especular
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent),
            startX = cx - faceW * 0.35f,
            endX = cx - faceW * 0.05f,
        ),
        topLeft = Offset(cx - faceW, top),
        size = Size(faceW * 2f, faceH * 2f),
    )
    // Micro costuras/paneles hex-like sugeridos (pocas líneas)
    val seam = accent.copy(alpha = 0.06f)
    for (k in -2..2) {
        val yy = cy + k * faceH * 0.12f
        drawLine(seam, Offset(cx - faceW * 0.46f, yy), Offset(cx + faceW * 0.46f, yy), strokeWidth = 0.8f)
    }
}

private fun DrawScope.drawFaceStructure(cx: Float, cy: Float, faceW: Float, faceH: Float, accent: Color, steel: Color) {
    val line = accent.copy(alpha = 0.11f)
    // Línea media
    drawLine(line, Offset(cx, cy - faceH * 0.44f), Offset(cx, cy + faceH * 0.46f), 1.dp.toPx())
    // Pómulos
    drawLine(line, Offset(cx - faceW * 0.12f, cy - faceH * 0.02f), Offset(cx - faceW * 0.46f, cy + faceH * 0.10f), 1.dp.toPx())
    drawLine(line, Offset(cx + faceW * 0.12f, cy - faceH * 0.02f), Offset(cx + faceW * 0.46f, cy + faceH * 0.10f), 1.dp.toPx())
    // Contorno mandíbula interno
    drawLine(line, Offset(cx - faceW * 0.18f, cy + faceH * 0.22f), Offset(cx - faceW * 0.34f, cy + faceH * 0.34f), 1.dp.toPx())
    drawLine(line, Offset(cx + faceW * 0.18f, cy + faceH * 0.22f), Offset(cx + faceW * 0.34f, cy + faceH * 0.34f), 1.dp.toPx())
    drawLine(line, Offset(cx - faceW * 0.18f, cy + faceH * 0.22f), Offset(cx + faceW * 0.18f, cy + faceH * 0.22f), 1.dp.toPx())
    // Sienes
    drawLine(steel.copy(alpha = 0.10f), Offset(cx - faceW * 0.36f, cy - faceH * 0.22f), Offset(cx - faceW * 0.46f, cy - faceH * 0.08f), 1.dp.toPx())
    drawLine(steel.copy(alpha = 0.10f), Offset(cx + faceW * 0.36f, cy - faceH * 0.22f), Offset(cx + faceW * 0.46f, cy - faceH * 0.08f), 1.dp.toPx())
    // Remaches decorativos
    val rivet = steel.copy(alpha = 0.25f)
    val points = listOf(
        Offset(cx - faceW * 0.40f, cy - faceH * 0.28f),
        Offset(cx + faceW * 0.40f, cy - faceH * 0.28f),
        Offset(cx - faceW * 0.42f, cy + faceH * 0.18f),
        Offset(cx + faceW * 0.42f, cy + faceH * 0.18f),
    )
    points.forEach { p ->
        drawCircle(rivet, radius = faceW * 0.012f, center = p)
        drawCircle(accent.copy(alpha = 0.20f), radius = faceW * 0.005f, center = p)
    }
}

private fun DrawScope.drawNeck(cx: Float, cy: Float, faceW: Float, faceH: Float, mid: Color, dark: Color, accent: Color, steel: Color) {
    val neckTop = cy + faceH * 0.48f
    val neckW = faceW * 0.38f
    val neckH = faceH * 0.22f
    val path = Path().apply {
        moveTo(cx - neckW, neckTop)
        lineTo(cx - neckW * 1.15f, neckTop + neckH)
        lineTo(cx + neckW * 1.15f, neckTop + neckH)
        lineTo(cx + neckW, neckTop)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(mid, dark), neckTop, neckTop + neckH))
    drawPath(path, steel.copy(alpha = 0.12f), style = Stroke(1.dp.toPx()))
    // Segmentos de cuello
    for (i in 1..3) {
        val yy = neckTop + neckH * (i / 4f)
        val ww = neckW * (1f + 0.04f * i)
        drawLine(accent.copy(alpha = 0.10f), Offset(cx - ww, yy), Offset(cx + ww, yy), 1.dp.toPx())
    }
}

private fun DrawScope.drawFacialFeatures(
    cx: Float, cy: Float, faceW: Float, faceH: Float,
    accent: Color, steel: Color, state: AssistantState, blink: Float, mouthWave: Float,
) {
    // Proporciones humanas (tercios): ojos ~40% desde arriba, nariz base ~66%, boca ~80%
    val eyeY = cy - faceH * 0.10f
    val noseBaseY = cy + faceH * 0.16f
    val mouthY = cy + faceH * 0.30f

    // Cejas
    val browY = eyeY - faceH * 0.075f
    val browW = faceW * 0.19f
    val browTilt = if (state == AssistantState.THINKING) -faceH * 0.015f else 0f
    drawBrow(cx - faceW * 0.21f, browY + browTilt, browW, accent, left = true)
    drawBrow(cx + faceW * 0.21f, browY - browTilt, browW, accent, left = false)

    // Ojos (separación ~1 ancho de ojo)
    val eyeOffsetX = faceW * 0.21f
    val eyeRx = faceW * 0.105f
    val eyeRy = faceH * 0.034f
    val eyeOpen = when (state) {
        AssistantState.IDLE -> 0.55f + 0.45f * blink
        AssistantState.LISTENING -> 0.78f + 0.22f * blink
        AssistantState.THINKING -> 0.82f
        AssistantState.SPEAKING -> 0.90f
        AssistantState.ERROR -> 1f
    }
    drawRealisticEye(cx - eyeOffsetX, eyeY, eyeRx, eyeRy, eyeOpen, accent, state)
    drawRealisticEye(cx + eyeOffsetX, eyeY, eyeRx, eyeRy, eyeOpen, accent, state)

    // Nariz (proporción humana: puente desde entrecejo, base más ancha)
    drawNose(cx, eyeY + faceH * 0.02f, noseBaseY, faceW, faceH, steel, accent)

    // Surco nasolabial sutil
    drawLine(steel.copy(alpha = 0.08f), Offset(cx - faceW * 0.10f, noseBaseY + faceH * 0.02f), Offset(cx - faceW * 0.14f, mouthY - faceH * 0.02f), 1.dp.toPx())
    drawLine(steel.copy(alpha = 0.08f), Offset(cx + faceW * 0.10f, noseBaseY + faceH * 0.02f), Offset(cx + faceW * 0.14f, mouthY - faceH * 0.02f), 1.dp.toPx())

    // Boca
    drawRealisticMouth(cx, mouthY, faceW, faceH, accent, state, mouthWave, blink)
}

private fun DrawScope.drawBrow(cx: Float, cy: Float, w: Float, accent: Color, left: Boolean) {
    val dir = if (left) -1f else 1f
    val path = Path().apply {
        moveTo(cx - w, cy + w * 0.08f)
        quadraticBezierTo(cx, cy - w * 0.10f, cx + w, cy + w * 0.12f)
    }
    drawPath(path, color = accent.copy(alpha = 0.40f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    // placa de ceja metálica
    drawLine(
        color = Color(0xFF2A4658).copy(alpha = 0.6f),
        start = Offset(cx - w * 0.95f, cy + w * 0.12f),
        end = Offset(cx + w * 0.95f, cy + w * 0.16f),
        strokeWidth = 1.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawRealisticEye(cx: Float, cy: Float, rx: Float, ry: Float, open: Float, accent: Color, state: AssistantState) {
    val h = (ry * 2f * open).coerceAtLeast(ry * 0.3f)
    val tl = Offset(cx - rx, cy - h / 2f)
    val sz = Size(rx * 2f, h)
    val cr = CornerRadius(rx * 0.9f, rx * 0.9f)

    // Sombra de párpado/cuenca
    drawOval(
        brush = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent), Offset(cx, cy), rx * 1.5f),
        topLeft = Offset(cx - rx * 1.5f, cy - ry * 1.6f),
        size = Size(rx * 3f, ry * 3.2f),
    )
    // Globo ocular
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFE8F4FF).copy(alpha = 0.12f), Color(0xFF0A1822)), tl.y, tl.y + h),
        topLeft = tl, size = sz, cornerRadius = cr,
    )
    drawRoundRect(accent.copy(alpha = 0.50f), tl, sz, cr, style = Stroke(1.dp.toPx()))

    if (open > 0.22f) {
        val irisR = ry * 0.95f * open.coerceAtMost(1f)
        // Iris detallado
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.9f), accent, Color(0xFF063040)),
                center = Offset(cx, cy), radius = irisR,
            ),
            radius = irisR, center = Offset(cx, cy),
        )
        // Anillo limbal
        drawCircle(accent.copy(alpha = 0.7f), radius = irisR, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
        // Pupila
        val pupilR = irisR * (if (state == AssistantState.LISTENING) 0.52f else 0.40f)
        drawCircle(Color(0xFF010406), radius = pupilR, center = Offset(cx, cy))
        // Reflejos
        drawCircle(Color.White.copy(alpha = 0.95f), radius = irisR * 0.20f, center = Offset(cx - irisR * 0.22f, cy - irisR * 0.26f))
        drawCircle(Color.White.copy(alpha = 0.5f), radius = irisR * 0.09f, center = Offset(cx + irisR * 0.20f, cy + irisR * 0.18f))
    }
    // Párpado superior metálico
    val lidPath = Path().apply {
        moveTo(cx - rx, cy - h / 2f + h * 0.1f)
        quadraticBezierTo(cx, cy - h / 2f - h * 0.05f, cx + rx, cy - h / 2f + h * 0.1f)
    }
    drawPath(lidPath, Color(0xFF1A3446).copy(alpha = 0.7f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawNose(cx: Float, topY: Float, baseY: Float, faceW: Float, faceH: Float, steel: Color, accent: Color) {
    val bridgeW = faceW * 0.045f
    val baseW = faceW * 0.11f
    // Puente
    drawLine(steel.copy(alpha = 0.22f), Offset(cx - bridgeW * 0.3f, topY), Offset(cx - bridgeW, baseY - faceH * 0.03f), 1.4.dp.toPx(), cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.06f), Offset(cx + bridgeW * 0.4f, topY), Offset(cx + bridgeW * 0.2f, baseY - faceH * 0.04f), 1.dp.toPx(), cap = StrokeCap.Round)
    // Lados
    drawLine(steel.copy(alpha = 0.18f), Offset(cx - bridgeW, baseY - faceH * 0.03f), Offset(cx - baseW, baseY), 1.2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(steel.copy(alpha = 0.14f), Offset(cx + bridgeW * 0.2f, baseY - faceH * 0.04f), Offset(cx + baseW * 0.7f, baseY), 1.2.dp.toPx(), cap = StrokeCap.Round)
    // Punta/base
    drawLine(accent.copy(alpha = 0.12f), Offset(cx - baseW, baseY), Offset(cx + baseW * 0.7f, baseY + faceH * 0.005f), 1.dp.toPx(), cap = StrokeCap.Round)
    // Fosas
    drawOval(Color.Black.copy(alpha = 0.35f), topLeft = Offset(cx - baseW * 0.55f, baseY - faceH * 0.005f), size = Size(faceW * 0.035f, faceH * 0.012f))
    drawOval(Color.Black.copy(alpha = 0.30f), topLeft = Offset(cx + baseW * 0.05f, baseY - faceH * 0.003f), size = Size(faceW * 0.03f, faceH * 0.011f))
}

private fun DrawScope.drawRealisticMouth(cx: Float, cy: Float, faceW: Float, faceH: Float, accent: Color, state: AssistantState, wave: Float, blink: Float) {
    val mouthW = faceW * 0.22f
    val amp = when (state) {
        AssistantState.SPEAKING -> 0.55f + 0.45f * (sin(wave.toDouble()).toFloat() * 0.5f + 0.5f)
        AssistantState.LISTENING -> 0.18f + 0.12f * blink
        else -> 0.08f
    }
    val openH = faceH * (0.008f + 0.030f * amp)
    // Labios metálicos / marco
    val lipPath = Path().apply {
        moveTo(cx - mouthW, cy)
        quadraticBezierTo(cx, cy - faceH * 0.012f, cx + mouthW, cy)
        quadraticBezierTo(cx, cy + faceH * 0.018f + openH, cx - mouthW, cy)
        close()
    }
    drawPath(lipPath, brush = Brush.verticalGradient(listOf(Color(0xFF1A3344), Color(0xFF0C1C28)), cy - faceH * 0.02f, cy + faceH * 0.03f))
    drawPath(lipPath, accent.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
    // Línea central labios
    drawLine(Color(0xFF050C12).copy(alpha = 0.9f), Offset(cx - mouthW * 0.95f, cy + openH * 0.2f), Offset(cx + mouthW * 0.95f, cy + openH * 0.2f), 1.2.dp.toPx(), cap = StrokeCap.Round)
    // Rejilla de voz
    if (openH > faceH * 0.012f) {
        val bars = 7
        val gap = mouthW * 1.6f / bars
        for (i in 0 until bars) {
            val bx = cx - mouthW * 0.8f + i * gap
            val bhFactor = when (state) {
                AssistantState.SPEAKING -> 0.3f + 0.7f * (sin((wave + i * 0.8f).toDouble()).toFloat() * 0.5f + 0.5f)
                AssistantState.LISTENING -> 0.2f + 0.15f * blink
                else -> 0.15f
            }
            val bh = (openH * 1.3f * bhFactor).coerceAtLeast(1.dp.toPx())
            drawRoundRect(
                color = accent.copy(alpha = 0.30f + 0.55f * bhFactor),
                topLeft = Offset(bx, cy - bh * 0.15f),
                size = Size(gap * 0.45f, bh),
                cornerRadius = CornerRadius(gap * 0.2f, gap * 0.2f),
            )
        }
    }
}

private fun DrawScope.drawSidePlate(x: Float, y: Float, faceW: Float, faceH: Float, accent: Color, steel: Color, left: Boolean) {
    val dir = if (left) -1f else 1f
    val w = faceW * 0.055f
    val h = faceH * 0.16f
    val tl = Offset(if (left) x - w else x, y - h / 2f)
    drawRoundRect(Color(0xFF102331).copy(alpha = 0.8f), tl, Size(w, h), CornerRadius(w / 3f, w / 3f))
    drawRoundRect(accent.copy(alpha = 0.25f), tl, Size(w, h), CornerRadius(w / 3f, w / 3f), style = Stroke(1.dp.toPx()))
    drawLine(steel.copy(alpha = 0.25f), Offset(tl.x + w * 0.2f, tl.y + h * 0.3f), Offset(tl.x + w * 0.8f, tl.y + h * 0.3f), 1.dp.toPx())
    drawLine(steel.copy(alpha = 0.25f), Offset(tl.x + w * 0.2f, tl.y + h * 0.55f), Offset(tl.x + w * 0.8f, tl.y + h * 0.55f), 1.dp.toPx())
    drawCircle(accent.copy(alpha = 0.6f), radius = w * 0.12f, center = Offset(tl.x + w / 2f, tl.y + h * 0.78f))
}