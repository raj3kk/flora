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

    /** Voice-per-persona (v2.22, chunk 4): persona ke hisaab se
     *  pitch/rate — flora: saaf aur halki; romantic: thodi narm
     *  aur dheemi. Sirf tone badalta hai, koi voice copy nahi.
     *  Voice sample (v2.22, user selection 2026-10-28: Sample 2
     *  Lily fixed): sample = awaaz ki pehchaan, isliye sample
     *  profile persona se upar lagti hai — har speak() par,
     *  har jagah. Lily: gahri (pitch neeche) + dheemi + mellow. */
    @Volatile
    var persona: String = "flora"

    /** Chuna hua voice sample: "isabella" / "lily" / "myrtle".
     *  Default "lily" — user ne Sample 2 ko har jagah fix kiya
     *  hai (2026-10-28). Deepgram aane par isi style ki voice
     *  dhoondhi jayegi; tab tak system TTS par ye profile. */
    @Volatile
    var voiceSample: String = "lily"

    private fun applyPersona() {
        if (voiceSample == "lily") {
            tts?.setPitch(0.95f)
            tts?.setSpeechRate(0.88f)
        } else if (voiceSample == "isabella") {
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(0.90f)
        } else if (voiceSample == "myrtle") {
            tts?.setPitch(1.15f)
            tts?.setSpeechRate(0.92f)
        } else if (persona == "romantic") {
            tts?.setPitch(1.25f)
            tts?.setSpeechRate(0.90f)
        } else {
            tts?.setPitch(1.1f)
            tts?.setSpeechRate(0.95f)
        }
    }

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val hindi = tts?.setLanguage(Locale("hi", "IN"))
                if (hindi == TextToSpeech.LANG_MISSING_DATA || hindi == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.ENGLISH)
                }
                tts?.setSpeechRate(0.95f)
                tts?.setPitch(1.1f)
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

    /**
     * TTS safai (Phase 2 voice pipeline): bolne se pehle raw
     * text saaf hota hai — Flora kabhi emoji ke NAAM nahi
     * bolegi, na emoji character, na markdown, na URL, na
     * bekaar spaces. Hindi (Devanagari) poora surakshit hai —
     * sirf symbol/emoji ranges hat te hain. Saare voice.speak
     * callers isi speak() se guzarte hain, isliye safai ek
     * jagah hi kaafi hai. Saaf hone ke baad kuch na bache to
     * kuch nahi bola jata (aur free-talk limit me bhi nahi gina jata).
     */
    fun sanitizeForSpeech(raw: String): String {
        var s = raw.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        s = s.replace(Regex("https?://\\S+"), " ")
        s = s.replace(Regex("[#*`>_|~]"), " ")
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val cp = s.codePointAt(i)
            if (!isEmojiCodePoint(cp)) sb.appendCodePoint(cp)
            i += Character.charCount(cp)
        }
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }

    private fun isEmojiCodePoint(cp: Int): Boolean {
        return cp in 0x1F000..0x1FAFF ||
            cp in 0x2600..0x27BF ||
            cp in 0x2B00..0x2BFF ||
            cp in 0x2190..0x21FF ||
            cp in 0x2300..0x23FF ||
            cp == 0x200D ||
            cp in 0xFE00..0xFE0F ||
            cp == 0x20E3
    }

    fun speak(text: String) {
        if (!ready) return
        val clean = sanitizeForSpeech(text)
        if (clean.isEmpty()) return
        applyPersona()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "flora-reply")
        val seconds = maxOf(1L, (clean.length / 13).toLong())
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
