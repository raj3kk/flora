package com.flora.app.planner

import android.content.Context
import androidx.startup.Initializer
import timber.log.Timber

/**
 * B5-09 Timber + App Startup.
 *
 * - Timber = planner-step logging ka EK standard raasta
 *   (PlannerEngine.log Timber.tag("Planner") se bhi log karta
 *   hai — har tool apna alag logging nahi karega).
 * - App Startup = tools registry init ka single point: ye
 *   Initializer app start par Timber plant karta hai (manifest
 *   meta-data se auto-discover hota hai, koi manual provider
 *   nahi). FloraApplication.onCreate bhi isi ensure ko call
 *   karta hai (double-plant guard ke saath) — dono raaste ek
 *   hi function par aate hain, do alag init nahi.
 *
 * Timber log me kabhi free-text detail (plan input, file
 * content, key) nahi jata — sirf planId / step / status jaisa
 * pattern-level text (PlannerEngine.log dekho).
 */
object FloraToolsStartup {

    @Volatile
    private var planted = false

    fun ensureInitialized(context: Context) {
        if (planted) return
        synchronized(this) {
            if (planted) return
            if (Timber.forest().isEmpty()) {
                Timber.plant(Timber.DebugTree())
            }
            Timber.tag("FloraTools").d("tools startup ready")
            planted = true
        }
    }
}

/** App Startup initializer — dependencies koi nahi (pehle chalne wala init). */
class FloraToolsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        FloraToolsStartup.ensureInitialized(context)
        return Unit
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
