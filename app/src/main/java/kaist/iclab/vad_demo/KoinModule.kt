package kaist.iclab.vad_demo

import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kaist.iclab.vad_demo.core.model.ModelInterface
import kaist.iclab.vad_demo.core.model.VADModel
import kaist.iclab.vad_demo.viewmodel.VADViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    single { AudioCollector() }
    single<ModelInterface> { VADModel(androidContext()) }
    viewModel { VADViewModel(get(), get()) }
}
