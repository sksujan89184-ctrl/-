package com.example.maya

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

class AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startRecording() {
        try {
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
            Log.d("AudioRecorder", "Recording started")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Could not start recording: ${e.message}")
        }
    }

    fun stopRecording(): ShortArray {
        isRecording = false
        val buffer = ShortArray(sampleRate * 3) // ৩ সেকেন্ডের বাফার
        
        try {
            audioRecord?.let {
                it.read(buffer, 0, buffer.size)
                it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recording: ${e.message}")
        } finally {
            audioRecord = null
        }
        
        return buffer
    }
    
    fun isRecordingNow(): Boolean = isRecording
}
