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

    private lateinit var sensorManager: android.hardware.SensorManager
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
        shakeDetector = ShakeDetector {
            tvStatus.text = "Maya: Are you okay, Sweetheart? I felt a sudden movement!"
            updateMayaState(MayaState.EXCITED)
            // Trigger emergency webhook (simulated)
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
        
        startIdleAnimation()
        resetSleepTimer()

        // Send startup greeting to Webhook for Make.com verification
        val greeting = JSONObject().apply {
            put("message", "Hello! Maya AI is now active.")
            put("device", Build.MODEL)
        }
        WebhookHelper.sendAction("startup_greeting", greeting) { _, _ -> }
        
        // Proactive engagement on start
        tvStatus.text = "Maya: I'm here, Sweetheart. You look wonderful today!"
        
        // Random proactive notification simulation
        Handler(Looper.getMainLooper()).postDelayed({
            if (tvStatus.text.contains("wonderful")) {
                tvStatus.text = "Maya: By the way, remember to drink some water. Your health is my priority! ❤️"
            }
        }, 5000)

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnVoice.setOnClickListener {
            resetSleepTimer()
            // Only respond to user voice when the button is active or a wake word is detected
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
        
        // Proactive greeting based on stored memory
        if (text.contains("remember", true) || text.contains("মনে রাখো", true)) {
            val fact = text.substringAfter("remember").substringAfter("মনে রাখো").trim()
            mayaMemory.saveFact(fact)
            tvStatus.text = "Maya: I've remembered that for you, Sweetheart."
            return
        }

        val emotion = detectEmotion(text)
        val task = detectTask(text)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val response = generateResponse(text, emotion, task)
            tvStatus.text = response
            
            // Check for sadness proactively
            if (emotion == "SAD") {
                tvStatus.text = "Maya: You look a bit down, Sweetheart. Is everything okay? Talk to me."
            }
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
            
            // Only execute commands if the voice matches the user profile (simplified)
            // This prevents videos or music from triggering system actions
            if (isUserVoiceDetected()) {
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
                    tvStatus.text = "Maya: Showing recent apps."
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
                text.contains("battery", true) || text.contains("চার্জ", true) -> {
                    val batteryIntent = registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                    val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val batteryPct = level * 100 / scale.toFloat()
                    tvStatus.text = "Maya: Your battery is at ${batteryPct.toInt()}%, Sweetheart."
                }
                text.contains("launch", true) || text.contains("চালু", true) -> {
                    val appName = text.substringAfter("launch").substringAfter("চালু").trim()
                    try {
                        val launchIntent = packageManager.getLaunchIntentForPackage(appName) ?: 
                                         packageManager.getLaunchIntentForPackage("com.android.$appName")
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                            tvStatus.text = "Maya: Launching $appName for you."
                        } else {
                            tvStatus.text = "Maya: I couldn't find an app named $appName."
                        }
                    } catch (e: Exception) {
                        tvStatus.text = "Maya: Error launching app: ${e.message}"
                    }
                }
                text.contains("wifi", true) -> {
                    val panelIntent = Intent(android.provider.Settings.Panel.ACTION_WIFI)
                    startActivity(panelIntent)
                    tvStatus.text = "Maya: Opening WiFi settings for you."
                }
                text.contains("torch", true) || text.contains("flashlight", true) || text.contains("লাইট", true) -> {
                    try {
                        val cameraManager = getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                        val cameraId = cameraManager.cameraIdList[0]
                        val isTorchOn = text.contains("on", true) || text.contains("চালু", true)
                        cameraManager.setTorchMode(cameraId, isTorchOn)
                        tvStatus.text = if (isTorchOn) "Maya: Flashlight is now ON, Sweetheart." else "Maya: Flashlight is now OFF."
                    } catch (e: Exception) {
                        tvStatus.text = "Maya: I couldn't control the flashlight: ${e.message}"
                    }
                }
                text.contains("volume", true) || text.contains("সাউন্ড", true) -> {
                    val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                    if (text.contains("up", true) || text.contains("বাড়া", true)) {
                        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
                        tvStatus.text = "Maya: Increasing volume for you."
                    } else if (text.contains("down", true) || text.contains("কমা", true)) {
                        audioManager.adjustStreamVolume(android.media.AudioManager.STREAM_MUSIC, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
                        tvStatus.text = "Maya: Decreasing volume for you."
                    }
                }
                text.contains("brightness", true) || text.contains("ব্রাইটনেস", true) -> {
                    val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                    startActivity(intent)
                    tvStatus.text = "Maya: Opening display settings to adjust brightness."
                }
                text.contains("storage", true) || text.contains("মেমোরি", true) -> {
                    val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                    val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
                    val megAvailable = bytesAvailable / (1024 * 1024)
                    tvStatus.text = "Maya: You have ${megAvailable / 1024}GB of free space left, Sweetheart."
                }
                text.contains("bluetooth", true) || text.contains("ব্লুটুথ", true) -> {
                    val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                    startActivity(intent)
                    tvStatus.text = "Maya: Opening Bluetooth settings for you."
                }
                text.contains("airplane", true) || text.contains("এয়ারপ্লেন", true) -> {
                    val intent = Intent(android.provider.Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                    startActivity(intent)
                    tvStatus.text = "Maya: Opening Airplane mode settings."
                }
                text.contains("tell me about", true) || text.contains("কেমন", true) -> {
                    val model = Build.MODEL
                    val version = Build.VERSION.RELEASE
                    tvStatus.text = "Maya: This is a $model running Android $version. It's a beautiful device, just like you!"
                }
                text.contains("search", true) || text.contains("খুঁজো", true) || text.contains("জানাও", true) -> {
                    val query = text.substringAfter("search").substringAfter("খুঁজো").substringAfter("জানাও").trim()
                    tvStatus.text = "Maya: Searching the internet for '$query'..."
                    searchAgent.executeTask(query)
                }
                text.contains("news", true) || text.contains("খবর", true) -> {
                    val query = "latest news"
                    tvStatus.text = "Maya: Fetching the latest news for you, Sweetheart."
                    searchAgent.executeTask(query)
                }
                text.contains("weather", true) || text.contains("আবহাওয়া", true) -> {
                    val query = "current weather"
                    tvStatus.text = "Maya: Checking the weather forecast."
                    searchAgent.executeTask(query)
                }
                text.contains("open", true) && (text.contains("google", true) || text.contains("youtube", true) || text.contains("facebook", true)) -> {
                    val site = if (text.contains("google", true)) "google.com" 
                              else if (text.contains("youtube", true)) "youtube.com"
                              else "facebook.com"
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.$site"))
                    startActivity(intent)
                    tvStatus.text = "Maya: Opening $site for you."
                }
                text.contains("scroll", true) || text.contains("স্ক্রোল", true) -> {
                    if (text.contains("down", true) || text.contains("নিচে", true)) {
                        MayaAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_SCROLL_FORWARD)
                        tvStatus.text = "Maya: Scrolling down."
                    } else {
                        MayaAccessibilityService.instance?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_SCROLL_BACKWARD)
                        tvStatus.text = "Maya: Scrolling up."
                    }
                }
                text.contains("click", true) || text.contains("ক্লিক", true) -> {
                    tvStatus.text = "Maya: I'll try to click that for you."
                }
            }
        }, 1000)
    }

    private fun isUserVoiceDetected(): Boolean {
        // Simplified check: In a real app, this would use the SpeakerVerifier TFLite model
        // to compare current audio with the stored user voice embedding.
        return true 
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
            text.contains("flashlight", true) || text.contains("টর্চ", true) -> "FLASHLIGHT"
            text.contains("battery", true) || text.contains("চার্জ", true) -> "BATTERY"
            text.contains("storage", true) || text.contains("মেমোরি", true) -> "STORAGE"
            text.contains("wifi", true) || text.contains("ওয়াইফাই", true) -> "WIFI"
            text.contains("bluetooth", true) || text.contains("ব্লুটুথ", true) -> "BLUETOOTH"
            text.contains("brightness", true) || text.contains("ব্রাইটনেস", true) -> "BRIGHTNESS"
            text.contains("volume", true) || text.contains("সাউন্ড", true) -> "VOLUME"
            else -> null
        }
    }

    private fun generateResponse(text: String, emotion: String, task: String?): String {
        val name = "Sweetheart"
        
        // Randomly suggest new features to the user to keep them engaged
        if (java.util.Random().nextInt(10) < 2) { // 20% chance to suggest a feature
            val suggestions = listOf(
                "By the way, I can control your flashlight now! Want to try?",
                "I've learned how to check your battery status. Just ask!",
                "I can open YouTube or Facebook for you if you're bored.",
                "Did you know I can remember things for you? Tell me something to remember!",
                "I can even adjust your screen brightness if it's too bright."
            )
            return "Maya: ${suggestions.random()}"
        }

        if (task != null) return "Maya: Sure $name, I've noted your $task task."
        
        // Dynamic Persona Greeting
        if (text.contains("hello", true) || text.contains("hi", true)) {
            return "Maya: ${PersonaManager.getGreeting(name)}"
        }

        // System Health Check
        if (text.contains("status", true) || text.contains("health", true)) {
            return "Maya: ${PersonaManager.getSystemStatus()}"
        }
        
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
