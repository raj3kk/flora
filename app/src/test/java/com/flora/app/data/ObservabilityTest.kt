package com.flora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservabilityTest {

    @Test
    fun traceIdFormatAndValidation() {
        val id = Observability.newTraceId(1700000000000L, "aB3x9Q")
        assertTrue(Observability.isValidTraceId(id))
        assertTrue(id.startsWith("flora-1700000000000-"))
        assertFalse(Observability.isValidTraceId("nope"))
        assertFalse(Observability.isValidTraceId("flora-123-short"))
    }

    @Test
    fun metricCountersCount() {
        val counters = Observability.MetricCounters()
        counters.increment("provider-fail")
        counters.increment("provider-fail")
        counters.increment("http-5xx")
        assertEquals(2L, counters.snapshot()["provider-fail"])
        assertEquals(1L, counters.snapshot()["http-5xx"])
    }

    @Test
    fun allEightPlaybooksPresent() {
        val expected = setOf(
            "provider-outage", "leaked-key", "payment-dispute", "bad-release",
            "migration-fail", "fcm-outage", "account-compromise", "restore"
        )
        assertEquals(expected, Observability.PLAYBOOKS.keys)
        assertTrue(Observability.ALERT_THRESHOLDS.containsKey("crash-spike"))
        assertTrue(Observability.ALERT_THRESHOLDS.containsKey("quota-anomaly"))
    }
}
