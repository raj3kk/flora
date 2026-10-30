package com.flora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Catcher sanitizer ke unit tests (pure JVM).
 */
class CatcherSanitizeTest {

    @Test
    fun masksUtr() {
        val out = CatcherSanitize.sanitize("UTR 123456789012 bheja gaya")
        assertTrue(out.contains("********9012"))
        assertTrue(!out.contains("123456789012"))
    }

    @Test
    fun masksFloraKey() {
        val out = CatcherSanitize.sanitize("key FLORA-ABCD-EFGH-IJKL mili")
        assertTrue(out.contains("FLORA-****-****-****"))
        assertTrue(!out.contains("FLORA-ABCD"))
    }

    @Test
    fun masksLongToken() {
        val token = "a".repeat(40)
        val out = CatcherSanitize.sanitize("token $token yahan")
        assertTrue(out.contains("[token-masked]"))
        assertTrue(!out.contains(token))
    }

    @Test
    fun masksEmail() {
        val out = CatcherSanitize.sanitize("contact user@example.com par karo")
        assertTrue(out.contains("[email-masked]"))
        assertTrue(!out.contains("user@example.com"))
    }

    @Test
    fun stripsUrlQuery() {
        val out = CatcherSanitize.sanitize("https://example.com/a?token=abc123&b=1 kholo")
        assertTrue(out.contains("https://example.com/a"))
        assertTrue(!out.contains("token=abc123"))
    }

    @Test
    fun masksSecretAssign() {
        val out = CatcherSanitize.sanitize("api key=supersecret123 check karo")
        assertTrue(out.contains("key=[masked]"))
        assertTrue(!out.contains("supersecret123"))
    }

    @Test
    fun idempotent() {
        val input = "UTR 123456789012, key FLORA-ABCD-EFGH-IJKL, " +
            "user@example.com, https://example.com/a?token=abc&b=1"
        val once = CatcherSanitize.sanitize(input)
        assertEquals(once, CatcherSanitize.sanitize(once))
    }

    @Test
    fun leavesNormalText() {
        val input = "youtube kholo to arijit ka gana bajao"
        assertEquals(input, CatcherSanitize.sanitize(input))
    }
}
