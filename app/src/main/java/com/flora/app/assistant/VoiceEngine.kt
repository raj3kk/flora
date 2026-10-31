package com.flora.app.assistant

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.flora.app.data.CatcherEvents
import com.flora.app.data.CatcherHooks
import com.flora.app.voice.ServerTtsClient
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Neural-first + system-TTS fallback (free, on-device voices). Neural engine
 * juda ho (NeuralVoiceAttach se KokoroOrtEngine) to wahi bolega, warna ya
 * engine fail ho to system TTS (Lily profile) bolega. No proprietary voice is
 * cloned or copied. TTS starts immediately on a streaming-style reply:
 * the UI speaks the reply as soon as it arrives.
 */
class VoiceEngine(context: Context) {
    private val appContext: Context = context
    private var tts: TextToSpeech? = null
    private var ready = false

    /** Media mode (parity batch 2026-10-31, M10): ON = system
     * TTS default (media stream, speaker). OFF = voice-call
     * stream (earpiece ki taraf, private). FloraApp prefs se
     * set karta hai. Sirf system TTS path par asar; neural
     * engine apna routing use karta hai. Default ON. */
    var mediaMode: Boolean = true

    /**
     * Phase 1 (server-shift) Item B — cloud TTS lane (optional,
     * attach-only). MainActivity attach karta hai (prefs +
     * licenceClient wahi maujood hain); FloraInCallService /
     * BackgroundMicService attach nahi karte — wahan lane null
     * rehta hai aur system path bilkul waisa hi chalta hai.
     * Lane attach hone par bhi koi hard dependency nahi:
     * fetch fail/unavailable/free-limit/error par speak()
     * turant system path par girta hai — awaaz kabhi nahi
     * rukti. Key PENDING hai, isliye aaj lane 503 dega aur
     * fallback hi bolega; fallback tab tak rahega.
     */
    var cloudTts: ServerTtsClient? = null
        private set

    fun attachCloudTts(client: ServerTtsClient) {
        cloudTts = client
    }

    private val cloudScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cloudJob: Job? = null
    private var cloudPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

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
        // Item 34 dead-branch fix: purana romantic else-if kabhi
        // nahi chalta tha — voiceSample hamesha lily/isabella/
        // myrtle me se ek hota hai, isliye sample branch pehle
        // jeet jaati thi aur romantic tone dead tha. Ab romantic
        // PEHLE check hota hai, sample ke base par narm/dheemi
        // modulation ke saath (non-romantic branches bilkul wahi).
        // Sirf system TTS tone; neural engine ko chheda nahi.
        if (persona == "romantic") {
            if (voiceSample == "lily") {
                tts?.setPitch(0.90f)
                tts?.setSpeechRate(0.84f)
            } else if (voiceSample == "isabella") {
                tts?.setPitch(1.00f)
                tts?.setSpeechRate(0.86f)
            } else if (voiceSample == "myrtle") {
                tts?.setPitch(1.10f)
                tts?.setSpeechRate(0.88f)
            } else {
                tts?.setPitch(1.25f)
                tts?.setSpeechRate(0.90f)
            }
        } else if (voiceSample == "lily") {
            tts?.setPitch(0.95f)
            tts?.setSpeechRate(0.88f)
        } else if (voiceSample == "isabella") {
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(0.90f)
        } else if (voiceSample == "myrtle") {
            tts?.setPitch(1.15f)
            tts?.setSpeechRate(0.92f)
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
                                CatcherEvents.record("TTS", "utterance error")
                                CatcherHooks.laneError("system-tts", "utterance-error")
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
        utteranceSeq += 1
        val seq = utteranceSeq
        val lane = cloudTts
        if (lane != null) {
            // Nayi utterance purani ko flush karti hai (QUEUE_FLUSH
            // jaisa): purana cloud fetch cancel, purana player stop.
            // Seq guard (neeche) purani job ko dobara bolne nahi dega.
            cloudJob?.cancel()
            cloudJob = null
            try {
                cloudPlayer?.stop()
            } catch (e: Exception) {
            }
            try {
                cloudPlayer?.release()
            } catch (e: Exception) {
            }
            cloudPlayer = null
            utteranceStartMs = System.currentTimeMillis()
            utteranceCounted = true
            cloudJob = cloudScope.launch {
                val res = try {
                    lane.fetchAudio(clean)
                } catch (e: Exception) {
                    ServerTtsClient.TtsResult.Error("tts-proxy call fail")
                }
                // Playback + fallback main thread par — pehle wala
                // speak() bhi UI thread se hi chalta tha, isliye
                // MediaPlayer/TTS ka thread wahi rahega.
                mainHandler.post {
                    if (seq != utteranceSeq) return@post
                    when (res) {
                        is ServerTtsClient.TtsResult.Audio -> playCloudAudio(clean, seq, res.bytes)
                        else -> speakLocal(clean, seq)
                    }
                }
            }
            return
        }
        speakLocal(clean, seq)
    }

