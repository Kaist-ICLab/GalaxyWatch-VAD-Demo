package kaist.iclab.vad_demo.core.utils

import android.util.Log
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue

class UniversalAudioInputStream(
    private val format: TarsosDSPAudioFormat
) : TarsosDSPAudioInputStream {

    companion object {
        private const val MAX_QUEUE_SIZE = 1000 // Limit queue size
        private const val CHUNK_SIZE = 512  // Read in 512-byte chunks
        private const val MAX_RETRIES = 10  // Retry up to 10 times
    }

    private val audioBuffer = LinkedBlockingQueue<Short>(MAX_QUEUE_SIZE)

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        Log.d("UniversalAudioInputStream", "🎧 Attempting to read $length bytes from buffer")

        val safeLength = minOf(length, buffer.size - offset) // Prevent exceeding buffer limit
        val byteBuffer = ByteBuffer.wrap(buffer, offset, safeLength).order(ByteOrder.LITTLE_ENDIAN)
        var bytesRead = 0
        var retries = 0

        while (bytesRead < safeLength && retries < MAX_RETRIES) {
            val sample = audioBuffer.poll()

            if (sample == null) {
                Log.w("UniversalAudioInputStream", "Buffer empty! Retrying ${MAX_RETRIES - retries} more times.")
                Thread.sleep(50)
                retries++
                continue
            }

            // Ensure ByteBuffer has enough space before writing
            if (byteBuffer.remaining() >= 2) {
                byteBuffer.putShort(sample)
                bytesRead += 2
            } else {
                Log.e("UniversalAudioInputStream", "ByteBuffer overflow prevented!")
                break
            }
        }

        if (bytesRead == 0) {
            Log.e("UniversalAudioInputStream", "No audio data read! Returning -1 to prevent crashes.")
            return -1
        }

        Log.d("UniversalAudioInputStream", "Processed $bytesRead bytes of audio")
        return bytesRead
    }


    fun write(shorts: ShortArray) {
        for (sample in shorts) {
            if (!audioBuffer.offer(sample)) {
                // Remove oldest sample if queue is full to prevent blockage
                audioBuffer.poll()
                audioBuffer.offer(sample)
            }
        }
    }

    override fun getFormat(): TarsosDSPAudioFormat = format

    override fun skip(n: Long): Long = 0L // Not required for real-time audio

    override fun close() {}

    override fun getFrameLength(): Long = -1 // Unknown for streaming audio
}
