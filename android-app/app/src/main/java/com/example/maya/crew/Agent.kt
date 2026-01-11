package com.example.maya.crew

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Agent(val id: Int, val name: String, val persona: String)

interface CrewListener {
    fun onAgentLog(agent: Agent, message: String)
}

class AgentRunner(private val agent: Agent, private val listener: CrewListener) {
    suspend fun doTask(task: String) {
        listener.onAgentLog(agent, "accepted task: $task")
        // simulate work split into small steps so multiple agents can interleave
        repeat(3) { step ->
            delay(400L + (agent.id * 100))
            listener.onAgentLog(agent, "working on '$task' (step ${step + 1}/3)")
        }
        // small post-processing delay
        delay(200L)
        listener.onAgentLog(agent, "completed task: $task")
    }
}
