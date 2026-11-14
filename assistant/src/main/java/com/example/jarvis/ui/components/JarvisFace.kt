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
import kotlin.math.sin

/**
 * Androide de medio cuerpo de JARVIS: cabeza, cuello, hombros y torso
 * con proporciones humanas, textura metálica y núcleo reactivo al estado.
 */
@Composable
fun JarvisFace(
    state: AssistantState,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "jarvis-body")

    val breathe by transition.animateFloat(
        initialValue = 0.994f,
        targetValue = 1.006f,
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
    val corePulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "core",
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

    val metalDark = Color(0xFF08131C)
    val metalMid = Color(0xFF122433)
    val metalLight = Color(0xFF1E3A4D)
    val steel = Color(0xFF8FB6C8)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Escala general del cuerpo dentro del canvas
        val bodyH = h * 0.92f * breathe
        val headH = bodyH * 0.34f
        val headW = headH * 0.72f
        val headCy = h * 0.22f
        val neckTop = headCy + headH * 0.46f
        val shoulderY = neckTop + bodyH * 0.08f
        val torsoBottom = h * 0.96f
        val torsoW = bodyH * 0.62f

        // Halo ambiental
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.04f), Color.Transparent),
                center = Offset(cx, h * 0.45f),
                radius = w * 0.55f,
            ),
            radius = w * 0.55f,
            center = Offset(cx, h * 0.45f),
        )

        // Anillo HUD detrás de cabeza
        rotate(ringRot, pivot = Offset(cx, headCy)) {
            drawArc(accent.copy(alpha = 0.22f), -30f, 70f, false,
                Offset(cx - headW * 0.85f, headCy - headH * 0.55f), Size(headW * 1.7f, headH * 1.1f),
                style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
        }
        rotate(-ringRot * 0.6f, pivot = Offset(cx, headCy)) {
            drawArc(steel.copy(alpha = 0.12f), 140f, 60f, false,
                Offset(cx - headW * 0.78f, headCy - headH * 0.50f), Size(headW * 1.56f, headH * 1.0f),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round))
        }

        // Torso y hombros
        drawTorso(cx, shoulderY, torsoW, torsoBottom, metalDark, metalMid, metalLight, steel, accent, corePulse, state)

        // Cuello
        drawNeck(cx, neckTop, headW, shoulderY, metalMid, metalDark, steel, accent)

        // Cabeza
        val head = headPath(cx, headCy, headW, headH)
        drawPath(head, Brush.linearGradient(
            colors = listOf(metalLight, metalMid, metalDark, metalMid),
            start = Offset(cx - headW * 0.5f, headCy - headH * 0.5f),
            end = Offset(cx + headW * 0.5f, headCy + headH * 0.5f),
        ))
        drawPath(head, Brush.verticalGradient(
            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent, Color.Black.copy(alpha = 0.22f)),
            startY = headCy - headH / 2f, endY = headCy + headH / 2f,
        ))
        drawPath(head, Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.09f), Color.Transparent),
            center = Offset(cx - headW * 0.18f, headCy - headH * 0.12f),
            radius = headW * 0.55f,
        ))

        clipPath(head) {
            drawMetalTexture(cx, headCy, headW, headH, accent, steel)
            drawFaceStructure(cx, headCy, headW, headH, accent, steel)
            val scanY = headCy - headH * 0.52f + headH * scan
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.12f), Color.Transparent),
                    startY = scanY - headH * 0.06f, endY = scanY + headH * 0.06f,
                ),
                topLeft = Offset(cx - headW, scanY - headH * 0.06f),
                size = Size(headW * 2f, headH * 0.12f),
            )
        }

        drawPath(head, Brush.verticalGradient(
            colors = listOf(accent.copy(alpha = 0.60f), accent.copy(alpha = 0.22f), accent.copy(alpha = 0.40f)),
            startY = headCy - headH / 2f, endY = headCy + headH / 2f,
        ), style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))

        // Rasgos faciales
        drawFacialFeatures(cx, headCy, headW, headH, accent, steel, state, blink, mouthWave)

        // Placas laterales cabeza
        drawSidePlate(cx - headW * 0.52f, headCy, headW, headH, accent, steel, true)
        drawSidePlate(cx + headW * 0.52f, headCy, headW, headH, accent, steel, false)
    }
}

