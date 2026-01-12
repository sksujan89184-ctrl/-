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
                callback(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
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
