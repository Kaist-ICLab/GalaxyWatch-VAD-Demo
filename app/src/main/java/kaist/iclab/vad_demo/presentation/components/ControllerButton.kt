package kaist.iclab.vad_demo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon

@Composable
fun ControllerButton(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(
                if (isRunning) Color(0xFF427495)
                else Color(0xFF1A3A77),
                shape = CircleShape
            )
            .clickable(onClick = { if(isRunning) onStop() else onStart()}),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Filled.Pause
            else Icons.Filled.PlayArrow,
            contentDescription = if (isRunning) "Stop VAD"
            else "Start VAD",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}