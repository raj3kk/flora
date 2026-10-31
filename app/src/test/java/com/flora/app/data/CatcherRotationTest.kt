package com.flora.app.data

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P3-01 rotation fix: 100KB paar hone par aadha-drop NAHI —
 * poori file prev me archive hoti hai + marker line aati hai,
 * aur naya event kabhi nahi khota.
 */
class CatcherRotationTest {

    @Test
    fun overLimitArchivesInsteadOfHalfDrop() {
        val dir = Files.createTempDirectory("catcher-rotation-test").toFile()
        try {
            var i = 0
            while (File(dir, "catcher_events.log").let { !it.exists() || it.length() <= 100 * 1024 }) {
                CatcherEvents.recordTo(dir, "TEST", "filler event number $i padding padding padding")
                i++
                if (i > 10000) break
            }
            // Ek aur event rotation trigger karega.
            CatcherEvents.recordTo(dir, "TEST", "newest event after rotation")
            val tail = CatcherEvents.readTail(dir)
            assertTrue(tail.any { it.contains("ROTATION") })
            assertTrue(tail.any { it.contains("newest event after rotation") })
            assertTrue(CatcherEvents.prevLogExists(dir))
            val prev = File(dir, CatcherEvents.PREV_FILE_NAME).readText()
            assertTrue(prev.contains("filler event number 0"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun initHealthProbeReportsDirState() {
        val dir = Files.createTempDirectory("catcher-health-test").toFile()
        try {
            assertTrue(CatcherHealth.probe(dir).state == CatcherHealth.InitState.OK)
            CatcherHealth.recordInitProbe(dir)
            assertTrue(CatcherHealth.healthLine().contains("OK"))
        } finally {
            dir.deleteRecursively()
        }
        val missing = File(dir, "no-such-dir")
        assertTrue(CatcherHealth.probe(missing).state == CatcherHealth.InitState.DIR_MISSING)
    }
}
