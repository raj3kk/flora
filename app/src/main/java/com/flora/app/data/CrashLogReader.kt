package com.flora.app.data

import java.io.File

/**
 * Crash-log reader (fix-cycle B4) — pure file read, koi Android import nahi
 * (isliye unit test me seedha chalta hai).
 *
 * Writer: FloraApplication ka crash handler (filesDir/crash.log, aakhri
 * 4000 chars per crash, 200KB par rotate). Reader kabhi crash nahi
 * karega — file na ho ya padhne me error ho to "" milega.
 *
 * Display ke liye aakhri [maxChars] chars (nayi crash aakhir me
 * append hoti hai, isliye takeLast); share ke liye poora text.
 */
object CrashLogReader {
    fun readCrashLog(filesDir: File, maxChars: Int = 4000): String {
        return try {
            val log = File(filesDir, "crash.log")
            if (!log.exists()) return ""
            val text = log.readText()
            if (text.length <= maxChars) text else text.takeLast(maxChars)
        } catch (e: Exception) {
            ""
        }
    }

    fun readFullForShare(filesDir: File): String {
        return try {
            val log = File(filesDir, "crash.log")
            if (!log.exists()) "" else log.readText().takeLast(200 * 1024)
        } catch (e: Exception) {
            ""
        }
    }
}
