package kaist.iclab.vad_demo.core.tarsosandroid

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream

object AudioDispatcherFactory {

    fun fromDefaultMicrophone(
        context: Context, // FIX 1: Pass Context for permission check
        sampleRate: Int,
        audioBufferSize: Int,
        bufferOverlap: Int
    ): AudioDispatcher {

        // Check for RECORD_AUDIO permission before proceeding
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Missing RECORD_AUDIO permission")
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)

        val buffer = ShortArray(audioBufferSize)
        val floatBuffer = FloatArray(audioBufferSize)

        audioRecord.startRecording()

        // Convert PCM16 to Float and stream it continuously
        Thread {
            while (audioRecord.read(buffer, 0, buffer.size) > 0) {
                for (i in buffer.indices) {
                    floatBuffer[i] = buffer[i] / 32768.0f
                }
            }
        }.start()

        val audioStream: TarsosDSPAudioInputStream = UniversalAudioInputStream(floatBuffer, format) // FIX 2: Pass FloatArray instead of AudioRecord
        return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
    }





/**
 * Create a dispatcher from a raw float array.
 *
 * @param audioData The raw float array containing audio samples.
 * @param sampleRate The sample rate of the data.
 * @param audioBufferSize The size of the processing buffer.
 * @param bufferOverlap The overlap between buffers.
 * @return A new AudioDispatcher
 */
fun fromFloatArray(
    audioData: FloatArray,
    sampleRate: Int,
    audioBufferSize: Int,
    bufferOverlap: Int
): AudioDispatcher {
    val format = TarsosDSPAudioFormat(
        sampleRate.toFloat(),
        16,
        1,
        true,
        false
    )
    val audioStream = UniversalAudioInputStream(audioData, format)
    return AudioDispatcher(audioStream, audioBufferSize, bufferOverlap)
}
}