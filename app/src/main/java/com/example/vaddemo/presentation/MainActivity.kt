package com.example.vaddemo.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vaddemo.presentation.theme.VADDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: VADViewModel = viewModel()
            VADDemoTheme {
                VADApp(viewModel)
            }
        }
    }
}

@Composable
fun VADApp(viewModel: VADViewModel) {
    val isRunning = viewModel.isRunning.collectAsState().value
    val isDetected = viewModel.isDetected.collectAsState().value

    VADScreen(
        isRunning = isRunning,
        isDetected = isDetected,
        onStart = { viewModel.startVAD() },
        onStop = { viewModel.stopVAD() }
    )
}
