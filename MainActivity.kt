package com.example.myjarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val REQUEST_CODE_SPEECH_INPUT = 100
    private val PERMISSIONS_REQUEST_CODE = 200
    private lateinit var tts: TextToSpeech
    private var isTTSInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tts = TextToSpeech(this, this)
        
        btn_listen.setOnClickListener {
            checkPermissionsAndStartListening()
        }
    }

    private fun checkPermissionsAndStartListening() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = ArrayList<String>()
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        } else {
            startVoiceRecognition()
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए सर...")
        }
        startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.let { results ->
                val command = results[0].toLowerCase(Locale.getDefault())
                processCommand(command)
            }
        }
    }

    private fun processCommand(command: String) {
        when {
            command.contains("कॉल") -> handleCall(command)
            command.contains("मैसेज") -> handleSMS(command)
            command.contains("खोल") -> openApp(command)
            command.contains("जवाब") -> handleQuestion(command)
            command.contains("स्थान") -> getLocation()
            else -> speak("कृपया कमांड दोहराएं")
        }
    }

    private fun handleCall(command: String) {
        val number = command.replace("कॉल", "").trim()
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(intent)
        }
    }

    private fun handleSMS(command: String) {
        val parts = command.split("मैसेज")
        if (parts.size == 2) {
            val number = parts[0].trim()
            val message = parts[1].trim()
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$number"))
            intent.putExtra("sms_body", message)
            startActivity(intent)
        }
    }

    private fun openApp(command: String) {
        val appName = command.replace("खोल", "").trim()
        try {
            val intent = packageManager.getLaunchIntentForPackage("com.$appName")
            startActivity(intent)
        } catch (e: Exception) {
            speak("ऐप नहीं मिला")
        }
    }

    private fun getLocation() {
        // LocationManager का उपयोग करें
        speak("आपका स्थान प्राप्त किया जा रहा है")
    }

    private fun handleQuestion(question: String) {
        val cleanedQuestion = question.replace("जवाब", "").trim()
        Thread {
            val answer = GPTManager.getResponse(cleanedQuestion)
            runOnUiThread {
                speak(answer)
                tv_response.text = answer
            }
        }.start()
    }

    private fun speak(text: String) {
        if (isTTSInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("hi", "IN")
            isTTSInitialized = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startVoiceRecognition()
            }
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
