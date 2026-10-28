package com.flora.app.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.delay

data class LaunchableApp(val label: String, val packageName: String)

/**
 * Maya-style verified app-open (user order 2026-10-29 — AppLauncher
 * ka dispatch-Boolean hata diya gaya hai).
 *
 * PURANA JHUTH: launch() intent fire + no-exception par true deta
 * tha, aur Flora us par "khol diya" bol deti thi — jabki app
 * saamne aayi hi nahi hoti thi (Android 10+ background-start
 * restriction chupchaap drop kar sakta hai, koi exception nahi).
 *
 * NAYA NIYAM: "khol diya" SIRF tab bola jayega jab target package
 * sach me foreground me dikhe. Verification Accessibility
 * service ke zariye hoti hai — window-state events ka tracker +
 * live window tree (rootInActiveWindow ka package). UsageStats
 * par rukna nahi hai (uske liye alag permission + Settings step
 * chahiye hota; Accessibility Flora me pehle se automation ka
 * raasta hai).
 *
 * - Service OFF ho to app fire HI NAHI hoti: NoVerifier wapas
 *   aata hai, aur caller user ko batata hai ki Accessibility
 *   ON karni hai. Chupchaap "khol diya" kabhi nahi.
 * - Website ke liye alag niyam hai: browser intent fire ho sakta
 *   hai jab verify na ho sake (chooser case) — tab FiredUnverified
 *   milta hai, jis par caller sirf progress bolega ("khol raha
 *   hun"), kabhi "khol diya" nahi.
 */
object VerifiedAppLauncher {

    sealed class OpenResult {
        /** Target package verify ho kar foreground me aaya. */
        object Opened : OpenResult()
        /** Target pehle se saamne tha — fire karne ki zaroorat nahi padi. */
        object AlreadyOpen : OpenResult()
        /** Intent fire hua par target timeout tak foreground me nahi aaya. */
        object NotVisible : OpenResult()
        /** App-open: service OFF, isliye fire hi nahi kiya (verify impossible). */
        object NoVerifier : OpenResult()
        /** Package ka koi launch intent hi nahi mila. */
        object NoLaunchIntent : OpenResult()
        /** Website-only: intent fire hua par verify nahi ho saka (chooser). */
        object FiredUnverified : OpenResult()
    }

    fun listApps(context: Context): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
        return apps.mapNotNull { info ->
            val label = info.loadLabel(pm)?.toString() ?: return@mapNotNull null
            LaunchableApp(label, info.activityInfo.packageName)
        }.sortedBy { it.label.lowercase() }
    }

    fun findByName(context: Context, name: String): List<LaunchableApp> {
        val wanted = name.trim().lowercase()
        if (wanted.isEmpty()) return emptyList()
        return listApps(context).filter { it.label.lowercase().contains(wanted) }
    }

    /**
     * App kholo + verify karo ki wo sach me saamne aayi.
     * "khol diya" sirf Opened / AlreadyOpen par bola jaye.
     */
    suspend fun openVerified(
        context: Context,
        app: LaunchableApp,
        timeoutMs: Long = 3500L,
    ): OpenResult {
        val service = FloraAutomationService.instance ?: return OpenResult.NoVerifier
        if (service.foregroundPackage() == app.packageName) return OpenResult.AlreadyOpen
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            ?: return OpenResult.NoLaunchIntent
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            return OpenResult.NotVisible
        }
        return if (service.waitForForeground(app.packageName, timeoutMs)) {
            OpenResult.Opened
        } else {
            OpenResult.NotVisible
        }
    }

    /**
     * Website kholo + verify karo ki default browser saamne aaya.
     * Chooser case (koi default browser nahi) ya service OFF ho to
     * intent fire karke FiredUnverified deta hai — caller is par
     * sirf progress bolega, "khol diya" nahi.
     */
    suspend fun openWebsiteVerified(
        context: Context,
        site: String,
        timeoutMs: Long = 3500L,
    ): OpenResult {
        var url = site.trim()
        if (url.isEmpty()) return OpenResult.NoLaunchIntent
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if ("." in url) "https://$url"
            else "https://www.google.com/search?q=" + Uri.encode(url)
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val browserPkg = try {
            context.packageManager
                .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            return OpenResult.NotVisible
        }
        if (browserPkg == null) return OpenResult.FiredUnverified
        val service = FloraAutomationService.instance ?: return OpenResult.FiredUnverified
        return if (service.waitForForeground(browserPkg, timeoutMs)) {
            OpenResult.Opened
        } else {
            OpenResult.NotVisible
        }
    }
}
