package com.example.jarvis.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.jarvis.domain.model.AssistantState
import com.example.jarvis.ui.JarvisColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Cara android de JARVIS: expresión basada en el estado del asistente.
 *
 * - Ojos que cambian de color e intensidad según LISTENING/THINKING/SPEAKING.
 * - Boca/visor que se anima cuando habla.
 * - Halo estilo Iron Man con el color del estado actual.
 */
@Composable
fun JarvisFace(
    state: AssistantState,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "jarvis-face")

    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "parpadeo",
    )

    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respirar",
    )

    val mouthWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
        ),
        label = "boca",
    )

    val accent by animateColorAsState(
        targetValue = when (state) {
            AssistantState.LISTENING -> JarvisColors.Cyan
            AssistantState.THINKING -> JarvisColors.Primary
            AssistantState.SPEAKING -> JarvisColors.Ice
            AssistantState.ERROR -> JarvisColors.Red
            AssistantState.IDLE -> JarvisColors.Primary
        },
        animationSpec = tween(600),
        label = "colorCara",
    )

    Canvas(modifier.aspectRatio(1f)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.78f * breathe

        // Halo exterior
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.22f),
                    accent.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                center = center,
                radius = size.minDimension / 2f,
            ),
            radius = size.minDimension / 2f,
            center = center,
        )

        // Contorno de la cara
        drawCircle(
            color = accent.copy(alpha = 0.25f),
            radius = radius,
            center = center,
            style = Stroke(2.dp.toPx()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    JarvisColors.PanelHigh.copy(alpha = 0.95f),
                    JarvisColors.Background.copy(alpha = 0.98f),
                ),
                center = center,
                radius = radius,
            ),
            radius = radius * 0.98f,
            center = center,
        )

        // Línea central vertical (detalle estilo visor)
        drawLine(
            color = accent.copy(alpha = 0.18f),
            start = Offset(center.x, center.y - radius * 0.85f),
            end = Offset(center.x, center.y + radius * 0.85f),
            strokeWidth = 1.dp.toPx(),
        )

        // Ojos
        val eyeY = center.y - radius * 0.18f
        val eyeXOffset = radius * 0.28f
        val eyeRadius = radius * 0.11f
        val eyeAlpha = when (state) {
            AssistantState.LISTENING -> 0.6f + 0.4f * blink
            AssistantState.THINKING -> 0.95f
            AssistantState.SPEAKING -> 0.85f
            AssistantState.ERROR -> 0.9f
            AssistantState.IDLE -> 0.55f + 0.25f * blink
        }

        // Ojo izquierdo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = eyeAlpha),
                    accent.copy(alpha = eyeAlpha * 0.3f),
                    Color.Transparent,
                ),
                center = Offset(center.x - eyeXOffset, eyeY),
                radius = eyeRadius * 1.6f,
            ),
            radius = eyeRadius * 1.6f,
            center = Offset(center.x - eyeXOffset, eyeY),
        )
        drawCircle(
            color = accent.copy(alpha = eyeAlpha),
            radius = eyeRadius,
            center = Offset(center.x - eyeXOffset, eyeY),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = eyeRadius * 0.35f,
            center = Offset(center.x - eyeXOffset, eyeY),
        )

        // Ojo derecho
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = eyeAlpha),
                    accent.copy(alpha = eyeAlpha * 0.3f),
                    Color.Transparent,
                ),
                center = Offset(center.x + eyeXOffset, eyeY),
                radius = eyeRadius * 1.6f,
            ),
            radius = eyeRadius * 1.6f,
            center = Offset(center.x + eyeXOffset, eyeY),
        )
        drawCircle(
            color = accent.copy(alpha = eyeAlpha),
            radius = eyeRadius,
            center = Offset(center.x + eyeXOffset, eyeY),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = eyeRadius * 0.35f,
            center = Offset(center.x + eyeXOffset, eyeY),
        )

        // Boca / visor de voz
        val mouthY = center.y + radius * 0.28f
        val mouthWidth = radius * 0.55f
        val baseHeight = radius * 0.06f
        val speakAmp = if (state == AssistantState.SPEAKING) {
            (sin(mouthWave.toDouble()).toFloat() * 0.5f + 0.5f)
        } else if (state == AssistantState.LISTENING) {
            0.25f + 0.15f * blink
        } else {
            0.08f
        }
        val mouthHeight = baseHeight + radius * 0.18f * speakAmp

        // Barras de voz (estilo ecualizador)
        val bars = 9
        val barWidth = mouthWidth * 1.6f / (bars * 2f - 1f)
        val startX = center.x - mouthWidth * 0.8f
        for (i in 0 until bars) {
            val phase = mouthWave + i * 0.6f
            val barAmp = if (state == AssistantState.SPEAKING) {
                (sin(phase.toDouble()).toFloat() * 0.5f + 0.5f)
            } else if (state == AssistantState.THINKING) {
                0.3f + 0.2f * (sin((mouthWave * 0.5f + i * 0.8f).toDouble()).toFloat() * 0.5f + 0.5f)
            } else {
                0.15f
            }
            val h = baseHeight + radius * 0.22f * barAmp
            val x = startX + i * barWidth * 2f
            drawRoundRect(
                color = accent.copy(alpha = 0.4f + 0.5f * barAmp),
                topLeft = Offset(x, mouthY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }

        // Reactor central pequeño (detalle Iron Man)
        val reactorR = radius * 0.06f
        drawCircle(
            color = accent.copy(alpha = 0.7f),
            radius = reactorR,
            center = center,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = reactorR * 0.4f,
            center = center,
        )
    }
}
