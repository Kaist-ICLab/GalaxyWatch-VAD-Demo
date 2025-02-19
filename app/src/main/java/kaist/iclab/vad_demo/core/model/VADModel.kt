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
    private val numFrame = 100
    private val numMFCCs = 64
    private val vadThreshold = .6f

    private val modelFilePath = "sgvad_mfcc.tflite"
    private val _outputStateFlow = MutableStateFlow(false)
    override val outputStateFlow: StateFlow<Boolean>
        get() = _outputStateFlow

    /**
     * Starts real-time VAD processing.
     */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun start() {
        Log.d("VADModel", "Starting VAD Processing.")
        loadModel()
        mfccPreprocessor.listener = { mfccInput ->
            inference(mfccInput)
        }
    }

    /**
     * Stops VAD processing and releases resources.
     */
    override fun stop() {
        Log.d("VADModel", "Stopping VAD Processing.")
        mfccPreprocessor.listener = null
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

    private val inputBuffer =
        ByteBuffer.allocateDirect(numFrame * numMFCCs * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
    private val outputBuffer =
        ByteBuffer.allocateDirect(Float.SIZE_BYTES).order(ByteOrder.nativeOrder())

    private fun inference(mfccInput: Array<FloatArray>) {
        try {
            inputBuffer.rewind()
            mfccInput.forEach { it.forEach { value -> inputBuffer.putFloat(value) } }
            tfliteInterpreter?.run(inputBuffer, outputBuffer)
            outputBuffer.rewind()
            val vadScore = outputBuffer.float
            _outputStateFlow.value = vadScore > vadThreshold
        } catch (e: Exception) {
            Log.e("VADModel", "Error during inference: ${e.message}")
            e.printStackTrace()
        }
    }
}
