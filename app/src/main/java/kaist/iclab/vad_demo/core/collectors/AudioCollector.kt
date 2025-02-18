package kaist.iclab.vad_demo.core.collectors

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import androidx.annotation.RequiresPermission
import kaist.iclab.vad_demo.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioCollector: CollectorInterface {

    override var listener: ((AudioDataEntity) -> Unit)? = null

    data class Config(
        val invokeRate: Long,
        val samplesPerFrame: Int,
        val overlap: Int,
        val sampleRate: Int,
        val micType: Int,
        val channel: Int,
        val encoding: Int,
    )

    private val defaultConfig = Config(
        invokeRate = 1000L, // 1 second
        samplesPerFrame = 2048,
        overlap = 1024,
        sampleRate = 16000,
        AudioSource.VOICE_RECOGNITION,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val configFlow = MutableStateFlow(defaultConfig)


    private var job: Job? = null

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun start() {
        Util.log("Start audio collection")
        job = CoroutineScope(Dispatchers.IO).launch{
            getAudioFlow().collect {
                listener?.invoke(AudioDataEntity(it.map { it.toFloat() }.toFloatArray()))
            }
        }

    }

    override fun stop() {
        Util.log("Stop audio collection")
        job?.cancel()
        job = null
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun getAudioFlow(): Flow<ShortArray> = flow {
        val bufferSize = AudioRecord.getMinBufferSize(
            configFlow.value.sampleRate,
            configFlow.value.channel,
            configFlow.value.encoding
        )
        val audioRecord = AudioRecord(
            configFlow.value.micType,
            configFlow.value.sampleRate,
            configFlow.value.channel,
            configFlow.value.encoding,
            bufferSize
        )
        audioRecord.startRecording()
        val buffer = ShortArray(bufferSize)
        try {
            while (currentCoroutineContext().isActive) { // Keep collecting data while the coroutine is active
                val readSize = audioRecord.read(buffer, 0, buffer.size)
                if (readSize > 0) {
                    emit(buffer.copyOf(readSize)) // Emit the audio data
                }
                delay(configFlow.value.invokeRate) // Small delay to avoid CPU overload
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }
}


//        audioCollector.listener = { audioData ->
//            if (audioData.data.isNotEmpty()) {
//                Log.d("VADModel", "Received MFCC Data for Inference: ${audioData.data.contentToString()}")
//
//                // 🔹 Maintain Rolling Buffer of 100 Frames
//                if (mfccFrameBuffer.size >= requiredFrameCount) {
//                    mfccFrameBuffer.removeFirst() // Remove oldest frame
//                }
//                mfccFrameBuffer.add(audioData.data) // Add new frame
//
//                // 🔹 Run inference once buffer is full (100 frames)
//                if (mfccFrameBuffer.size == requiredFrameCount) {
//                    val vadResult = inference(mfccFrameBuffer.toTypedArray())
//                    _outputStateFlow.value = vadResult
//
//                    Log.d("VADModel", "VAD Result: $vadResult")
//                }
//            } else {
//                Log.e("VADModel", "No MFCC data received for inference!")
//            }
//        }