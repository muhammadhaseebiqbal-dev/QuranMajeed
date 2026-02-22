package com.haseeb.quranapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A wavy circular progress indicator inspired by Play Store style.
 * Shows a circular track with a filled arc that has wavy "ripple" edges.
 */
@Composable
fun WavyCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    strokeWidth: Dp = 3.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    waveAmplitude: Dp = 1.5.dp,
    waveFrequency: Int = 8
) {
    // Animate the wave phase for a continuous ripple effect
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size.minDimension
        val radius = (canvasSize - strokeWidth.toPx()) / 2f
        val center = Offset(canvasSize / 2f, canvasSize / 2f)
        val strokePx = strokeWidth.toPx()
        val ampPx = waveAmplitude.toPx()

        // Draw background track
        drawCircle(
            color = trackColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )

        // Draw wavy progress arc using small line segments
        val sweepAngle = progress * 360f
        val startAngle = -90f // Start from top
        val segments = (sweepAngle * 2).toInt().coerceAtLeast(1) // More segments = smoother

        if (segments > 1) {
            val angleStep = sweepAngle / segments

            for (i in 0 until segments) {
                val angleDeg = startAngle + i * angleStep
                val angleRad = angleDeg * PI.toFloat() / 180f
                val nextAngleDeg = startAngle + (i + 1) * angleStep
                val nextAngleRad = nextAngleDeg * PI.toFloat() / 180f

                // Apply sine wave to radius for wavy effect
                val waveOffset1 = sin(waveFrequency * angleRad + wavePhase) * ampPx
                val waveOffset2 = sin(waveFrequency * nextAngleRad + wavePhase) * ampPx

                val r1 = radius + waveOffset1
                val r2 = radius + waveOffset2

                val x1 = center.x + r1 * cos(angleRad)
                val y1 = center.y + r1 * sin(angleRad)
                val x2 = center.x + r2 * cos(nextAngleRad)
                val y2 = center.y + r2 * sin(nextAngleRad)

                drawLine(
                    color = progressColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
