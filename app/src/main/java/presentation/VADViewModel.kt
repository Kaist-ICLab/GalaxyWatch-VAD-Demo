package presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VADViewModel : ViewModel() {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _isDetected = MutableStateFlow(false)
    val isDetected: StateFlow<Boolean> = _isDetected

    fun startVAD() {
        _isRunning.value = true
        viewModelScope.launch {
            while (_isRunning.value) {
                delay(1000) // Simulate detection interval
                _isDetected.value = (0..1).random() == 1 // Random detection to simulate for time being
            }
        }
    }

    fun stopVAD() {
        _isRunning.value = false
        _isDetected.value = false
    }
}
