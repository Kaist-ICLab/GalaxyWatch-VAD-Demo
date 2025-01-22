package core.mfcc_extractor

import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

class MFCC(
    private val sampleRate: Int,
    private val numCoefficients: Int = 13,
    private val numFilters: Int = 26,
    private val fftSize: Int = 512
) {
    private val melFilterBank: Array<DoubleArray>
    private val fft = FFT(fftSize)

    init {
        melFilterBank = createMelFilterBank()
    }

    private fun createMelFilterBank(): Array<DoubleArray> {
        val melFilterBank = Array(numFilters) { DoubleArray(fftSize / 2 + 1) }
        val melMin = 0.0
        val melMax = 2595 * log10(1 + sampleRate / 2.0 / 700)
        val melPoints = DoubleArray(numFilters + 2) { i -> melMin + i * (melMax - melMin) / (numFilters + 1) }
        val hzPoints = melPoints.map { 700 * (10.0.pow(it / 2595) - 1) }
        val bin = hzPoints.map { (it * fftSize / sampleRate).toInt() }

        for (i in 1 until bin.size - 1) {
            for (j in bin[i - 1]..bin[i]) {
                melFilterBank[i - 1][j] = (j - bin[i - 1]).toDouble() / (bin[i] - bin[i - 1])
            }
            for (j in bin[i]..bin[i + 1]) {
                melFilterBank[i - 1][j] = (bin[i + 1] - j).toDouble() / (bin[i + 1] - bin[i])
            }
        }

        return melFilterBank
    }

    fun extractFeatures(samples: DoubleArray): DoubleArray {
        val real = samples.copyOf(fftSize)
        val imag = DoubleArray(fftSize)
        fft.fft(real, imag)

        val powerSpectrum = real.zip(imag) { re, im -> re * re + im * im }
        val melEnergies = melFilterBank.map { filter ->
            filter.zip(powerSpectrum).sumOf { it.first * it.second }
        }

        val logMelEnergies = melEnergies.map { ln(it + 1e-6) }.toDoubleArray()
        return computeDCT(logMelEnergies)
    }

    private fun computeDCT(input: DoubleArray): DoubleArray {
        val result = DoubleArray(numCoefficients)
        val factor = Math.PI / input.size
        for (i in result.indices) {
            result[i] = input.indices.sumOf { j ->
                input[j] * cos(factor * i * (j + 0.5))
            }
        }
        return result
    }
}
