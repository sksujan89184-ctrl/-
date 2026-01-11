package com.example.maya

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var btnVoice: ImageButton
    private lateinit var btnSettings: ImageButton

    enum class MayaState { IDLE, THINKING, SPEAKING, HAPPY }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        ivAvatar = findViewById(R.id.iv_maya_avatar)
        btnVoice = findViewById(R.id.btn_voice)
        btnSettings = findViewById(R.id.btn_settings)
        
        startIdleAnimation()

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnVoice.setOnClickListener {
            updateMayaState(MayaState.SPEAKING)
            tvStatus.text = "Maya: I'm listening, Sweetheart..."
            startPulseAnimation(btnVoice)
        }

        findViewById<EditText>(R.id.et_input).setOnEditorActionListener { v, _, _ ->
            val text = (v as EditText).text.toString()
            if (text.isNotBlank()) {
                v.setText("")
                handleUserInput(text)
            }
            true
        }
    }

    private fun handleUserInput(text: String) {
        updateMayaState(MayaState.THINKING)
        tvStatus.text = "Maya: Thinking..."
        
        Handler(Looper.getMainLooper()).postDelayed({
            updateMayaState(MayaState.HAPPY)
            tvStatus.text = "Maya: I understand! Let me help you with that."
            simulateLipSync()
        }, 2000)
    }

    private fun updateMayaState(state: MayaState) {
        when (state) {
            MayaState.IDLE -> ivAvatar.alpha = 1.0f
            MayaState.THINKING -> {
                ivAvatar.animate().scaleX(1.05f).scaleY(1.05f).setDuration(500).start()
            }
            MayaState.SPEAKING -> {
                ivAvatar.animate().rotation(2f).setDuration(200).start()
            }
            MayaState.HAPPY -> {
                ivAvatar.animate().scaleX(1.1f).scaleY(1.1f).setDuration(300).start()
            }
        }
    }

    private fun simulateLipSync() {
        val handler = Handler(Looper.getMainLooper())
        var count = 0
        val runnable = object : Runnable {
            override fun run() {
                if (count < 10) {
                    ivAvatar.scaleY = if (count % 2 == 0) 1.02f else 1.0f
                    count++
                    handler.postDelayed(this, 150)
                } else {
                    ivAvatar.scaleY = 1.0f
                    updateMayaState(MayaState.IDLE)
                }
            }
        }
        handler.post(runnable)
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
