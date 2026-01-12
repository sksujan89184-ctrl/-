package com.example.maya

import android.os.Build

object PersonaManager {
    enum class Persona { NORMAL, ANIME_FLIRTY, PROFESSIONAL, HELPFUL }
    var currentPersona = Persona.ANIME_FLIRTY

    fun getGreeting(name: String): String {
        return when (currentPersona) {
            Persona.ANIME_FLIRTY -> "Hello $name, I was waiting just for you! ❤️"
            Persona.PROFESSIONAL -> "Good day $name. How may I assist with your system tasks?"
            else -> "Hey $name, I'm here to help!"
        }
    }

    fun getSystemStatus(): String {
        val thermal = "Optimal" // Simulated
        return "System health: $thermal. Battery: Healthy. I'm feeling great!"
    }
}
