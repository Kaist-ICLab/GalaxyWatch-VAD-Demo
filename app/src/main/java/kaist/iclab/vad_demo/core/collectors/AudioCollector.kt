package kaist.iclab.vad_demo.core.collectors

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import androidx.annotation.RequiresPermission
import kaist.iclab.vad_demo.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioCollector:CollectorInterface {
    /*
    * Frame Length: The number of samples in each frame
    * Overlap: The number of samples that overlap between adjacent frames
    * */
    data class Config(
//        val numFrames: Int,
//        val frameLength: Int,
//        val overlap: Int,
        val bufferSize: Int,
        val sampleRate: Int,
        val micType: Int,
        val channel: Int,
        val encoding: Int,
    )

    private val defaultConfig = Config(
//        numFrames = 100,
//        frameLength = 320, // 0.02 seconds
//        overlap = 160, // 0.01 seconds
        bufferSize = 16000, // 1 second
        sampleRate = 16000, // 16 kHz
        AudioSource.VOICE_RECOGNITION,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val configFlow = MutableStateFlow(defaultConfig)

    private var audioPipedOutputStream: PipedOutputStream? = null
    override var audioPipedInputStream: PipedInputStream? = null
    private var job: Job? = null

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun start() {
        Util.log("Start audio collection")
        audioPipedOutputStream = PipedOutputStream()
        audioPipedInputStream = PipedInputStream(audioPipedOutputStream, configFlow.value.bufferSize)
        job = startAudioInput()
        job?.start()

    }

    override fun stop() {
        Util.log("Stop audio collection")
        audioPipedOutputStream?.close()
        audioPipedOutputStream = null
        audioPipedInputStream?.close()
        audioPipedInputStream = null
        job?.cancel()
        job = null
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startAudioInput(): Job {
        return CoroutineScope(Dispatchers.IO).launch {
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
            try {
                while(isActive){
                    val tempBuffer = ShortArray(bufferSize / 2) // 16-bit encoding -> 2 bytes per sample -> 1 Short per sample
                    val numRead = audioRecord.read(tempBuffer, 0, tempBuffer.size)
                    val byteBuffer = ByteBuffer.allocate(numRead * 2).order(ByteOrder.LITTLE_ENDIAN)
                    tempBuffer.take(numRead).forEach { byteBuffer.putShort(it) }
                    audioPipedOutputStream?.write(byteBuffer.array())
                }
            } finally {
                audioRecord.stop()
                audioRecord.release()
            }
        }
    }
}
