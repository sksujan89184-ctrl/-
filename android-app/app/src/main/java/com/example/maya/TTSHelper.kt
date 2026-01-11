package com.example.maya

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

/**
 * TTSHelper
 * - Placeholder helper showing how to call an external Edge-TTS server endpoint.
 * - For production, host a secure server that calls Edge-TTS Nabanita (or other voices)
 *   and returns audio. Do not call Edge-TTS directly from the device with secret keys.
 */
object TTSHelper {
    private val client = OkHttpClient()

    fun requestTts(serverUrl: String, text: String, lang: String = "bn"): ByteArray? {
        val json = "{\"text\": \"${escapeJson(text)}\", \"lang\": \"$lang\" }"
        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)
        val request = Request.Builder().url(serverUrl).post(body).build()
        client.newCall(request).execute().use { resp ->
            if (resp.isSuccessful) return resp.body?.bytes()
            return null
        }
    }

    private fun escapeJson(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
}
