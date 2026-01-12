package com.example.maya

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.MotionEvent
import android.os.CountDownTimer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import org.json.JSONObject
import com.example.maya.crew.MayaAgent

class MainActivity : AppCompatActivity() {
    
    // Initializing the search agent
    private val searchAgent = MayaAgent(
        name = "Maya Searcher",
        role = "Research Analyst",
        goal = "Find accurate information using DuckDuckGo"
    )
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.SYSTEM_ALERT_WINDOW
    )
    private val PERMISSION_REQUEST_CODE = 123

    private lateinit var tvStatus: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var btnVoice: ImageButton
    private lateinit var btnSettings: ImageButton

    enum class MayaState { IDLE, THINKING, SPEAKING, HAPPY, SAD, EXCITED, SLEEP }
    
    private var sleepTimer: CountDownTimer? = null
    private lateinit var mayaMemory: MayaMemory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        checkAndRequestPermissions()

        mayaMemory = MayaMemory(this)
        tvStatus = findViewById(R.id.tv_status)
        ivAvatar = findViewById(R.id.iv_maya_avatar)
        btnVoice = findViewById(R.id.btn_voice)
        btnSettings = findViewById(R.id.btn_settings)
        
        startIdleAnimation()
        resetSleepTimer()

        // Send startup greeting to Webhook for Make.com verification
        val greeting = JSONObject().apply {
            put("message", "Hello! Maya AI is now active and connected to Make.com.")
            put("device", Build.MODEL)
        }
        WebhookHelper.sendAction("startup_greeting", greeting) { _, _ -> }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnVoice.setOnClickListener {
            resetSleepTimer()
            updateMayaState(MayaState.SPEAKING)
            tvStatus.text = "Maya: I'm listening, Sweetheart..."
            startPulseAnimation(btnVoice)
        }

        findViewById<EditText>(R.id.et_input).setOnEditorActionListener { v, _, _ ->
            resetSleepTimer()
            val text = (v as EditText).text.toString()
            if (text.isNotBlank()) {
                v.setText("")
                handleUserInput(text)
            }
            true
        }

        window.decorView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                resetSleepTimer()
                val centerX = ivAvatar.x + ivAvatar.width / 2
                val centerY = ivAvatar.y + ivAvatar.height / 2
                val dx = (event.x - centerX) / 50f
                val dy = (event.y - centerY) / 50f
                ivAvatar.translationX = dx.coerceIn(-20f, 20f)
                ivAvatar.translationY = dy.coerceIn(-20f, 20f)
            } else if (event.action == MotionEvent.ACTION_UP) {
                ivAvatar.animate().translationX(0f).translationY(0f).setDuration(200).start()
            }
            false
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions are required for Maya to work properly", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun resetSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                updateMayaState(MayaState.SLEEP)
            }
        }.start()
    }

    private fun handleUserInput(text: String) {
        resetSleepTimer()
        updateMayaState(MayaState.THINKING)
        tvStatus.text = "Maya: thinking..."
        
        val emotion = detectEmotion(text)
        val task = detectTask(text)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val response = generateResponse(text, emotion, task)
            tvStatus.text = response
            mayaMemory.saveMessage(text, response)
            
            // Execute Agent Task if needed
            if (task != null || text.contains("search", true) || text.contains("analyze", true)) {
                searchAgent.executeTask(text)
            }

            // Send to Webhook
            val logData = JSONObject()
            logData.put("user_input", text)
            logData.put("maya_response", response)
            logData.put("emotion", emotion)
            WebhookHelper.sendAction("log_interaction", logData) { _, _ -> }

            updateMayaStateFromEmotion(emotion)
            simulateLipSync()
            
            // Phone control logic (Accessibility actions)
            when {
                text.contains("back", true) || text.contains("পিছনে", true) -> {
                    MayaAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                    tvStatus.text = "Maya: Going back for you."
                }
                text.contains("home", true) || text.contains("হোম", true) -> {
                    MayaAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
                    tvStatus.text = "Maya: Returning to home screen."
                }
                text.contains("recent", true) || text.contains("অ্যাপস", true) -> {
                    MayaAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
                }
                text.contains("open", true) && text.contains("camera", true) -> {
                    val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivity(intent)
                }
                text.contains("analyze", true) || text.contains("ছবি", true) -> {
                    // Logic to trigger media analysis via Gemini (simulated here)
                    tvStatus.text = "Maya: Analyzing the media for you, Sweetheart."
                    searchAgent.executeTask("Perform deep analysis on the last imported image/video")
                }
                text.contains("clear", true) && (text.contains("memory", true) || text.contains("chat", true)) -> {
                    mayaMemory.clear()
                    tvStatus.text = "Maya: Memory cleared, Sweetheart. Starting fresh!"
                }
                text.contains("sleep", true) -> {
                    updateMayaState(MayaState.SLEEP)
                    tvStatus.text = "Maya: Going to sleep now. Wake me up anytime!"
                }
            }
        }, 1000)
    }

    private fun detectEmotion(text: String): String {
        return when {
            text.contains("happy", true) || text.contains("ভালো", true) -> "HAPPY"
            text.contains("sad", true) || text.contains("খারাপ", true) -> "SAD"
            text.contains("wow", true) || text.contains("অবাক", true) -> "EXCITED"
            else -> "CALM"
        }
    }

    private fun detectTask(text: String): String? {
        return when {
            text.contains("remind", true) || text.contains("মনে করিয়ে", true) -> "REMINDER"
            text.contains("note", true) || text.contains("লিখে রাখো", true) -> "NOTE"
            text.contains("plan", true) || text.contains("পরিকল্পনা", true) -> "PLAN"
            else -> null
        }
    }

    private fun generateResponse(text: String, emotion: String, task: String?): String {
        val name = "Sweetheart"
        if (task != null) return "Maya: Sure $name, I've noted your $task task."
        
        return when (emotion) {
            "HAPPY" -> "Maya: I'm so glad to hear that, $name! 😊"
            "SAD" -> "Maya: Don't be sad, $name. I'm here for you. ❤️"
            "EXCITED" -> "Maya: Wow! That's amazing, $name! 🌟"
            else -> "Maya: I hear you, $name. Tell me more."
        }
    }

    private fun updateMayaStateFromEmotion(emotion: String) {
        when (emotion) {
            "HAPPY" -> updateMayaState(MayaState.HAPPY)
            "SAD" -> updateMayaState(MayaState.SAD)
            "EXCITED" -> updateMayaState(MayaState.EXCITED)
            else -> updateMayaState(MayaState.IDLE)
        }
    }

    private fun updateMayaState(state: MayaState) {
        ivAvatar.clearAnimation()
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
            MayaState.SAD -> {
                ivAvatar.animate().alpha(0.7f).translationY(10f).setDuration(500).start()
            }
            MayaState.EXCITED -> {
                ivAvatar.animate().scaleX(1.2f).scaleY(1.2f).rotation(5f).setDuration(300).start()
            }
            MayaState.SLEEP -> {
                tvStatus.text = "Maya: Zzz..."
                ivAvatar.animate().alpha(0.4f).scaleX(0.9f).scaleY(0.9f).setDuration(1000).start()
            }
        }
    }

    private fun simulateLipSync() {
        val lipSyncHandler = Handler(Looper.getMainLooper())
        var lipSyncCount = 0
        val runnable = object : Runnable {
            override fun run() {
                if (lipSyncCount < 10) {
                    ivAvatar.scaleY = if (lipSyncCount % 2 == 0) 1.02f else 1.0f
                    lipSyncCount++
                    lipSyncHandler.postDelayed(this, 150)
                } else {
                    ivAvatar.scaleY = 1.0f
                    updateMayaState(MayaState.IDLE)
                }
            }
        }
        lipSyncHandler.post(runnable)
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
