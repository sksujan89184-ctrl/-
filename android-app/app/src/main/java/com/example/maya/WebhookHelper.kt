package com.example.maya

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.ByteString
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object WebhookHelper {
    private val client = OkHttpClient()

    // Send signed JSON payload using HMAC-SHA256
    fun sendSignedCommand(url: String, jsonPayload: String, base64Key: String): Pair<Int, String?> {
        // Decode key from base64
        val keyBytes = android.util.Base64.decode(base64Key, android.util.Base64.DEFAULT)
        val hmac = hmacSha256(keyBytes, jsonPayload.toByteArray(Charsets.UTF_8))
        val signature = ByteString.of(*hmac).hex()

        val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), jsonPayload)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("X-Signature", signature)
            .build()
        client.newCall(request).execute().use { resp ->
            val respBody = resp.body?.string()
            return Pair(resp.code, respBody)
        }
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val algo = "HmacSHA256"
        val mac = Mac.getInstance(algo)
        val keySpec = SecretKeySpec(key, algo)
        mac.init(keySpec)
        return mac.doFinal(data)
    }
}
