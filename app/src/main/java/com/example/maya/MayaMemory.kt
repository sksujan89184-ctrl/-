package com.example.maya

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

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

    fun getLastContext(): String {
        val history = getHistory()
        if (history.length() > 0) {
            return history.getString(history.length() - 1)
        }
        return ""
    }
}
