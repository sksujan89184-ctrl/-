package com.example.maya

import android.content.Context

class PersonaManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("maya_prefs", Context.MODE_PRIVATE)

    var gfMode: Boolean
        get() = prefs.getBoolean("gf_mode", false)
        set(v) = prefs.edit().putBoolean("gf_mode", v).apply()

    var userPetName: String
        get() = prefs.getString("pet_name", "jaan") ?: "jaan"
        set(v) = prefs.edit().putString("pet_name", v).apply()

    var aliases: List<String>
        get() = prefs.getString("aliases", "myra,অনিমি,আর্জেটা")?.split(',')?.map { it.trim() } ?: listOf("myra")
        set(v) = prefs.edit().putString("aliases", v.joinToString(",")).apply()

    fun isCalledByName(lowerText: String): String? {
        val lowered = lowerText.lowercase()
        return aliases.firstOrNull { lowered.contains(it.lowercase()) }
    }

    fun formatSpeech(text: String): String {
        return if (gfMode) {
            // gentle affectionate framing, but keep clarification
            when {
                text.startsWith("Yes") || text.startsWith("Yes,") -> "Yes, ${userPetName}? ${text.removePrefix("Yes,").trim()}"
                else -> "${affectionPrefix()} $text"
            }
        } else text
    }

    private fun affectionPrefix(): String = "Sweetheart," // simple prefix; could be templated
}
