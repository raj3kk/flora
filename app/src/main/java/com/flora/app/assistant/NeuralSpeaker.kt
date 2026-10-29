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

    /**
     * Cycle 3, F4: chal rahi neural utterance roko. Engine abhi
     * null hai (koi implementation repo me nahi), isliye ye
     * sirf contract hai — koi neural engine implement nahi kiya
     * gaya hai, aur system TTS hi bolega. Engine judne par use
     * ye method bhi implement karni hogi (warna stop kaam nahi
     * karega — isliye default empty nahi rakha gaya).
     */
    fun stop()
}

object NeuralSpeaker {

    @Volatile
    var engine: NeuralEngine? = null

    fun isNeuralAvailable(): Boolean = engine != null

    /**
     * Cycle 3, F4: neural engine ka stop plumbing. Engine null
     * ho to kuch nahi hota (system fallback waisa hi); engine
     * ho to uska stop() chalta hai. VoiceEngine.stop() isi ko
     * bulata hai, isliye Stop button + barge-in stop path dono
     * neural raaste par bhi lagenge (jab engine aayega).
     */
    /**
     * N4/NOTCH-06: speakFixed ke system-TTS branch ka handle.
     * Pehle ye private instance kisi stop ke haath nahi aata
     * tha (wake ack / call / event announcement ruk nahi sakta
     * tha). Ab VoiceEngine.stop() -> NeuralSpeaker.stop() isi
     * handle ko bhi rokta hai — ek hi stop sab raaston ka
     * malik hai (R-N-3, route: NeuralSpeaker-owned handle).
     */
    @Volatile
    private var fixedTts: TextToSpeech? = null

    fun stop() {
        try {
            engine?.stop()
        } catch (e: Exception) {
            // Engine stop fail: awaaz rokne ki koshish kabhi
            // caller ko crash nahi degi; system stop alag se
            // VoiceEngine.stop() me chalta hai.
        }
        val fixed = fixedTts
        fixedTts = null
        if (fixed != null) {
            try {
                fixed.stop()
            } catch (e: Exception) {
            }
            try {
                fixed.shutdown()
            } catch (e: Exception) {
            }
        }
    }

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
                            // A3 N1-P2-3: onDone par handle clear —
                            // warna baad ka stop() already-shutdown
                            // instance ko dobara chhuta tha.
                            if (fixedTts === tts) fixedTts = null
                            tts?.shutdown()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            if (fixedTts === tts) fixedTts = null
                            tts?.shutdown()
                        }
                    }
                )
                tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "flora-announce")
            }
        }
        // NOTCH-06: system branch ka handle bachao taaki
        // VoiceEngine.stop() ise bhi rok sake (pehle ye
        // instance caller ke haath se nikal jata tha).
        // A3 N1-P2-3: concurrent speakFixed par purana handle
        // chupchaap overwrite nahi — pehle wala flush (speak
        // me QUEUE_FLUSH jaisa hi), phir naya handle.
        val prevFixed = fixedTts
        fixedTts = null
        if (prevFixed != null) {
            try {
                prevFixed.stop()
            } catch (e: Exception) {
            }
            try {
                prevFixed.shutdown()
            } catch (e: Exception) {
            }
        }
        fixedTts = tts
    }
}
