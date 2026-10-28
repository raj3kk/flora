package com.flora.app.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * System TTS only (free, on-device voices). No proprietary voice is
 * cloned or copied. TTS starts immediately on a streaming-style reply:
 * the UI speaks the reply as soon as it arrives.
 */
class VoiceEngine(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    /**
     * Har bolne par andaza seconds (Hindi TTS ~13 chars/sec).
     * Free talk-limit (600 sec/day) isi se record hota hai — ye
     * andaza hai, stopwatch nahi; limit ke paas pahunchne par Flora
     * pehle batayegi, achanak nahi rokegi.
     */
    var onSpoken: ((Long) -> Unit)? = null

    /** TTS poora bolne par (v2.14): continuous mic isi se dobara
     * sunna shuru karta hai — taaki Flora apni awaaz khud na sune. */
    var onSpeechDone: (() -> Unit)? = null

    /** TTS shuru hote hi (v2.14): UI ko "Bol rahi hun" dikhane ke
     * liye — taaki user ko baari ka pata rahe. */
    var onSpeechStart: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindi = tts?.setLanguage(Locale("hi", "IN"))
                if (hindi == TextToSpeech.LANG_MISSING_DATA || hindi == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setOnUtteranceProgressListener(
                    object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            if (utteranceId == "flora-reply") onSpeechStart?.invoke()
                        }
                        override fun onError(utteranceId: String?) {
                            if (utteranceId == "flora-reply") onSpeechDone?.invoke()
                        }

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId == "flora-reply") onSpeechDone?.invoke()
                        }
                    }
                )
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flora-reply")
        val seconds = maxOf(1L, (text.length / 13).toLong())
        onSpoken?.invoke(seconds)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
