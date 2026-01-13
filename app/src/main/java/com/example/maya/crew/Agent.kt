package com.example.maya.crew

class Agent(
    val name: String,
    val role: String,
    val goal: String
) {
    fun executeTask(task: String): String {
        return "Agent $name ($role) is working on goal '$goal' to complete task: $task"
    }
}
