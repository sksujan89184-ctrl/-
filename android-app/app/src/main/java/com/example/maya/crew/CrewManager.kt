package com.example.maya.crew

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * CrewManager: creates N agents with identical persona and coordinates tasks.
 * Simple orchestration: tasks are offered to agents via a Channel; agents pick
 * tasks and notify the listener as they progress.
 */
class CrewManager(private val listener: CrewListener) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val taskChannel = Channel<String>(Channel.UNLIMITED)
    private val agents = mutableListOf<AgentRunner>()

    fun startCrew(count: Int = 4, name: String = "Myra", persona: String = "helpful, calm, Bengali voice") {
        // create identical agents
        for (i in 1..count) {
            val agent = Agent(i, name, persona)
            val runner = AgentRunner(agent, listener)
            agents += runner
            // each agent consumes tasks
            scope.launch {
                for (task in taskChannel) {
                    try {
                        runner.doTask(task)
                    } catch (e: Exception) {
                        listener.onAgentLog(agent, "error: ${e.message}")
                    }
                }
            }
        }
        listener.onAgentLog(Agent(0, name, persona), "Crew started with $count agents")
    }

    fun submitTasks(vararg tasks: String) {
        scope.launch {
            tasks.forEach { taskChannel.send(it) }
        }
    }

    fun stop() {
        scope.launch { taskChannel.close() }
        listener.onAgentLog(Agent(0, "Crew", ""), "Crew stopped")
    }
}
