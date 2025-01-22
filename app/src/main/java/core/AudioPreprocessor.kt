package core

interface AudioPreprocessor {
    /**
     * Processes raw audio data and extracts features (e.g., MFCCs).
     * @param audioData The raw audio data as a FloatArray.
     * @return Extracted audio features as a FloatArray.
     */
    fun processAudio(audioData: FloatArray): FloatArray
}
