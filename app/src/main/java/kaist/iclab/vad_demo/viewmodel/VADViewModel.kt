package kaist.iclab.vad_demo.viewmodel

import androidx.lifecycle.ViewModel
import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kaist.iclab.vad_demo.core.model.ModelInterface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VADViewModel(
    private val audioCollector: AudioCollector,
    private val vadModel: ModelInterface<Boolean>,
): ViewModel(), ViewModelInterface {
    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean>
        get() = _isRunning

    override val isDetected =  vadModel.outputStateFlow

    override fun startVAD() {
        _isRunning.value = true
        audioCollector.start()
        vadModel.start()

    }

    override fun stopVAD() {
        _isRunning.value = false
        audioCollector.stop()
        vadModel.stop()
    }

    //    private val audioCollector -> Use Koin to define it
    //    private val notificationManager ->

//
//    fun startProcessing() {
//        _isRunning.value = true
//        audioInput.startRecording()
//
//        viewModelScope.launch {
//            while (_isRunning.value) {
//                val detected = (0..1).random() == 1 // Simulated VAD result
//                _isDetected.update { detected }
//
//                if (detected) {
//                    notificationManager.disableNotifications()
//                } else {
//                    notificationManager.enableNotifications()
//                }
//
//                delay(1000L)
//            }
//        }
//    }
//
//    fun stopProcessing() {
//        _isRunning.value = false
//        audioInput.stopRecording()
//        notificationManager.enableNotifications()
//        _isDetected.update { false }
//    }
}
