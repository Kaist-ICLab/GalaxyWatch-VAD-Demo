package kaist.iclab.vad_demo.core.tarsosandroid

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UniversalAudioInputStream(
    private val audioData: FloatArray,
    private val format: TarsosDSPAudioFormat
) : TarsosDSPAudioInputStream {

    private var readIndex = 0

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (readIndex >= audioData.size) return -1 // End of stream

        val samplesToRead = minOf(length / 4, audioData.size - readIndex) // 4 bytes per sample (32-bit)
        val byteBuffer = ByteBuffer.wrap(buffer, offset, samplesToRead * 4).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until samplesToRead) {
            val sample = (audioData[readIndex++] * Int.MAX_VALUE).toInt() // Convert to 32-bit PCM
            byteBuffer.putInt(sample)
        }

        return samplesToRead * 4 // Return number of bytes read
    }


    override fun getFormat(): TarsosDSPAudioFormat = format

    override fun skip(n: Long): Long {
        val skipped = minOf(n, (audioData.size - readIndex).toLong())
        readIndex += skipped.toInt()
        return skipped
    }

    override fun close() {}

    override fun getFrameLength(): Long = audioData.size.toLong()
}

