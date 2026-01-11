package com.example.maya

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.SpeechRecognizer.RESULTS_RECOGNITION
import android.speech.tts.TextToSpeech
import android.text.Html
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

// উল্লেখ্য: AudioRecorder, SpeakerVerifier, PersonaManager 
// এই ক্লাসগুলো আপনার প্রোজেক্টের অন্য ফাইলে থাকতে হবে।

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnMakePdf: Button
    private lateinit var btnDeleteFolder: Button
    private lateinit var btnEnroll: Button
    private lateinit var btnVerify: Button
    private lateinit var btnReadNews: Button
    private lateinit var btnVoiceCommand: Button
    private lateinit var btnStartCrew: Button
    private lateinit var btnGrantPermissions: Button
    private lateinit var btnEnableGf: Button
    private lateinit var tvLog: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var tts: TextToSpeech
    
    // এই ক্লাসগুলো অবশ্যই আপনার প্রোজেক্টে থাকতে হবে
    private var recorder: AudioRecorder? = null
    private var verifier: SpeakerVerifier? = null
    private var gfMode: Boolean = false
    private val gfPersona = "caring, affectionate, empathetic, human-like girlfriend (AI)"
    private lateinit var personaManager: PersonaManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI Initialization
        btnMakePdf = findViewById(R.id.btn_make_pdf)
        ivAvatar = findViewById(R.id.iv_avatar)
        val btnLoadAvatar: Button = findViewById(R.id.btn_load_avatar)
        btnDeleteFolder = findViewById(R.id.btn_delete_folder)
        btnEnroll = findViewById(R.id.btn_enroll)
        btnVerify = findViewById(R.id.btn_verify)
        tvLog = findViewById(R.id.tv_log)
        btnReadNews = findViewById(R.id.btn_read_news)
        btnVoiceCommand = findViewById(R.id.btn_voice_command)
        btnStartCrew = findViewById(R.id.btn_start_crew)
        btnGrantPermissions = findViewById(R.id.btn_grant_permissions)
        btnEnableGf = findViewById(R.id.btn_enable_gf)

        tts = TextToSpeech(this, this)
        
        // Initialize helpers (নিশ্চিত করুন এই ফাইলগুলো আপনার প্রোজেক্টে আছে)
        try {
            verifier = SpeakerVerifier(this.applicationContext)
            recorder = AudioRecorder()
            personaManager = PersonaManager(this)
            gfMode = personaManager.gfMode
        } catch (e: Exception) {
            Log.e("MainActivity", "Initialization error: ${e.message}")
        }

        loadAvatarIfPresent()
        setupAvatarInteractions()

        // Button Listeners
        btnLoadAvatar.setOnClickListener { promptLoadAvatarUrl() }
        
        btnMakePdf.setOnClickListener {
            verifySpeaker { ok -> if (ok) pickImagesForPdf() else speak("Access Denied") }
        }

        btnReadNews.setOnClickListener {
            val newsUrl = "https://apbnews.com/"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(newsUrl)))
                CoroutineScope(Dispatchers.IO).launch { fetchAndReadNews(newsUrl) }
            } catch (e: Exception) { appendLog("Error: ${e.message}") }
        }

        btnVoiceCommand.setOnClickListener {
            if (hasRecordPermission()) startVoiceCommandListening() else requestRecordPermission()
        }

        btnEnableGf.setOnClickListener {
            gfMode = !gfMode
            personaManager.gfMode = gfMode
            speak(if (gfMode) "Girlfriend persona enabled." else "Persona disabled.")
            btnEnableGf.text = if (gfMode) "Disable GF" else "Enable GF"
        }

        btnGrantPermissions.setOnClickListener { requestAllPermissions() }
        
        btnStartCrew.setOnClickListener { startCrewDemo() }
    }

    // --- Helper Functions ---

    private fun appendLog(text: String) {
        runOnUiThread { tvLog.append("[${Date()}] $text\n") }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    private fun hasRecordPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestRecordPermission() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1234)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale("bn", "BD")
    }

    // ... (বাকি ফাংশনগুলো যেমন mergeImagesToPdf, fetchAndReadNews আগের মতোই থাকবে)
    // শুধু নিশ্চিত করুন সব ব্র্যাকেট { } ঠিকমতো ক্লোজ হয়েছে।
}
