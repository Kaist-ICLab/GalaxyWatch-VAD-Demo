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
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue

class AudioCollector(private val context: Context) : CollectorInterface, TarsosDSPAudioInputStream {

    private var dispatcher: AudioDispatcher? = null
    override var listener: ((AudioDataEntity) -> Unit)? = null
    private val mfccProcessor = MFCC(2048, 16000f, 13, 20, 300f, 8000f)

    private var shouldRecord = true
    private var audioRecord: AudioRecord? = null
    private val audioBuffer = LinkedBlockingQueue<Short>(1000)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun start() {
        Log.d("AudioCollector", "Starting Audio Collection.")

        if (dispatcher != null) {
            Log.d("AudioCollector", "Stopping existing dispatcher before restarting")
            stop()
        }

        val sampleRate = 16000
        val bufferSize = 2048
        val bufferOverlap = 1024

        try {
            dispatcher = createAudioDispatcher(sampleRate, bufferSize, bufferOverlap)

            dispatcher?.addAudioProcessor(mfccProcessor)
            dispatcher?.addAudioProcessor(object : be.tarsos.dsp.AudioProcessor {
                override fun process(audioEvent: be.tarsos.dsp.AudioEvent): Boolean {
                    coroutineScope.launch {
                        Log.d("AudioCollector", "Processing audio, buffer size: ${audioEvent.bufferSize}")

                        mfccProcessor.process(audioEvent)
                        val mfccValues = mfccProcessor.mfcc
                        if (mfccValues.isNotEmpty() && mfccValues.all { it.isFinite() }) {
                            val scaledMFCC = mfccValues.map { it * 5f }.toFloatArray()
                            listener?.invoke(AudioDataEntity(scaledMFCC))
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

            Log.d("AudioCollector", "Starting AudioDispatcher in Coroutine")
            coroutineScope.launch {
                dispatcher?.run()
            }

        } catch (e: SecurityException) {
            Log.e("AudioCollector", "Permission denied: ${e.message}")
        }
    }

    override fun stop() {
        Log.d("AudioCollector", "Stopping AudioCollector and AudioDispatcher.")
        dispatcher?.stop()
        dispatcher = null
        stopRecording()
        coroutineScope.cancel()

        Log.d("AudioCollector", "AudioCollector stopped successfully.")
    }

    private fun createAudioDispatcher(sampleRate: Int, bufferSize: Int, bufferOverlap: Int): AudioDispatcher? {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("AudioCollector", "Permission denied! User must enable RECORD_AUDIO manually in settings.")
            return null
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096) * 4

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        shouldRecord = true
        audioRecord?.startRecording()
        Log.d("AudioCollector", "AudioRecord started recording.")

        coroutineScope.launch {
            val buffer = ShortArray(bufferSize)
            while (shouldRecord) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (readSize > 0) {
                    writeAudioBuffer(buffer.copyOf(readSize))
                    Log.d("AudioCollector", "Added ${readSize} samples to buffer.")
                } else {
                    Log.e("AudioCollector", "AudioRecord.read() returned $readSize, possible error!")
                }
                delay(5) // Prevents excessive CPU usage
            }
            Log.d("AudioCollector", "Stopping AudioRecord coroutine.")
        }

        return AudioDispatcher(this, bufferSize, bufferOverlap).also {
            Log.d("AudioCollector", "AudioDispatcher created with bufferSize=$bufferSize, bufferOverlap=$bufferOverlap")
        }
    }

    private fun stopRecording() {
        shouldRecord = false
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
        Log.d("AudioCollector", "AudioRecord stopped and released.")
    }

    // 🔹 Implemented `TarsosDSPAudioInputStream` Functions Inside `AudioCollector`
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        Log.d("AudioCollector", "Attempting to read $length bytes from buffer")

        val safeLength = minOf(length, buffer.size - offset)
        val byteBuffer = ByteBuffer.wrap(buffer, offset, safeLength).order(ByteOrder.LITTLE_ENDIAN)
        var bytesRead = 0
        var retries = 0

        while (bytesRead < safeLength && retries < 10) {
            val sample = audioBuffer.poll()

            if (sample == null) {
                Log.w("AudioCollector", "Buffer empty! Retrying ${10 - retries} more times.")

                runBlocking {
                    withContext(Dispatchers.IO) {
                        delay(50) // Prevents UI blocking
                    }
                }

                retries++
                continue
            }

            if (byteBuffer.remaining() >= 2) {
                byteBuffer.putShort(sample)
                bytesRead += 2
            } else {
                Log.e("AudioCollector", "ByteBuffer overflow prevented!")
                break
            }
        }

        if (bytesRead == 0) {
            Log.e("AudioCollector", "No audio data read! Returning -1 to prevent crashes.")
            return -1
        }

        Log.d("AudioCollector", "Processed $bytesRead bytes of audio")
        return bytesRead
    }

    override fun getFormat(): TarsosDSPAudioFormat {
        return TarsosDSPAudioFormat(16000f, 16, 1, true, false)
    }

    override fun skip(n: Long): Long = 0L
    override fun close() {}
    override fun getFrameLength(): Long = -1

    private fun writeAudioBuffer(shorts: ShortArray) {
        for (sample in shorts) {
            if (!audioBuffer.offer(sample)) {
                audioBuffer.poll()
                audioBuffer.offer(sample)
            }
        }
    }
}