private fun headPath(cx: Float, cy: Float, w: Float, h: Float): Path {
    val left = cx - w / 2f
    val right = cx + w / 2f
    val top = cy - h / 2f
    val bottom = cy + h / 2f
    return Path().apply {
        moveTo(cx, top + h * 0.02f)
        cubicTo(cx + w * 0.22f, top, right - w * 0.02f, top + h * 0.10f, right, cy - h * 0.12f)
        cubicTo(right + w * 0.01f, cy + h * 0.02f, right - w * 0.02f, cy + h * 0.18f, right - w * 0.08f, cy + h * 0.30f)
        cubicTo(right - w * 0.14f, cy + h * 0.42f, cx + w * 0.20f, bottom - h * 0.02f, cx, bottom)
        cubicTo(cx - w * 0.20f, bottom - h * 0.02f, left + w * 0.14f, cy + h * 0.42f, left + w * 0.08f, cy + h * 0.30f)
        cubicTo(left + w * 0.02f, cy + h * 0.18f, left - w * 0.01f, cy + h * 0.02f, left, cy - h * 0.12f)
        cubicTo(left + w * 0.02f, top + h * 0.10f, cx - w * 0.22f, top, cx, top + h * 0.02f)
        close()
    }
}

private fun DrawScope.drawTorso(
    cx: Float, shoulderY: Float, torsoW: Float, bottom: Float,
    dark: Color, mid: Color, light: Color, steel: Color, accent: Color,
    corePulse: Float, state: AssistantState,
) {
    val topW = torsoW * 0.48f
    val midW = torsoW * 0.55f
    val botW = torsoW * 0.42f
    val torsoPath = Path().apply {
        moveTo(cx - topW, shoulderY)
        // hombro izquierdo
        cubicTo(cx - torsoW * 0.52f, shoulderY + (bottom - shoulderY) * 0.05f, cx - midW, shoulderY + (bottom - shoulderY) * 0.35f, cx - botW, bottom)
        lineTo(cx + botW, bottom)
        // lado derecho
        cubicTo(cx + midW, shoulderY + (bottom - shoulderY) * 0.35f, cx + torsoW * 0.52f, shoulderY + (bottom - shoulderY) * 0.05f, cx + topW, shoulderY)
        close()
    }
    drawPath(torsoPath, Brush.verticalGradient(listOf(light, mid, dark), shoulderY, bottom))
    drawPath(torsoPath, Brush.horizontalGradient(
        listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent, Color.Black.copy(alpha = 0.18f)),
        cx - torsoW * 0.55f, cx + torsoW * 0.55f,
    ))
    clipPath(torsoPath) {
        // Textura de placas
        var y = shoulderY
        var i = 0
        while (y < bottom) {
            val a = if (i % 4 == 0) 0.05f else 0.02f
            drawLine(steel.copy(alpha = a), Offset(cx - torsoW, y), Offset(cx + torsoW, y), 1f)
            y += (bottom - shoulderY) * 0.03f
            i++
        }
        // Líneas de armadura pectoral
        val chestY = shoulderY + (bottom - shoulderY) * 0.28f
        drawLine(accent.copy(alpha = 0.14f), Offset(cx, shoulderY + (bottom - shoulderY) * 0.05f), Offset(cx, bottom - (bottom - shoulderY) * 0.08f), 1.2.dp.toPx())
        drawLine(accent.copy(alpha = 0.12f), Offset(cx - torsoW * 0.10f, shoulderY + (bottom - shoulderY) * 0.12f), Offset(cx - torsoW * 0.38f, chestY), 1.dp.toPx())
        drawLine(accent.copy(alpha = 0.12f), Offset(cx + torsoW * 0.10f, shoulderY + (bottom - shoulderY) * 0.12f), Offset(cx + torsoW * 0.38f, chestY), 1.dp.toPx())
        drawLine(accent.copy(alpha = 0.10f), Offset(cx - torsoW * 0.38f, chestY), Offset(cx - torsoW * 0.30f, bottom - (bottom - shoulderY) * 0.15f), 1.dp.toPx())
        drawLine(accent.copy(alpha = 0.10f), Offset(cx + torsoW * 0.38f, chestY), Offset(cx + torsoW * 0.30f, bottom - (bottom - shoulderY) * 0.15f), 1.dp.toPx())
        drawArc(accent.copy(alpha = 0.10f), 20f, 140f, false,
            Offset(cx - torsoW * 0.28f, chestY - torsoW * 0.06f), Size(torsoW * 0.56f, torsoW * 0.30f),
            style = Stroke(1.dp.toPx()))
    }
    drawPath(torsoPath, accent.copy(alpha = 0.28f), style = Stroke(1.8.dp.toPx(), join = StrokeJoin.Round))

    // Hombros (pauldrons)
    drawShoulder(cx - torsoW * 0.52f, shoulderY + (bottom - shoulderY) * 0.06f, torsoW, bottom - shoulderY, dark, mid, steel, accent, left = true)
    drawShoulder(cx + torsoW * 0.52f, shoulderY + (bottom - shoulderY) * 0.06f, torsoW, bottom - shoulderY, dark, mid, steel, accent, left = false)

    // Núcleo del reactor (pecho)
    val coreCy = shoulderY + (bottom - shoulderY) * 0.38f
    val coreR = torsoW * 0.11f * corePulse
    val glow = if (state == AssistantState.THINKING || state == AssistantState.SPEAKING) 1f else 0.7f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.35f * glow), accent.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(cx, coreCy), radius = coreR * 2.8f,
        ),
        radius = coreR * 2.8f, center = Offset(cx, coreCy),
    )
    drawCircle(Color(0xFF071018), radius = coreR * 1.25f, center = Offset(cx, coreCy))
    drawCircle(accent.copy(alpha = 0.7f * glow), radius = coreR * 1.25f, center = Offset(cx, coreCy), style = Stroke(1.5.dp.toPx()))
    // anillo interior
    drawCircle(accent.copy(alpha = 0.55f), radius = coreR * 0.85f, center = Offset(cx, coreCy), style = Stroke(1.dp.toPx()))
    // segmentos de reactor
    for (i in 0 until 6) {
        val a = i * 60f - 90f
        drawArc(accent.copy(alpha = 0.75f * glow), a + 8f, 44f, false,
            Offset(cx - coreR, coreCy - coreR), Size(coreR * 2f, coreR * 2f),
            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.95f), accent.copy(alpha = 0.6f), Color.Transparent), Offset(cx, coreCy), coreR * 0.55f),
        radius = coreR * 0.55f, center = Offset(cx, coreCy),
    )
}

