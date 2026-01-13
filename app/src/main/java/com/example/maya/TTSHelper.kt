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
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        instance = null
    }
}
