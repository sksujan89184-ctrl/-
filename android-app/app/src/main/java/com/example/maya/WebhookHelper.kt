package com.example.maya

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

    // এটি অবশ্যই কো-রুটিন (suspend) হতে হবে যাতে মেইন থ্রেড ব্লক না হয়
    fun sendSignedCommand(url: String, jsonPayload: String, base64Key: String): Pair<Int, String?> {
        return try {
            // Decode key from base64
            val keyBytes = Base64.decode(base64Key, Base64.DEFAULT)
            val hmac = hmacSha256(keyBytes, jsonPayload.toByteArray(Charsets.UTF_8))
            val signature = ByteString.of(*hmac).hex()

            val body = RequestBody.create("application/json; charset=utf-8".toMediaType(), jsonPayload)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-Signature", signature)
                .build()

            // নেটওয়ার্ক কল
            client.newCall(request).execute().use { resp ->
                val respBody = resp.body?.string()
                Pair(resp.code, respBody)
            }
        } catch (e: Exception) {
            // এরর হলে ০ এবং মেসেজ রিটার্ন করবে
            Pair(0, e.message)
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
