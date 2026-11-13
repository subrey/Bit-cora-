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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.jarvis.domain.model.AssistantState
import com.example.jarvis.ui.JarvisColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Núcleo animado de JARVIS.
 *
 * Un solo [Canvas] dibuja anillos, arcos y ondas según [state]; no usa
 * imágenes ni desenfoques, así que se mantiene fluido en teléfonos modestos.
 */
@Composable
fun JarvisCore(
    state: AssistantState,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "nucleo")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "rotacion",
    )
    val breath by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "respiracion",
    )
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing)),
        label = "onda",
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
        label = "colorNucleo",
    )

    Canvas(modifier.aspectRatio(1f)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val radius = maxRadius * 0.60f * breath
        val phase = wave / (2f * PI.toFloat())

        // Halo exterior.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.26f),
                    accent.copy(alpha = 0.07f),
                    Color.Transparent,
                ),
                center = center,
                radius = maxRadius,
            ),
            radius = maxRadius,
            center = center,
        )

        // Anillos fijos.
        drawCircle(
            color = JarvisColors.Outline.copy(alpha = 0.55f),
            radius = radius * 1.42f,
            center = center,
            style = Stroke(1.dp.toPx()),
        )
        drawCircle(
            color = accent.copy(alpha = 0.16f),
            radius = radius * 1.26f,
            center = center,
            style = Stroke(1.dp.toPx()),
        )

        // Arcos que giran en sentidos opuestos.
        rotate(rotation, pivot = center) {
            drawArc(
                color = accent.copy(alpha = 0.9f),
                startAngle = 0f,
                sweepAngle = 58f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.34f, center.y - radius * 1.34f),
                size = Size(radius * 2.68f, radius * 2.68f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = JarvisColors.Cyan.copy(alpha = 0.45f),
                startAngle = 180f,
                sweepAngle = 22f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.34f, center.y - radius * 1.34f),
                size = Size(radius * 2.68f, radius * 2.68f),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        rotate(-rotation * 1.7f, pivot = center) {
            drawArc(
                color = accent.copy(alpha = 0.30f),
                startAngle = 40f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(center.x - radius * 1.18f, center.y - radius * 1.18f),
                size = Size(radius * 2.36f, radius * 2.36f),
                style = Stroke(1.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Marcas del dial.
        for (i in 0 until 60) {
            val angle = Math.toRadians((i * 6).toDouble())
            val inner = radius * 1.47f
            val isMajor = i % 5 == 0
            val outer = inner + if (isMajor) 9.dp.toPx() else 3.dp.toPx()
            drawLine(
                color = accent.copy(alpha = if (isMajor) 0.55f else 0.18f),
                start = center + Offset(
                    cos(angle).toFloat() * inner,
                    sin(angle).toFloat() * inner,
                ),
                end = center + Offset(
                    cos(angle).toFloat() * outer,
                    sin(angle).toFloat() * outer,
                ),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }

        // Disco interior.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(JarvisColors.PanelHigh, JarvisColors.Background),
                center = center,
                radius = radius * 0.94f,
            ),
            radius = radius * 0.94f,
            center = center,
        )
        drawCircle(
            color = accent.copy(alpha = 0.65f),
            radius = radius * 0.94f,
            center = center,
            style = Stroke(1.4.dp.toPx()),
        )
        drawCircle(
            color = accent.copy(alpha = 0.85f),
            radius = radius * 0.43f,
            center = center,
            style = Stroke(1.dp.toPx()),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.45f),
                    accent.copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 0.42f,
            ),
            radius = radius * 0.42f,
            center = center,
        )

        when (state) {
            AssistantState.LISTENING -> {
                // Ondas que se expanden mientras el micrófono escucha.
                repeat(3) { index ->
                    val fraction = (phase + index / 3f) % 1f
                    drawCircle(
                        color = accent.copy(alpha = (1f - fraction) * 0.55f),
                        radius = radius * (0.98f + fraction * 0.72f),
                        center = center,
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            accent.copy(alpha = 0.30f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius * 0.30f,
                    ),
                    radius = radius * 0.30f,
                    center = center,
                )
            }

            AssistantState.THINKING -> {
                // Barrido de radar y tres puntos en órbita.
                drawArc(
                    color = accent,
                    startAngle = rotation * 2f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.86f, center.y - radius * 0.86f),
                    size = Size(radius * 1.72f, radius * 1.72f),
                    style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round),
                )
                repeat(3) { index ->
                    val angle = Math.toRadians((rotation * 2f + index * 120f).toDouble())
                    val point = center + Offset(
                        cos(angle).toFloat() * radius * 0.72f,
                        sin(angle).toFloat() * radius * 0.72f,
                    )
                    drawCircle(color = accent.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = point)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                        center = center,
                        radius = radius * 0.34f,
                    ),
                    radius = radius * 0.34f,
                    center = center,
                )
            }

            AssistantState.SPEAKING -> {
                // Ecualizador circular: la voz se ve alrededor del núcleo.
                val bars = 56
                repeat(bars) { index ->
                    val angle = (index.toFloat() / bars) * 2f * PI.toFloat()
                    val amplitude = (sin(wave * 3f + index * 0.42f) * 0.5f + 0.5f)
                    val inner = radius * 1.00f
                    val outer = inner + 3.dp.toPx() + amplitude * 22.dp.toPx()
                    drawLine(
                        color = accent.copy(alpha = 0.30f + amplitude * 0.65f),
                        start = center + Offset(cos(angle) * inner, sin(angle) * inner),
                        end = center + Offset(cos(angle) * outer, sin(angle) * outer),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            accent.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius * 0.28f,
                    ),
                    radius = radius * 0.28f,
                    center = center,
                )
            }

            AssistantState.ERROR -> {
                drawCircle(
                    color = accent.copy(alpha = 0.9f),
                    radius = radius * 0.16f,
                    center = center,
                )
            }

            AssistantState.IDLE -> {
                // Latido suave.
                drawCircle(
                    color = accent.copy(alpha = 0.55f),
                    radius = 10.dp.toPx() * breath,
                    center = center,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.85f),
                    radius = 3.5.dp.toPx(),
                    center = center,
                )
            }
        }
    }
}
