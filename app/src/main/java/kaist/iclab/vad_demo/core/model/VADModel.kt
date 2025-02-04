package kaist.iclab.vad_demo.core.model

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory
import be.tarsos.dsp.mfcc.MFCC
import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sin
import kotlin.math.PI

class VADModel(
    private val audioCollector: AudioCollector
) : ModelInterface<Boolean> {
    private val _outputStateFlow = MutableStateFlow(false)
    override val outputStateFlow: StateFlow<Boolean>
        get() = _outputStateFlow

    /**
     * Generates fake audio data for testing.
     */
    private fun generateFakeAudioData(samples: Int, sampleRate: Int): FloatArray {
        val fakeAudio = FloatArray(samples)
        val frequency = 440f // Simulated speech frequency

        for (i in fakeAudio.indices) {
            fakeAudio[i] = sin(2.0 * PI * frequency * i / sampleRate).toFloat()
        }

        return fakeAudio
    }

    /**
     * Extracts MFCC features from audio data.
     */
    private fun preprocess(audio: FloatArray): FloatArray {
        val sampleRate = 16000
        val frameSize = 1024
        val numMFCCs = 13

        val audioFormat = be.tarsos.dsp.io.TarsosDSPAudioFormat(
            sampleRate.toFloat(), 16, 1, true, false
        )

        val mfccProcessor = MFCC(frameSize, sampleRate.toFloat(), numMFCCs, 20, 300f, 8000f)
        val mfccFeatures = mutableListOf<FloatArray>()


        for (i in 0 until audio.size step frameSize) {
            val end = (i + frameSize).coerceAtMost(audio.size)
            val frame = FloatArray(frameSize) { index ->
                if (index < end - i) audio[i + index] else 0f
            }

            val audioEvent = AudioEvent(audioFormat)
            audioEvent.setFloatBuffer(frame)


            mfccProcessor.process(audioEvent)
            mfccFeatures.add(mfccProcessor.mfcc)
            // Print extracted MFCCs for each frame
            //println("MFCC Frame ${mfccFeatures.size}: ${mfccProcessor.mfcc.joinToString(", ")}")
        }

        println("MFCC Extraction Complete: ${mfccFeatures.size} frames processed.")

        return mfccFeatures.flatMap { it.toList() }.toFloatArray()
    }




    /**
     * Simulated inference using MFCC features.
     */
    private fun inference(input: FloatArray): Boolean {
        println("Running inference on ${input.size} MFCC features...")
        return input.sum() > 0 // Simulated decision logic
    }

    /**
     * Starts VAD processing using **fake** audio data.
     */
    override fun start() {
        val sampleRate = 16000 // Standard sample rate
        val fakeAudio = generateFakeAudioData(16000, sampleRate) // 1 second of fake audio

        val mfccFeatures = preprocess(fakeAudio) // Extract MFCC features
        _outputStateFlow.value = inference(mfccFeatures) // Simulated decision

        println("VAD Processing Complete: ${_outputStateFlow.value}")
    }

    override fun stop() {
        println("VAD Stopped")
    }
}
