package com.flora.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N1–N5 notch batch unit tests — sirf pure-JVM maths +
 * state mapping (NotchState.kt me koi Android import nahi
 * hai, isliye ye yahan chalte hain). Repo CI sirf
 * :app:assembleDebug chalata hai, isliye ye tests CI par
 * execute nahi hote — local `./gradlew :app:testDebugUnitTest`
 * se chalane ke liye hain (execution PENDING).
 */
class NotchAmplitudeTest {

    @Test
    fun silencePcmIsZero() {
        val silent = ShortArray(1600)
        assertEquals(0f, NotchAmplitude.fromPcm(silent, silent.size), 0.0001f)
    }

    @Test
    fun emptyPcmIsZero() {
        assertEquals(0f, NotchAmplitude.fromPcm(ShortArray(0), 0), 0.0001f)
    }

    @Test
    fun loudPcmStaysInRangeAndAboveQuiet() {
        val quiet = ShortArray(1600) { 500 }
        val loud = ShortArray(1600) { 20000 }
        val quietAmp = NotchAmplitude.fromPcm(quiet, quiet.size)
        val loudAmp = NotchAmplitude.fromPcm(loud, loud.size)
        assertTrue(quietAmp > 0f && quietAmp < 1f)
        assertTrue(loudAmp > quietAmp && loudAmp <= 1f)
    }

    @Test
    fun maxPcmClampsToOne() {
        val max = ShortArray(1600) { Short.MAX_VALUE }
        assertEquals(1f, NotchAmplitude.fromPcm(max, max.size), 0.0001f)
    }

    @Test
    fun rmsDbSilenceIsNearZeroAndLoudIsHigher() {
        val silent = NotchAmplitude.fromRmsDb(-60f)
        val mid = NotchAmplitude.fromRmsDb(-20f)
        val loud = NotchAmplitude.fromRmsDb(0f)
        assertEquals(0f, silent, 0.0001f)
        assertTrue(mid > silent && loud > mid && loud <= 1f)
    }

    @Test
    fun rmsDbInvalidIsZero() {
        assertEquals(0f, NotchAmplitude.fromRmsDb(Float.NaN), 0.0001f)
    }

    @Test
    fun statePrioritySpeakingOverThinkingOverListening() {
        assertEquals(
            IslandState.SPEAKING,
            NotchAmplitude.stateFor(true, true, true, true),
        )
        assertEquals(
            IslandState.THINKING,
            NotchAmplitude.stateFor(true, true, false, true),
        )
        assertEquals(
            IslandState.LISTENING,
            NotchAmplitude.stateFor(true, true, false, false),
        )
        assertEquals(
            IslandState.CONVERSATION_OPEN,
            NotchAmplitude.stateFor(true, false, false, false),
        )
        assertEquals(
            IslandState.IDLE,
            NotchAmplitude.stateFor(false, true, true, true),
        )
    }
}
