package com.flora.app.data

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Catcher report builder ke unit tests (pure JVM).
 */
class CatcherReportTest {

    private fun base(
        note: String = "",
        crash: String = "",
        events: List<String> = emptyList(),
        autoLog: List<String> = emptyList(),
    ): String {
        return CatcherReport.buildReport(
            appLine = "Flora 2.22 (44)",
            androidLine = "14 (API 34), Device: Test Model",
            timeLine = "2026-10-30 14:00:00",
            licenceLines = listOf("Licence: free / active / valid=true"),
            autoMode = true,
            fcmStatus = "Push: ON",
            note = note,
            crashText = crash,
            events = events,
            autoLogLines = autoLog,
            systemLines = listOf("Mic permission: haan"),
        )
    }

    @Test
    fun allSectionsPresent() {
        val out = base(
            note = "youtube kholte hi band hui",
            crash = "NullPointerException at Foo",
            events = listOf("2026-10-30 13:00:00 | API | brain lanes fail codes=[429]"),
            autoLog = listOf("13:00 — Auto mode ON"),
        )
        assertTrue(out.contains("FLORA PROBLEM REPORT"))
        assertTrue(out.contains("Flora 2.22 (44)"))
        assertTrue(out.contains("youtube kholte hi band hui"))
        assertTrue(out.contains("NullPointerException at Foo"))
        assertTrue(out.contains("brain lanes fail codes=[429]"))
        assertTrue(out.contains("Auto mode ON"))
        assertTrue(out.contains("Mic permission: haan"))
    }

    @Test
    fun emptySectionsHonest() {
        val out = base()
        assertTrue(out.contains("Koi crash record nahi hai."))
        assertTrue(out.contains("Koi error event nahi hai."))
        assertTrue(out.contains("Koi auto kaam nahi hua hai."))
        assertTrue(out.contains("User note: —"))
    }

    @Test
    fun reportSanitized() {
        val out = base(note = "UTR 123456789012 daala tha")
        assertTrue(out.contains("********9012"))
        assertTrue(!out.contains("123456789012"))
    }
}
