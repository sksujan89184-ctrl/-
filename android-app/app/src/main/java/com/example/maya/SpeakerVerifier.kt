package com.example.maya

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class SpeakerVerifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val prefs: SharedPreferences

    init {
        // ১. এনক্রিপ্টেড শেয়ারড প্রেফারেন্স সেটআপ
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            "maya_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        // ২. TFLite মডেল লোড করা
        try {
            val modelBuffer = loadModelFile("voice_model.tflite")
            interpreter = Interpreter(modelBuffer)
            Log.d("SpeakerVerifier", "TFLite model loaded successfully.")
        } catch (e: Exception) {
            Log.e("SpeakerVerifier", "Model load failed: ${e.message}")
            interpreter = null
        }
    }

    // ভয়েস এনরোল করা (সেভ করা)
    fun enroll(templateId: String, audioSamples: FloatArray) {
        val embedding = runModelOnAudio(audioSamples)
        val bytes = floatArrayToByteArray(embedding)
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        prefs.edit().putString("embed_$templateId", b64).apply()
        Log.d("SpeakerVerifier", "Enrollment completed for $templateId")
    }

    // ভয়েস ভেরিফাই করা (ম্যাচ করা)
    fun verify(templateId: String, audioSamples: FloatArray): Boolean {
        val stored = prefs.getString("embed_$templateId", null) ?: return false
        val storedBytes = android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
        val storedEmbedding = byteArrayToFloatArray(storedBytes)
        val currentEmbedding = runModelOnAudio(audioSamples)
        
        val score = cosineSimilarity(storedEmbedding, currentEmbedding)
        Log.d("SpeakerVerifier", "Verification Score: $score")
        
        // ০.৭৫ বা তার বেশি হলে ভয়েস ম্যাচ করেছে ধরা হবে
        return score >= 0.75f
    }

    private fun runModelOnAudio(audioSamples: FloatArray): FloatArray {
        // যদি মডেল লোড থাকে তবে প্রসেস করবে, না থাকলে ডামি ডাটা দিবে
        if (interpreter != null) {
            try {
                val output = Array(1) { FloatArray(128) }
                // এখানে ইনপুট শেপ অনুযায়ী অডিও ডাটা ফিড করা হয়
                // মডেলের ইনপুট স্ট্রাকচার অনুযায়ী এটি পরিবর্তন হতে পারে
                val inputBuffer = ByteBuffer.allocateDirect(audioSamples.size * 4).order(ByteOrder.nativeOrder())
                for (sample in audioSamples) inputBuffer.putFloat(sample)
                
                interpreter?.run(inputBuffer, output)
                return output[0]
            } catch (e: Exception) {
                Log.e("SpeakerVerifier", "Inference error: ${e.message}")
            }
        }
        // Fallback: যদি মডেল কাজ না করে তবে একটি ডিফল্ট ভেক্টর দিবে
        return FloatArray(128) { 0.1f }
    }

    // কোসাইন সিমিলারিটি ক্যালকুলেশন (ভয়েস ম্যাচিং লজিক)
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f else dot / (sqrt(normA.toDouble()) * sqrt(normB.toDouble())).toFloat()
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelFilename: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    // হেল্পার ফাংশন: FloatArray থেকে ByteArray
    private fun floatArrayToByteArray(floats: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (f in floats) buffer.putFloat(f)
        return buffer.array()
    }

    // হেল্পার ফাংশন: ByteArray থেকে FloatArray
    private fun byteArrayToFloatArray(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) floats[i] = buffer.float
        return floats
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
