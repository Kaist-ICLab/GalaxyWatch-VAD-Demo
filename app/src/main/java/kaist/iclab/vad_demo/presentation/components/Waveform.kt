package kaist.iclab.vad_demo.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun Waveform(isDetected: Boolean) {
    val barCount = 15

    // Maintain a list of target heights for bars
    val targetHeights = remember { mutableStateListOf<Float>().apply { repeat(barCount) { add(0.5f) } } }

    // Update the target heights dynamically
    LaunchedEffect(Unit) { // Always run, regardless of detection state
        while (true) {
            for (i in 0 until barCount) {
                targetHeights[i] = (0.3f + 0.7f * Math.random().toFloat()) // Random heights between 0.3 and 1.0
            }
            delay(100L) // Updates (every 100ms)
        }
    }

    // Smoothly animate the bar heights
    val animatedHeights = targetHeights.map { targetHeight ->
        animateFloatAsState(
            targetValue = targetHeight,
            animationSpec = tween(durationMillis = 200)
        ).value
    }

    // Draw the waveform
    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(30.dp)
            .padding(horizontal = 8.dp)
    ) {
        val color = if (isDetected) Color(0xFFD0ECF7) else Color.Gray // Blue when detected, grey otherwise
        val spacing = size.width / (barCount * 1.3f)
        val barSpacing = spacing * 0.5f
        val totalBarWidth = barCount * spacing + (barCount - 1) * barSpacing // Total width of all bars and spacing
        val startXOffset = (size.width - totalBarWidth) / 2 // Offset to center bars
        val maxBarHeight = size.height

        for (i in 0 until barCount) {
            val barHeight = maxBarHeight * animatedHeights[i] // Use animated heights
            drawRoundRect(
                color = color,
                topLeft = Offset(
                    x = startXOffset + i * (spacing + barSpacing), // Start bars at calculated offset
                    y = (maxBarHeight - barHeight) / 2
                ),
                size = androidx.compose.ui.geometry.Size(
                    width = spacing,
                    height = barHeight
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f) // Rounded edges for bars
            )
        }
    }
}