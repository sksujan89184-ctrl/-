package com.example.maya.crew

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

interface CrewListener { fun onAgentLog(agent: Agent, message: String) }
data class Agent(val id: Int, val name: String, val persona: String)

class CrewManager(private val context: Context, private val listener: CrewListener) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val taskChannel = Channel<String>(Channel.UNLIMITED)

    fun startCrew(count: Int) {
        for (i in 1..count) {
            val agent = Agent(i, "Maya-$i", "সহকারী")
            scope.launch {
                for (task in taskChannel) {
                    listener.onAgentLog(agent, "কাজ শুরু: $task")
                    delay(2000)
                    listener.onAgentLog(agent, "সম্পন্ন: $task")
                }
            }
        }
    }

    fun submitTasks(vararg tasks: String) {
        scope.launch { tasks.forEach { taskChannel.send(it) } }
    }
}
