package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.UltronCyan
import com.example.ui.theme.UltronRed
import com.example.ui.theme.UltronRedBright
import com.example.ui.theme.UltronRedDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VoiceReactorVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "UltronReactor")

    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening || isSpeaking) 700 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val subtleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening || isSpeaking) 4000 else 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val rmsAnimated by animateFloatAsState(
        targetValue = if (isListening || isSpeaking) audioRms.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "rms"
    )

    Box(
        modifier = modifier
            .size(190.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.width / 2f) - 8.dp.toPx()

            // 1. Outermost subtle red-900 border ring: border border-red-900/30
            val ring1Radius = baseRadius
            drawCircle(
                color = Color(0x4D7F1D1D),
                radius = ring1Radius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 2. Middle animated pulse ring: border-2 border-red-600/20 animate-pulse
            val ring2Radius = (baseRadius * 0.84f) * pulseAnimation + (rmsAnimated * 6.dp.toPx())
            drawCircle(
                color = if (isListening) Color(0x403B82F6) else Color(0x33DC2626),
                radius = ring2Radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Dynamic acoustic expansion waves
            if (isListening || isSpeaking) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (isListening) UltronCyan else UltronRed).copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = ring2Radius + (rmsAnimated * 14.dp.toPx())
                    ),
                    radius = ring2Radius + (rmsAnimated * 14.dp.toPx()),
                    center = center
                )
            }

            // 3. Inner tactical accent ring: border-4 border-red-500/40
            val ring3Radius = baseRadius * 0.68f
            drawCircle(
                color = if (isListening) Color(0x663B82F6) else Color(0x66EF4444),
                radius = ring3Radius,
                center = center,
                style = Stroke(width = 3.5.dp.toPx())
            )

            // Subtle rotation ticks on ring 3
            rotate(subtleRotation, pivot = center) {
                val segments = 6
                val sweep = 24f
                for (i in 0 until segments) {
                    val startAngle = i * (360f / segments)
                    drawArc(
                        color = if (isListening) UltronCyan else UltronRedBright,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - ring3Radius, center.y - ring3Radius),
                        size = androidx.compose.ui.geometry.Size(ring3Radius * 2, ring3Radius * 2),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // 4. Core Glowing Orb: bg-gradient-to-tr from-red-900 to-red-600 shadow-[0_0_40px_rgba(220,38,38,0.4)]
            val coreRadius = (baseRadius * 0.50f) * (if (isSpeaking) pulseAnimation else 1.0f) + (rmsAnimated * 8.dp.toPx())
            val coreGradient = if (isListening) {
                Brush.radialGradient(
                    colors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1E3A8A)),
                    center = center,
                    radius = coreRadius
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(UltronRedBright, UltronRed, UltronRedDark),
                    center = center,
                    radius = coreRadius
                )
            }

            drawCircle(
                brush = coreGradient,
                radius = coreRadius,
                center = center
            )

            // 5. Center Audio Equalizer / Aperture Glyphs (graphic_eq soundwave)
            val barCount = 5
            val barSpacing = 5.dp.toPx()
            val totalWidth = (barCount * 3.dp.toPx()) + ((barCount - 1) * barSpacing)
            val startX = center.x - (totalWidth / 2f)

            for (i in 0 until barCount) {
                val multiplier = when (i) {
                    0, 4 -> 0.45f + (rmsAnimated * 0.5f)
                    1, 3 -> 0.75f + (rmsAnimated * 0.8f)
                    else -> 1.0f + (rmsAnimated * 1.1f)
                }
                val barHeight = (12.dp.toPx() * multiplier).coerceIn(4.dp.toPx(), 28.dp.toPx())
                val x = startX + (i * (3.dp.toPx() + barSpacing))
                drawLine(
                    color = Color.White,
                    start = Offset(x, center.y - (barHeight / 2f)),
                    end = Offset(x, center.y + (barHeight / 2f)),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

