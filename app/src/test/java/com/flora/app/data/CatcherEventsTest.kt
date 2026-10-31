package com.flora.app.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Catcher events store ke unit tests (pure JVM — koi Android call nahi).
 */
class CatcherEventsTest {

    private fun tempDir(): File {
        return Files.createTempDirectory("catcher-events-test").toFile()
    }

    @Test
    fun missingFileReadsEmpty() {
        val dir = tempDir()
        try {
            assertEquals(emptyList<String>(), CatcherEvents.readTail(dir))
            CatcherEvents.clear(dir)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun recordAndReadBack() {
        val dir = tempDir()
        try {
            CatcherEvents.recordTo(dir, "API", "brain lanes fail codes=[429, 503]")
            val tail = CatcherEvents.readTail(dir)
            assertEquals(1, tail.size)
            assertTrue(tail[0].contains("API"))
            assertTrue(tail[0].contains("brain lanes fail codes=[429, 503]"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun detailSanitizedOnRecord() {
        val dir = tempDir()
        try {
            CatcherEvents.recordTo(dir, "API", "UTR 123456789012 token=supersecretvalue")
            val tail = CatcherEvents.readTail(dir)
            assertEquals(1, tail.size)
            assertTrue(tail[0].contains("********9012"))
            assertTrue(!tail[0].contains("123456789012"))
            assertTrue(tail[0].contains("token=[masked]"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun newlineCollapsedToSingleLine() {
        val dir = tempDir()
        try {
            CatcherEvents.recordTo(dir, "PLANNER", "line one\nline two")
            val lines = File(dir, "catcher_events.log").readLines()
            assertEquals(1, lines.size)
            assertTrue(lines[0].contains("line one line two"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun rotationCapsFileSize() {
        val dir = tempDir()
        try {
            val big = "x".repeat(500)
            repeat(300) { i ->
                CatcherEvents.recordTo(dir, "API", "event $i $big")
            }
            val log = File(dir, "catcher_events.log")
            assertTrue(log.length() <= 100 * 1024 + 1024)
            val tail = CatcherEvents.readTail(dir)
            assertEquals(50, tail.size)
            assertTrue(tail.last().contains("event 299"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun clearDeletesFile() {
        val dir = tempDir()
        try {
            CatcherEvents.recordTo(dir, "API", "something")
            CatcherEvents.clear(dir)
            assertTrue(!File(dir, "catcher_events.log").exists())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun countsPureFunctionSkipsMalformedLines() {
        // FIXER ISSUE-5/6: pure count(lines) — koi IO nahi.
        val counts = CatcherEvents.count(
            listOf(
                "2026-10-31 10:00:00 | API | first",
                "garbage line with no separators",
                "2026-10-31 10:00:01 |  | empty category",
                "2026-10-31 10:00:02 | API",
                "2026-10-31 10:00:03 | STT | third",
            )
        )
        assertEquals(1, counts["API"])
        assertEquals(1, counts["STT"])
        assertEquals(2, counts.size)
    }

    @Test
    fun countsByCategorySkipsMalformedLines() {
        // FIXER ISSUE-5: file-based wrapper bhi kharab line skip kare.
        val dir = tempDir()
        try {
            File(dir, "catcher_events.log").writeText(
                "garbage line with no separators\n" +
                    "2026-10-31 10:00:00 |  | empty category\n" +
                    "2026-10-31 10:00:01 | API | ok\n" +
                    "2026-10-31 10:00:02 | API\n"
            )
            val counts = CatcherEvents.countsByCategory(dir)
            assertEquals(1, counts["API"])
            assertEquals(1, counts.size)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun countsByCategoryIncludesPrevGeneration() {
        // FIXER ISSUE-5 (ISSUE-4 ko cover karta hai): rotation ke
        // baad prev file ki counts bhi judi honi chahiye; prev na
        // ho to sirf current gini jaye.
        val dir = tempDir()
        try {
            CatcherEvents.recordTo(dir, "API", "current one")
            assertEquals(1, CatcherEvents.countsByCategory(dir)["API"])
            File(dir, CatcherEvents.PREV_FILE_NAME).writeText(
                "2026-10-31 09:00:00 | API | old one\n" +
                    "2026-10-31 09:00:01 | API | old two\n" +
                    "2026-10-31 09:00:02 | STT | old three\n"
            )
            val counts = CatcherEvents.countsByCategory(dir)
            assertEquals(3, counts["API"])
            assertEquals(1, counts["STT"])
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun countsByCategoryGroupsPerClass() {
        val dir = tempDir()
        try {
            CatcherEvents.recordTo(dir, "API", "first")
            CatcherEvents.recordTo(dir, "API", "second")
            CatcherEvents.recordTo(dir, "STT", "third")
            val counts = CatcherEvents.countsByCategory(dir)
            assertEquals(2, counts["API"])
            assertEquals(1, counts["STT"])
            val empty = tempDir()
            try {
                assertEquals(emptyMap<String, Int>(), CatcherEvents.countsByCategory(empty))
            } finally {
                empty.deleteRecursively()
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
