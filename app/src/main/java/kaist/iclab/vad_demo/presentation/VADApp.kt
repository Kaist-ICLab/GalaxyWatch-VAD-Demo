package kaist.iclab.vad_demo.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kaist.iclab.vad_demo.viewmodel.VADViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun VADApp(
    viewModel: VADViewModel = koinViewModel()
) {
    val isRunning = viewModel.isRunning.collectAsState().value
    val isDetected = viewModel.isDetected.collectAsState().value

    VADScreen(
        isRunning = isRunning,
        isDetected = isDetected,
        onStart = {viewModel.startVAD()},
        onStop = {viewModel.stopVAD()}
    )
}