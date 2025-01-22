package core.mfcc_extractor

import kotlin.math.*

class FFT(private val size: Int) {
    private val cos: DoubleArray
    private val sin: DoubleArray

    init {
        if (Integer.highestOneBit(size) != size) {
            throw IllegalArgumentException("Size must be a power of 2.")
        }
        cos = DoubleArray(size / 2)
        sin = DoubleArray(size / 2)
        for (i in cos.indices) {
            val angle = -2.0 * Math.PI * i / size
            cos[i] = cos(angle)
            sin[i] = sin(angle)
        }
    }

    /**
     * Performs an in-place FFT on the provided real and imaginary arrays.
     */
    fun fft(real: DoubleArray, imag: DoubleArray) {
        if (real.size != size || imag.size != size) {
            throw IllegalArgumentException("Input arrays must match the FFT size.")
        }

        // Bit-reversal permutation
        val n = size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j >= bit) {
                j -= bit
                bit = bit shr 1
            }
            j += bit
            if (i < j) {
                real.swap(i, j)
                imag.swap(i, j)
            }
        }

        // Danielson-Lanczos section
        var blockSize = 2
        while (blockSize <= n) {
            val halfBlockSize = blockSize / 2
            val tableStep = size / blockSize
            for (i in 0 until n step blockSize) {
                var k = 0
                for (j in i until i + halfBlockSize) {
                    val tReal = cos[k] * real[j + halfBlockSize] - sin[k] * imag[j + halfBlockSize]
                    val tImag = cos[k] * imag[j + halfBlockSize] + sin[k] * real[j + halfBlockSize]
                    real[j + halfBlockSize] = real[j] - tReal
                    imag[j + halfBlockSize] = imag[j] - tImag
                    real[j] += tReal
                    imag[j] += tImag
                    k += tableStep
                }
            }
            blockSize *= 2
        }
    }
}

/**
 * Swaps two elements in a DoubleArray.
 */
private fun DoubleArray.swap(i: Int, j: Int) {
    val temp = this[i]
    this[i] = this[j]
    this[j] = temp
}
