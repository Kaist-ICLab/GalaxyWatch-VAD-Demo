package kaist.iclab.vad_demo

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class VADApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VADApplication)
            androidLogger(level = Level.NONE)
            modules(koinModule)
        }
    }
}