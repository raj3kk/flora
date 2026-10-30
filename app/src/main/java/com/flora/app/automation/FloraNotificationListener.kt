package com.flora.app.automation

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.flora.app.assistant.NeuralSpeaker
import com.flora.app.data.AutoLogStore
import com.flora.app.data.FloraPreferences
import com.flora.app.data.NotifItem
import com.flora.app.data.NotificationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FloraNotificationListener (v2.22, P2-F10, READ ONLY store +
 * Batch 2 announce).
 * User khud Settings se is service ko notification access
 * dega (Flora kabhi khud ON nahi kar sakti). ON hone par
 * aane wali har notification ka sirf title + text is phone
 * par save hota hai — Flora kuch dismiss, reply ya action
 * nahi karti. Flora ki apni notifications ignore hoti hain.
 *
 * Batch 2 (announce): toggle ON + app important-apps set me ho +
 * OTP/sensitive na ho + DND rule pass ho, tabhi sirf app ka naam
 * + TITLE bola jayega ("Boss, {app} se notification: {title}") —
 * poora message kabhi nahi bola jayega. OTP/code wali notification
 * store bhi nahi hogi (Batch 2 privacy fix — pehle store ho jaati
 * thi). Auto Log me sirf app ka naam, title/text kabhi nahi.
 * Sensitive content bhi isi phone par hi rehta hai, server par
 * kabhi nahi jata; Notifications screen se ek tap par saaf ho
 * jata hai.
 */
class FloraNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val spokenRecently = mutableMapOf<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notif = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return
        if (sbn.isOngoing) return
        val extras = notif.extras
        val title = extras?.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras?.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        // OTP/sensitive hard-exclude: na store, na speak, na log body.
        if (AnnounceFilters.isSensitive(title, text)) return
        val item = NotifItem(
            pkg = pkg,
            title = title.take(200),
            text = text.take(200),
            timeMs = sbn.postTime,
        )
        scope.launch {
            NotificationStore.add(applicationContext, item)
            try {
                val prefs = FloraPreferences(applicationContext)
                if (!prefs.notifyAnnounce()) return@launch
                if (pkg !in prefs.notifyImportantApps()) return@launch
                val driving = try {
                    prefs.drivingMode()
                } catch (e: Exception) {
                    false
                }
                if (!driving && !prefs.notifyDndAnnounce() &&
                    AnnounceFilters.isDndOn(applicationContext)
                ) return@launch
                val key = pkg + "|" + title
                val now = System.currentTimeMillis()
                val dup = synchronized(spokenRecently) {
                    val last = spokenRecently[key]
                    if (last != null && now - last < 60_000L) {
                        true
                    } else {
                        spokenRecently[key] = now
                        false
                    }
                }
                if (dup) return@launch
                val label = try {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, 0)
                    ).toString()
                } catch (e: Exception) {
                    pkg
                }
                NeuralSpeaker.speakFixed(
                    applicationContext,
                    "Boss, $label se notification: $title",
                )
                try {
                    AutoLogStore(applicationContext).add(
                        "Notification announce — $label"
                    )
                } catch (e: Exception) {
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Read-only: hatne par kuch nahi karna.
    }
}
