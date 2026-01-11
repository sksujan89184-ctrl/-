package com.example.maya

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.maya.crew.* // Crew ফোল্ডার থেকে ইমপোর্ট
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private lateinit var personaManager: PersonaManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        personaManager = PersonaManager(this)
        tts = TextToSpeech(this, this)

        // Crew বাটন লজিক
        findViewById<Button>(R.id.btn_start_crew).setOnClickListener {
            val crew = CrewManager(this, object : CrewListener {
                override fun onAgentLog(agent: Agent, message: String) {
                    runOnUiThread { 
                        findViewById<TextView>(R.id.tv_log).append("\n[${agent.name}]: $message") 
                    }
                }
            })
            crew.startCrew(4)
            crew.submitTasks("অ্যান্ড্রয়েড অপ্টিমাইজেশন", "ভয়েস সিস্টেম চেক")
        }

        findViewById<Button>(R.id.btn_enable_gf).setOnClickListener {
            personaManager.gfMode = !personaManager.gfMode
            val status = if (personaManager.gfMode) "GF মোড চালু" else "স্ট্যান্ডার্ড মোড"
            tts.speak(status, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale("bn", "BD")
    }
}
