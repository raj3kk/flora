package com.flora.app

import android.app.Application

/**
 * Flora application class. No background work starts on its own.
 *
 * Crash-evidence (v2.22, production-ready bar): koi bhi
 * uncaught crash hone par uska stacktrace phone par hi
 * crash.log me likha jata hai (koi server upload nahi),
 * phir pehle wala default handler hi chalta hai — app ka
 * crash behaviour bilkul nahi badalta, sirf saboot rehta
 * hai taaki failed cheez ko theek kiya ja sake.
 */
class FloraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val log = java.io.File(filesDir, "crash.log")
                val text = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss", java.util.Locale.US
                ).format(java.util.Date()) + " " +
                    (error.stackTraceToString().take(4000)) + "\n"
                if (log.exists() && log.length() > 200_000) {
                    log.writeText(text)
                } else {
                    log.appendText(text)
                }
            } catch (e: Exception) {
            }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                // Default handler na ho to process khud band karo —
                // warna app tooti haalat me zinda reh jayegi.
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}
