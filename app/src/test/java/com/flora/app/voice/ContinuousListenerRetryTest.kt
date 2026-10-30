package com.flora.app.voice

import android.speech.SpeechRecognizer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 0, Item 3 — session-internal retry decision ke unit tests
 * (pure JVM — retry decision me koi Android call nahi, isliye
 * SpeechRecognizer ke error constants yahan bhi chalenge).
 * Cover karta hai: transient errors retry hote hain, cap par rukte
 * hain, non-transient (CLIENT/PERMISSIONS/AUDIO/BUSY/SERVER)
 * kabhi retry nahi hote.
 */
class ContinuousListenerRetryTest {

    @Test
    fun transientErrorsRetry() {
        assertTrue(shouldRetrySttError(SpeechRecognizer.ERROR_NO_MATCH, 0))
        assertTrue(shouldRetrySttError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, 0))
        assertTrue(shouldRetrySttError(SpeechRecognizer.ERROR_NETWORK, 0))
        assertTrue(shouldRetrySttError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT, 0))
    }

    @Test
    fun capStopsRetry() {
        assertTrue(
            shouldRetrySttError(
                SpeechRecognizer.ERROR_NO_MATCH,
                STT_INTERNAL_MAX_RETRIES - 1,
            )
        )
        assertFalse(
            shouldRetrySttError(
                SpeechRecognizer.ERROR_NO_MATCH,
                STT_INTERNAL_MAX_RETRIES,
            )
        )
        assertFalse(
            shouldRetrySttError(
                SpeechRecognizer.ERROR_NO_MATCH,
                STT_INTERNAL_MAX_RETRIES + 10,
            )
        )
    }

    @Test
    fun nonTransientErrorsNeverRetry() {
        assertFalse(shouldRetrySttError(SpeechRecognizer.ERROR_CLIENT, 0))
        assertFalse(shouldRetrySttError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, 0))
        assertFalse(shouldRetrySttError(SpeechRecognizer.ERROR_AUDIO, 0))
        assertFalse(shouldRetrySttError(SpeechRecognizer.ERROR_RECOGNIZER_BUSY, 0))
        assertFalse(shouldRetrySttError(SpeechRecognizer.ERROR_SERVER, 0))
        assertFalse(shouldRetrySttError(-999, 0))
    }
}
