package com.flora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3-01 hooks ke tests — sink override karke, bina file ke.
 * Har hook kabhi throw nahi karta aur sahi category deta hai.
 */
class CatcherHooksTest {

    private fun capture(block: () -> Unit): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        val old = CatcherHooks.sink
        CatcherHooks.sink = { c, d -> out.add(c to d) }
        try {
            block()
        } finally {
            CatcherHooks.sink = old
        }
        return out
    }

    @Test
    fun allHooksEmitSafeCategories() {
        val events = capture {
            CatcherHooks.dbError("journal", "read", "io-fail")
            CatcherHooks.storageError("backup-write", "disk-full")
            CatcherHooks.networkError("brain", "timeout", "504")
            CatcherHooks.laneError("deepgram", "auth-fail")
            CatcherHooks.laneError("kokoro", "model-missing")
            CatcherHooks.laneError("wakeword", "mic-busy")
            CatcherHooks.featureError("whatsapp-reply", "settle", "node-missing")
            CatcherHooks.featureError("home-sync", "connect", "timeout")
            CatcherHooks.permissionDenied("RECORD_AUDIO", "wakeword")
            CatcherHooks.licenceRecord("gate-deny")
            CatcherHooks.serverSurfaced("licence-verify", "401")
            CatcherHooks.fcmDeliveryNote()
        }
        assertEquals(12, events.size)
        assertEquals("DB", events[0].first)
        assertEquals("LANE", events[3].first)
        assertEquals("FEATURE", events[6].first)
        assertEquals("PERMISSION", events[8].first)
        assertEquals("LICENCE", events[9].first)
        assertEquals("SERVER", events[10].first)
        assertTrue(events[11].second.contains("not capturable on-device"))
    }

    @Test
    fun hookNeverThrowsEvenIfSinkThrows() {
        val old = CatcherHooks.sink
        CatcherHooks.sink = { _, _ -> throw RuntimeException("boom") }
        try {
            CatcherHooks.dbError("journal", "read", "io-fail")
            CatcherHooks.featureError("docs", "save", "fail")
        } finally {
            CatcherHooks.sink = old
        }
    }

    @Test
    fun featureAndLaneListsCoverPlanSurfaces() {
        assertTrue(CatcherHooks.FEATURES.contains("whatsapp-reply"))
        assertTrue(CatcherHooks.FEATURES.contains("home-sync"))
        assertTrue(CatcherHooks.FEATURES.contains("builder"))
        assertTrue(CatcherHooks.LANES.contains("deepgram"))
        assertTrue(CatcherHooks.LANES.contains("kokoro"))
        assertTrue(CatcherHooks.LANES.contains("wakeword"))
    }
}
