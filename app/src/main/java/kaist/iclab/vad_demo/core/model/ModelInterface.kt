package kaist.iclab.vad_demo.core.model

import kotlinx.coroutines.flow.StateFlow

interface ModelInterface<T> {
    val outputStateFlow: StateFlow<T>
    fun start()
    fun stop()
}