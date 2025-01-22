package core

interface VADInference {
    /**
     * Initialize the VAD model, preparing it for inference.
     */
    fun initialize()

    /**
     * Perform voice activity detection using MFCC features.
     *
     * @param mfccFeatures A FloatArray of extracted MFCC features.
     * @return A Boolean indicating whether voice activity is detected.
     */
    fun detect(mfccFeatures: FloatArray): Boolean

    /**
     * Release resources used by the VAD inference engine.
     */
    fun release()
}
