package kaist.iclab.vad_demo.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kaist.iclab.vad_demo.core.model.ModelInterface
import kaist.iclab.vad_demo.core.preprocess.TarsosDSPMFCCPreprocessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VADViewModel(
    private val context: Context,
    private val audioCollector: AudioCollector,
    private val vadModel: ModelInterface,
    private val mfccPreprocessor: TarsosDSPMFCCPreprocessor
) : ViewModel(), ViewModelInterface {

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean>
        get() = _isRunning

    override val isDetected = vadModel.outputStateFlow

    fun toggleVAD() {
        if (_isRunning.value) {
            stopVAD()
        } else {
            startVAD()
        }
    }

    override fun startVAD() {
        if (!_isRunning.value) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                _isRunning.value = true

                // 1) Start audio collection
                audioCollector.start()

                // 2) Initialize and start MFCC processing
                val audioStream = audioCollector.audioPipedInputStream
                if (audioStream != null) {
                    mfccPreprocessor.init(audioStream)
                    mfccPreprocessor.start()
                } else {
                    Log.e("VADViewModel", "Audio stream is null!")
                }

                // 3) Start VAD model processing
                vadModel.start()
            } else {
                Log.e("VADViewModel", "Microphone permission not granted!")
            }
        }
    }

    override fun stopVAD() {
        if (_isRunning.value) {
            _isRunning.value = false

            // 1) Stop VAD processing
            vadModel.stop()

            // 2) Stop MFCC processing
            mfccPreprocessor.stop()

            // 3) Stop Audio collection
            audioCollector.stop()
        }
    }

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

