package com.example.maya

import org.json.JSONObject
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.ByteString
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebhookHelper {
    private val client = OkHttpClient()
    private val WEBHOOK_URL = System.getenv("WEBHOOK_URL") ?: "https://your-backend-api.com/maya-webhook"

    private val SUPABASE_URL = System.getenv("SUPABASE_URL") ?: ""
    private val SUPABASE_ANON_KEY = System.getenv("SUPABASE_ANON_KEY") ?: ""

    fun sendToSupabase(table: String, data: JSONObject) {
        if (SUPABASE_URL.isBlank()) return

        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), data.toString())
        val request = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/$table")
            .post(body)
            .addHeader("apikey", SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_ANON_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                android.util.Log.e("Supabase", "Failed to sync: ${e.message}")
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                // Success
            }
        })
    }

    fun sendAction(action: String, data: JSONObject, callback: (Boolean, String?) -> Unit) {
        val json = JSONObject()
        json.put("action", action)
        json.put("payload", data)
        json.put("timestamp", System.currentTimeMillis())

        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), json.toString())
        val request = Request.Builder()
            .url(WEBHOOK_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                // Handle offline or error by logging locally or retrying
                android.util.Log.e("WebhookHelper", "Failed to send action: ${e.message}")
                callback(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!response.isSuccessful) {
                    android.util.Log.e("WebhookHelper", "Server error: ${response.code}")
                }
                callback(response.isSuccessful, response.body?.string())
            }
        })
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val algo = "HmacSHA256"
        val mac = Mac.getInstance(algo)
        val keySpec = SecretKeySpec(key, algo)
        mac.init(keySpec)
        return mac.doFinal(data)
    }
}
