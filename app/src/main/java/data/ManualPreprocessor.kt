package core

import core.mfcc_extractor.MFCC

class ManualPreprocessor(
    private val sampleRate: Int,
    private val fftSize: Int,
    private val numCoefficients: Int,
    private val numFilters: Int
) : AudioPreprocessor {

    private val mfccExtractor = MFCC(sampleRate, numCoefficients, numFilters, fftSize)

    override fun processAudio(audioData: FloatArray): FloatArray {
        // Ensure FFT size is a power of 2
        if (fftSize and (fftSize - 1) != 0) {
            throw IllegalArgumentException("FFT size must be a power of 2")
        }

        // Convert FloatArray to DoubleArray for processing
        val doubleAudioData = audioData.map { it.toDouble() }.toDoubleArray()

        // Use extractFeatures from MFCC to calculate the MFCC features
        val mfccFeatures = mfccExtractor.extractFeatures(doubleAudioData)

        // Convert DoubleArray back to FloatArray for the result
        return mfccFeatures.map { it.toFloat() }.toFloatArray()
    }
}
