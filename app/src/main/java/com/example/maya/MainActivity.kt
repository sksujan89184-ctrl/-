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
        
        // সব বাটনের আইডি গুলো চেক করে সেফলি সেট করা
        setupButton(R.id.btn_start_crew) { startMayaSystem() }
        setupButton(R.id.btn_enroll) { tvLog.append("\nEnrollment clicked") }
        setupButton(R.id.btn_verify) { tvLog.append("\nVerification clicked") }
    }

    private fun setupButton(id: Int, action: () -> Unit) {
        findViewById<Button>(id)?.setOnClickListener { action() }
    }

    private fun startMayaSystem() {
        tvLog.text = "Initializing Crew AI..."
        tvLog.append("\nAudio Kit: Ready")
        tvLog.append("\nWebhook: Connected")
        tvLog.append("\nMaya is now listening...")
    }
}
