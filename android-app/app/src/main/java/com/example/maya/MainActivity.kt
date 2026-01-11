package com.example.maya

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer.*
import android.speech.tts.TextToSpeech
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.Request
import android.text.Html

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnMakePdf: Button
    private lateinit var btnDeleteFolder: Button
    private lateinit var btnEnroll: Button
    private lateinit var btnVerify: Button
    private lateinit var btnReadNews: Button
        private lateinit var btnVoiceCommand: Button
        private lateinit var btnStartCrew: Button
    private lateinit var tvLog: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var tts: TextToSpeech
    private var recorder: AudioRecorder? = null
    private var verifier: SpeakerVerifier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnMakePdf = findViewById(R.id.btn_make_pdf)
        ivAvatar = findViewById(R.id.iv_avatar)
        val btnLoadAvatar: Button = findViewById(R.id.btn_load_avatar)
        btnDeleteFolder = findViewById(R.id.btn_delete_folder)
        btnEnroll = findViewById(R.id.btn_enroll)
        btnVerify = findViewById(R.id.btn_verify)
        tvLog = findViewById(R.id.tv_log)

        loadAvatarIfPresent()
        setupAvatarInteractions()
        btnLoadAvatar.setOnClickListener { promptLoadAvatarUrl() }

        tts = TextToSpeech(this, this)
        verifier = SpeakerVerifier(this.applicationContext)
        recorder = AudioRecorder()

        btnMakePdf.setOnClickListener {
            appendLog("Verification: starting speaker verification...")
            verifySpeaker { ok ->
                if (!ok) {
                    appendLog("Status: Access Denied: Voice mismatch.")
                    speak("Access Denied: Voice mismatch.")
                    return@verifySpeaker
                }
                pickImagesForPdf()
            }
        }
        btnEnroll.setOnClickListener {
            appendLog("Enrollment: starting speaker verification...")
            verifySpeaker { ok ->
                if (!ok) {
                    appendLog("Status: Access Denied: Voice mismatch.")
                    speak("Access Denied: Voice mismatch.")
                    return@verifySpeaker
                }
                enrollFlow()
            }
        }

        btnVerify.setOnClickListener {
            appendLog("Verification: starting speaker verification (probe)...")
            verifySpeaker { ok ->
                if (!ok) {
                    appendLog("Status: Access Denied: Voice mismatch.")
                    speak("Access Denied: Voice mismatch.")
                    return@verifySpeaker
                }
                verifyFlow()
            }
        }
        // Read news button: opens browser and reads top headlines
        btnReadNews = findViewById(R.id.btn_read_news)
        btnReadNews.setOnClickListener {
            val newsUrl = "https://apbnews.com/" // replace with desired news channel URL
            // open in browser
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                appendLog("Could not open browser: ${e.message}")
            }
            // fetch and read headlines
            CoroutineScope(Dispatchers.IO).launch {
                fetchAndReadNews(newsUrl)
            }
        }
        btnDeleteFolder.setOnClickListener {
            appendLog("Verification: starting speaker verification for deletion...")
            verifySpeaker { ok ->
                if (!ok) {
                    appendLog("Status: Access Denied: Voice mismatch.")
                    speak("Access Denied: Voice mismatch.")
                    return@verifySpeaker
                }
                requestDeleteFolderWithVoiceConfirm()
            }
        }
    }

    private fun loadAvatarIfPresent() {
        try {
            val ims = assets.open("myra.png")
            val bmp = BitmapFactory.decodeStream(ims)
            ims.close()
            ivAvatar.setImageBitmap(bmp)
        } catch (e: Exception) {
            // no avatar asset — keep ImageView empty
            ivAvatar.setImageResource(R.drawable.myra_placeholder)
        }
    }

    private fun setupAvatarInteractions() {
        ivAvatar.setOnClickListener {
            // small friendly animation (scale + tilt)
            val scaleX = ObjectAnimator.ofFloat(ivAvatar, "scaleX", 1f, 1.06f, 1f)
            val scaleY = ObjectAnimator.ofFloat(ivAvatar, "scaleY", 1f, 1.06f, 1f)
            val rot = ObjectAnimator.ofFloat(ivAvatar, "rotation", 0f, -6f, 0f)
            val set = AnimatorSet()
            set.playTogether(scaleX, scaleY, rot)
            set.duration = 600
            set.start()
            speak("Hello, I am Myra. How can I help?")
        }

        ivAvatar.setOnLongClickListener {
            speak("Long press detected. You can load a custom avatar via the button.")
            true
        }
    }

    private fun promptLoadAvatarUrl() {
        val edit = EditText(this)
        edit.hint = "https://example.com/myra.png"
        AlertDialog.Builder(this)
            .setTitle("Load avatar from URL")
            .setView(edit)
            .setPositiveButton("Load") { _, _ ->
                val url = edit.text.toString().trim()
                if (url.isNotEmpty()) loadAvatarFromUrl(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAvatarFromUrl(url: String) {
        appendLog("Avatar: loading from $url")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val conn = java.net.URL(url).openConnection()
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val ins = conn.getInputStream()
                    val bmp = BitmapFactory.decodeStream(ins)
                    ins.close()
                    bmp
                }
                if (bitmap != null) {
                    ivAvatar.setImageBitmap(bitmap)
                    appendLog("Avatar: loaded successfully")
                } else {
                    appendLog("Avatar: failed to decode image")
                }
            } catch (e: Exception) {
                appendLog("Avatar load failed: ${e.message}")
            }
        }
    }

    private fun appendLog(text: String) {
        val now = Date()
        tvLog.append("[${now}] $text\n")
    }

    // Placeholder speaker verification. Replace with real speaker-ID implementation.
    private fun verifySpeaker(callback: (Boolean) -> Unit) {
        if (!hasRecordPermission()) {
            appendLog("Requesting RECORD_AUDIO permission for quick verification")
            requestRecordPermission()
            callback(false)
            return
        }
        appendLog("Speaker verification: recording probe (2s)")
        CoroutineScope(Dispatchers.Main).launch {
            recorder?.startRecording()
            delay(2000)
            val shorts = recorder?.stopRecording() ?: ShortArray(0)
            val floats = FloatArray(shorts.size)
            for (i in shorts.indices) floats[i] = shorts[i] / 32768.0f
            val ok = verifier?.verify("owner", floats) ?: false
            appendLog("Speaker verification result: ${'$'}ok")
            callback(ok)
        }
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1234)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // no-op: flows will check permission again
    }

    private fun enrollFlow() {
        if (!hasRecordPermission()) {
            appendLog("Requesting RECORD_AUDIO permission for enrollment")
            requestRecordPermission()
            return
        }
        appendLog("Enrollment: please speak now (3 seconds)")
        speak("Please speak now to enroll.")
        CoroutineScope(Dispatchers.Main).launch {
            recorder?.startRecording()
            delay(3000)
            val shorts = recorder?.stopRecording() ?: ShortArray(0)
            // convert to float normalized -1..1
            val floats = FloatArray(shorts.size)
            for (i in shorts.indices) floats[i] = shorts[i] / 32768.0f
            verifier?.enroll("owner", floats)
            appendLog("Enrollment: completed for owner")
            speak("Enrollment completed.")
        }
    }

    private fun verifyFlow() {
        if (!hasRecordPermission()) {
            appendLog("Requesting RECORD_AUDIO permission for verification")
            requestRecordPermission()
            return
        }
        appendLog("Verification: please speak now (2 seconds)")
        speak("Please speak now to verify.")
        CoroutineScope(Dispatchers.Main).launch {
            recorder?.startRecording()
            delay(2000)
            val shorts = recorder?.stopRecording() ?: ShortArray(0)
            val floats = FloatArray(shorts.size)
            for (i in shorts.indices) floats[i] = shorts[i] / 32768.0f
            val ok = verifier?.verify("owner", floats) ?: false
            appendLog("Verification result: ${'$'}ok")
            if (ok) speak("Access granted. Hello, owner.") else speak("Access Denied: Voice mismatch.")
        }
    }

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris == null || uris.isEmpty()) {
            appendLog("PDF creation canceled: no images selected")
            return@registerForActivityResult
        }
        appendLog("Status: Processing PDF...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outFile = File(getExternalFilesDir(null), "merged_${System.currentTimeMillis()}.pdf")
                mergeImagesToPdf(uris, outFile)
                runOnUiThread {
                    appendLog("Status: PDF saved to ${outFile.absolutePath}")
                    speak("Your PDF is ready. Saved to ${outFile.absolutePath}")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    appendLog("Status: PDF creation failed: ${e.message}")
                    speak("I couldn't create the PDF.")
                }
            }
        }
    }

    private fun pickImagesForPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImagesLauncher.launch(arrayOf("image/*"))
    }

    private fun mergeImagesToPdf(uris: List<Uri>, outputFile: File) {
        val document = PdfDocument()
        uris.forEachIndexed { index, uri ->
            val input: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(input)
            input?.close()

            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
        }
        val fos = FileOutputStream(outputFile)
        document.writeTo(fos)
        fos.close()
        document.close()
    }

    // Deletion flow: requires double confirmation phrase spoken by the owner.
    private fun requestDeleteFolderWithVoiceConfirm() {
        appendLog("Status: Deleting Folder... awaiting voice confirmation")
        // Start speech recognizer to capture exact phrase: "Confirm delete"
        val sr = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                appendLog("Deletion canceled: confirmation failed (speech error)")
                speak("Deletion canceled: confirmation failed.")
                sr.destroy()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(RESULTS_RECOGNITION)
                val heard = matches?.firstOrNull() ?: ""
                appendLog("Voice confirmation heard: $heard")
                if (heard.equals("Confirm delete", true) || heard.equals("confirm delete", true)) {
                    // Proceed to delete a demo folder - replace with actual target
                    val demoFolder = File(getExternalFilesDir(null), "demo_delete")
                    val success = deleteRecursively(demoFolder)
                    if (success) {
                        appendLog("Status: Deleting Folder... Completed")
                        speak("Folder deleted successfully.")
                    } else {
                        appendLog("Status: Deleting Folder... Failed")
                        speak("I couldn't delete the folder.")
                    }
                } else {
                    appendLog("Deletion canceled: confirmation failed")
                    speak("Deletion canceled: confirmation failed.")
                }
                sr.destroy()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        sr.startListening(intent)
        speak("Please say 'Confirm delete' to proceed.")
    }

    private fun deleteRecursively(target: File?): Boolean {
        if (target == null || !target.exists()) return false
        if (target.isDirectory) {
            target.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        return target.delete()
    }

    private fun fetchAndReadNews(url: String) {
        val client = OkHttpClient()
        try {
            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    runOnUiThread {
                        appendLog("News fetch failed: ${'$'}{resp.code}")
                        speak("I couldn't fetch the news.")
                    }
                    return
                }
                val html = resp.body?.string() ?: ""
                val regex = Regex("<h[1-3][^>]*>(.*?)</h[1-3]>", RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL)
                val found = regex.findAll(html).map { stripHtml(it.groupValues[1]) }.filter { it.isNotBlank() }.toList()
                val headlines = if (found.isNotEmpty()) found.take(5) else {
                    val titleRegex = Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL)
                    titleRegex.find(html)?.groupValues?.get(1)?.let { listOf(stripHtml(it)) } ?: emptyList()
                }
                if (headlines.isEmpty()) {
                    runOnUiThread {
                        appendLog("News: no headlines found on page")
                        speak("I couldn't find headlines on that page.")
                    }
                    return
                }
                val speakText = buildString {
                    append("Here are the top headlines: ")
                    headlines.forEachIndexed { i, h ->
                        append(h)
                        if (i < headlines.size - 1) append(". Next: ")
                    }
                }
                runOnUiThread {
                    appendLog("News fetched: ${'$'}{headlines.size} headlines")
                    speak(speakText)
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                appendLog("News fetch error: ${e.message}")
                speak("I couldn't fetch the news: ${e.message}")
            }
        }
    }

    private fun stripHtml(input: String): String {
        return Html.fromHtml(input, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }

    // --- Voice command listener and parser ---
    private fun startVoiceCommandListening() {
        appendLog("Voice command: listening...")
        speak("Listening for your command")
        val sr = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                appendLog("Voice command error: $error")
                speak("I couldn't hear that. Try again.")
                sr.destroy()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(RESULTS_RECOGNITION)
                val heard = matches?.firstOrNull() ?: ""
                appendLog("Voice command heard: $heard")
                handleVoiceCommand(heard)
                sr.destroy()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        sr.startListening(intent)
    }

    private fun handleVoiceCommand(text: String) {
        val lower = text.lowercase(Locale.getDefault())
        when {
            lower.contains("open chrome") || lower.contains("open browser") || lower.contains("chrome") -> {
                appendLog("Command: open browser")
                speak("Opening browser")
                try {
                    val newsUrl = "https://apbnews.com/"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(newsUrl))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } catch (e: Exception) {
                    appendLog("Could not open browser: ${e.message}")
                    speak("I couldn't open the browser")
                }
            }
            lower.contains("news") || lower.contains("read news") || lower.contains("headlines") -> {
                appendLog("Command: read news")
                speak("Fetching today\'s news")
                CoroutineScope(Dispatchers.IO).launch { fetchAndReadNews("https://apbnews.com/") }
            }
            lower.contains("delete") || lower.contains("remove") -> {
                appendLog("Command: delete requested")
                speak("This is a protected action. Verifying your voice now.")
                verifySpeaker { ok ->
                    if (!ok) {
                        appendLog("Voice command delete: verification failed")
                        speak("Verification failed. I will not delete anything.")
                        return@verifySpeaker
                    }
                    // perform demo delete
                    val demoFolder = File(getExternalFilesDir(null), "demo_delete")
                    val success = deleteRecursively(demoFolder)
                    if (success) {
                        appendLog("Voice command delete: completed")
                        speak("Folder deleted successfully.")
                    } else {
                        appendLog("Voice command delete: failed")
                        speak("I couldn't delete the folder.")
                    }
                }
            }
            lower.contains("make pdf") || lower.contains("create pdf") || lower.contains("pdf") -> {
                appendLog("Command: make pdf")
                speak("Verifying before creating PDF")
                verifySpeaker { ok ->
                    if (!ok) {
                        appendLog("PDF creation: verification failed")
                        speak("Verification failed. I will not create the PDF.")
                        return@verifySpeaker
                    }
                    pickImagesForPdf()
                }
            }
            lower.contains("enroll") || lower.contains("enrol") -> {
                appendLog("Command: enroll voice")
                enrollFlow()
            }
            lower.contains("verify") || lower.contains("check voice") -> {
                appendLog("Command: verify voice")
                verifyFlow()
            }
            lower.contains("stop") || lower.contains("silence") -> {
                appendLog("Command: stop TTS")
                tts.stop()
                speak("Stopped")
            }
            else -> {
                appendLog("Unrecognized command: $text")
                speak("I didn't understand the command. Try again.")
            }
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("bn") // Bengali preference; fallback handled by system
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
        btnVoiceCommand = findViewById(R.id.btn_voice_command)
        btnVoiceCommand.setOnClickListener {
            if (!hasRecordPermission()) {
                appendLog("Requesting RECORD_AUDIO permission for voice commands")
                requestRecordPermission()
                return@setOnClickListener
            }
            startVoiceCommandListening()
        }
        btnStartCrew = findViewById(R.id.btn_start_crew)
        btnStartCrew.setOnClickListener {
            startCrewDemo()
        }

    private fun startCrewDemo() {
        val crew = com.example.maya.crew.CrewManager(this@MainActivity, object : com.example.maya.crew.CrewListener {
            override fun onAgentLog(agent: com.example.maya.crew.Agent, message: String) {
                runOnUiThread {
                    appendLog("[Agent ${agent.id}:${agent.name}] $message")
                    try {
                        when {
                            message.startsWith("TTS:") -> {
                                val text = message.removePrefix("TTS:")
                                speak(text)
                            }
                            message.startsWith("OPEN_BROWSER:") -> {
                                val url = message.removePrefix("OPEN_BROWSER:")
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    appendLog("Could not open browser: ${e.message}")
                                }
                            }
                            message.startsWith("headlines:") -> {
                                val payload = message.removePrefix("headlines:")
                                val headlines = payload.split("||").take(5)
                                val speech = "Top headlines: ${headlines.joinToString(", ")}" 
                                speak(speech)
                            }
                            message.startsWith("summary:") -> {
                                val summary = message.removePrefix("summary:")
                                speak(summary)
                            }
                            message.startsWith("completed") || message.contains("accepted task") || message.startsWith("archived") -> {
                                speak("${agent.name} ${message}")
                            }
                        }
                    } catch (e: Exception) {
                        appendLog("Agent log handler error: ${e.message}")
                    }
                }
            }
        })
        crew.startCrew(count = 4)
        // submit a batch of tasks; agents will self-assign
        crew.submitTasks(
            "Fetch headlines from APB",
            "Summarize latest news",
            "Prepare TTS audio",
            "Open news channel in browser",
            "Archive today's news",
            "Cleanup temp files"
        )
        // stop crew after a delay
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(8000L)
            crew.stop()
        }
    }
