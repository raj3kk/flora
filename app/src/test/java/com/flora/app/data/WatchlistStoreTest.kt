package com.flora.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * GAP-M8 watchlist export ke unit tests (FIXER ISSUE-5).
 * exportText pure hai — ek symbol per line, blank filter.
 * Robolectric runner isliye ki WatchlistStore file me AndroidX
 * DataStore import hai (plain JVM par class-load risk na ho).
 * NOTE: repo CI sirf :app:assembleDebug chalata hai, isliye ye
 * tests CI par execute nahi hote — local
 * `./gradlew :app:testDebugUnitTest` se chalane ke liye hain.
 */
@RunWith(RobolectricTestRunner::class)
class WatchlistStoreTest {

    @Test
    fun exportTextJoinsOnePerLine() {
        assertEquals(
            "RELIANCE\nTCS\nINFY",
            WatchlistStore.exportText(listOf("RELIANCE", "TCS", "INFY"))
        )
    }

    @Test
    fun exportTextFiltersBlanks() {
        assertEquals(
            "RELIANCE\nTCS",
            WatchlistStore.exportText(listOf("RELIANCE", "", "   ", "TCS"))
        )
    }

    @Test
    fun exportTextEmptyListIsEmpty() {
        assertEquals("", WatchlistStore.exportText(emptyList()))
        assertEquals("", WatchlistStore.exportText(listOf("", "   ")))
    }
}
