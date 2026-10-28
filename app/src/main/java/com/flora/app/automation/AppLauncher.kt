package com.flora.app.automation

import android.content.Context
import android.content.Intent

data class LaunchableApp(val label: String, val packageName: String)

/**
 * Any-app launcher: only apps the system reports as launchable
 * (MAIN/LAUNCHER) are listed — no QUERY_ALL_PACKAGES, no guessing.
 * Name matching is contains-based, case-insensitive; on multiple
 * matches the caller must confirm which one (never auto-pick a
 * look-alike when the name is ambiguous).
 */
object AppLauncher {

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

    /** Launches the app. Returns false when no launch intent exists. */
    fun launch(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Opens a website in the user's browser (Chrome if default). */
    fun openWebsite(context: Context, site: String): Boolean {
        var url = site.trim()
        if (url.isEmpty()) return false
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = if ("." in url) "https://$url"
            else "https://www.google.com/search?q=" +
                android.net.Uri.encode(url)
        }
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
