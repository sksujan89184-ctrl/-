package com.example.maya

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Simple audio recorder that captures PCM 16-bit mono at 16 kHz.
 * Use `startRecording()` and `stopRecording()` to get raw PCM samples.
 */
class AudioRecorder(private val sampleRate: Int = 16000) {
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var recording = false
    private var pcmBytes: ByteArrayOutputStream = ByteArrayOutputStream()

    fun startRecording() {
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, max(minBufSize, sampleRate))
        recorder?.startRecording()
        recording = true
        pcmBytes = ByteArrayOutputStream()
        recordingThread = Thread {
            val buffer = ShortArray(1024)
            while (recording) {
                val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    val byteBuf = ByteBuffer.allocate(read * 2).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until read) byteBuf.putShort(buffer[i])
                    pcmBytes.write(byteBuf.array())
                }
            }
        }
        recordingThread?.start()
    }

    fun stopRecording(): ShortArray {
        recording = false
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
        }
        recorder = null
        recordingThread = null
        val bytes = pcmBytes.toByteArray()
        val shorts = ShortArray(bytes.size / 2)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in shorts.indices) shorts[i] = bb.short
        return shorts
    }

    fun saveWavFile(target: File, samples: ShortArray) {
        val fos = FileOutputStream(target)
        val channels = 1
        val byteRate = 16 * sampleRate * channels / 8
        // WAV header
        fos.write("RIFF".toByteArray())
        fos.write(intToByteArrayLE(36 + samples.size * 2))
        fos.write("WAVE".toByteArray())
        fos.write("fmt ".toByteArray())
        fos.write(intToByteArrayLE(16))
        fos.write(shortToByteArrayLE(1))
        fos.write(shortToByteArrayLE(channels.toShort()))
        fos.write(intToByteArrayLE(sampleRate))
        fos.write(intToByteArrayLE(byteRate))
        fos.write(shortToByteArrayLE((channels * 16 / 8).toShort()))
        fos.write(shortToByteArrayLE(16))
        fos.write("data".toByteArray())
        fos.write(intToByteArrayLE(samples.size * 2))
        // PCM data
        val bb = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (s in samples) bb.putShort(s)
        fos.write(bb.array())
        fos.flush()
        fos.close()
    }

    private fun intToByteArrayLE(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArrayLE(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }

    private fun max(a: Int, b: Int) = if (a > b) a else b
}
