package com.flora.app.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fix-cycle B4: crash-log reader ke unit tests (pure JVM — koi Android call nahi).
 */
class CrashLogReaderTest {

    private fun tempDir(): File {
        return Files.createTempDirectory("crashlog-test").toFile()
    }

    @Test
    fun missingFileIsEmpty() {
        val dir = tempDir()
        try {
            assertEquals("", CrashLogReader.readCrashLog(dir))
            assertEquals("", CrashLogReader.readFullForShare(dir))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun shortContentReturnedAsIs() {
        val dir = tempDir()
        try {
            File(dir, "crash.log").writeText("NullPointerException at Foo:12")
            assertEquals(
                "NullPointerException at Foo:12",
                CrashLogReader.readCrashLog(dir),
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun longContentTruncatedToLastChars() {
        val dir = tempDir()
        try {
            val text = "x".repeat(5000) + "TAIL"
            File(dir, "crash.log").writeText(text)
            val out = CrashLogReader.readCrashLog(dir, maxChars = 4000)
            assertEquals(4000, out.length)
            assertTrue(out.endsWith("TAIL"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun shareReadsFullContent() {
        val dir = tempDir()
        try {
            val text = "y".repeat(9000)
            File(dir, "crash.log").writeText(text)
            assertEquals(9000, CrashLogReader.readFullForShare(dir).length)
        } finally {
            dir.deleteRecursively()
        }
    }
}
