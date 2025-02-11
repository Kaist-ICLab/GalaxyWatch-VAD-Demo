package kaist.iclab.vad_demo.core.tarsosandroid

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import kaist.iclab.vad_demo.core.utils.UniversalAudioInputStream

object AudioDispatcherFactory {
    private var shouldRecord = true  // ✅ Control flag to stop recording properly
    private var audioRecord: AudioRecord? = null  // ✅ Store AudioRecord reference

    fun fromDefaultMicrophone(
        context: Context,
        sampleRate: Int,
        audioBufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Missing RECORD_AUDIO permission")
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096) * 4

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val audioStream = UniversalAudioInputStream(format)

        val buffer = ShortArray(audioBufferSize)

        shouldRecord = true  // ✅ Allow recording to start
        audioRecord?.startRecording()
        Log.d("AudioDispatcherFactory", "✅ AudioRecord started recording.")

        val recordingThread = Thread {
            while (shouldRecord) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (readSize > 0) {
                    Log.d("AudioDispatcherFactory", "Read $readSize samples from microphone.")
                    audioStream.write(buffer.copyOf(readSize))
                } else {
                    Log.e("AudioDispatcherFactory", "AudioRecord.read() returned $readSize, possible error!")
                }
            }
            Log.d("AudioDispatcherFactory", "Stopping AudioRecord thread.")
        }
        recordingThread.start()

        return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap).also {
            Log.d("AudioDispatcherFactory", "AudioDispatcher created with bufferSize=$audioBufferSize, bufferOverlap=$bufferOverlap")
        }

    }

    fun stopRecording() {  // ✅ Call this when stopping VAD
        shouldRecord = false  // ✅ Stop the recording loop
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        Log.d("AudioDispatcherFactory", "AudioRecord stopped and released.")
    }
}
