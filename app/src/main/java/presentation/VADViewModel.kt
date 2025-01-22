package presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.AudioInput
import core.AudioPreprocessor
import core.ManualPreprocessor
import core.NotificationManager
import data.AudioInputImpl
import data.NotificationManagerImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VADViewModel(context: Context) : ViewModel() {
    private val audioInput: AudioInput = AudioInputImpl(context)
    private val preprocessor: AudioPreprocessor = ManualPreprocessor(
        sampleRate = 16000,
        fftSize = 512,
        numCoefficients = 32,
        numFilters = 32
    )
    private val notificationManager: NotificationManager = NotificationManagerImpl(context)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> get() = _isRunning

    private val _isDetected = MutableStateFlow(false)
    val isDetected: StateFlow<Boolean> get() = _isDetected

    fun startProcessing() {
        _isRunning.value = true
        audioInput.startRecording()

        viewModelScope.launch {
            while (_isRunning.value) {
                val detected = (0..1).random() == 1 // Simulated VAD result
                _isDetected.update { detected }

                if (detected) {
                    notificationManager.disableNotifications()
                } else {
                    notificationManager.enableNotifications()
                }

                delay(1000L)
            }
        }
    }

    fun stopProcessing() {
        _isRunning.value = false
        audioInput.stopRecording()
        notificationManager.enableNotifications()
        _isDetected.update { false }
    }
}
