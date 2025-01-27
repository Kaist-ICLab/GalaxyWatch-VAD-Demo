package kaist.iclab.vad_demo.core.model

import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VADModel(
    private val audioCollector: AudioCollector):ModelInterface<Boolean> {
    private val _outputStateFlow = MutableStateFlow(false)
    override val outputStateFlow: StateFlow<Boolean>
        get() = _outputStateFlow

    private fun preprocess(audio: Array<Float>): Array<Float> {
        TODO("Not yet implemented")
    }

    private fun inference(input: Array<Float>): Boolean {
        TODO("Not yet implemented")
    }

    override fun start() {
        audioCollector.listener = { audioDataEntity ->
//            TODO: convert audioDataEntity to audio input
//            TODO: you may need to use preprocess() as well
            val audio_input = arrayOf(.1f, .1f,)
            _outputStateFlow.value = inference(audio_input)
        }
    }

    override fun stop() {
        audioCollector.listener = null
    }
}