private fun DrawScope.drawShoulder(cx: Float, cy: Float, torsoW: Float, torsoH: Float, dark: Color, mid: Color, steel: Color, accent: Color, left: Boolean) {
    val dir = if (left) -1f else 1f
    val sw = torsoW * 0.22f
    val sh = torsoH * 0.22f
    val path = Path().apply {
        moveTo(cx, cy - sh * 0.5f)
        cubicTo(cx + dir * sw * 0.6f, cy - sh * 0.45f, cx + dir * sw, cy - sh * 0.1f, cx + dir * sw * 0.85f, cy + sh * 0.4f)
        cubicTo(cx + dir * sw * 0.5f, cy + sh * 0.55f, cx + dir * sw * 0.1f, cy + sh * 0.35f, cx, cy + sh * 0.1f)
        close()
    }
    drawPath(path, Brush.linearGradient(listOf(mid, dark), Offset(cx, cy - sh), Offset(cx + dir * sw, cy + sh)))
    drawPath(path, accent.copy(alpha = 0.22f), style = Stroke(1.2.dp.toPx()))
    drawLine(steel.copy(alpha = 0.20f), Offset(cx + dir * sw * 0.15f, cy - sh * 0.1f), Offset(cx + dir * sw * 0.65f, cy + sh * 0.05f), 1.dp.toPx())
    drawCircle(accent.copy(alpha = 0.35f), radius = sw * 0.06f, center = Offset(cx + dir * sw * 0.55f, cy))
}

