package com.flora.app.viewing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lane A item 37 — CI-safe unit tests (plain JUnit, koi Android
 * dependency nahi — ViewingSampler.kt me koi Android import
 * nahi hai, isliye ye tests JVM par chalte hain).
 *
 * Cover karta hai: rate clamp (2-5 fps), interval (200-500ms),
 * guard order (fail-closed), latest-frame-wins + stale drop,
 * rate-cap gate. Screenshot/polling ka LIVE behavior CI par
 * test NAHI ho sakta (CI me device screen nahi hoti) — wo
 * sirf phone/debug APK protocol (§10) se verify hoga, iska
 * daava yahan nahi kiya gaya hai.
 */
class ViewingSamplerTest {

    @Test
    fun rateClampsToTwoToFive() {
        assertEquals(2, ViewingRate.clamp(1))
        assertEquals(2, ViewingRate.clamp(2))
        assertEquals(3, ViewingRate.clamp(3))
        assertEquals(5, ViewingRate.clamp(5))
        assertEquals(5, ViewingRate.clamp(9))
        assertEquals(2, ViewingRate.DEFAULT_FPS)
    }

    @Test
    fun rateIntervalIs200To500ms() {
        assertEquals(500L, ViewingRate.intervalMs(2))
        assertEquals(200L, ViewingRate.intervalMs(5))
        assertEquals(333L, ViewingRate.intervalMs(3))
    }

    @Test
    fun guardsPassWhenAllTrue() {
        assertNull(
            ViewingGuards.evaluate(
                userConfirmed = true,
                plannerConfirmed = true,
                serviceOn = true,
                apiLevel = 34,
                indicatorReady = true,
            )
        )
    }

    @Test
    fun guardOrderIsFailClosedFirstFailureWins() {
        // User request sabse pehle — baaki sab fail hon to bhi yehi verdict.
        assertEquals(
            ViewingGuardFailure.USER_REQUEST_MISSING,
            ViewingGuards.evaluate(false, false, false, 26, false)
        )
        // Planner doosra.
        assertEquals(
            ViewingGuardFailure.PLANNER_NOT_CONFIRMED,
            ViewingGuards.evaluate(true, false, false, 26, false)
        )
        // Service OFF, API guard se pehle (§8 kram: ON check point 3, API point 4).
        assertEquals(
            ViewingGuardFailure.SERVICE_OFF,
            ViewingGuards.evaluate(true, true, false, 26, false)
        )
        // API <30 -> graceful unavailable, koi fallback nahi.
        assertEquals(
            ViewingGuardFailure.API_TOO_LOW,
            ViewingGuards.evaluate(true, true, true, 29, false)
        )
        // Indicator na ho to polling nahi.
        assertEquals(
            ViewingGuardFailure.INDICATOR_UNAVAILABLE,
            ViewingGuards.evaluate(true, true, true, 30, false)
        )
        // API 30 boundary pass hai.
        assertNull(
            ViewingGuards.evaluate(true, true, true, 30, true)
        )
    }

    @Test
    fun samplerLatestFrameWinsAndDropsStale() {
        val sampler = ViewingSampler()
        assertTrue(sampler.accept(1L))
        assertTrue(sampler.accept(2L))
        assertFalse(sampler.accept(1L))
        assertFalse(sampler.accept(2L))
        assertTrue(sampler.accept(3L))
        assertEquals(3L, sampler.accepted)
        assertEquals(2L, sampler.droppedStale)
        assertTrue(sampler.isLatest(3L))
        assertFalse(sampler.isLatest(2L))
    }

    @Test
    fun pollGateCapsRate() {
        val gate = ViewingPollGate()
        assertTrue(gate.shouldRequest(1000L, 500L))
        gate.recordRequest(1000L)
        assertFalse(gate.shouldRequest(1200L, 500L))
        assertFalse(gate.shouldRequest(1499L, 500L))
        assertTrue(gate.shouldRequest(1500L, 500L))
    }
}
