package com.example.maya

import android.content.Context
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.tensorflow.lite.Interpreter
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
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            "maya_secure_prefs", masterKeyAlias, context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        try {
            // আপনার assets ফোল্ডারে voice_model.tflite থাকতে হবে
            interpreter = Interpreter(loadModelFile("voice_model.tflite"))
        } catch (e: Exception) {
            Log.e("SpeakerVerifier", "Model Load Failed: ${e.message}")
        }
    }

    fun enroll(templateId: String, audioSamples: FloatArray) {
        val embedding = runInference(audioSamples)
        val b64 = android.util.Base64.encodeToString(floatToByte(embedding), android.util.Base64.NO_WRAP)
        prefs.edit().putString("embed_$templateId", b64).apply()
    }

    fun verify(templateId: String, audioSamples: FloatArray): Boolean {
        val stored = prefs.getString("embed_$templateId", null) ?: return false
        val storedBytes = android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
        val score = calculateCosineSimilarity(byteToFloat(storedBytes), runInference(audioSamples))
        return score >= 0.75f // থ্রেশহোল্ড
    }

    private fun runInference(samples: FloatArray): FloatArray {
        val output = Array(1) { FloatArray(128) }
        if (interpreter != null) {
            val input = ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.nativeOrder())
            samples.forEach { input.putFloat(it) }
            interpreter?.run(input, output)
            return output[0]
        }
        return FloatArray(128) { 0.1f } 
    }

    private fun calculateCosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var na = 0f; var nb = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]
        }
        return if (na == 0f || nb == 0f) 0f else (dot / (sqrt(na) * sqrt(nb)))
    }

    private fun floatToByte(floats: FloatArray): ByteArray {
        val bb = ByteBuffer.allocate(floats.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        floats.forEach { bb.putFloat(it) }; return bb.array()
    }

    private fun byteToFloat(bytes: ByteArray): FloatArray {
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val floats = FloatArray(bytes.size / 4)
        for (i in floats.indices) floats[i] = bb.float; return floats
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fd = context.assets.openFd(modelName)
        val stream = FileInputStream(fd.fileDescriptor)
        return stream.channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }
}
