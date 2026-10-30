package com.flora.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 1 Item B: ServerTtsClient ke pure helpers ke unit tests
 * (koi Android/Network call nahi — sirf cap + free-limit mapping).
 */
class ServerTtsCapTest {

    @Test
    fun capKeepsShortTextWhole() {
        assertEquals("hello", ServerTtsClient.capText("hello"))
    }

    @Test
    fun capTruncatesAt1000() {
        val long = "a".repeat(1500)
        assertEquals(1000, ServerTtsClient.capText(long).length)
    }

    @Test
    fun freeLimitOn429() {
        assertTrue(ServerTtsClient.isFreeLimit(429, ""))
    }

    @Test
    fun freeLimitOnErrorKey() {
        assertTrue(ServerTtsClient.isFreeLimit(200, "free_limit"))
    }

    @Test
    fun notFreeLimitOnOtherErrors() {
        assertFalse(ServerTtsClient.isFreeLimit(503, "tts keys not configured"))
        assertFalse(ServerTtsClient.isFreeLimit(502, "tts provider failed"))
        assertFalse(ServerTtsClient.isFreeLimit(401, "unauthorized"))
    }
}
