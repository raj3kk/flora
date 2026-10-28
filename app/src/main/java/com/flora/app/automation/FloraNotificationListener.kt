package com.flora.app.automation

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.flora.app.data.NotifItem
import com.flora.app.data.NotificationStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * FloraNotificationListener (v2.22, P2-F10, READ ONLY).
 * User khud Settings se is service ko notification access
 * dega (Flora kabhi khud ON nahi kar sakti). ON hone par
 * aane wali har notification ka sirf title + text is phone
 * par save hota hai — Flora kuch dismiss, reply ya action
 * nahi karti. Flora ki apni notifications ignore hoti hain.
 * Sensitive content (OTP, private message) bhi isi phone par
 * hi rehta hai, server par kabhi nahi jata; Notifications
 * screen se ek tap par saaf ho jata hai.
 */
class FloraNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notif = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return
        if (pkg == packageName) return
        if (sbn.isOngoing) return
        val extras = notif.extras
        val title = extras?.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras?.getCharSequence("android.text")?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val item = NotifItem(
            pkg = pkg,
            title = title.take(200),
            text = text.take(200),
            timeMs = sbn.postTime,
        )
        scope.launch {
            NotificationStore.add(applicationContext, item)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Read-only: hatne par kuch nahi karna.
    }
}
