package kaist.iclab.vad_demo.core.collectors

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import kotlinx.coroutines.*
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioCollector(private val context: Context) : CollectorInterface {

    override var listener: ((AudioDataEntity) -> Unit)? = null
    private var dispatcher: AudioDispatcher? = null
    private var audioRecord: AudioRecord? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val mfccProcessor = MFCC(2048, 16000f, 13, 20, 300f, 8000f)

    private val pipedOutputStream = PipedOutputStream()
    private val pipedInputStream = PipedInputStream(pipedOutputStream, 4096) // Streaming Buffer

    override fun start() {
        Log.d("AudioCollector", "Starting audio collection")

        if (dispatcher != null) stop()

        if (!hasAudioPermission()) {
            Log.e("AudioCollector", "Permission denied! RECORD_AUDIO permission is required.")
            return
        }

        try {
            val sampleRate = 16000
            val bufferSize = 2048
            val bufferOverlap = 1024

            audioRecord = createAudioRecord(sampleRate)
            audioRecord?.startRecording()
            Log.d("AudioCollector", "AudioRecord started")

            coroutineScope.launch { readAudioData() }

            // Convert PCM data into a continuously updating InputStream
            val audioInputStream = UniversalAudioInputStream(
                pipedInputStream,
                TarsosDSPAudioFormat(16000f, 16, 1, true, false)
            )

            dispatcher = AudioDispatcher(audioInputStream, bufferSize, bufferOverlap).apply {
                addAudioProcessor(mfccProcessor)
                addAudioProcessor(object : be.tarsos.dsp.AudioProcessor {
                    override fun process(audioEvent: be.tarsos.dsp.AudioEvent): Boolean {
                        coroutineScope.launch {
                            mfccProcessor.process(audioEvent)
                            val mfccValues = mfccProcessor.mfcc
                            if (mfccValues.isNotEmpty() && mfccValues.all { it.isFinite() }) {
                                listener?.invoke(AudioDataEntity(mfccValues.map { it * 5f }.toFloatArray()))
                            } else {
                                Log.e("AudioCollector", "MFCC computation failed or returned empty values")
                            }
                        }
                        return true
                    }

                    override fun processingFinished() {
                        Log.d("AudioCollector", "MFCC processing finished.")
                    }
                })
            }

            coroutineScope.launch { dispatcher?.run() }

        } catch (e: SecurityException) {
            Log.e("AudioCollector", "Permission error: ${e.message}")
        }
    }

    override fun stop() {
        Log.d("AudioCollector", "Stopping audio collection")
        dispatcher?.stop()
        dispatcher = null
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        coroutineScope.cancel()

        // Close piped streams when stopping
        pipedOutputStream.close()
        pipedInputStream.close()
    }

    private fun createAudioRecord(sampleRate: Int): AudioRecord? {
        if (!hasAudioPermission()) {
            Log.e("AudioCollector", "Permission denied! RECORD_AUDIO permission is required.")
            return null
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096) * 2

        return try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize
            )
        } catch (e: SecurityException) {
            Log.e("AudioCollector", "SecurityException: Unable to access microphone. ${e.message}")
            null
        }
    }

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private suspend fun readAudioData() {
        val buffer = ShortArray(2048)
        while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: -1
            if (readSize > 0) {
                // Convert short array to byte array and write to PipedOutputStream
                val byteBuffer = ByteBuffer.allocate(readSize * 2).order(ByteOrder.LITTLE_ENDIAN)
                buffer.take(readSize).forEach { byteBuffer.putShort(it) }

                try {
                    pipedOutputStream.write(byteBuffer.array()) // Stream data continuously
                } catch (e: Exception) {
                    Log.e("AudioCollector", "Error writing to PipedOutputStream: ${e.message}")
                }
            } else {
                Log.e("AudioCollector", "AudioRecord.read() returned $readSize, possible error!")
            }
            delay(5) // Prevents excessive CPU usage
        }
    }
}
