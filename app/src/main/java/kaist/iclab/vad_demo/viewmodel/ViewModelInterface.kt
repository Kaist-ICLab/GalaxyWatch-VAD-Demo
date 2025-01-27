package kaist.iclab.vad_demo.viewmodel

import kotlinx.coroutines.flow.StateFlow

interface ViewModelInterface {
    val isRunning: StateFlow<Boolean>
    val isDetected: StateFlow<Boolean>

    fun startVAD()
    fun stopVAD()
}