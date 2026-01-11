package com.example.maya // 'Package' এর জায়গায় 'package' হবে

import android.content.Context

class PersonaManager(private val context: Context) {
    // maya_prefs নামে ডাটা সেভ হবে
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
            when {
                text.startsWith("Yes") || text.startsWith("Yes,") -> 
                    "Yes, ${userPetName}? ${text.removePrefix("Yes,").replaceFirst("Yes", "").trim()}"
                else -> "${affectionPrefix()} $text"
            }
        } else text
    }

    private fun affectionPrefix(): String = "Sweetheart," 
}
