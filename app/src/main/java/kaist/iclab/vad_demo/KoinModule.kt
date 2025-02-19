package kaist.iclab.vad_demo

import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kaist.iclab.vad_demo.core.model.ModelInterface
import kaist.iclab.vad_demo.core.model.VADModel
import kaist.iclab.vad_demo.core.preprocess.TarsosDSPMFCCPreprocessor
import kaist.iclab.vad_demo.viewmodel.VADViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    single { AudioCollector() }
    single { TarsosDSPMFCCPreprocessor() }  // Provide MFCCPreprocessor
    single<ModelInterface> { VADModel(androidContext(), get()) } // Inject MFCCPreprocessor
    viewModel { VADViewModel(androidContext(), get(), get(), get()) }
}

