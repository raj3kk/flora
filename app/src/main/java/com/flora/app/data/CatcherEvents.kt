package com.flora.app.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Catcher event store (in-app Catcher, G2) — on-device diagnostic log.
 *
 * Pure File IO, koi Android import nahi, isliye unit test me seedha
 * chalta hai. File: filesDir/catcher_events.log, append-only lines:
 * `ISO-time | CATEGORY | detail`.
 *
 * Hooks isko [record] se bulate hain — ye kabhi throw nahi karta aur
 * init na hua ho to no-op hai, isliye hook wali jagah ka behaviour
 * kabhi nahi badalta. Detail likhte waqt sanitize hoti hai
 * (CatcherSanitize) + single-line + 500 chars cap. Rotation: file
 * 100KB se upar ho to aadhi purani lines hata do.
 *
 * Crash isme nahi aata — crash FloraApplication ke crash.log me hai,
 * Catcher use CrashLogReader se padhta hai.
 */
object CatcherEvents {
    private const val FILE_NAME = "catcher_events.log"
    private const val MAX_BYTES = 100 * 1024
    private const val MAX_DETAIL_CHARS = 500
    const val MAX_TAIL_EVENTS = 50

    @Volatile
    private var dir: File? = null

    fun init(filesDir: File) {
        try {
            dir = filesDir
        } catch (e: Exception) {
        }
    }

    /** Safe hook entry — never throws, no-op when not initialised. */
    fun record(category: String, detail: String) {
        val d = dir ?: return
        try {
            recordTo(d, category, detail)
        } catch (e: Exception) {
        }
    }

    fun recordTo(filesDir: File, category: String, detail: String) {
        val clean = CatcherSanitize.sanitize(detail)
            .replace('\n', ' ').replace('\r', ' ').trim()
            .take(MAX_DETAIL_CHARS)
        if (clean.isEmpty()) return
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val line = "$ts | $category | $clean\n"
        val log = File(filesDir, FILE_NAME)
        try {
            if (log.exists() && log.length() > MAX_BYTES) {
                val lines = log.readLines()
                val kept = if (lines.size > 1) lines.drop(lines.size / 2) else emptyList()
                log.writeText(if (kept.isEmpty()) line else kept.joinToString("\n") + "\n" + line)
            } else {
                log.appendText(line)
            }
        } catch (e: Exception) {
        }
    }

    fun readTail(filesDir: File, maxEvents: Int = MAX_TAIL_EVENTS): List<String> {
        return try {
            val log = File(filesDir, FILE_NAME)
            if (!log.exists()) emptyList() else log.readLines().takeLast(maxEvents)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Init dir wali tail — init na hua ho to empty (kabhi throw nahi). */
    fun readTail(maxEvents: Int = MAX_TAIL_EVENTS): List<String> {
        val d = dir ?: return emptyList()
        return readTail(d, maxEvents)
    }

    fun clear(filesDir: File) {
        try {
            File(filesDir, FILE_NAME).delete()
        } catch (e: Exception) {
        }
    }
}
