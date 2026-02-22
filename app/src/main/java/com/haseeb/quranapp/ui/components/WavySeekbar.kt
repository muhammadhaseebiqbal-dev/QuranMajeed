package com.haseeb.quranapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A custom wavy seekbar with smooth sine-wave track and reliable drag support.
 * The filled portion has animated waves, the unfilled portion is a flat track.
 */
@Composable
fun WavySeekbar(
    value: Float, // 0f to 1f
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trackHeight: Dp = 48.dp,
    waveAmplitude: Dp = 7.dp,
    waveFrequency: Float = 3.0f, // waves per 100dp
    strokeWidth: Dp = 4.5.dp,
    thumbRadius: Dp = 7.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current

    // Animate wave phase for continuously moving waves
    val infiniteTransition = rememberInfiniteTransition(label = "wavySeekbar")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = modifier
            .height(trackHeight)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(newValue)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    },
                    onDragEnd = {
                        onValueChangeFinished?.invoke()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newValue = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val centerY = size.height / 2f
            val ampPx = waveAmplitude.toPx()
            val strokePx = strokeWidth.toPx()
            val thumbPx = thumbRadius.toPx()
            val thumbX = canvasWidth * value

            // Wave frequency in radians per pixel
            val freq = waveFrequency * 2f * PI.toFloat() / with(density) { 100.dp.toPx() }

            // Draw inactive track (flat line from thumb to end)
            drawLine(
                color = inactiveColor,
                start = Offset(thumbX, centerY),
                end = Offset(canvasWidth, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )

            // Draw active wavy track (from start to thumb)
            if (value > 0.005f) {
                val wavePath = Path()
                val step = 2f // pixels per segment
                var x = 0f
                val startY = centerY + sin(wavePhase) * ampPx
                wavePath.moveTo(0f, startY)

                while (x <= thumbX) {
                    x += step
                    if (x > thumbX) x = thumbX
                    val y = centerY + sin(freq * x + wavePhase) * ampPx
                    wavePath.lineTo(x, y)
                    if (x >= thumbX) break
                }

                drawPath(
                    path = wavePath,
                    color = activeColor,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            // Draw thumb circle
            val thumbY = centerY + sin(freq * thumbX + wavePhase) * ampPx
            // Thumb shadow
            drawCircle(
                color = thumbColor.copy(alpha = 0.2f),
                radius = thumbPx + 4f,
                center = Offset(thumbX, thumbY)
            )
            // Thumb
            drawCircle(
                color = thumbColor,
                radius = thumbPx,
                center = Offset(thumbX, thumbY)
            )
        }
    }
}
