package com.flora.app.automation

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings

/**
 * Announce filters (call + notification announce, Batch 2).
 * Teen faisle ek jagah — listener aur UI dono isi ko chhuenge:
 * (1) OTP/sensitive hard-exclude, (2) DND state, (3) listener grant.
 * Koi filter kabhi notification body ko log/server/backup me nahi
 * bhejta — ye functions sirf boolean jawab dete hain.
 */
object AnnounceFilters {

    private val sensitiveKeywords = listOf(
        "otp",
        "one-time",
        "one time",
        "verification code",
        "verification",
        "password reset",
        "reset code",
        "login code",
        "security code",
    )
    private val codePattern = Regex("\\b\\d{4,8}\\b")

    /**
     * OTP / verification code / sensitive notification? Match hote hi
     * item DROP hoga — na store, na speak, na Auto Log body. Fail-closed:
     * shak ho to drop karo (ek chhooti notification ka nuksaan, ek OTP
     * leak se hazaar guna chhota hai).
     */
    fun isSensitive(title: String, text: String): Boolean {
        val combined = "$title $text"
        val lower = combined.lowercase()
        if (sensitiveKeywords.any { lower.contains(it) }) return true
        if (lower.contains("code") && codePattern.containsMatchIn(combined)) return true
        return false
    }

    /** DND ON hai? (interruption filter ALL nahi = DND ya priority-only.) */
    fun isDndOn(context: Context): Boolean {
        return try {
            val nm = context.getSystemService(NotificationManager::class.java)
                ?: return false
            nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        } catch (e: Exception) {
            false
        }
    }

    /** NotificationListener grant mila hua hai? (system settings flag se.) */
    fun isListenerEnabled(context: Context): Boolean {
        return try {
            val flat = Settings.Secure.getString(
                context.contentResolver, "enabled_notification_listeners"
            ) ?: return false
            flat.contains(context.packageName)
        } catch (e: Exception) {
            false
        }
    }
}
