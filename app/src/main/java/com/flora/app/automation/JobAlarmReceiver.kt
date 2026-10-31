package com.flora.app.automation
import com.flora.app.data.CatcherHooks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.flora.app.data.JobStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * JobAlarmReceiver (v2.22, chunk 4): waqt aane par
 * - due-flag SharedPreferences me likhta hai (app khulte hi
 *   Flora routine chalati hai — Auto Mode ON ho to khud,
 *   warna ek message se batati hai),
 * - notification dikhata hai (permission na ho to chupchaap
 *   skip, crash nahi),
 * - agle din ka alarm dobara lagata hai.
 */
class JobAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val routine = intent.getStringExtra("routine") ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("flora_job_due", Context.MODE_PRIVATE)
                val due = prefs.getStringSet("due", emptySet())?.toMutableSet() ?: mutableSetOf()
                due.add(routine)
                prefs.edit().putStringSet("due", due).apply()
                showNotification(context, routine)
                val job = JobStore.load(context).firstOrNull { it.routineName == routine }
                if (job != null && job.enabled) {
                    JobScheduler.schedule(context, job)
                }
            } catch (e: Exception) {
                CatcherHooks.featureError("routines", "onReceive", "exception")
            } finally {
                pending.finish()
            }
        }
    }

    private fun showNotification(context: Context, routine: String) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val channelId = "flora_jobs"
            val channel = NotificationChannel(
                channelId,
                "Flora routines",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Flora: routine ka waqt")
                .setContentText("'$routine' ka waqt ho gaya hai — app kholo.")
                .setAutoCancel(true)
                .build()
            nm.notify(routine.hashCode(), notif)
        } catch (e: Exception) {
            CatcherHooks.featureError("routines", "showNotification", "exception")
        }
    }
}
