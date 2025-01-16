package com.example.vaddemo.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vaddemo.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
fun getFontFamilyForEnvironment(): FontFamily {
    return if (LocalInspectionMode.current) {
        FontFamily.Default // Use default font in Preview
    } else {
        MontserratFontFamily // Use Montserrat in runtime
    }
}

val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat_regular)
)


@Composable
fun VADScreen(
    isRunning: Boolean,
    isDetected: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val fontFamily = getFontFamilyForEnvironment()

    androidx.wear.compose.material.MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Black, // Black
                            Color(0xFF000522)  // Indigo
                        )
                    )
                )
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Use fontFamily in all Text components
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isRunning) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .offset(y = (-30.35).dp)
                            .background(Color(0xFF1A3A77), shape = androidx.compose.foundation.shape.CircleShape)
                            .clickable(onClick = { onStart() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(id = R.string.start_vad),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.turn_on_vad),
                        fontSize = 16.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = fontFamily,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.offset(y = (-8).dp)
                    )

                } else {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFF427495), shape = androidx.compose.foundation.shape.CircleShape)
                            .clickable(onClick = { onStop() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = stringResource(id = R.string.stop_vad),
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(id = R.string.inferring_voice),
                        fontSize = 14.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = fontFamily,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Waveform(isDetected = isDetected)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isDetected) stringResource(id = R.string.vad_detected)
                        else stringResource(id = R.string.vad_not_detected),
                        fontSize = 12.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = fontFamily,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}



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




@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun PreviewVADScreenRunningDetected() {
    VADScreen(
        isRunning = true,
        isDetected = true,
        onStart = {},
        onStop = {}
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun PreviewVADScreenRunningNotDetected() {
    VADScreen(
        isRunning = true,
        isDetected = false,
        onStart = {},
        onStop = {}
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun PreviewVADScreen() {
    VADScreen(
        isRunning = false,
        isDetected = false,
        onStart = {},
        onStop = {}
    )
}


