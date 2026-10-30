package com.flora.app.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.flora.app.data.CatcherEvents

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
/**
 * N2/N5: system path ke naye callbacks (sabke default khali
 * hain, purane callers bina badle chalte hain):
 * - onSpeechStarted: bolna shuru hua (sirf ARM — bina text
 *   ke stop nahi, warna Flora ki apni awaaz bleed karke
 *   khud ko rok degi).
 * - onPartial: aadha pehchana text (non-blank, non-echo
 *   hone par hi caller TTS rokega).
 * - RMS har frame par NotchState ko jata hai (R-N-6: system
 *   path active ho to yehi wave ka source hai).
 */
class ContinuousListener(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onError: () -> Unit,
    private val onSpeechStarted: () -> Unit = {},
    private val onPartial: (String) -> Unit = {},
    // B3 (fix-cycle): alternatives ka optional callback — default khali,
    // isliye maujooda 3 call sites bina badle compile honge.
    private val onAlternatives: (List<String>) -> Unit = {},
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    /**
     * Mic beep mute (v2.15): system SpeechRecognizer kuch phones
     * par sunna shuru/khatam hote hi "tu-ta" beep bajata hai.
     * Sunne ke dauraan system/music/notification stream mute
     * karke beep dabayi jati hai; result/error/stop par turant
     * unmute, aur 20s ka failsafe bhi hai taaki phone kabhi
     * mute state me na chhoote. Kuch phones ki beep isse bhi
     * nahi rukti (device ka apna sound) — wo imandari se
     * bataya jayega, chhupaya nahi.
     */
    private val mainHandler = Handler(Looper.getMainLooper())
    private var muted = false
    private val unmuteFailsafe = Runnable { unmuteBeeps() }

    private fun muteBeeps() {
        try {
            val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return
            @Suppress("DEPRECATION")
            am.setStreamMute(AudioManager.STREAM_SYSTEM, true)
            @Suppress("DEPRECATION")
            am.setStreamMute(AudioManager.STREAM_MUSIC, true)
            @Suppress("DEPRECATION")
            am.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
            muted = true
            mainHandler.removeCallbacks(unmuteFailsafe)
            mainHandler.postDelayed(unmuteFailsafe, 20_000)
        } catch (e: Exception) {
        }
    }

    private fun unmuteBeeps() {
        if (!muted) return
        try {
            val am = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return
            @Suppress("DEPRECATION")
            am.setStreamMute(AudioManager.STREAM_SYSTEM, false)
            @Suppress("DEPRECATION")
            am.setStreamMute(AudioManager.STREAM_MUSIC, false)
            @Suppress("DEPRECATION")
            am.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
        } catch (e: Exception) {
        }
        muted = false
        mainHandler.removeCallbacks(unmuteFailsafe)
    }

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
            CatcherEvents.record("STT", "STT unavailable")
            onError()
            return
        }
        try {
            val rec = SpeechRecognizer.createSpeechRecognizer(appContext)
            rec.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {
                    try {
                        onSpeechStarted()
                    } catch (e: Exception) {
                    }
                }
                override fun onRmsChanged(rmsdB: Float) {
                    try {
                        NotchState.reportRms(rmsdB)
                    } catch (e: Exception) {
                    }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        try {
                            onPartial(partial)
                        } catch (e: Exception) {
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onResults(results: Bundle?) {
                    listening = false
                    unmuteBeeps()
                    // B3 (fix-cycle): pehle sirf firstOrNull liya jata tha —
                    // ab confidence se behtar candidate chuno (SttPick).
                    // Scores na milen to pickBestResult pehla result deta hai
                    // (maujooda behavior, fail-closed).
                    val list = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: emptyList()
                    val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                    val text = pickBestResult(list, scores)
                    if (list.size > 1) {
                        try {
                            onAlternatives(list)
                        } catch (e: Exception) {
                        }
                    }
                    if (!text.isNullOrBlank()) onResult(text)
                }

                override fun onError(error: Int) {
                    listening = false
                    unmuteBeeps()
                    CatcherEvents.record("STT", "system STT error code=$error")
                    onError()
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                // B3 (fix-cycle): 1 → 5 — recognizer se alternatives maango
                // taaki confidence se behtar candidate chuna ja sake.
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }
            recognizer = rec
            listening = true
            muteBeeps()
            rec.startListening(intent)
        } catch (e: Exception) {
            listening = false
            recognizer = null
            unmuteBeeps()
            CatcherEvents.record("STT", "STT start fail")
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
        unmuteBeeps()
        try {
            NotchState.clearAmplitude()
        } catch (e: Exception) {
        }
    }
}
