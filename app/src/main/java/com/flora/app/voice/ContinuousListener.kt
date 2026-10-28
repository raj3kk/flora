package com.flora.app.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat

/**
 * Continuous listener (v2.14): system SpeechRecognizer se bina dialog
 * ke sunna — app foreground me rehte hue, mute dabane tak.
 *
 * Flow: start() -> suno -> onResults -> result MainActivity ko ->
 * Flora jawab bolegi -> TTS done par MainActivity dobara start()
 * karega. Error (chuppi/timeout) par bhi restart MainActivity se
 * hota hai, taaki ye class khud loop me na phase.
 *
 * Privacy: ye sirf foreground me chalta hai (activity pause hote
 * hi MainActivity stop() karta hai). Background me sunna WakeWord
 * service ka kaam hai ("Hey Flora"), iska nahi. Koi audio kabhi
 * save, log ya server par nahi jata — sirf pehchana hua text
 * chat pipeline ko milta hai.
 */
class ContinuousListener(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onError: () -> Unit,
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    fun isListening(): Boolean = listening

    fun start() {
        if (listening) return
        if (ContextCompat.checkSelfPermission(
                appContext, android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError()
            return
        }
        try {
            val rec = SpeechRecognizer.createSpeechRecognizer(appContext)
            rec.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onResults(results: Bundle?) {
                    listening = false
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) onResult(text)
                }

                override fun onError(error: Int) {
                    listening = false
                    onError()
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer = rec
            listening = true
            rec.startListening(intent)
        } catch (e: Exception) {
            listening = false
            recognizer = null
            onError()
        }
    }

    fun stop() {
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
        }
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
        }
        recognizer = null
        listening = false
    }
}
