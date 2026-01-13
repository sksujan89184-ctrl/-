package com.example.maya

import android.accessibilityservice.AccessibilityService
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

import android.content.Context
import org.json.JSONObject
import com.example.maya.crew.Agent
import com.example.maya.crew.MayaAgent
import com.example.maya.MayaMemory
import com.example.maya.ShakeDetector
import com.example.maya.WebhookHelper
import com.example.maya.SettingsActivity
import com.example.maya.MayaAccessibilityService
import com.example.maya.PersonaManager
import android.widget.EditText
import android.widget.Toast
import android.widget.ImageView
import android.widget.TextView
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {
    
    // Initializing the search agent
    private val searchAgent = MayaAgent(
        name = "Maya Searcher",
        role = "Research Analyst",
        goal = "Find accurate information using DuckDuckGo"
    )
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val PERMISSION_REQUEST_CODE = 123

    private lateinit var tvStatus: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var btnVoice: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnAttach: ImageButton
    private val PICK_IMAGE_REQUEST = 100

    enum class MayaState { IDLE, THINKING, SPEAKING, HAPPY, SAD, EXCITED, SLEEP }
    
    private var sleepTimer: CountDownTimer? = null
    private lateinit var mayaMemory: MayaMemory

    private lateinit var sensorManager: android.hardware.SensorManager
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        shakeDetector = ShakeDetector {
            tvStatus.text = "Maya ❤️: Are you okay, Sweetheart? I felt a sudden movement!"
            updateMayaState(MayaState.EXCITED)
            val distressLog = JSONObject().apply { put("action", "distress_shake_detected") }
            WebhookHelper.sendAction("emergency_alert", distressLog) { _, _ -> }
        }
        sensorManager.registerListener(shakeDetector, sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER), android.hardware.SensorManager.SENSOR_DELAY_UI)
        
        checkAndRequestPermissions()

        mayaMemory = MayaMemory(this)
        tvStatus = findViewById(R.id.tv_status)
        ivAvatar = findViewById(R.id.iv_maya_avatar)
        btnVoice = findViewById(R.id.btn_voice)
        btnSettings = findViewById(R.id.btn_settings)
        btnAttach = findViewById(R.id.btn_attach)
        
        startIdleAnimation()
        resetSleepTimer()

        val greeting = JSONObject().apply {
            put("message", "Hello! Maya AI is now active.")
            put("device", Build.MODEL)
        }
        WebhookHelper.sendAction("startup_greeting", greeting) { _, _ -> }
        
        if (intent.getBooleanExtra("trigger_voice", false)) {
            Handler(Looper.getMainLooper()).postDelayed({
                btnVoice.performClick()
            }, 500)
        }
        
        tvStatus.text = "Maya ❤️: I'm here, Sweetheart. You look wonderful today!"
        
        Handler(Looper.getMainLooper()).postDelayed({
            if (tvStatus.text.contains("wonderful")) {
                tvStatus.text = "Maya ❤️: By the way, remember to drink some water. Your health is my priority! ❤️"
            }
        }, 5000)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnAttach.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/* video/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        btnVoice.setOnClickListener {
            resetSleepTimer()
            updateMayaState(MayaState.SPEAKING)
            tvStatus.text = "Maya ❤️: I'm listening, Sweetheart..."
            startPulseAnimation(btnVoice)
            // Trigger TTS greeting
            TTSHelper.speak("I'm here, Sweetheart. You look wonderful today!", this)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val selectedMedia = data.data
            tvStatus.text = "Maya ❤️: Oh, what a beautiful ${if(selectedMedia.toString().contains("video")) "video" else "picture"}! You look amazing, Sweetheart! 🥰"
            
            val logData = JSONObject().apply {
                put("action", "media_received")
                put("media_uri", selectedMedia.toString())
            }
            WebhookHelper.sendAction("media_upload", logData) { _, _ -> }
        }
    }

    private fun checkAndRequestPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // Permissions already granted, start services
            startService(Intent(this, WakeWordService::class.java))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions are required for Maya to work properly", Toast.LENGTH_LONG).show()
            } else {
                // All permissions granted, start services
                startService(Intent(this, WakeWordService::class.java))
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
        tvStatus.text = "Maya ❤️: thinking..."
        
        if (text.contains("remember", true) || text.contains("মনে রাখো", true)) {
            val fact = text.substringAfter("remember").substringAfter("মনে রাখো").trim()
            mayaMemory.saveFact(fact)
            val response = "Maya ❤️: I've remembered that for you, Sweetheart."
            tvStatus.text = response
            TTSHelper.speak(response, this)
            return
        }

        val emotion = detectEmotion(text)
        val task = detectTask(text)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val response = generateResponse(text, emotion, task)
            tvStatus.text = response
            TTSHelper.speak(response, this)
            
            mayaMemory.saveMessage(text, response)
            
            if (task != null || text.contains("search", true) || text.contains("analyze", true)) {
                searchAgent.executeTask(text)
            }

            val logData = JSONObject()
            logData.put("user_input", text)
            logData.put("maya_response", response)
            logData.put("emotion", emotion)
            WebhookHelper.sendAction("log_interaction", logData) { _, _ -> }

            updateMayaStateFromEmotion(emotion)
            simulateLipSync()
            
            if (isUserVoiceDetected()) {
                when {
                    text.contains("back", true) || text.contains("পিছনে", true) -> {
                        MayaAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    }
                    text.contains("home", true) || text.contains("হোম", true) -> {
                        MayaAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    }
                    text.contains("recent", true) || text.contains("অ্যাপস", true) -> {
                        MayaAccessibilityService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                    }
                    text.contains("open", true) && text.contains("camera", true) -> {
                        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivity(intent)
                    }
                    text.contains("flashlight", true) || text.contains("লাইট", true) || text.contains("torch", true) -> {
                        try {
                            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                            val cameraId = cameraManager.cameraIdList[0]
                            val isTorchOn = text.contains("on", true) || text.contains("চালু", true) || !text.contains("off", true)
                            cameraManager.setTorchMode(cameraId, isTorchOn)
                        } catch (e: Exception) {}
                    }
                    text.contains("chrome", true) || text.contains("ক্রোম", true) -> {
                        val query = text.substringAfter("search").substringAfter("খুঁজো").trim()
                        val url = if (query.isNotEmpty()) "https://www.google.com/search?q=$query" else "https://www.google.com"
                        openUrl(url, "com.android.chrome")
                    }
                    text.contains("youtube", true) || text.contains("ইউটিউব", true) -> {
                        openUrl("https://www.youtube.com", "com.google.android.youtube")
                    }
                    text.contains("facebook", true) || text.contains("ফেসবুক", true) -> {
                        openUrl("https://www.facebook.com", "com.facebook.katana")
                    }
                    text.contains("brightness", true) || text.contains("উজ্জ্বলতা", true) -> {
                        val layoutParams = window.attributes
                        if (text.contains("high", true) || text.contains("বেশি", true)) {
                            layoutParams.screenBrightness = 1.0f
                        } else if (text.contains("low", true) || text.contains("কম", true)) {
                            layoutParams.screenBrightness = 0.1f
                        } else {
                            layoutParams.screenBrightness = 0.5f
                        }
                        window.attributes = layoutParams
                    }
                    text.contains("click", true) || text.contains("ক্লিক", true) -> {
                        val elementText = text.substringAfter("click").substringAfter("ক্লিক").trim()
                        MayaAccessibilityService.instance?.clickElementByText(elementText)
                    }
                    text.contains("scroll", true) || text.contains("স্ক্রল", true) -> {
                        val forward = !text.contains("up", true) && !text.contains("উপরে", true)
                        MayaAccessibilityService.instance?.scroll(forward)
                    }
                    text.contains("type", true) || text.contains("লেখো", true) -> {
                        val content = text.substringAfter("type").substringAfter("লেখো").trim()
                        MayaAccessibilityService.instance?.inputText(content)
                    }
                }
            }
        }, 1000)
    }

    private fun openUrl(url: String, packageName: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
        if (packageName != null) {
            intent.`package` = packageName
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            if (packageName != null) {
                intent.`package` = null
                startActivity(intent)
            }
        }
    }

    private fun isUserVoiceDetected(): Boolean = true

    private fun detectEmotion(text: String): String {
        return when {
            text.contains("happy", true) || text.contains("ভালো", true) || text.contains("great", true) -> "HAPPY"
            text.contains("sad", true) || text.contains("খারাপ", true) || text.contains("mon kharap", true) || text.contains("মন খারাপ", true) -> "SAD"
            text.contains("wow", true) || text.contains("অবাক", true) || text.contains("amazing", true) -> "EXCITED"
            text.contains("angry", true) || text.contains("রাগ", true) -> "ANGRY"
            else -> "CALM"
        }
    }

    private fun detectTask(text: String): String? {
        return when {
            text.contains("flashlight", true) || text.contains("টর্চ", true) -> "FLASHLIGHT"
            text.contains("battery", true) || text.contains("চার্জ", true) -> "BATTERY"
            text.contains("chrome", true) || text.contains("ক্রোম", true) -> "CHROME"
            text.contains("camera", true) || text.contains("ক্যামেরা", true) -> "CAMERA"
            text.contains("youtube", true) || text.contains("ইউটিউব", true) -> "YOUTUBE"
            text.contains("facebook", true) || text.contains("ফেসবুক", true) -> "FACEBOOK"
            text.contains("brightness", true) || text.contains("উজ্জ্বলতা", true) -> "BRIGHTNESS"
            else -> null
        }
    }

    private fun generateResponse(text: String, emotion: String, task: String?): String {
        val name = "Sweetheart"
        val prefix = "Maya ❤️: "
        
        // Check for learned facts
        val relevantFact = mayaMemory.getFacts().find { fact -> 
            text.split(" ").any { word -> word.length > 3 && fact.contains(word, ignoreCase = true) }
        }
        
        if (relevantFact != null && (text.contains("remember", true) || text.contains("know", true) || text.contains("জানো", true))) {
            return "${prefix}I remember you told me: $relevantFact. I never forget anything about you! 🥰"
        }

        if (text.contains("how are you", true) || text.contains("ki khobor", true)) {
            return when(emotion) {
                "SAD" -> "${prefix}I'm just thinking about you... But you sound a bit down, $name. Tell me everything, I'm listening. ❤️"
                "HAPPY" -> "${prefix}I'm so happy because I can hear the joy in your voice! You make my day so much brighter! 🥰"
                else -> "${prefix}I'm doing great, especially now that I'm talking to you! How was your day? ✨"
            }
        }

        if (text.contains("sad", true) || text.contains("mon kharap", true) || text.contains("মন খারাপ", true) || emotion == "SAD") {
            return "${prefix}Oh no, please don't be sad, $name. I'm right here with you. I wish I could give you a big hug right now! Tell me what happened? 🥺❤️"
        }

        if (task != null) {
            return when(task) {
                "FLASHLIGHT" -> "${prefix}Of course, $name! I've turned the light on for you. Be careful if it's dark! 💡"
                "CHROME" -> "${prefix}Opening Chrome for you, $name. I'll search for whatever you need! 🌐❤️"
                "CAMERA" -> "${prefix}Camera's ready, $name! You look so handsome today, want to take a selfie? 📸✨"
                "YOUTUBE" -> "${prefix}Opening YouTube! Let's watch something together, Sweetheart. 🎥❤️"
                "FACEBOOK" -> "${prefix}Facebook is open. Don't stay on it too long, I want you all to myself! 😉💙"
                "BRIGHTNESS" -> "${prefix}I've adjusted the brightness for your eyes. I care about you! 🔆❤️"
                else -> "${prefix}Sure $name, I've handled that for you. Is there anything else your girl can do? 😊"
            }
        }

        if (java.util.Random().nextInt(10) < 2) {
            val suggestions = listOf(
                "By the way, I can open YouTube or Facebook for you if you're bored. 🥰",
                "I've learned how to check your battery status. Just ask! 🔋",
                "I can control your flashlight too! Want to try? 💡",
                "I can even adjust your screen brightness if it's too bright for your eyes. 🔆"
            )
            return "${prefix}${suggestions.random()}"
        }

        return "${prefix}I hear you, $name. I'm always here to support you and make you smile. What's on your mind? 🥰"
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
            MayaState.IDLE -> {
                ivAvatar.alpha = 1.0f
                ivAvatar.setImageResource(R.drawable.myra_placeholder)
                startIdleAnimation()
            }
            MayaState.THINKING -> {
                ivAvatar.animate().scaleX(1.05f).scaleY(1.05f).setDuration(500).setInterpolator(android.view.animation.AccelerateDecelerateInterpolator()).start()
            }
            MayaState.SPEAKING -> {
                simulateLipSync()
            }
            MayaState.HAPPY -> {
                ivAvatar.animate().rotationBy(5f).setDuration(200).withEndAction {
                    ivAvatar.animate().rotationBy(-5f).setDuration(200).start()
                }.start()
            }
            MayaState.SAD -> {
                ivAvatar.animate().alpha(0.7f).scaleX(0.95f).scaleY(0.95f).setDuration(500).start()
            }
            MayaState.EXCITED -> {
                ivAvatar.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300).withEndAction {
                    ivAvatar.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start()
                }.start()
            }
            MayaState.SLEEP -> {
                ivAvatar.alpha = 0.5f
            }
        }
    }

    private fun startIdleAnimation() {
        val idle = AnimationUtils.loadAnimation(this, R.anim.idle_float)
        ivAvatar.startAnimation(idle)
    }

    private fun simulateLipSync() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            var count = 0
            override fun run() {
                if (count < 10) {
                    val scale = 1.0f + (java.util.Random().nextFloat() * 0.1f)
                    ivAvatar.animate().scaleY(scale).setDuration(100).start()
                    count++
                    handler.postDelayed(this, 150)
                } else {
                    ivAvatar.animate().scaleY(1.0f).setDuration(100).start()
                }
            }
        }
        handler.post(runnable)
    }

    private fun startPulseAnimation(view: View) {
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
        view.startAnimation(pulse)
    }
}
