package kaist.iclab.vad_demo.core.preprocess

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.PipedInputStream

class TarsosDSPMFCCPreprocessor {
    private val numFrame = 128  // Collect 128 frames before inference
    private val numMFCCs = 32
    private val sampleRate = 16000
    private val frameLength = 400
    private val overlap = 240
    private val numMelFilters = 64
    private val lowerFilterFreq = 0
    private val upperFilterFreq = 8000

    var inputDeque = ArrayDeque<FloatArray>(numFrame)
    private var dispatcher: AudioDispatcher? = null
    private var job: Job? = null
    var listener: ((Array<FloatArray>) -> Unit)? = null

    fun init(audioPipedInputStream: PipedInputStream) {
        val audioInputStream = UniversalAudioInputStream(
            audioPipedInputStream,
            TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        )

        val mfccProcessor = MFCC(
            frameLength,
            sampleRate.toFloat(),
            numMFCCs,
            numMelFilters,
            lowerFilterFreq.toFloat(),
            upperFilterFreq.toFloat()
        )

        dispatcher = AudioDispatcher(audioInputStream, frameLength, overlap).apply {
            addAudioProcessor(mfccProcessor)
            addAudioProcessor(object : AudioProcessor {
                override fun process(audioEvent: AudioEvent?): Boolean {
                    val mfcc = mfccProcessor.mfcc
                    if (inputDeque.size == numFrame) inputDeque.removeFirst()
                    inputDeque.addLast(mfcc)

                    if (inputDeque.size == numFrame) {
                        listener?.invoke(inputDeque.toTypedArray())  // Only send when we have 128 frames
                    }
                    return true
                }
                override fun processingFinished() {}
            })
        }
    }

    fun start() {
        inputDeque.clear()
        job = CoroutineScope(Dispatchers.IO).launch {
            dispatcher?.run()
        }
    }

    fun stop() {
        dispatcher?.stop()
        job?.cancel()
        dispatcher = null
        job = null
    }
}
