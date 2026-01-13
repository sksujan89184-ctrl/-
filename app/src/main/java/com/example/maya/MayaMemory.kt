package com.example.maya

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

class MayaMemory(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maya_memory", Context.MODE_PRIVATE)

    fun saveMessage(user: String, bot: String) {
        val history = getHistory()
        history.put("$user|$bot")
        // Keep last 10 messages for context
        val limitedHistory = if (history.length() > 10) {
            val newArr = JSONArray()
            for (i in history.length() - 10 until history.length()) {
                newArr.put(history.get(i))
            }
            newArr
        } else history
        
        prefs.edit().putString("chat_history", limitedHistory.toString()).apply()
    }

    fun getHistory(): JSONArray {
        val data = prefs.getString("chat_history", "[]")
        return JSONArray(data)
    }

    fun clear() {
        prefs.edit().remove("chat_history").apply()
    }

    fun saveFact(fact: String) {
        val facts = getFacts()
        facts.add(fact)
        prefs.edit().putStringSet("user_facts", facts.toSet()).apply()
        
        // Sync to Supabase
        val data = JSONObject()
        data.put("fact", fact)
        data.put("device_id", Build.MODEL)
        // WebhookHelper.sendToSupabase("user_facts", data)
    }

    fun getFacts(): MutableList<String> {
        return prefs.getStringSet("user_facts", emptySet())?.toMutableList() ?: mutableListOf()
    }

    fun getLastContext(): String {
        val history = getHistory()
        if (history.length() > 0) {
            return history.getString(history.length() - 1)
        }
        return ""
    }
}
