package core

interface AudioInput {
    fun startRecording() // Starts audio recording
    fun stopRecording()  // Stops audio recording
    fun getAudioData(): FloatArray // Fetches raw audio data
}
