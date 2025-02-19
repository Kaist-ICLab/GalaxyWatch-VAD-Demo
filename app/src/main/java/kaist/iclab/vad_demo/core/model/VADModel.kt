package kaist.iclab.vad_demo.core.model

import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import kaist.iclab.vad_demo.core.preprocess.TarsosDSPMFCCPreprocessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class VADModel(
    private val context: Context,
    private val mfccPreprocessor: TarsosDSPMFCCPreprocessor
) : ModelInterface {
    private val numFrame = 128  // Ensure we collect 128 frames before inference
    private val numMFCCs = 32
    private val vadThreshold = -0.095

    private val modelFilePath = "sgvad_fixed.tflite"
    private val _outputStateFlow = MutableStateFlow(false)
    override val outputStateFlow: StateFlow<Boolean>
        get() = _outputStateFlow

    private var tfliteInterpreter: Interpreter? = null

    private fun checkModelInputShape() {
        tfliteInterpreter?.let {
            val inputDetails = it.getInputTensor(0)
            val shape = inputDetails.shape() // Get input shape

            Log.d("VADModel", "Expected Model Input Shape: ${shape.joinToString(", ")}")
        }
    }
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun start() {
        Log.d("VADModel", "Starting VAD Processing.")
        loadModel()
        mfccPreprocessor.listener = { mfccInput ->
            if (mfccInput.size == numFrame) {  // Only run inference when 128 frames are available
                inference(mfccInput)
            }
        }
    }

    override fun stop() {
        Log.d("VADModel", "Stopping VAD Processing.")
        mfccPreprocessor.listener = null
        mfccPreprocessor.stop()
        releaseModel()
    }

    private fun loadModel() {
        if (tfliteInterpreter == null) {
            try {
                val assetManager = context.assets
                assetManager.openFd(modelFilePath).use { fileDescriptor ->
                    FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                        val fileChannel = inputStream.channel
                        val startOffset = fileDescriptor.startOffset
                        val declaredLength = fileDescriptor.declaredLength
                        tfliteInterpreter = Interpreter(
                            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                        )
                    }
                }
                checkModelInputShape()
            } catch (e: Exception) {
                Log.e("VADModel", "Model Load Failed: ${e.message}")
            }
        }
    }

    private fun releaseModel() {
        tfliteInterpreter?.close()
        tfliteInterpreter = null
    }



    private val vadScoresHistory = ArrayDeque<Float>(128)
    private var sumVadScores = 0f  // Store the sum for faster rolling average calculation

    private fun inference(mfccInput: Array<FloatArray>) {
        try {
            val inputBuffer = ByteBuffer.allocateDirect(32 * 1 * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())

            // Send only the last frame
            mfccInput.last().forEach { value -> inputBuffer.putFloat(value) }
            inputBuffer.rewind()

            val outputBuffer = ByteBuffer.allocateDirect(1 * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())

            tfliteInterpreter?.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()

            val vadScore = outputBuffer.float
            Log.d("VADModel", "VAD Score: $vadScore")

            // Rolling window update: remove oldest, add newest
            if (vadScoresHistory.size == 128) {
                sumVadScores -= vadScoresHistory.removeFirst()  // Subtract removed value from sum
            }
            vadScoresHistory.addLast(vadScore)
            sumVadScores += vadScore  // Add new value to sum

            // Compute rolling average in O(1) time instead of O(n)
            val avgScore = if (vadScoresHistory.size == 128) sumVadScores / 128f else sumVadScores / vadScoresHistory.size
            Log.d("VADModel", "Rolling VAD Avg Score: $avgScore")

            _outputStateFlow.value = avgScore > vadThreshold
        } catch (e: Exception) {
            Log.e("VADModel", "Error during inference: ${e.message}")
            e.printStackTrace()
        }
    }


}