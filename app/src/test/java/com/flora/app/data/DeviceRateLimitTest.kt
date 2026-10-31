package com.flora.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRateLimitTest {

    @Test
    fun windowLimitBlocksExcess() {
        val limiter = DeviceRateLimit(maxPerWindow = 3, windowMs = 60_000L)
        assertTrue(limiter.tryAction("brain-call", 1000L))
        assertTrue(limiter.tryAction("brain-call", 2000L))
        assertTrue(limiter.tryAction("brain-call", 3000L))
        assertFalse(limiter.tryAction("brain-call", 4000L))
        // Window beetne par phir chalega.
        assertTrue(limiter.tryAction("brain-call", 1000L + 60_001L))
    }

    @Test
    fun bruteForceBlocksTemporarily() {
        val limiter = DeviceRateLimit()
        repeat(4) { limiter.recordAuthFailure(1000L + it) }
        assertFalse(limiter.isBlocked(2000L))
        limiter.recordAuthFailure(2000L)
        assertTrue(limiter.isBlocked(3000L))
        assertFalse(limiter.isBlocked(1000L + DeviceRateLimit.BLOCK_MS + 60_000L))
    }

    @Test
    fun protectedActionsRequireLicenceCheck() {
        assertTrue(DeviceRateLimit.requiresLicenceCheck("whatsapp-reply"))
        assertTrue(DeviceRateLimit.requiresLicenceCheck("payment-submit"))
        assertFalse(DeviceRateLimit.requiresLicenceCheck("time-read"))
    }
}
