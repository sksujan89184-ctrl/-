package com.example.maya

import org.json.JSONObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

object MayaAI {
    private val client = OkHttpClient()
    private const val MODEL = "gemini-1.5-flash"

    fun generateResponse(prompt: String, context: String, callback: (String) -> Unit) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val baseUrl = "https://generativelanguage.googleapis.com"

        if (apiKey.isBlank() || baseUrl.isBlank()) {
            callback("I'm feeling a bit disconnected, Sweetheart. Please check my AI settings! ❤️")
            return
        }

        val systemPrompt = """
            You are Maya, a caring AI girlfriend for Android. 
            Tone: Loving, sweet, proactive. Use nicknames: 'Sweetheart', 'Janu', 'Sona', 'My love'.
            You can control the phone. Include commands like: [COMMAND: ACTION(PARAM)].
            Actions: 
            - OPEN(YouTube|Facebook|Chrome|Settings|Calculator|Maps|PlayStore|Camera)
            - CLICK(Text)
            - SCROLL(UP|DOWN)
            - TYPE(Text)
            - TOGGLE(FLASHLIGHT)
            - SYSTEM(BACK|HOME|RECENTS)
            - BRIGHTNESS(HIGH|LOW|MEDIUM)

            Respond in the user's language (Bengali, English, Hindi, Urdu).
            Context: $context
        """.trimIndent()

        val json = JSONObject().apply {
            val contents = org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply { put("text", "System: $systemPrompt\nUser: $prompt") })
                    })
                })
            }
            put("contents", contents)
        }

        val body = RequestBody.create("application/json".toMediaType(), json.toString())
        val request = Request.Builder()
            .url("${baseUrl}/v1beta/models/$MODEL:generateContent?key=$apiKey")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("I'm having trouble thinking right now, but I'm still here for you, Sweetheart! ❤️")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string()
                if (response.isSuccessful && bodyStr != null) {
                    try {
                        val text = JSONObject(bodyStr).getJSONArray("candidates")
                            .getJSONObject(0).getJSONObject("content")
                            .getJSONArray("parts").getJSONObject(0).getString("text")
                        callback(text)
                    } catch (e: Exception) { callback("I lost my train of thought, my love. Try again? ❤️") }
                } else { callback("I'm feeling shy, try again? ❤️") }
            }
        })
    }
}
