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
}
