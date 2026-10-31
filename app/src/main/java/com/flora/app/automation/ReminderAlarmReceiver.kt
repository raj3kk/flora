package com.flora.app.automation
import com.flora.app.data.CatcherHooks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.flora.app.data.ReminderStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ReminderAlarmReceiver (parity batch 2026-10-31, M2): waqt
 * aane par sirf notification dikhata hai (permission na ho to
 * chupchaap skip, crash nahi) aur agle din ka alarm dobara
 * lagata hai. Koi routine, koi action, koi SMS/call kabhi
 * nahi chalta — reminder sirf yaad dilata hai.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = ReminderStore.load(context).firstOrNull { it.title == title }
                showNotification(context, title, reminder?.message ?: "")
                if (reminder != null && reminder.enabled) {
                    ReminderScheduler.schedule(context, reminder)
                }
            } catch (e: Exception) {
                CatcherHooks.featureError("reminders", "onReceive", "exception")
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val channelId = "flora_reminders"
            val channel = NotificationChannel(
                channelId,
                "Flora reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
            val text = if (message.isNotBlank()) message else "Reminder ka waqt ho gaya hai — app kholo."
            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Flora reminder: $title")
                .setContentText(text)
                .setAutoCancel(true)
                .build()
            nm.notify(("reminder:" + title).hashCode(), notif)
        } catch (e: Exception) {
            CatcherHooks.featureError("reminders", "showNotification", "exception")
        }
    }
}
