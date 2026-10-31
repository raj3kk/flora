package com.flora.app.data

import java.io.File

/**
 * P3-01 — Catcher init-health indicator.
 *
 * Pehle init fail silent no-op tha: Catcher screen khaali dikhta
 * tha aur pata nahi chalta tha ki Catcher init hua bhi ya nahi.
 * Ab [FloraApplication] start par [recordInitProbe] chalata hai aur
 * natija [lastProbe] me rehta hai — Catcher screen isse dikhata
 * hai (khaali screen kabhi nahi). Probe sirf dir ki haalat batata
 * hai; koi data kahin nahi bhejta.
 */
object CatcherHealth {

    enum class InitState { OK, DIR_MISSING, NOT_WRITABLE, NOT_READABLE, PROBE_FAILED }

    data class InitProbe(val state: InitState, val detail: String)

    @Volatile
    var lastProbe: InitProbe? = null
        private set

    fun probe(filesDir: File): InitProbe {
        return try {
            if (!filesDir.exists() || !filesDir.isDirectory) {
                InitProbe(InitState.DIR_MISSING, "catcher dir missing")
            } else if (!filesDir.canRead()) {
                InitProbe(InitState.NOT_READABLE, "catcher dir not readable")
            } else if (!filesDir.canWrite()) {
                InitProbe(InitState.NOT_WRITABLE, "catcher dir not writable")
            } else {
                InitProbe(InitState.OK, "catcher initialised")
            }
        } catch (e: Exception) {
            InitProbe(InitState.PROBE_FAILED, "catcher probe failed")
        }
    }

    fun recordInitProbe(filesDir: File) {
        try {
            lastProbe = probe(filesDir)
        } catch (e: Exception) {
            lastProbe = InitProbe(InitState.PROBE_FAILED, "catcher probe failed")
        }
    }

    /** Catcher screen ke liye ek line — khaali screen ki jagah. */
    fun healthLine(): String {
        return when (val p = lastProbe) {
            null -> "Catcher: init probe not run yet"
            else -> if (p.state == InitState.OK) {
                "Catcher: OK — events on-device record ho rahe hain"
            } else {
                "Catcher: INIT PROBLEM — ${p.detail} (events record nahi ho rahe)"
            }
        }
    }
}
