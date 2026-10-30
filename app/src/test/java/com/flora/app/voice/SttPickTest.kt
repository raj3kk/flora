package com.flora.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Fix-cycle B3: STT result picker ke unit tests (pure JVM — koi Android call nahi).
 */
class SttPickTest {

    @Test
    fun bestConfidenceWins() {
        assertEquals("b", pickBestResult(listOf("a", "b", "c"), floatArrayOf(0.2f, 0.9f, 0.5f)))
    }

    @Test
    fun missingScoresFallsBackToFirst() {
        assertEquals("a", pickBestResult(listOf("a", "b"), null))
    }

    @Test
    fun sizeMismatchFallsBackToFirst() {
        assertEquals("a", pickBestResult(listOf("a", "b"), floatArrayOf(0.9f)))
    }

    @Test
    fun allUnavailableFallsBackToFirst() {
        assertEquals("a", pickBestResult(listOf("a", "b"), floatArrayOf(-1f, -1f)))
    }

    @Test
    fun nanScoresSkipped() {
        assertEquals("b", pickBestResult(listOf("a", "b"), floatArrayOf(Float.NaN, 0.7f)))
    }

    @Test
    fun emptyListIsNull() {
        assertNull(pickBestResult(emptyList(), null))
    }

    @Test
    fun singleResultReturnedAsIs() {
        assertEquals("only", pickBestResult(listOf("only"), floatArrayOf(0.4f)))
    }
}
