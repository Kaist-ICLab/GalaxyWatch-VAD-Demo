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
    private val numFrame = 100 /* TODO: Why we need 100? */
    private val numMFCCs = 64
    private val sampleRate = 16000
    private val frameLength = 320
    private val overlap = 160
    private val numMelFilters = 64
    private val lowerFilterFreq = 0
    private val upperFilterFreq = 8000 //TODO there's no upper Limit in the original code

    var inputDeque = ArrayDeque<FloatArray>(numFrame)
    private var dispatcher: AudioDispatcher? = null
    private var job: Job? = null
    var listener: ((Array<FloatArray>) -> Unit)? = null

    fun init(audioPipedInputStream: PipedInputStream) {
        val audioInputStream = UniversalAudioInputStream(
            audioPipedInputStream,
            TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        ) // 16-bit encoding, mono channel

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
                    if(inputDeque.size == numFrame) inputDeque.removeFirst()
                    inputDeque.addLast(mfcc)
                    if(inputDeque.size == numFrame){
                        listener?.invoke(inputDeque.toTypedArray())
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