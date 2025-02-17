package kaist.iclab.vad_demo

import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kaist.iclab.vad_demo.core.model.ModelInterface
import kaist.iclab.vad_demo.core.model.VADModel
import kaist.iclab.vad_demo.viewmodel.VADViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val koinModule = module {
    single { AudioCollector(androidContext()) }
    single<ModelInterface<Boolean>> { VADModel(androidContext(), get()) }
    viewModel { VADViewModel(get(), get()) }
}
