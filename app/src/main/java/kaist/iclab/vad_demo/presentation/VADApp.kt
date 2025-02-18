package kaist.iclab.vad_demo.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kaist.iclab.vad_demo.viewmodel.VADViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun VADApp(
    viewModel: VADViewModel = koinViewModel()
) {
    val isRunning = viewModel.isRunning.collectAsState().value
    val isDetected = viewModel.isDetected.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // 🔹 Ensures full black background
    ) {
        VADScreen(
            isRunning = isRunning,
            isDetected = isDetected,
            onStart = { viewModel.startVAD() },
            onStop = { viewModel.stopVAD() }
        )
    }
}
