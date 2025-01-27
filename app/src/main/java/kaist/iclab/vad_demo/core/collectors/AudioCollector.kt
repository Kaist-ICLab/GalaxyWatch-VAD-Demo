package kaist.iclab.vad_demo.core.collectors

class AudioCollector: CollectorInterface {
    override fun start() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override var listener: ((AudioDataEntity) -> Unit)?
        get() = TODO("Not yet implemented")
        set(value) {}
}