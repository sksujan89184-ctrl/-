package com.example.maya.crew

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import android.content.Context

// ১. লিসেনার ইন্টারফেসটি এখানে ডিফাইন করা হলো
interface CrewListener {
    fun onAgentLog(agent: Agent, message: String)
}

// ২. এজেন্ট ডাটা ক্লাস
data class Agent(val id: Int, val name: String, val persona: String)

// ৩. এজেন্ট রানার ক্লাস (যা টাস্ক প্রসেস করবে)
class AgentRunner(val agent: Agent, val listener: CrewListener, val context: Context) {
    suspend fun doTask(task: String) {
        listener.onAgentLog(agent, "Working on: $task")
        // এখানে আপনার কাজের লজিক থাকবে
        kotlinx.coroutines.delay(1000) 
        listener.onAgentLog(agent, "completed: $task")
    }
}

// ৪. আপনার মেইন ক্রু ম্যানেজার
class CrewManager(private val context: Context, private val listener: CrewListener) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val taskChannel = Channel<String>(Channel.UNLIMITED)
    private val agents = mutableListOf<AgentRunner>()

    fun startCrew(count: Int = 4, name: String = "Myra", persona: String = "helpful, calm, Bengali voice") {
        for (i in 1..count) {
            val agent = Agent(i, name, persona)
            val runner = AgentRunner(agent, listener, context)
            agents.add(runner)
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
        taskChannel.close()
        listener.onAgentLog(Agent(0, "Crew", ""), "Crew stopped")
    }
}
