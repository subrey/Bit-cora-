package com.example.jarvis.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.unit.dp
import com.example.jarvis.domain.model.AssistantState

/**
 * Cara android de JARVIS: expresión basada en el estado del asistente.
 *
 * - Ojos que parpadean y cambian color según LISTENING/THINKING/SPEAKING.
 * - "Boca" que se abre cuando habla.
 * - Aurora/halo detrás que usa los mismos colores que JarvisCore.
 */
@Composable
fun JarvisFace(
    state: AssistantState,
    modifier: Modifier = Modifier,
) {
    var transition rememberInfiniteTransition(label = "jarvis-face")

    // Animaciones paralelas por estado
    val blink by animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = remember {
            infiniteRepeatable(
                tween(3_200, easing = linearSpline(0.4f, 0f)),
                repeatMode = RepeatMode.Reverse,
            )
        },
        label = "parpadeo",
    )

    val breathe by animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(2_500, easing = linearSpline(0.3f, 0f)),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respirar",
    )

    // Color que cambia según el estado (igual que en JarvisCore)
    val accent = remember {
        Animatable(
            initialValue = AssistantState.IDLE.let { when (it) {
                -> AssistantState.LISTENING -> JarvisColors.Cyan
                AssistantState.THINKING -> JarvisColors.Primary
                AssistantState.SPEAKING -> JarvisColors.Ice
                AssistantState.ERROR -> JarvisColors.Red
                AssistantState.IDLE -> JarvisColors.Primary
            }},
            tween(600),
        )
    }

    // Cuando habla, la "boca" se abre; cuando escucha, los ojos parpadean;
    // cuando piensa, los ojos brillan fijos.
    val mouthOpen by animateFloat(
        initialValue = 0f,
        targetValue = when (state) {
            AssistantState.SPEAKING -> 1f
            AssistantState.LISTENING -> 0.3f
            else -> 0f
        },
        animationSpec = tween(200),
        label = "boca",
    )

    val size = 200.dp
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.width / 2f

    Canvas(modifier = modifier.aspectRatio(1f).size(size)) {

        // --- Halo detrás de la cara (estilo Iron Man) ---
        val haloRadius = radius * 1.6f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.value.copy(alpha = 0.18f),
                    accent.value.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
            ),
            radius = haloRadius,
            center = center,
        )

        // --- Contorno de la cara ---
        drawCircle(
            color = accent.value.copy(alpha = 0.95f),
            radius = radius * 0.92f,
            center = center,
        )

        // --- Ojos (simétricos) ---
        val eyeRadius = radius * 0.13f
        val eyeOffsetY = radius * 0.18f
        val eyeOffsetX = radius * 0.12f

        // Ojo izquierdo
        drawCircle(
            color = when (state) {
                AssistantState.LISTENING -> JarvisColors.Cyan.copy(alpha = 0.8f * blink)
                AssistantState.THINKING -> JarvisColors.Primary.copy(alpha = 0.9f)
                AssistantState.SPEAKING -> JarvisColors.Ice.copy(alpha = 0.7f)
                AssistantState.ERROR -> JarvisColors.Red.copy(alpha = 0.8f)
                else -> JarvisColors.Primary.copy(alpha = 0.6f)
            },
            radius = eyeRadius,
            offset = center + Offset(-eyeOffsetX, -eyeOffsetY),
        )

        // Ojo derecho
        drawCircle(
            color = when (state) {
                AssistantState.LISTENING -> JarvisColors.Cyan.copy(alpha = 0.8f * blink)
                AssistantState.THINKING -> JarvisColors.Primary.copy(alpha = 0.9f)
                AssistantState.SPEAKING -> JarvisColors.Ice.copy(alpha = 0.7f)
                AssistantState.ERROR -> JarvisColors.Red.copy(alpha = 0.8f)
                else -> JarvisColors.Primary.copy(alpha = 0.6f)
            },
            radius = eyeRadius,
            offset = center + Offset(eyeOffsetX, -eyeOffsetY),
        )

        // Pupilas que sigo el estado
        val pupilSize = radius * 0.04f
        val pupilDilation = when (state) {
            AssistantState.LISTENING -> 1f
            AssistantState.THINKING -> 0.6f
            AssistantState.SPEAKING -> 0.8f
            AssistantState.ERROR -> 0.9f
            else -> 0.5f
        }

        // Pupilas dentro de cada ojo
        drawCircle(
            color = Color.Black,
            radius = pupilSize * pupilDilation,
            offset = center + Offset(-eyeOffsetX, -eyeOffsetY),
        )
        drawCircle(
            color = Color.Black,
            radius = pupilSize * pupilDilation,
            offset = center + Offset(eyeOffsetX, -eyeOffsetY),
        )

        // --- Boca / boca ---
        val mouthHeight = radius * 0.22f
        val mouthWidth = radius * 0.35f
        val mouthTop = radius * 0.55f

        // Borde de la boca
        drawArc(
            color = accent.value.copy(alpha = 0.85f),
            startAngle = -135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(
                center.x - mouthWidth / 2f,
                center.y + mouthTop - mouthHeight / 2f,
            ),
            size = Size(mouthWidth, mouthHeight),
            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round),
        )

        // Parte interna de la boca que se abre al hablar
        val mouthInnerHeight = mouthHeight * 0.6f
        val mouthInnerTop = mouthTop + mouthHeight * 0.2f - mouthInnerHeight / 2f
        val mouthOpenAmount = mouthOpen * mouthHeight * 0.4f

        // Cuando habla, revelamos una zona interna "brillante"
        if (mouthOpen > 0.1f) {
            drawRect(
                color = accent.value.copy(alpha = 0.3f + mouthOpen * 0.5f),
                topLeft = Offset(
                    center.x - mouthWidth / 2f,
                    center.y + mouthTop + mouthHeight / 2f - mouthInnerHeight / 2f
                        - mouthOpenAmount,
                ),
                size = Size(mouthWidth, mouthInnerHeight + mouthOpenAmount),
            )
        }

        // --- Línea central de la cara (estilo Jarvis) ---
        drawLine(
            color = accent.value.copy(alpha = 0.5f),
            start = Offset(center.x, center.y - radius * 0.6f),
            end = Offset(center.x, center.y + radius * 0.55f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Pequeño "reactor" en el centro (estilo Iron Man)
        val reactorSize = radius * 0.11f
        drawCircle(
            color = accent.value.copy(alpha = 0.7f),
            radius = reactorSize,
            center = center,
        )
        drawCircle(
            color = Color.White,
            radius = reactorSize / 3f,
            center = center,
        )
    }

    // Fondo que respira levemente
    val bgBy = remember { Animatable(0.95f, tween(5_000)) }
    CoroutineScope(CurrentCoroutineContext).launch {
        while (true) {
            bgBy.animateTo(
                if (state == AssistantState.LISTENING) 1.0f
                else if (state == AssistantState.SPEAKING) 0.9f
                else 0.95f,
                tween(800),
            )
            Thread.sleep(2_000)
        }
    }
}