package com.example.maya

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, 
            AudioFormat.CHANNEL_IN_MONO, 
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, 
            sampleRate, 
            AudioFormat.CHANNEL_IN_MONO, 
            AudioFormat.ENCODING_PCM_16BIT, 
            bufferSize
        )
        audioRecord?.startRecording()
        isRecording = true
    }

    fun stopRecording(): ShortArray {
        isRecording = false
        val buffer = ShortArray(sampleRate * 3) // ৩ সেকেন্ডের জন্য বাফার
        audioRecord?.read(buffer, 0, buffer.size)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        return buffer
    }
}
