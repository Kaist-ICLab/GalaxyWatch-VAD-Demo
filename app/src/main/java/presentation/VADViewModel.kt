package presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import core.AudioInput
import core.AudioPreprocessor
import data.AudioInputImpl
import core.ManualPreprocessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VADViewModel(context: Context) : ViewModel() {
    private val sampleRate = 16000
    private val fftSize = 512
    private val numCoefficients = 13
    private val numFilters = 26

    private val audioInput: AudioInput = AudioInputImpl(context)
    private val preprocessor: AudioPreprocessor = ManualPreprocessor(
        sampleRate = sampleRate,
        fftSize = fftSize,
        numCoefficients = numCoefficients,
        numFilters = numFilters
    )

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> get() = _isRunning

    private val _isDetected = MutableStateFlow(false)
    val isDetected: StateFlow<Boolean> get() = _isDetected

    private val _mfccFeatures = MutableStateFlow(FloatArray(0))
    val mfccFeatures: StateFlow<FloatArray> get() = _mfccFeatures

    fun startProcessing() {
        _isRunning.value = true
        audioInput.startRecording()

        // Start a coroutine that updates isDetected every second
        viewModelScope.launch {
            while (_isRunning.value) {
                // Randomize detection
                val detected = (0..1).random() == 1
                _isDetected.update { detected }

                // Wait for 1 second before updating again
                delay(1000L)
            }
        }
    }

    fun stopProcessing() {
        _isRunning.value = false
        audioInput.stopRecording()
        _isDetected.update { false } // Reset detection state
    }
}
