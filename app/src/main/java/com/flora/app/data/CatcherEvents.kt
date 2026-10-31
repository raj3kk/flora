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
 * (CatcherSanitize) + single-line + 500 chars cap.
 *
 * Rotation (P3-01 fix — purana aadha-drop hataya gaya): file
 * 100KB se upar ho to poori file ek generation ke liye
 * catcher_events.prev.log me archive hoti hai, phir nayi file ek
 * ROTATION marker line se shuru hoti hai. Koi line chupchaap
 * aadhi-drop nahi hoti — purana evidence prev file me milta hai.
 * Auto-upload kabhi nahi; copy/share/save sirf user-tap par.
 *
 * Crash isme nahi aata — crash FloraApplication ke crash.log me hai,
 * Catcher use CrashLogReader se padhta hai.
 */
object CatcherEvents {
    private const val FILE_NAME = "catcher_events.log"
    const val PREV_FILE_NAME = "catcher_events.prev.log"
    private const val MAX_BYTES = 100 * 1024
    private const val MAX_DETAIL_CHARS = 500
    const val MAX_TAIL_EVENTS = 50

    /** True jab [init] chala ho — Catcher screen isse init-health dikhata hai. */
    fun isInitialised(): Boolean = dir != null

    fun prevLogExists(filesDir: File): Boolean {
        return try {
            File(filesDir, PREV_FILE_NAME).exists()
        } catch (e: Exception) {
            false
        }
    }

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
                // P3-01: archive poori file, marker ke saath nayi shuruaat.
                // Archive fail ho to bhi naya event kabhi nahi khota — append karo.
                try {
                    log.copyTo(File(filesDir, PREV_FILE_NAME), overwrite = true)
                    log.writeText("$ts | ROTATION | previous generation archived to $PREV_FILE_NAME\n" + line)
                } catch (e: Exception) {
                    log.appendText(line)
                }
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

    /**
     * REG-F9 residual (Part 1): per-class counts — log ki har
     * category ki ginti. Pure File IO; line format
     * `time | CATEGORY | detail` hai, kharab line skip hoti
     * hai. Kabhi throw nahi karta (fail par empty map).
     */
    fun countsByCategory(filesDir: File): Map<String, Int> {
        return try {
            val log = File(filesDir, FILE_NAME)
            if (!log.exists()) return emptyMap()
            val counts = mutableMapOf<String, Int>()
            for (line in log.readLines()) {
                val parts = line.split(" | ")
                if (parts.size < 3) continue
                val category = parts[1].trim()
                if (category.isEmpty()) continue
                counts[category] = (counts[category] ?: 0) + 1
            }
            counts
        } catch (e: Exception) {
            emptyMap()
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
