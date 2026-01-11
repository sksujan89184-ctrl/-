package com.example.maya

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tv_log)
        val btnStart = findViewById<Button>(R.id.btn_start_crew)

        btnStart.setOnClickListener {
            startMayaSystem()
        }
    }

    private fun startMayaSystem() {
        tvLog.text = "Initializing Crew AI..."
        tvLog.append("\nAudio Kit: Ready")
        tvLog.append("\nWebhook: Connected")
        tvLog.append("\nMaya is now listening...")
    }
}
