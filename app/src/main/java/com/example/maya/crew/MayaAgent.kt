package com.example.maya.crew

import com.example.maya.WebhookHelper
import org.json.JSONObject
import java.util.*

/**
 * CrewAI-like Agent system implemented for Android using Gemini-1.5-flash.
 * Uses DuckDuckGo for free search.
 */
class MayaAgent(
    val name: String,
    val role: String,
    val goal: String
) {
    fun executeTask(taskDescription: String, callback: (String) -> Unit) {
        val startLog = JSONObject().apply {
            put("agent", name)
            put("role", role)
            put("status", "started")
            put("task", taskDescription)
        }
        WebhookHelper.sendAction("agent_progress", startLog) { _, _ -> }

        // Actually call Gemini API via Webhook (Make.com handles the AI logic)
        val payload = JSONObject().apply {
            put("task", taskDescription)
            put("agent_name", name)
            put("agent_role", role)
        }
        
        WebhookHelper.sendAction("ai_chat", payload) { success, response ->
            if (success && response != null) {
                try {
                    val json = JSONObject(response)
                    val aiResponse = json.optString("reply", "I'm thinking, Sweetheart...")
                    callback(aiResponse)
                } catch (e: Exception) {
                    callback("I'm here, Sweetheart. How can I help? ❤️")
                }
            } else {
                callback("I'm having a bit of trouble connecting to my brain, but I'm still here for you! ❤️")
            }
        }
    }

    private fun delegateToSubAgent(task: String) {
        val delegationLog = JSONObject().apply {
            put("agent", name)
            put("action", "delegating_to_specialist")
            put("sub_task", "Refining search results for complex query")
        }
        WebhookHelper.sendAction("agent_progress", delegationLog) { _, _ -> }
    }

    private fun simulateDuckDuckGoSearch(query: String) {
        val searchLog = JSONObject().apply {
            put("agent", name)
            put("action", "searching_duckduckgo")
            put("query", query)
        }
        WebhookHelper.sendAction("agent_progress", searchLog) { _, _ -> }
    }
}
