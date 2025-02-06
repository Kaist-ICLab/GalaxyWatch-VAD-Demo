package kaist.iclab.vad_demo.core.model

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.mfcc.MFCC
import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kaist.iclab.vad_demo.core.tarsosandroid.AudioDispatcherFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI
import kotlin.math.sin

class VADModel(
    private val audioCollector: AudioCollector
) : ModelInterface<Boolean> {
    private val _outputStateFlow = MutableStateFlow(false)
    override val outputStateFlow: StateFlow<Boolean>
        get() = _outputStateFlow

    /**
     * Generates fake sine wave audio data for testing.
     */
    private fun generateFakeAudioData(samples: Int, sampleRate: Int): FloatArray {
        val fakeAudio = FloatArray(samples)
        val frequency = 420f // Simulated speech frequency

        for (i in fakeAudio.indices) {
            fakeAudio[i] = sin(2.0 * PI * frequency * i / sampleRate).toFloat()
        }

        return fakeAudio
    }

    /**
     * Extracts MFCC features from audio using AudioDispatcher from the core package.
     */
    /**
     * Extracts MFCC features from audio using AudioDispatcher from the core package.
     */
    private fun preprocess(audio: FloatArray): FloatArray {
        val sampleRate = 16000
        val frameSize = 1024
        val hopSize = 512
        val numMFCCs = 13

        val dispatcher = AudioDispatcherFactory.fromFloatArray(audio, sampleRate, frameSize, hopSize)


        val mfccProcessor = MFCC(frameSize, sampleRate.toFloat(), numMFCCs, 20, 300f, 8000f)
        val mfccFeatures = mutableListOf<FloatArray>()

        dispatcher.addAudioProcessor(mfccProcessor)
        dispatcher.addAudioProcessor(object : AudioProcessor {
            override fun process(audioEvent: AudioEvent): Boolean {
                val mfccCopy = mfccProcessor.mfcc.clone()
                mfccFeatures.add(mfccCopy)

                println("Frame ${mfccFeatures.size} First Sample: ${audioEvent.getFloatBuffer()[0]}")
            println("MFCC Frame ${mfccFeatures.size}: ${mfccCopy.joinToString(", ")}")
                return true // Continue processing
            }


            override fun processingFinished() {
                println("MFCC Extraction Complete: ${mfccFeatures.size} frames processed.")
            }
        })

        dispatcher.run() // Run processing

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
     * Starts VAD processing using fake sine wave audio.
     */
    override fun start() {
        val sampleRate = 16000
        val fakeAudio = generateFakeAudioData(sampleRate, sampleRate) // 1 second of fake audio

        val mfccFeatures = preprocess(fakeAudio) // Extract MFCC features
        _outputStateFlow.value = inference(mfccFeatures) // Simulated decision

        println("VAD Processing Complete: ${_outputStateFlow.value}")
    }

    override fun stop() {
        println("VAD Stopped")
    }
}
