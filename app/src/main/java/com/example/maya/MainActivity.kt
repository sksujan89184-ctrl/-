package com.example.maya

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var tvStatus: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var btnVoice: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tv_log)
        tvStatus = findViewById(R.id.tv_status)
        ivAvatar = findViewById(R.id.iv_maya_avatar)
        btnVoice = findViewById(R.id.btn_voice)
        
        // Start Idle Animation
        startIdleAnimation()

        // Start wake word service
        val wakeIntent = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(wakeIntent)
        } else {
            startService(wakeIntent)
        }
        
        btnVoice.setOnClickListener {
            tvStatus.text = "Maya: Listening..."
            startPulseAnimation(btnVoice)
        }

        findViewById<EditText>(R.id.et_input).setOnEditorActionListener { v, _, _ ->
            val text = (v as EditText).text.toString()
            if (text.isNotBlank()) {
                tvStatus.text = "Maya: Thinking..."
                v.setText("")
                // Logic to respond...
            }
            true
        }
    }

    private fun startIdleAnimation() {
        val idle = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        idle.duration = 2000
        idle.repeatMode = Animation.REVERSE
        idle.repeatCount = Animation.INFINITE
        ivAvatar.startAnimation(idle)
    }

    private fun startPulseAnimation(view: View) {
        val pulse = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        pulse.duration = 500
        pulse.repeatMode = Animation.REVERSE
        pulse.repeatCount = 5
        view.startAnimation(pulse)
    }
}
