package kaist.iclab.vad_demo

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VADApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VADApplication)  // Ensure this is present
            modules(koinModule)
        }
    }
}