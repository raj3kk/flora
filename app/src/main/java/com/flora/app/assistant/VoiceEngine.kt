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

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindi = tts?.setLanguage(Locale("hi", "IN"))
                if (hindi == TextToSpeech.LANG_MISSING_DATA || hindi == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flora-reply")
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
