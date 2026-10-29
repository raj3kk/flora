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
     * Har bolne par ASLI bola hua time (seconds, stopwatch se).
     * Free talk-limit (600 sec/day) isi se record hota hai.
     * Cycle 1, APP-C1-011 se pehle ye speak-start par andaza
     * tha (Hindi TTS ~13 chars/sec) — ab utterance ke shuru
     * (onStart) se khatam/interrupt (onDone/onError/stop) tak
     * ka guzra waqt gina jata hai. Cap ka matlab waisa hi hai,
     * sirf counting sahi hui hai; limit ke paas pahunchne par
     * Flora pehle batayegi, achanak nahi rokegi.
     */
    var onSpoken: ((Long) -> Unit)? = null

    // APP-C1-011 utterance accounting: ek waqt par ek hi
    // utterance (QUEUE_FLUSH) — seq se purani (interrupt hui)
    // utterance ka late callback nayi utterance ko count nahi
    // karega. startMs onStart par lagta hai (sachhi shuruaat),
    // neural path me speak() waqt fallback ke taur par.
    // Cycle 2, APP-C1-023: system-TTS listener block me bhi
    // neural jaisa capture kiya hua seq use hota hai — onStart
    // par listenerSeq pakda jata hai, onDone/onError wahi seq
    // dete hain (live utteranceSeq nahi), taaki tez flush par
    // purani utterance ka late callback nayi ka elapsed galat
    // na gine. Core counting (011) ka matlab waisa hi hai.
    @Volatile
    private var utteranceSeq = 0

    @Volatile
    private var listenerSeq = 0

    @Volatile
    private var utteranceStartMs = 0L

    @Volatile
    private var utteranceCounted = true

    private fun finishUtterance(seq: Int) {
        if (seq != utteranceSeq || utteranceCounted) return
        utteranceCounted = true
        val start = utteranceStartMs
        if (start > 0L) {
            val seconds = maxOf(
                1L,
                (System.currentTimeMillis() - start) / 1000L
            )
            try {
                onSpoken?.invoke(seconds)
            } catch (e: Exception) {
            }
        }
    }

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
                            if (utteranceId == "flora-reply") {
                                // APP-C1-023: isi utterance ka seq
                                // capture karo — done/error isi ko denge.
                                listenerSeq = utteranceSeq
                                utteranceStartMs = System.currentTimeMillis()
                                utteranceCounted = false
                                onSpeechStart?.invoke()
                            }
                        }
                        override fun onError(utteranceId: String?) {
                            if (utteranceId == "flora-reply") {
                                finishUtterance(listenerSeq)
                                onSpeechDone?.invoke()
                            }
                        }

                        override fun onDone(utteranceId: String?) {
                            if (utteranceId == "flora-reply") {
                                finishUtterance(listenerSeq)
                                onSpeechDone?.invoke()
                            }
                        }
                    }
                )
                ready = true
            }
        }
    }

    /**
     * TTS safai (Phase 2 voice pipeline, Phase 8 me NeuralSpeaker
     * par single-source): bolne se pehle raw text saaf hota hai —
     * Flora kabhi emoji ke NAAM nahi bolegi, na emoji character,
     * na markdown, na URL, na bekaar spaces. Hindi (Devanagari)
     * poora surakshit hai — sirf symbol/emoji ranges hat te hain.
     * Saare voice.speak callers isi speak() se guzarte hain,
     * isliye safai ek jagah hi kaafi hai. Saaf hone ke baad
     * kuch na bache to kuch nahi bola jata (aur free-talk
     * limit me bhi nahi gina jata).
     */
    fun sanitizeForSpeech(raw: String): String = NeuralSpeaker.sanitize(raw)

    fun speak(text: String) {
        val clean = sanitizeForSpeech(text)
        if (clean.isEmpty()) return
        // Phase 8 neural voice: engine juda ho to wahi bolega
        // (Kokoro hf_alpha jab engine + model dono hon) — iske
        // liye system TTS ka ready hona zaroori nahi. Engine
        // na ho ya fail ho to neeche system TTS (Lily profile)
        // hi chalega — system TTS is build me hataya NAHI gaya
        // (gate: hatana sirf phone test pass ke baad).
        utteranceSeq += 1
        val seq = utteranceSeq
        utteranceStartMs = System.currentTimeMillis()
        // Counted=true jab tak onStart na aaye: purani utterance
        // ka late onDone/onError nayi utterance ko samay se
        // pehle count nahi karega; onStart aate hi false hoga
        // (system TTS + neural engine contract dono onStart dete
        // hain — bina bole kuch count nahi hoga, jo sahi hai).
        utteranceCounted = true
        val eng = NeuralSpeaker.engine
        if (eng != null) {
            try {
                eng.speak(
                    clean,
                    {
                        utteranceStartMs = System.currentTimeMillis()
                        utteranceCounted = false
                        onSpeechStart?.invoke()
                    },
                    {
                        finishUtterance(seq)
                        onSpeechDone?.invoke()
                    }
                )
                return
            } catch (e: Exception) {
                // Engine fail: system TTS par gir jao, awaaz na ruke.
                // (Seq/start waisa hi — system path ke callbacks
                // isi utterance ko count karenge.)
            }
        }
        if (!ready) {
            utteranceCounted = true
            return
        }
        applyPersona()
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "flora-reply")
    }

    fun stop() {
        // Interrupt par bhi ab tak bola hua waqt gino — poora
        // text kabhi count nahi hoga jab aadha hi bola ho.
        finishUtterance(utteranceSeq)
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
