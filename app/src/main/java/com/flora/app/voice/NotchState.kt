package com.flora.app.voice

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Notch island state (N1–N5 batch, user order 2026-10-29).
 *
 * - IslandState: conversation-session se chalta hai (R-N-4) —
 *   raw listener callback se NAHI, taaki window boundary par
 *   flicker na ho. Priority: SPEAKING > THINKING > LISTENING >
 *   CONVERSATION_OPEN; session band ho to IDLE.
 * - Amplitude: jo listener ACTIVE hai wahi deta hai (R-N-6) —
 *   Deepgram path par BargeIn PCM RMS, system path par
 *   onRmsChanged. Notch kabhi apna AudioRecord NAHI kholta
 *   (NOTCH-08) — ye sirf shared value hai, mic ka malik nahi.
 * - Chuppi = asli low-amplitude state hai (0 ke paas), koi
 *   idle animation nahi — wave tabhi hilti hai jab awaaz hai.
 * - Ye file me koi Android import nahi hai (sirf JVM) taaki
 *   amplitude maths + state mapping unit test me chal sake.
 */
enum class IslandState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    CONVERSATION_OPEN,
}

object NotchAmplitude {

    /**
     * System recognizer RMS (dB) -> 0..1. dB se linear map
     * (10^(dB/20)): chuppi (bahut negative dB) ~0, zor ki
     * awaaz (0 dB ke paas) ~1. NaN ya bahut chhota seedha 0.
     */
    fun fromRmsDb(rmsDb: Float): Float {
        if (rmsDb.isNaN() || rmsDb.isInfinite()) return 0f
        if (rmsDb <= -60f) return 0f
        val linear = Math.pow(10.0, (rmsDb / 20f).toDouble()).toFloat()
        return linear.coerceIn(0f, 1f)
    }

    /**
     * PCM 16-bit samples -> 0..1. RMS / 32768 par sqrt lift
     * (aam bolne ki RMS 2000–8000 hoti hai, bina lift ke
     * wave bahut chhoti dikhti). Bilkul chup (sab 0) = 0.
     */
    fun fromPcm(samples: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        val n = minOf(count, samples.size)
        if (n <= 0) return 0f
        var sum = 0.0
        for (i in 0 until n) {
            val s = samples[i].toDouble()
            sum += s * s
        }
        val rms = kotlin.math.sqrt(sum / n)
        val linear = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
        return kotlin.math.sqrt(linear)
    }

    /** Session flag se island state (R-N-4 priority order). */
    fun stateFor(
        conversationOpen: Boolean,
        listening: Boolean,
        speaking: Boolean,
        thinking: Boolean,
    ): IslandState {
        if (!conversationOpen) return IslandState.IDLE
        if (speaking) return IslandState.SPEAKING
        if (thinking) return IslandState.THINKING
        if (listening) return IslandState.LISTENING
        return IslandState.CONVERSATION_OPEN
    }
}

/**
 * Process-shared notch state. Services (OverlayMicService ka
 * island view) aur FloraApp isi se padhte hain; jo listener
 * active hai wahi amplitude likhta hai. Listener callbacks
 * View ke liye hain (postInvalidate) — koi timer/animation
 * loop nahi chalata.
 */
object NotchState {

    @Volatile
    var amplitude: Float = 0f
        private set

    @Volatile
    var islandState: IslandState = IslandState.IDLE
        private set

    /**
     * Shared "interrupted" state (adopted direction): N4 path
     * (partial/final arm) aur manual Stop button ek hi flag
     * dekhte hain — Stop prompt kabhi taaza user utterance
     * ke upar nahi bolega. Nayi utterance shuru hote hi
     * false hota hai (FloraApp onSpeechStart chain).
     */
    @Volatile
    var interrupted: Boolean = false

    /**
     * Island tap handler (R-N-2): bolte waqt tap = interrupt +
     * listen, warna chat aage lao. Handler ka malik wahi jo
     * bol raha hai — foreground me MainActivity, background
     * me BackgroundMicService. Koi handler na ho to island
     * view khud chat aage la deta hai (kabhi mute toggle nahi).
     */
    @Volatile
    var onIslandTap: (() -> Unit)? = null

    private val listeners = CopyOnWriteArrayList<(IslandState, Float) -> Unit>()

    fun addListener(listener: (IslandState, Float) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (IslandState, Float) -> Unit) {
        listeners.remove(listener)
    }

    fun reportRms(rmsDb: Float) {
        amplitude = NotchAmplitude.fromRmsDb(rmsDb)
        notifyListeners()
    }

    fun reportPcm(samples: ShortArray, count: Int) {
        amplitude = NotchAmplitude.fromPcm(samples, count)
        notifyListeners()
    }

    fun setState(state: IslandState) {
        islandState = state
        notifyListeners()
    }

    fun clearAmplitude() {
        amplitude = 0f
        notifyListeners()
    }

    private fun notifyListeners() {
        val state = islandState
        val amp = amplitude
        for (listener in listeners) {
            try {
                listener(state, amp)
            } catch (e: Exception) {
            }
        }
    }
}