private fun DrawScope.drawNeck(cx: Float, top: Float, headW: Float, shoulderY: Float, mid: Color, dark: Color, steel: Color, accent: Color) {
    val neckW = headW * 0.28f
    val bottom = shoulderY + (shoulderY - top) * 0.15f
    val path = Path().apply {
        moveTo(cx - neckW, top)
        lineTo(cx - neckW * 1.25f, bottom)
        lineTo(cx + neckW * 1.25f, bottom)
        lineTo(cx + neckW, top)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(mid, dark), top, bottom))
    drawPath(path, steel.copy(alpha = 0.14f), style = Stroke(1.dp.toPx()))
    for (i in 1..3) {
        val yy = top + (bottom - top) * (i / 4f)
        val ww = neckW * (1f + 0.07f * i)
        drawLine(accent.copy(alpha = 0.10f), Offset(cx - ww, yy), Offset(cx + ww, yy), 1.dp.toPx())
    }
    // Cables/detalle lateral cuello
    drawLine(accent.copy(alpha = 0.18f), Offset(cx - neckW * 0.9f, top + (bottom - top) * 0.2f), Offset(cx - neckW * 1.1f, bottom - (bottom - top) * 0.2f), 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(accent.copy(alpha = 0.18f), Offset(cx + neckW * 0.9f, top + (bottom - top) * 0.2f), Offset(cx + neckW * 1.1f, bottom - (bottom - top) * 0.2f), 1.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawMetalTexture(cx: Float, cy: Float, faceW: Float, faceH: Float, accent: Color, steel: Color) {
    val left = cx - faceW
    val right = cx + faceW
    val top = cy - faceH
    val bottom = cy + faceH
    var y = top
    var i = 0
    while (y < bottom) {
        val alpha = if (i % 3 == 0) 0.045f else 0.02f
        drawLine(steel.copy(alpha = alpha), Offset(left, y), Offset(right, y), 1f)
        y += faceH * 0.02f
        i++
    }
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent),
            startX = cx - faceW * 0.35f, endX = cx - faceW * 0.05f,
        ),
        topLeft = Offset(left, top), size = Size(faceW * 2f, faceH * 2f),
    )
}

private fun DrawScope.drawFaceStructure(cx: Float, cy: Float, faceW: Float, faceH: Float, accent: Color, steel: Color) {
    val line = accent.copy(alpha = 0.10f)
    drawLine(line, Offset(cx, cy - faceH * 0.44f), Offset(cx, cy + faceH * 0.46f), 1.dp.toPx())
    drawLine(line, Offset(cx - faceW * 0.12f, cy - faceH * 0.02f), Offset(cx - faceW * 0.46f, cy + faceH * 0.10f), 1.dp.toPx())
    drawLine(line, Offset(cx + faceW * 0.12f, cy - faceH * 0.02f), Offset(cx + faceW * 0.46f, cy + faceH * 0.10f), 1.dp.toPx())
    drawLine(line, Offset(cx - faceW * 0.18f, cy + faceH * 0.22f), Offset(cx - faceW * 0.34f, cy + faceH * 0.34f), 1.dp.toPx())
    drawLine(line, Offset(cx + faceW * 0.18f, cy + faceH * 0.22f), Offset(cx + faceW * 0.34f, cy + faceH * 0.34f), 1.dp.toPx())
    drawLine(line, Offset(cx - faceW * 0.18f, cy + faceH * 0.22f), Offset(cx + faceW * 0.18f, cy + faceH * 0.22f), 1.dp.toPx())
    val rivet = steel.copy(alpha = 0.22f)
    listOf(
        Offset(cx - faceW * 0.40f, cy - faceH * 0.28f),
        Offset(cx + faceW * 0.40f, cy - faceH * 0.28f),
        Offset(cx - faceW * 0.42f, cy + faceH * 0.18f),
        Offset(cx + faceW * 0.42f, cy + faceH * 0.18f),
    ).forEach { p -> drawCircle(rivet, radius = faceW * 0.012f, center = p) }
}

