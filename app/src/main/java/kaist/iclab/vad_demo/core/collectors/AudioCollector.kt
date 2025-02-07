package kaist.iclab.vad_demo.core.collectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import kaist.iclab.vad_demo.core.tarsosandroid.AudioDispatcherFactory

class AudioCollector(private val context: Context) : CollectorInterface {

    private var audioRecord: AudioRecord? = null
    private var dispatcher: AudioDispatcher? = null
    private var isRecording = false

    override var listener: ((AudioDataEntity) -> Unit)? = null

    override fun start() {
        if (!hasMicrophonePermission()) {
            Log.e("AudioCollector", "Microphone permission not granted! Requesting permission...")
            requestMicrophonePermission()
            return
        }

        val sampleRate = 16000  // Adjust as needed
        val bufferSize = 512
        val bufferOverlap = 256

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val finalBufferSize = minBufferSize.coerceAtLeast(bufferSize * 2)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, // Optimized for smartwatch speech
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                finalBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioCollector", "AudioRecord initialization failed!")
                return
            }

            val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)

            // Create AudioDispatcher
            dispatcher = AudioDispatcherFactory.fromFloatArray(
                FloatArray(finalBufferSize / 2),
                sampleRate,
                bufferSize,
                bufferOverlap
            )

            // Start Recording
            audioRecord?.startRecording()
            isRecording = true

            Thread {
                val buffer = ShortArray(finalBufferSize / 2)
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        val floatBuffer = buffer.map { it / 32768.0f }.toFloatArray()
                        listener?.invoke(AudioDataEntity(floatBuffer))
                    }
                }
            }.start()

        } catch (e: SecurityException) {
            Log.e("AudioCollector", "Permission denied: ${e.message}")
        }
    }

    override fun stop() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        dispatcher?.stop()
    }

    private fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMicrophonePermission() {
        if (context is ComponentActivity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}
