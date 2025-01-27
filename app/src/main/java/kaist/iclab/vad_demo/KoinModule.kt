package kaist.iclab.vad_demo

import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kaist.iclab.vad_demo.core.model.VADModel
import kaist.iclab.vad_demo.viewmodel.VADViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val koinModule = module {
    singleOf(::AudioCollector)
    singleOf(::VADModel)
    viewModelOf(::VADViewModel)
}