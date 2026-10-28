package com.flora.app.automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.delay

/**
 * App automation (v2.16, user order): koi bhi app install karni ho to
 * Play Store se; image banani ho to ChatGPT app se. Koi silent action
 * nahi — har step par Flora bol kar batayega kya kar raha hai.
 * Paid app ka payment step hamesha user khud Play Store me karega;
 * Flora payment confirm nahi karta. Install detect karne ke liye
 * sirf launch-intent check hota hai (koi QUERY_ALL_PACKAGES nahi).
 */
object AppAutomation {

    const val CHATGPT_PKG = "com.openai.chatgpt"

    private val KNOWN_APPS = mapOf(
        "chatgpt" to CHATGPT_PKG,
        "chat gpt" to CHATGPT_PKG,
        "whatsapp" to "com.whatsapp",
        "whatsapp business" to "com.whatsapp.w4b",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "telegram" to "org.telegram.messenger",
        "spotify" to "com.spotify.music",
    )

    /** Spoken app name → package, ya null (tab Play Store search khulega). */
    fun resolvePackage(spokenName: String): String? {
        val wanted = spokenName.trim().lowercase()
        if (wanted.isEmpty()) return null
        KNOWN_APPS[wanted]?.let { return it }
        for ((key, pkg) in KNOWN_APPS) {
            if (wanted.contains(key)) return pkg
        }
        return null
    }

    fun isInstalled(context: Context, pkg: String): Boolean =
        try {
            context.packageManager.getLaunchIntentForPackage(pkg) != null
        } catch (e: Exception) {
            false
        }

    /**
     * Verified app-open (Maya jaisa): "khol diya" sirf tab jab
     * target sach me foreground me aaye. Service OFF ho to fire
     * hi nahi hota (NoVerifier) — caller imaandaar failure bolega.
     */
    suspend fun launch(
        context: Context,
        pkg: String,
    ): VerifiedAppLauncher.OpenResult =
        VerifiedAppLauncher.openVerified(context, LaunchableApp("", pkg))

    fun openPlayStoreDetails(context: Context, pkg: String): Boolean {
        val market = Intent(
            Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return try {
            context.startActivity(market)
            true
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun openPlayStoreSearch(context: Context, query: String): Boolean {
        val market = Intent(
            Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(query))
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return try {
            context.startActivity(market)
            true
        } catch (e: Exception) {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://play.google.com/store/search?q=" +
                                Uri.encode(query) + "&c=apps"
                        )
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    /** Polls until the package becomes launchable, or the timeout ends. */
    suspend fun waitForInstalled(
        context: Context, pkg: String, timeoutMs: Long
    ): Boolean {
        var waited = 0L
        while (waited < timeoutMs) {
            if (isInstalled(context, pkg)) return true
            delay(3000)
            waited += 3000
        }
        return isInstalled(context, pkg)
    }
}
