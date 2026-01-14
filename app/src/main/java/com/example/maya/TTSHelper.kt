package com.example.maya

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class TTSHelper private constructor(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context, this)
    }

    companion object {
        @Volatile
        private var instance: TTSHelper? = null

        fun getInstance(context: Context): TTSHelper {
            return instance ?: synchronized(this) {
                instance ?: TTSHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Support multiple languages
            val result = tts?.setLanguage(Locale("bn", "BD")) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            isReady = true
        }
    }

    fun setLanguage(lang: String) {
        val locale = when(lang) {
            "bn" -> Locale("bn", "BD")
            "ur" -> Locale("ur", "PK")
            "hi" -> Locale("hi", "IN")
            else -> Locale.US
        }
        val result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.US)
        }
    }

    fun speak(text: String, lang: String = "en") {
        if (isReady) {
            setLanguage(lang)
            val params = android.os.Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "MayaTTS")
        }
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        instance = null
    }
}
