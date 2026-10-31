package com.flora.app

import android.app.Application
import com.flora.app.data.CatcherEvents
import com.flora.app.data.CatcherHealth
import com.flora.app.models.NeuralVoiceAttach
import com.flora.app.planner.FloraToolsStartup

/**
 * Flora application class. No background work starts on its own.
 *
 * Crash-evidence (v2.22): koi bhi
 * uncaught crash hone par uska stacktrace phone par hi
 * crash.log me likha jata hai (koi server upload nahi),
 * phir pehle wala default handler hi chalta hai — app ka
 * crash behaviour bilkul nahi badalta, sirf saboot rehta
 * hai taaki failed cheez ko theek kiya ja sake.
 */
class FloraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // B5-09: tools registry init — App Startup initializer bhi
        // isi ensure ko call karta hai (double-plant guard andar
        // hai); crash handler uske baad lagta hai, pehle jaisa hi.
        FloraToolsStartup.ensureInitialized(this)
        // Lane C: neural voice attach — async, onCreate ko
        // kabhi block nahi karta (attach khud IO-scope me
        // chalta hai; fail ho to engine null hi rehta hai aur
        // system TTS barkaraar rehta hai).
        NeuralVoiceAttach.attach(this)
        // Catcher (G2): diagnostic event store ka dir — hooks iske baad
        // record kar sakte hain; init na ho to record no-op hai.
        CatcherEvents.init(filesDir)
        // P3-01 init-health: init fail silent no-op na rahe — probe ka
        // natija memory me rehta hai, Catcher screen use dikhata hai.
        CatcherHealth.recordInitProbe(filesDir)
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
