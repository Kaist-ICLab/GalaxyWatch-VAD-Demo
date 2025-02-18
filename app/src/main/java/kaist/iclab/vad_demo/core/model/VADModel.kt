package kaist.iclab.vad_demo.core.model

import android.content.Context
import android.media.AudioFormat
import android.util.Log
import androidx.annotation.RequiresPermission
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import be.tarsos.dsp.mfcc.MFCC
import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class VADModel(
    private val context: Context,
    private val audioCollector: AudioCollector
) : ModelInterface {
    private val numMFCCs = 13
    private val modelFilePath = "sgvad_mfcc.tflite"
    private val vadThreshold = .6f
    private val _outputStateFlow = MutableStateFlow(false)
    override val outputStateFlow: StateFlow<Boolean>
        get() = _outputStateFlow

    private val pipedOutputStream = PipedOutputStream()
    private val pipedInputStream = PipedInputStream(pipedOutputStream, 4096) // Streaming Buffer
    /**
     * Starts real-time VAD processing.
     */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun start() {
        Log.d("VADModel", "Starting VAD Processing.")
        loadModel()
        audioCollector.listener = {
            val buffer = ByteBuffer.allocate(it.data.size * Float.SIZE_BYTES)
            it.data.forEach { buffer.putFloat(it) }
            pipedOutputStream.write(buffer.array())
//            _outputStateFlow.value = inference(it.data)
        }
        audioCollector.start()
    }

    /**
     * Stops VAD processing and releases resources.
     */
    override fun stop() {
        Log.d("VADModel", "Stopping VAD Processing.")
        audioCollector.stop()
        releaseModel()
    }

    private var tfliteInterpreter: Interpreter? = null

    private fun loadModel() {
        tfliteInterpreter?.let {
            try {
                val assetManager = context.assets
                val fileDescriptor = assetManager.openFd(modelFilePath)
                val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                tfliteInterpreter = Interpreter(
                    fileChannel.map(
                        FileChannel.MapMode.READ_ONLY,
                        startOffset,
                        declaredLength
                    )
                )
            } catch (e: Exception) {
                Log.e("VADModel", "Model Load Failed: ${e.message}")
            }
        }
    }

    private fun releaseModel() {
        tfliteInterpreter?.close()
        tfliteInterpreter = null
    }

    data class Config(
        val sampleRate: Int = 16000,
        val channel: Int = AudioFormat.CHANNEL_IN_MONO,
        val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
        val numMFCCs: Int = 13,
        val numMelFilters: Int =20,
        val lowerFitlerFreq: Float = 300f,
        val upperFilterFreq: Float = 8000f,
        val samplesPerFrame: Int = 2048,
    )
    private val defaultConfig = Config()

    private fun convert2MFCC(audioData: FloatArray): Array<FloatArray> {
        pipedInputStream.
        val mfccProcessor = MFCC(
            defaultConfig.samplesPerFrame,
            defaultConfig.sampleRate.toFloat(),
            defaultConfig.numMFCCs,
            defaultConfig.numMelFilters,
            defaultConfig.lowerFitlerFreq,
            defaultConfig.upperFilterFreq
        )

        // Convert PCM data into a continuously updating InputStream
        val audioInputStream = UniversalAudioInputStream(
            pipedInputStream,
            TarsosDSPAudioFormat(
                defaultConfig.sampleRate.toFloat(),
                when(defaultConfig.encoding) {
                    AudioFormat.ENCODING_PCM_16BIT -> 16
                    AudioFormat.ENCODING_PCM_8BIT -> 8
                    else -> 16
                }.toInt(),
                when(defaultConfig.channel) {
                    AudioFormat.CHANNEL_IN_MONO -> 1
                    AudioFormat.CHANNEL_IN_STEREO -> 2
                    else -> 1
                },
                true,
                false)
        )

        val dispatcher = AudioDispatcher(audioInputStream, bufferSize, bufferOverlap).apply {
            addAudioProcessor(mfccProcessor)
            addAudioProcessor(object : AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    coroutineScope.launch {
                        mfccProcessor.process(audioEvent)
                        val mfccValues = mfccProcessor.mfcc
                        if (mfccValues.isNotEmpty() && mfccValues.all { it.isFinite() }) {
                            listener?.invoke(AudioDataEntity(mfccValues.map { it * 5f }
                                .toFloatArray()))
                        }
                    }
                    return true
                }

                override fun processingFinished() {
                    Log.d("AudioCollector", "MFCC processing finished.")
                }
            })
        }
        dispatcher.run()
    }

    private val inputBuffer = ByteBuffer.allocateDirect(mfccInput.size * numMFCCs * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
    private val outputBuffer = ByteBuffer.allocateDirect(Float.SIZE_BYTES).order(ByteOrder.nativeOrder())

    private fun inference(audioData: FloatArray): Boolean {
        val mfccInput = convert2MFCC(audioData)

        try {
            inputBuffer.rewind()
            mfccInput.forEach { it.forEach { value -> inputBuffer.putFloat(value) } }
            tfliteInterpreter?.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()
            val vadScore = outputBuffer.float
            return vadScore <= vadThreshold
        } catch (e: Exception) {
            Log.e("VADModel", "Error during inference: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

}
