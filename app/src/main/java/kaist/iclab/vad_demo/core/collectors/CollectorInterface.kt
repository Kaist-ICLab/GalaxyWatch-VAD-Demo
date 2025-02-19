package kaist.iclab.vad_demo.core.collectors

import java.io.PipedInputStream

interface CollectorInterface {
//    val NAME: String
//
//    val permissions: Array<String>
//    val foregroundServiceTypes: Array<Int>

//    val configFlow: StateFlow<CollectorConfig>
//    fun updateConfig(config: CollectorConfig)
//    fun resetConfig()

//    val stateFlow: StateFlow<CollectorState>

    /* UNAVAILABLE => Check*/
//    fun initialize()
//    /* DISABLED => READY */
//    fun enable()
//    /* READY => DISABLED */
//    fun disable()
//    /* Request permission to collect data: PERMISSION_REQUIRED => READY */
//    fun requestPermissions(onResult: ((Boolean) -> Unit))
    /* Start collector to collect data: READY => RUNNING */
    fun start()
    /* Stop collector to stop collecting data: RUNNING => READY */
    fun stop()
    var audioPipedInputStream: PipedInputStream?
//
//    /* Based on the data, define action */
////    var listener: ((DataEntity) -> Unit)?
//    var listener: ((AudioDataEntity) -> Unit)?
}