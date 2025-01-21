package data

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import core.AudioInput

class AudioInputImpl(private val context: Context) : AudioInput {
    private var recorder: AudioRecord? = null
    private val bufferSize = AudioRecord.getMinBufferSize(
        16000, // 16 kHz sampling rate
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val audioBuffer = ShortArray(bufferSize)

    override fun startRecording() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted; throw an exception or handle it appropriately
            throw SecurityException("RECORD_AUDIO permission is not granted.")
        }

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, // Microphone as the audio source
            16000, // 16 kHz sample rate
            AudioFormat.CHANNEL_IN_MONO, // Mono channel
            AudioFormat.ENCODING_PCM_16BIT, // 16-bit PCM encoding
            bufferSize
        )

        if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord initialization failed")
        }

        recorder?.startRecording()
    }

    override fun stopRecording() {
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    override fun getAudioData(): FloatArray {
        val readSize = recorder?.read(audioBuffer, 0, audioBuffer.size) ?: 0
        return audioBuffer.copyOf(readSize).map { it.toFloat() / Short.MAX_VALUE }.toFloatArray()
    }
}
