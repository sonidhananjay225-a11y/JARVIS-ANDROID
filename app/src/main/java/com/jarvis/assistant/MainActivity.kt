package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 48)
        }
        val heading = TextView(this).apply {
            text = "JARVIS"
            textSize = 32f
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Ready. Tap the microphone and speak."
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        val listen = Button(this).apply {
            text = "🎙  TAP TO SPEAK"
            setOnClickListener { startListening() }
        }
        layout.addView(heading)
        layout.addView(status)
        layout.addView(listen)
        setContentView(layout)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say("Speech recognition is not available on this phone.")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "Processing…" }
            override fun onError(error: Int) { status.text = "Didn't catch that. Tap to try again." }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                status.text = spoken
                handleCommand(spoken)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command")
        }
        recognizer?.startListening(intent)
    }

    private fun handleCommand(raw: String) {
        val command = raw.lowercase(Locale.ROOT).trim()
        when {
            command.startsWith("search ") || command.startsWith("search for ") || command.startsWith("google ") -> {
                val query = command.removePrefix("search for ").removePrefix("search ").removePrefix("google ").trim()
                if (query.isNotBlank()) {
                    startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", query))
                    say("Searching the web.")
                }
            }
            command.contains("youtube") -> {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com")))
                say("Opening YouTube.")
            }
            command.contains("chrome") || command.contains("browser") -> {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")))
                say("Opening browser.")
            }
            command.contains("pw") || command.contains("physics wallah") -> {
                val launch = packageManager.getLaunchIntentForPackage("com.pw.live")
                if (launch != null) {
                    startActivity(launch)
                    say("Opening PW.")
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/search?q=Physics%20Wallah&c=apps")))
                    say("PW app was not found. Opening its Play Store listing.")
                }
            }
            command.contains("time") || command.contains("समय") -> {
                val now = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())
                say("The time is $now")
                status.text = "Time: $now"
            }
            else -> say("I can open PW, YouTube, the browser, and search the web. More commands will be added.")
        }
    }

    private fun say(message: String) {
        status.text = message
        if (::tts.isInitialized) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) tts.language = Locale("hi", "IN")
    }

    override fun onDestroy() {
        recognizer?.destroy()
        if (::tts.isInitialized) tts.shutdown()
        super.onDestroy()
    }
}
