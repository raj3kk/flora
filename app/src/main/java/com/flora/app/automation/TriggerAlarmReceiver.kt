package com.flora.app.automation
import com.flora.app.data.CatcherHooks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.flora.app.data.TriggerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * TriggerAlarmReceiver (parity batch 2026-10-31, M5 — spec
 * FLORA_EVENT_TRIGGERS_SPEC.md). Fire par sirf ye do raaste:
 * - actionType "routine": routine ka naam due-set me (wahi set
 *   jo routine jobs use karte hain). App khulte hi maujooda
 *   due-rule chalega (Auto Mode ON ho to routine khud, warna
 *   sirf bataya jayega). Receiver khud koi command nahi chalata.
 * - actionType "message": sirf notification, koi action nahi.
 * Call/SMS/payment/delete yahan se kabhi fire nahi hote.
 * Agle din ka alarm dobara lagta hai (permission na ho to
 * notification chupchaap skip, crash nahi).
 */
class TriggerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val trigger = TriggerStore.load(context).firstOrNull { it.name == name }
                if (trigger != null) {
                    if (trigger.actionType == "routine" && trigger.actionValue.isNotBlank()) {
                        val prefs = context.getSharedPreferences("flora_job_due", Context.MODE_PRIVATE)
                        val due = prefs.getStringSet("due", emptySet())?.toMutableSet() ?: mutableSetOf()
                        due.add(trigger.actionValue)
                        prefs.edit().putStringSet("due", due).apply()
                        showNotification(
                            context, name,
                            "Routine '${trigger.actionValue}' ka waqt — app kholo."
                        )
                    } else {
                        showNotification(
                            context, name,
                            if (trigger.actionValue.isNotBlank()) trigger.actionValue
                            else "Event trigger ka waqt ho gaya hai."
                        )
                    }
                    if (trigger.enabled) TriggerScheduler.schedule(context, trigger)
                }
            } catch (e: Exception) {
                CatcherHooks.featureError("triggers", "onReceive", "exception")
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, name: String, text: String) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val channelId = "flora_triggers"
            val channel = NotificationChannel(
                channelId,
                "Flora event triggers",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Flora trigger: $name")
                .setContentText(text)
                .setAutoCancel(true)
                .build()
            nm.notify(("trigger:" + name).hashCode(), notif)
        } catch (e: Exception) {
            CatcherHooks.featureError("triggers", "showNotification", "exception")
        }
    }
}