private fun DrawScope.drawFacialFeatures(
    cx: Float, cy: Float, faceW: Float, faceH: Float,
    accent: Color, steel: Color, state: AssistantState, blink: Float, mouthWave: Float,
) {
    val eyeY = cy - faceH * 0.10f
    val noseBaseY = cy + faceH * 0.16f
    val mouthY = cy + faceH * 0.30f
    val browY = eyeY - faceH * 0.075f
    val browW = faceW * 0.19f
    val browTilt = if (state == AssistantState.THINKING) -faceH * 0.015f else 0f
    drawBrow(cx - faceW * 0.21f, browY + browTilt, browW, accent, true)
    drawBrow(cx + faceW * 0.21f, browY - browTilt, browW, accent, false)

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
    drawNose(cx, eyeY + faceH * 0.02f, noseBaseY, faceW, faceH, steel, accent)
    drawRealisticMouth(cx, mouthY, faceW, faceH, accent, state, mouthWave, blink)
}

private fun DrawScope.drawBrow(cx: Float, cy: Float, w: Float, accent: Color, left: Boolean) {
    val path = Path().apply {
        moveTo(cx - w, cy + w * 0.08f)
        quadraticBezierTo(cx, cy - w * 0.10f, cx + w, cy + w * 0.12f)
    }
    drawPath(path, color = accent.copy(alpha = 0.40f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    drawLine(Color(0xFF2A4658).copy(alpha = 0.6f), Offset(cx - w * 0.95f, cy + w * 0.12f), Offset(cx + w * 0.95f, cy + w * 0.16f), 1.2.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawRealisticEye(cx: Float, cy: Float, rx: Float, ry: Float, open: Float, accent: Color, state: AssistantState) {
    val h = (ry * 2f * open).coerceAtLeast(ry * 0.3f)
    val tl = Offset(cx - rx, cy - h / 2f)
    val sz = Size(rx * 2f, h)
    val cr = CornerRadius(rx * 0.9f, rx * 0.9f)
    drawOval(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent), Offset(cx, cy), rx * 1.5f),
        topLeft = Offset(cx - rx * 1.5f, cy - ry * 1.6f), size = Size(rx * 3f, ry * 3.2f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE8F4FF).copy(alpha = 0.12f), Color(0xFF0A1822)), tl.y, tl.y + h), tl, sz, cr)
    drawRoundRect(accent.copy(alpha = 0.50f), tl, sz, cr, style = Stroke(1.dp.toPx()))
    if (open > 0.22f) {
        val irisR = ry * 0.95f * open.coerceAtMost(1f)
        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.9f), accent, Color(0xFF063040)), Offset(cx, cy), irisR), irisR, Offset(cx, cy))
        drawCircle(accent.copy(alpha = 0.7f), irisR, Offset(cx, cy), style = Stroke(1.dp.toPx()))
        val pupilR = irisR * (if (state == AssistantState.LISTENING) 0.52f else 0.40f)
        drawCircle(Color(0xFF010406), pupilR, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.95f), irisR * 0.20f, Offset(cx - irisR * 0.22f, cy - irisR * 0.26f))
        drawCircle(Color.White.copy(alpha = 0.5f), irisR * 0.09f, Offset(cx + irisR * 0.20f, cy + irisR * 0.18f))
    }
    val lidPath = Path().apply {
        moveTo(cx - rx, cy - h / 2f + h * 0.1f)
        quadraticBezierTo(cx, cy - h / 2f - h * 0.05f, cx + rx, cy - h / 2f + h * 0.1f)
    }
    drawPath(lidPath, Color(0xFF1A3446).copy(alpha = 0.7f), style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawNose(cx: Float, topY: Float, baseY: Float, faceW: Float, faceH: Float, steel: Color, accent: Color) {
    val bridgeW = faceW * 0.045f
    val baseW = faceW * 0.11f
    drawLine(steel.copy(alpha = 0.22f), Offset(cx - bridgeW * 0.3f, topY), Offset(cx - bridgeW, baseY - faceH * 0.03f), 1.4.dp.toPx(), cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.06f), Offset(cx + bridgeW * 0.4f, topY), Offset(cx + bridgeW * 0.2f, baseY - faceH * 0.04f), 1.dp.toPx(), cap = StrokeCap.Round)
    drawLine(steel.copy(alpha = 0.18f), Offset(cx - bridgeW, baseY - faceH * 0.03f), Offset(cx - baseW, baseY), 1.2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(steel.copy(alpha = 0.14f), Offset(cx + bridgeW * 0.2f, baseY - faceH * 0.04f), Offset(cx + baseW * 0.7f, baseY), 1.2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(accent.copy(alpha = 0.12f), Offset(cx - baseW, baseY), Offset(cx + baseW * 0.7f, baseY + faceH * 0.005f), 1.dp.toPx(), cap = StrokeCap.Round)
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
    val lipPath = Path().apply {
        moveTo(cx - mouthW, cy)
        quadraticBezierTo(cx, cy - faceH * 0.012f, cx + mouthW, cy)
        quadraticBezierTo(cx, cy + faceH * 0.018f + openH, cx - mouthW, cy)
        close()
    }
    drawPath(lipPath, Brush.verticalGradient(listOf(Color(0xFF1A3344), Color(0xFF0C1C28)), cy - faceH * 0.02f, cy + faceH * 0.03f))
    drawPath(lipPath, accent.copy(alpha = 0.35f), style = Stroke(1.dp.toPx()))
    drawLine(Color(0xFF050C12).copy(alpha = 0.9f), Offset(cx - mouthW * 0.95f, cy + openH * 0.2f), Offset(cx + mouthW * 0.95f, cy + openH * 0.2f), 1.2.dp.toPx(), cap = StrokeCap.Round)
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
            drawRoundRect(accent.copy(alpha = 0.30f + 0.55f * bhFactor), topLeft = Offset(bx, cy - bh * 0.15f), size = Size(gap * 0.45f, bh), cornerRadius = CornerRadius(gap * 0.2f, gap * 0.2f))
        }
    }
}

private fun DrawScope.drawSidePlate(x: Float, y: Float, faceW: Float, faceH: Float, accent: Color, steel: Color, left: Boolean) {
    val w = faceW * 0.055f
    val h = faceH * 0.16f
    val tl = Offset(if (left) x - w else x, y - h / 2f)
    drawRoundRect(Color(0xFF102331).copy(alpha = 0.8f), tl, Size(w, h), CornerRadius(w / 3f, w / 3f))
    drawRoundRect(accent.copy(alpha = 0.25f), tl, Size(w, h), CornerRadius(w / 3f, w / 3f), style = Stroke(1.dp.toPx()))
    drawLine(steel.copy(alpha = 0.25f), Offset(tl.x + w * 0.2f, tl.y + h * 0.3f), Offset(tl.x + w * 0.8f, tl.y + h * 0.3f), 1.dp.toPx())
    drawLine(steel.copy(alpha = 0.25f), Offset(tl.x + w * 0.2f, tl.y + h * 0.55f), Offset(tl.x + w * 0.8f, tl.y + h * 0.55f), 1.dp.toPx())
    drawCircle(accent.copy(alpha = 0.6f), radius = w * 0.12f, center = Offset(tl.x + w / 2f, tl.y + h * 0.78f))
}