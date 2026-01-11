package com.example.maya.crew

import android.content.Context
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request

data class Agent(val id: Int, val name: String, val persona: String)

interface CrewListener {
    fun onAgentLog(agent: Agent, message: String)
}

class AgentRunner(private val agent: Agent, private val listener: CrewListener, private val context: Context) {
    private val client = OkHttpClient()

    suspend fun doTask(task: String) {
        listener.onAgentLog(agent, "accepted task: $task")
        try {
            when {
                task.contains("Fetch headlines", true) -> {
                    listener.onAgentLog(agent, "working: fetching headlines")
                    val url = "https://apbnews.com/"
                    val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            listener.onAgentLog(agent, "fetch_failed:${resp.code}")
                        } else {
                            val html = resp.body?.string() ?: ""
                            val regex = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL)
                            val found = regex.findAll(html).map { stripHtml(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
                            val headlines = if (found.isNotEmpty()) found.take(6) else listOf("No headlines found")
                            listener.onAgentLog(agent, "headlines:${headlines.joinToString("||")}")
                            listener.onAgentLog(agent, "completed:fetch")
                        }
                    }
                }
                task.contains("Summarize", true) -> {
                    listener.onAgentLog(agent, "working: summarizing latest news")
                    // simple summarization: fetch and pick first sentences
                    val url = "https://apbnews.com/"
                    val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
                    client.newCall(req).execute().use { resp ->
                        val html = resp.body?.string() ?: ""
                        val text = stripHtml(html).replace("\n+", " ")
                        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
                        val summary = sentences.take(3).joinToString(" ")
                        listener.onAgentLog(agent, "summary:$summary")
                        listener.onAgentLog(agent, "completed:summarize")
                    }
                }
                task.contains("Prepare TTS", true) -> {
                    listener.onAgentLog(agent, "working: preparing TTS audio")
                    // produce a speakable string and hand to listener with TTS: prefix
                    val speakText = "Here are the synthesized highlights from the news."
                    listener.onAgentLog(agent, "TTS:$speakText")
                    listener.onAgentLog(agent, "completed:tts")
                }
                task.contains("Open news channel", true) || task.contains("Open", true) -> {
                    listener.onAgentLog(agent, "OPEN_BROWSER:https://apbnews.com/")
                    listener.onAgentLog(agent, "completed:open")
                }
                task.contains("Archive", true) -> {
                    listener.onAgentLog(agent, "working: archiving news")
                    // demo archive: just log
                    listener.onAgentLog(agent, "archived:today_articles")
                    listener.onAgentLog(agent, "completed:archive")
                }
                task.contains("Cleanup", true) -> {
                    listener.onAgentLog(agent, "working: cleanup temporary files")
                    // demo cleanup
                    delay(200)
                    listener.onAgentLog(agent, "completed:cleanup")
                }
                else -> {
                    // generic simulated work
                    repeat(3) { step ->
                        delay(350L + (agent.id * 50))
                        listener.onAgentLog(agent, "working on '$task' (step ${step + 1}/3)")
                    }
                    listener.onAgentLog(agent, "completed task: $task")
                }
            }
        } catch (e: Exception) {
            listener.onAgentLog(agent, "error:${e.message}")
        }
    }

    private fun stripHtml(input: String): String {
        return android.text.Html.fromHtml(input, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }
}
