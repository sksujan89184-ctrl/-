package com.example.maya

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max

/**
 * SpeakerVerifier
 * - Lightweight scaffold to load a TFLite model from assets and run inference.
 * - This file contains placeholders only. Replace model loading, preprocessing,
 *   and verification logic with your chosen model and pipeline.
 *
 * Implementation notes:
 * - Place your TFLite model in `app/src/main/assets/voice_model.tflite`.
 * - The model input shape and preprocessing must match the model you choose.
 * - Keep all voice templates encrypted if stored on-device.
 */
class SpeakerVerifier(private val context: Context) {

    private var interpreter: Interpreter? = null

    private val prefs: SharedPreferences

    init {
        // Setup encrypted prefs for storing templates securely (example)
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            "maya_secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        try {
            // load model from assets (placeholder path)
            interpreter = Interpreter(loadModelFile("voice_model.tflite"))
            Log.d("SpeakerVerifier", "TFLite model loaded (scaffold)")
        } catch (e: Exception) {
            Log.w("SpeakerVerifier", "Model load failed (expected for scaffold): ${e.message}")
            interpreter = null
        }
    }

    // Example API: enroll a voice template (placeholder)
    fun enroll(templateId: String, audioSamples: FloatArray) {
        // TODO: compute embedding from audioSamples and store securely
        // Example: val embedding = runModelOnAudio(audioSamples)
        // Store embedding encrypted in app-private storage / Keystore
        Log.d("SpeakerVerifier", "Enroll called for $templateId (scaffold)")
        // Placeholder: compute embedding and store as base64 in encrypted prefs
        val embedding = runModelOnAudio(audioSamples)
        val bytes = FloatArrayToByteArray(embedding)
        val b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        prefs.edit().putString("embed_$templateId", b64).apply()
    }

    // Example API: verify caller audio matches enrolled template
    fun verify(templateId: String, audioSamples: FloatArray): Boolean {
        // TODO: load stored embedding for templateId, compute embedding for audioSamples,
        // compare using cosine similarity or model-specific metric, and return result.
        Log.d("SpeakerVerifier", "Verify called for $templateId (scaffold)")
        val stored = prefs.getString("embed_$templateId", null) ?: return false
        val storedBytes = android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
        val storedEmbedding = ByteArrayToFloatArray(storedBytes)
        val embedding = runModelOnAudio(audioSamples)
        val score = cosineSimilarity(storedEmbedding, embedding)
        Log.d("SpeakerVerifier", "Cosine score=$score")
        // threshold is developer-configurable; placeholder 0.75
        return score >= 0.75f
    }

    // Run model on preprocessed audio and return embedding (placeholder signature)
    private fun runModelOnAudio(audioSamples: FloatArray): FloatArray {
        // Preprocess audio (basic normalization + log-mel) and attempt TFLite inference.
        // If interpreter is unavailable or inference fails, fall back to a deterministic
        // embedding computed from averaged log-mel energies (128-d).
        val preprocessed = preprocessAudio(audioSamples)

        // Try interpreter inference if model was loaded.
        if (interpreter != null) {
            try {
                val inputTensor = interpreter!!.getInputTensor(0)
                val shape = inputTensor.shape() // e.g. [1, T, F, 1] or [1, N]
                val dtype = inputTensor.dataType()

                // compute per-frame log-mel (and keep averaged fallback)
                val melFrames = computeLogMelSpectrogramFrames(preprocessed, sampleRate = 16000, melBins = 40)
                val avgLogMel = if (melFrames.isNotEmpty()) {
                    val avg = FloatArray(melFrames[0].size)
                    for (m in avg.indices) {
                        var s = 0f
                        for (t in melFrames.indices) s += melFrames[t][m]
                        avg[m] = s / melFrames.size
                    }
                    avg
                } else FloatArray(40) { 0f }

                // Prepare input according to expected shape
                val inputObj: Any = when (shape.size) {
                    2 -> {
                        // [1, N]
                        val n = shape[1]
                        val arr = Array(1) { FloatArray(n) }
                        // copy averaged mel if size matches otherwise pad/trim
                        for (i in 0 until n) arr[0][i] = if (i < avgLogMel.size) avgLogMel[i] else 0f
                        arr
                    }
                    3 -> {
                        // [1, T, F] -> create 3D float array from melFrames (pad/trim time)
                        val t = shape[1]
                        val f = shape[2]
                        val arr = Array(1) { Array(t) { FloatArray(f) } }
                        for (ti in 0 until t) {
                            val src = if (ti < melFrames.size) melFrames[ti] else FloatArray(f) { 0f }
                            for (fi in 0 until f) arr[0][ti][fi] = if (fi < src.size) src[fi] else 0f
                        }
                        arr
                    }
                    4 -> {
                        // [1, T, F, C] -> common for spectrograms
                        val t = shape[1]
                        val f = shape[2]
                        val c = shape[3]
                        val arr = Array(1) { Array(t) { Array(f) { FloatArray(c) } } }
                        for (ti in 0 until t) {
                            val src = if (ti < melFrames.size) melFrames[ti] else FloatArray(f) { 0f }
                            for (fi in 0 until f) for (ci in 0 until c) arr[0][ti][fi][ci] = if (fi < src.size) src[fi] else 0f
                        }
                        arr
                    }
                    else -> {
                        // fallback: single-dim
                        val arr = Array(1) { avgLogMel }
                        arr
                    }
                }

                // Build flat float buffer matching expected tensor size, then quantize if needed
                val totalSize = shape.reduce { acc, v -> acc * v }
                val flat = FloatArray(totalSize)
                // fill flat in row-major order (batch first). We assume batch=1.
                var idx = 0
                when (shape.size) {
                    2 -> {
                        val n = shape[1]
                        for (i in 0 until n) {
                            flat[idx++] = if (i < avgLogMel.size) avgLogMel[i] else 0f
                        }
                    }
                    3 -> {
                        val t = shape[1]
                        val f = shape[2]
                        for (ti in 0 until t) {
                            val src = if (ti < melFrames.size) melFrames[ti] else FloatArray(f) { 0f }
                            for (fi in 0 until f) {
                                flat[idx++] = if (fi < src.size) src[fi] else 0f
                            }
                        }
                    }
                    4 -> {
                        val t = shape[1]
                        val f = shape[2]
                        val c = shape[3]
                        for (ti in 0 until t) {
                            val src = if (ti < melFrames.size) melFrames[ti] else FloatArray(f) { 0f }
                            for (fi in 0 until f) for (ci in 0 until c) {
                                flat[idx++] = if (fi < src.size) src[fi] else 0f
                            }
                        }
                    }
                    else -> {
                        // fallback: copy avg into first dims
                        for (i in avgLogMel.indices) if (idx < flat.size) flat[idx++] = avgLogMel[i]
                    }
                }

                if (dtype == DataType.FLOAT32) {
                    val bb = ByteBuffer.allocateDirect(flat.size * 4).order(ByteOrder.nativeOrder())
                    for (v in flat) bb.putFloat(v)
                    bb.rewind()
                    val output = Array(1) { FloatArray(128) }
                    interpreter?.run(bb, output)
                    return output[0]
                } else if (dtype == DataType.UINT8 || dtype == DataType.INT8) {
                    // quantize using tensor's quantization params if available
                    val q = inputTensor.quantizationParams()
                    val scale = if (q != null) q.scale else 1.0f
                    val zeroPoint = if (q != null) q.zeroPoint else 0
                    val isSigned = (dtype == DataType.INT8)
                    val bb = ByteBuffer.allocateDirect(flat.size).order(ByteOrder.nativeOrder())
                    for (v in flat) {
                        val qv = kotlin.math.round(v / scale).toInt() + zeroPoint
                        val clamped = if (isSigned) qv.coerceIn(-128, 127) else qv.coerceIn(0, 255)
                        bb.put(clamped.toByte())
                    }
                    bb.rewind()
                    val output = Array(1) { FloatArray(128) }
                    interpreter?.run(bb, output)
                    return output[0]
                } else {
                    // unknown dtype -> fallback
                    Log.w("SpeakerVerifier", "Unsupported input tensor dtype: $dtype")
                }
            } catch (e: Exception) {
                Log.w("SpeakerVerifier", "Model inference failed, using fallback embedding: ${e.message}")
            }
        }
            } catch (e: Exception) {
                Log.w("SpeakerVerifier", "Model inference failed, using fallback embedding: ${e.message}")
            }
        }

        // Fallback: compute averaged log-mel (e.g., 40 bands) then map to 128-d vector.
        val logmel = computeLogMelSpectrogram(audioSamples, sampleRate = 16000, melBins = 40)
        return melTo128(logmel)
    }

    // Preprocessing and conversion use shared functions in AudioFeatures.kt
    // (preprocessAudio, computeLogMelSpectrogram, melTo128,
    //  FloatArrayToByteArray, ByteArrayToFloatArray)

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var na = 0f
        var nb = 0f
        val len = minOf(a.size, b.size)
        for (i in 0 until len) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        return if (na == 0f || nb == 0f) 0f else dot / (kotlin.math.sqrt(na) * kotlin.math.sqrt(nb))
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelFilename: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
