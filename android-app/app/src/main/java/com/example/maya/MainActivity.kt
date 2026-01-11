package com.example.maya

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.maya.crew.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tv_log)

        findViewById<Button>(R.id.btn_start_crew).setOnClickListener {
            val crew = CrewManager(this, object : CrewListener {
                override fun onAgentLog(agent: Agent, message: String) {
                    runOnUiThread { tvLog.append("\n[${agent.name}]: $message") }
                }
            })
            crew.startCrew(4)
            crew.submitTasks("স্পিকার ডাটা স্ক্যানিং", "সিকিউরিটি এনক্রিপশন")
        }
    }
}
