package kaist.iclab.vad_demo.core.model

import android.content.Context
import android.util.Log
import kaist.iclab.vad_demo.core.collectors.AudioCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class VADModel(
    private val context: Context,
    private val audioCollector: AudioCollector
) : ModelInterface<Boolean> {

    private val _outputStateFlow = MutableStateFlow(false)
    override val outputStateFlow: StateFlow<Boolean>
        get() = _outputStateFlow

    private var tfliteInterpreter: Interpreter? = null

    // 🔹 Buffer to accumulate MFCC frames (rolling buffer)
    private val mfccFrameBuffer = ArrayDeque<FloatArray>(100) // Efficient queue-like behavior
    private val requiredFrameCount = 100
    private val numMFCCs = 13
    private val modelFile = "sgvad_mfcc.tflite"

    init {
        Log.d("VADModel", "VADModel instance created")
    }

    /**
     * Initializes or reinitializes the TFLite interpreter
     */
    private fun startInterpreter() {
        if (tfliteInterpreter != null) {
            Log.d("VADModel", "Interpreter already initialized.")
            return
        }

        try {
            Log.d("VADModel", "Loading TFLite Model: $modelFile")
            tfliteInterpreter = Interpreter(loadModelFile(context, modelFile))
            Log.d("VADModel", "Model Loaded Successfully")
        } catch (e: Exception) {
            Log.e("VADModel", "Model Load Failed: ${e.message}")
        }
    }

    /**
     * Runs SG-VAD inference using TFLite.
     */
    private fun inference(mfccInput: Array<FloatArray>): Boolean {
        //Log.d("VADModel", "inference() function called!")

        if (tfliteInterpreter == null) {
            Log.e("VADModel", "Error: TFLite Interpreter is not initialized!")
            return false
        }

        val timeSteps = mfccInput.size
        //Log.d("VADModel", "Received MFCC frames for inference: $timeSteps frames")

        if (timeSteps < requiredFrameCount) {
            Log.e("VADModel", "Not enough MFCC frames for inference. TimeSteps: $timeSteps, Required: $requiredFrameCount")
            return false
        }

        try {
            val inputBuffer = ByteBuffer.allocateDirect(timeSteps * numMFCCs * 4)
                .order(ByteOrder.nativeOrder())

            mfccInput.forEach { it.forEach { value -> inputBuffer.putFloat(value) } }

            val outputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())

            //Log.d("VADModel", "Running inference on TFLite model...")
            tfliteInterpreter?.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val vadScore = outputBuffer.float
            Log.d("VADModel", "VAD Inference complete! Score: $vadScore")

            return vadScore <= 0.6f
        } catch (e: Exception) {
            Log.e("VADModel", "Error during inference: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Starts real-time VAD processing.
     */
    override fun start() {
        Log.d("VADModel", "Starting VAD Processing.")
        startInterpreter()
        audioCollector.start()

        audioCollector.listener = { audioData ->
            if (audioData.data.isNotEmpty()) {
                Log.d("VADModel", "Received MFCC Data for Inference: ${audioData.data.contentToString()}")

                // 🔹 Maintain Rolling Buffer of 100 Frames
                if (mfccFrameBuffer.size >= requiredFrameCount) {
                    mfccFrameBuffer.removeFirst() // Remove oldest frame
                }
                mfccFrameBuffer.add(audioData.data) // Add new frame

                // 🔹 Run inference once buffer is full (100 frames)
                if (mfccFrameBuffer.size == requiredFrameCount) {
                    val vadResult = inference(mfccFrameBuffer.toTypedArray())
                    _outputStateFlow.value = vadResult

                    Log.d("VADModel", "VAD Result: $vadResult")
                }
            } else {
                Log.e("VADModel", "No MFCC data received for inference!")
            }
        }
    }

    /**
     * Stops VAD processing and releases resources.
     */
    override fun stop() {
        Log.d("VADModel", "Stopping VAD Processing.")
        audioCollector.stop()
        tfliteInterpreter?.close()
        tfliteInterpreter = null
    }

    /**
     * Loads the TFLite model file from assets.
     */
    private fun loadModelFile(context: Context, modelFile: String): ByteBuffer {
        val assetManager = context.assets
        val fileDescriptor = assetManager.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
