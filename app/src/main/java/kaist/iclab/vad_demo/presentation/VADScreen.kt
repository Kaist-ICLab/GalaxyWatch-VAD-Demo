package kaist.iclab.vad_demo.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Typography
import androidx.wear.tooling.preview.devices.WearDevices
import kaist.iclab.vad_demo.presentation.components.ControllerButton
import kaist.iclab.vad_demo.presentation.components.Waveform


@Composable
fun VADScreen(
    isRunning: Boolean,
    isDetected: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    MaterialTheme(
        colors = Colors(
            background = Color(0xFF000522)
        ),
        typography = Typography(
//            defaultFontFamily = TODO: you can add font here
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ControllerButton(isRunning, onStart, onStop)

            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(.8f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isRunning) {
                    Text(
                        "Inferring Voice...",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Waveform(isDetected)
                    Spacer(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        if (isDetected) "Voice Activity Detected\nMute is activated"
                        else "No Voice Activity Detected\nMute is deactivated",
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(top = 36.dp),
                        text = "Turn on the VAD",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewVADScreenRunningDetected() {
    VADScreen(
        isRunning = true,
        isDetected = true,
        onStart = {},
        onStop = {}
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, backgroundColor = 0xFFFFFFFF)
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