    /**
     * Phase 1 Item B — cloud audio playback. Cache file me
     * likh kar MediaPlayer se bajta hai (byte array seedha
     * nahi bajta); complete/error par file delete + release.
     * Start se pehle error aaye to system path par fallback
     * (ek baar); start ke baad error par sirf finish — dobara
     * bolna double-speak hoga, isliye nahi.
     */
    private fun playCloudAudio(clean: String, seq: Int, bytes: ByteArray) {
        var started = false
        var fellBack = false
        try {
            val file = File(appContext.cacheDir, "flora-tts-$seq.mp3")
            file.writeBytes(bytes)
            val player = MediaPlayer()
            cloudPlayer = player
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener {
                if (seq != utteranceSeq) return@setOnCompletionListener
                finishUtterance(seq)
                try {
                    file.delete()
                } catch (e: Exception) {
                }
                try {
                    player.release()
                } catch (e: Exception) {
                }
                if (cloudPlayer == player) cloudPlayer = null
                mainHandler.post { onSpeechDone?.invoke() }
            }
            player.setOnErrorListener { _, _, _ ->
                if (seq != utteranceSeq) return@setOnErrorListener true
                try {
                    file.delete()
                } catch (e: Exception) {
                }
                try {
                    player.release()
                } catch (e: Exception) {
                }
                if (cloudPlayer == player) cloudPlayer = null
                if (!started && !fellBack) {
                    fellBack = true
                    speakLocal(clean, seq)
                } else {
                    finishUtterance(seq)
                    mainHandler.post { onSpeechDone?.invoke() }
                }
                true
            }
            player.prepare()
            if (seq != utteranceSeq) {
                try {
                    player.release()
                } catch (e: Exception) {
                }
                if (cloudPlayer == player) cloudPlayer = null
                try {
                    file.delete()
                } catch (e: Exception) {
                }
                return
            }
            player.start()
            started = true
            utteranceStartMs = System.currentTimeMillis()
            utteranceCounted = false
            mainHandler.post { onSpeechStart?.invoke() }
        } catch (e: Exception) {
            if (seq != utteranceSeq) return
            if (!started && !fellBack) {
                fellBack = true
                speakLocal(clean, seq)
            } else {
                finishUtterance(seq)
                mainHandler.post { onSpeechDone?.invoke() }
            }
        }
    }

    /**
     * Pehle wala speak() body — neural engine, phir system
     * TTS (Lily profile). Cloud lane ka fallback bhi yahi hai,
     * isliye seq caller se aata hai (dobara increment nahi).
     */
    private fun speakLocal(clean: String, seq: Int) {
        // Phase 8 neural voice: engine juda ho to wahi bolega
        // (Kokoro hf_alpha jab engine + model dono hon) — iske
        // liye system TTS ka ready hona zaroori nahi. Engine
        // na ho ya fail ho to neeche system TTS (Lily profile)
        // hi chalega — system TTS is build me hataya NAHI gaya
        // (gate: hatana sirf phone test pass ke baad).
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
        // M10 media mode: ON = system default (media stream);
        // OFF = voice-call stream (earpiece, private).
        val params = if (mediaMode) null else Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
        }
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, params, "flora-reply")
    }

    fun stop() {
        // Phase 1 Item B: cloud fetch cancel + cloud player stop
        // pehle — warna stop ke baad bhi cloud audio baj sakta hai.
        cloudJob?.cancel()
        cloudJob = null
        try {
            cloudPlayer?.stop()
        } catch (e: Exception) {
        }
        try {
            cloudPlayer?.release()
        } catch (e: Exception) {
        }
        cloudPlayer = null
        // Interrupt par bhi ab tak bola hua waqt gino — poora
        // text kabhi count nahi hoga jab aadha hi bola ho.
        finishUtterance(utteranceSeq)
        // Cycle 3, F4: neural engine bhi ruke (null ho to
        // NeuralSpeaker.stop() kuch nahi karta); system
        // tts?.stop() barkaraar hai.
        NeuralSpeaker.stop()
        tts?.stop()
    }

    fun shutdown() {
        cloudJob?.cancel()
        cloudJob = null
        try {
            cloudPlayer?.release()
        } catch (e: Exception) {
        }
        cloudPlayer = null
        cloudScope.cancel()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
