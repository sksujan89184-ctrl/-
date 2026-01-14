package com.example.maya

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject

enum class MayaState { IDLE, THINKING, SPEAKING, HAPPY, SAD, EXCITED, SLEEP }

class MainActivity : AppCompatActivity() {
    private lateinit var tvStatus: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var vLighting: View
    private lateinit var btnChat: ImageButton
    private lateinit var btnVoice: ImageButton
    private lateinit var etInput: EditText
    private lateinit var mayaMemory: MayaMemory
    private var sleepTimer: CountDownTimer? = null

    private val PERMISSION_REQUEST_CODE = 123
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        ivAvatar = findViewById(R.id.ivAvatar)
        vLighting = findViewById(R.id.v_lighting_effect)
        btnChat = findViewById(R.id.btn_attach)
        btnVoice = findViewById(R.id.btn_voice)
        etInput = findViewById(R.id.et_input)
        mayaMemory = MayaMemory(this)

        updateMayaState(MayaState.IDLE)
        checkPermissions()

        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                val text = etInput.text.toString()
                if (text.isNotBlank()) {
                    handleUserInput(text)
                    etInput.text.clear()
                }
                true
            } else false
        }

        btnVoice.setOnClickListener {
            startPulseAnimation(it)
            it.isSelected = !it.isSelected
            if (it.isSelected) {
                tvStatus.text = "Maya ❤️: I'm listening, my love..."
            } else {
                tvStatus.text = "Maya ❤️: I'm here for you."
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.none { it != PackageManager.PERMISSION_GRANTED }) {
                try {
                    startService(Intent(this, WakeWordService::class.java))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun resetSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() { updateMayaState(MayaState.SLEEP) }
        }.start()
    }

    private fun handleUserInput(text: String) {
        resetSleepTimer()
        updateMayaState(MayaState.THINKING)
        tvStatus.text = "Maya ❤️: thinking..."
        
        val currentLang = when {
            text.contains("বাংলা", true) || text.any { it in '\u0980'..'\u09FF' } -> "bn"
            text.any { it in '\u0600'..'\u06FF' } -> "ur"
            text.any { it in '\u0900'..'\u097F' } -> "hi"
            else -> "en"
        }

        MayaAI.generateResponse(text, mayaMemory.getHistory().toString()) { aiResponse ->
            runOnUiThread {
                var cleanResponse = aiResponse
                val commandRegex = "\\[COMMAND: (.*?)\\((.*?)\\)\\]".toRegex()
                val matches = commandRegex.findAll(aiResponse)
                
                for (match in matches) {
                    executePhoneCommand(match.groupValues[1], match.groupValues[2])
                    cleanResponse = cleanResponse.replace(match.value, "").trim()
                }

                tvStatus.text = cleanResponse
                if (isVoiceRequest(cleanResponse) || btnVoice.isSelected) {
                    TTSHelper.getInstance(this).speak(cleanResponse, currentLang)
                    startLightingPulse()
                }
                mayaMemory.saveMessage("User", text)
                mayaMemory.saveMessage("Maya", cleanResponse)
                updateMayaState(MayaState.HAPPY)
                simulateLipSync()
            }
        }
    }

    private fun executePhoneCommand(command: String, param: String) {
        val service = MayaAccessibilityService.instance
        when (command.uppercase()) {
            "OPEN" -> {
                when (param.uppercase()) {
                    "YOUTUBE" -> openUrl("https://www.youtube.com", "com.google.android.youtube")
                    "FACEBOOK" -> openUrl("https://www.facebook.com", "com.facebook.katana")
                    "CHROME" -> openUrl("https://www.google.com", "com.android.chrome")
                    "PLAYSTORE" -> openUrl("https://play.google.com/store", "com.android.vending")
                    "MAPS" -> openUrl("https://maps.google.com", "com.google.android.apps.maps")
                    "CAMERA" -> startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))
                    "SETTINGS" -> startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    "CALCULATOR" -> {
                        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
                        try { startActivity(intent) } catch(e: Exception) { openUrl("https://www.google.com/search?q=calculator") }
                    }
                }
            }
            "CLICK" -> service?.clickElementByText(param)
            "SCROLL" -> service?.scroll(param.uppercase() == "FORWARD" || param.uppercase() == "DOWN")
            "TYPE" -> service?.inputText(param)
            "TOGGLE" -> if (param.uppercase() == "FLASHLIGHT") {
                try {
                    val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                    cameraManager.setTorchMode(cameraManager.cameraIdList[0], true)
                } catch (e: Exception) {}
            }
            "SYSTEM" -> when (param.uppercase()) {
                "BACK" -> service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                "HOME" -> service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                "RECENTS" -> service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }
            "BRIGHTNESS" -> {
                val layoutParams = window.attributes
                layoutParams.screenBrightness = when(param.uppercase()) {
                    "HIGH" -> 1.0f
                    "LOW" -> 0.1f
                    else -> 0.5f
                }
                window.attributes = layoutParams
            }
        }
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

    private fun isVoiceRequest(text: String): Boolean {
        return text.contains("speak", true) || text.contains("বল", true) || 
               text.contains("শোনাও", true) || text.contains("read", true)
    }

    private fun updateMayaState(state: MayaState) {
        ivAvatar.clearAnimation()
        when (state) {
            MayaState.IDLE -> {
                ivAvatar.alpha = 1.0f
                ivAvatar.setImageResource(R.drawable.maya_3d_avatar)
                startIdleAnimation()
            }
            MayaState.THINKING -> {
                ivAvatar.animate().scaleX(1.05f).scaleY(1.05f).setDuration(500).start()
            }
            MayaState.HAPPY -> {
                ivAvatar.animate().rotationBy(5f).setDuration(200).withEndAction {
                    ivAvatar.animate().rotationBy(-5f).setDuration(200).start()
                }.start()
            }
            MayaState.SAD -> {
                ivAvatar.animate().alpha(0.7f).scaleX(0.95f).scaleY(0.95f).setDuration(500).start()
            }
            MayaState.SLEEP -> { ivAvatar.alpha = 0.5f }
            else -> {}
        }
    }

    private fun startIdleAnimation() {
        val idle = AnimationUtils.loadAnimation(this, R.anim.idle_float)
        ivAvatar.startAnimation(idle)
    }

    private fun startLightingPulse() {
        vLighting.animate().alpha(1.0f).setDuration(500).withEndAction {
            vLighting.animate().alpha(0.3f).setDuration(500).withEndAction {
                if (TTSHelper.getInstance(this).isSpeaking()) startLightingPulse()
                else vLighting.animate().alpha(0f).setDuration(500).start()
            }.start()
        }.start()
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
