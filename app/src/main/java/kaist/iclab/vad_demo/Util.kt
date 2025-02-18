package kaist.iclab.vad_demo

import android.util.Log

object Util {
    fun log(message: String) {
        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace[3] // Get the calling method
        val tag = "${caller.className}.${caller.methodName}"
        Log.d(tag, message)
    }
}
