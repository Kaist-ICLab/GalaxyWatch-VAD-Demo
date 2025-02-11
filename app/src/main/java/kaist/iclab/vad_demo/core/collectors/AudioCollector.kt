package kaist.iclab.vad_demo.core.collectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import kaist.iclab.vad_demo.core.tarsosandroid.AudioDispatcherFactory
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.mfcc.MFCC

class AudioCollector(private val context: Context) : CollectorInterface {

    var dispatcher: AudioDispatcher? = null
    private var isPaused = false
    override var listener: ((AudioDataEntity) -> Unit)? = null

    private val mfccProcessor = MFCC(2048, 16000f, 13, 20, 300f, 8000f)

    // 🔄 Buffer to store MFCC frames
    private val mfccBuffer = mutableListOf<FloatArray>()

    override fun start() {
        Log.d("AudioCollector", "🔄 Starting Audio Collection.")

        if (dispatcher != null) {
            Log.d("AudioCollector", "Stopping existing dispatcher before restarting")
            stop()
        }

        val sampleRate = 16000
        val bufferSize = 2048
        val bufferOverlap = 1024

        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(context, sampleRate, bufferSize, bufferOverlap)

            dispatcher?.addAudioProcessor(mfccProcessor)
            dispatcher?.addAudioProcessor(object : AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    Log.d("AudioCollector", "🎤 Processing audio, buffer size: ${audioEvent.bufferSize}")

                    val mfccValues = mfccProcessor.mfcc
                    if (mfccValues.isNotEmpty() && mfccValues.all { it.isFinite() }) {

                        // ✅ Apply Scaling (Multiply MFCC values by 5)
                        val scaledMFCC = mfccValues.map { it * 5f }.toFloatArray()

                        // ✅ Store Scaled MFCC frame in buffer
                        mfccBuffer.add(scaledMFCC.copyOf())
                        Log.d("AudioCollector", "🎵 Collected MFCC frame: ${mfccBuffer.size}/1")

                        // ✅ When we have 1 frame, send to VADModel
                        if (mfccBuffer.size >= 1) {
                            Log.d("AudioCollector", "✅ Sending 1 MFCC frames to VADModel for inference.")
                            listener?.invoke(AudioDataEntity(mfccBuffer.flatMap { it.asList() }.toFloatArray()))


                            // Clear buffer after sending
                            mfccBuffer.clear()
                        }
                    } else {
                        Log.e("AudioCollector", "⚠️ MFCC computation failed or returned empty values")
                    }
                    return true
                }

                override fun processingFinished() {
                    Log.d("AudioCollector", "✅ MFCC processing finished.")
                }
            })

            Log.d("AudioCollector", "🔄 Starting AudioDispatcher Thread")
            val dispatcherThread = Thread(dispatcher, "AudioDispatcherThread")
            dispatcherThread.start()

            // ✅ Give dispatcher time to initialize
            Thread.sleep(500)

            // ✅ Confirm if dispatcher is actually running
            if (!dispatcherThread.isAlive) {
                Log.e("AudioCollector", "❌ AudioDispatcher thread failed! Attempting manual restart.")
                stop()
                start()
            } else {
                Log.d("AudioCollector", "✅ AudioDispatcher thread is running")
            }

        } catch (e: SecurityException) {
            Log.e("AudioCollector", "🚨 Permission denied: ${e.message}")
        }
    }

    override fun stop() {
        Log.d("AudioCollector", "🛑 Stopping AudioCollector and AudioDispatcher.")
        isPaused = true
        dispatcher?.stop()
        dispatcher = null

        // Stop microphone recording completely
        AudioDispatcherFactory.stopRecording()
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}