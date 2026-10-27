package com.flora.app.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.flora.app.data.FloraPreferences

/**
 * Consent gate for every automation step — the standing Flora rule:
 * default flow is ALWAYS manual (user turns things on himself, with a
 * visible stop). Nothing runs silently, nothing auto-posts, and no
 * Accessibility action runs without the user's explicit consent flag.
 */
class AutomationGate(private val prefs: FloraPreferences) {

    suspend fun isConsented(): Boolean = prefs.automationConsent()

    /**
     * Launches another app ONLY when consent is on and the launch is
     * user-initiated from the UI. Returns false (no action) otherwise.
     */
    fun launchApp(context: Context, packageName: String, consented: Boolean): Boolean {
        if (!consented) return false
        val pm: PackageManager = context.packageManager
        val intent: Intent = pm.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
