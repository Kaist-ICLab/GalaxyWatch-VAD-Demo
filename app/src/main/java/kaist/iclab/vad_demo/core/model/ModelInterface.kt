package kaist.iclab.vad_demo.core.model

import kotlinx.coroutines.flow.StateFlow

interface ModelInterface {
    val outputStateFlow: StateFlow<Boolean>
    fun start()
    fun stop()
}