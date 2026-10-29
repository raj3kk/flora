package com.flora.app.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Neural voice engine ka hook (Phase 8). Jab tak koi native
 * neural engine juda nahi hai, [engine] null rehta hai aur har
 * jagah system TTS hi chalta hai (Lily profile — gate: system
 * TTS sirf phone test pass ke baad hatega, is build me nahi).
 *
 * Engine judte hi (ek hi jagah set hoga) ye teenon raaste
 * apne aap neural ho jayenge, bina har caller chhede:
 *  1. VoiceEngine.speak() (main chat + saare voice.speak)
 *  2. CallAnnouncementReceiver (call announce)
 *  3. EventAnnounceReceiver (charger/battery announce)
 * Announcement raaston me free-talk counting kabhi nahi hoti —
 * ye farq engine aane par bhi wahi rahega.
 *
 * Engine contract: speak() async ho sakta hai; onStart bolna
 * shuru hote hi, onDone khatam hote hi fire kare. Engine throw
 * kare to caller system TTS par gir jayega (awaaz kabhi nahi
 * rukegi), isliye engine ko throw-proof hona chahiye.
 */
interface NeuralEngine {
    fun speak(text: String, onStart: () -> Unit, onDone: () -> Unit)
}

object NeuralSpeaker {

    @Volatile
    var engine: NeuralEngine? = null

    fun isNeuralAvailable(): Boolean = engine != null

    /**
     * TTS safai — VoiceEngine.sanitizeForSpeech ka asli code
     * yahan hai (Phase 2 wala hi: emoji/markdown/URL hat te
     * hain, Devanagari poora surakshit). VoiceEngine ka method
     * isi ko delegate karta hai taaki safai ek hi jagah rahe.
     */
    fun sanitize(raw: String): String {
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

    /**
     * Announcement raaston (call/event) ke liye shared speak.
     * Neural engine ho to wahi, warna purana system TTS raasta
     * (bolne ke baad shutdown — receivers ka pehle wala
     * behaviour bilkul wahi). Free-talk counting yahan kabhi
     * nahi hoti.
     */
    fun speakFixed(context: Context, text: String) {
        val clean = sanitize(text)
        if (clean.isEmpty()) return
        val eng = engine
        if (eng != null) {
            try {
                eng.speak(clean, {}, {})
                return
            } catch (e: Exception) {
                // Engine fail: neeche system TTS par gir jao.
            }
        }
        val appContext = context.applicationContext
        var tts: TextToSpeech? = null
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
                tts?.setOnUtteranceProgressListener(
                    object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) {
                            tts?.shutdown()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            tts?.shutdown()
                        }
                    }
                )
                tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "flora-announce")
            }
        }
    }
}
