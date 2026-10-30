package com.flora.app.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.flora.app.data.Reminder
import com.flora.app.data.ReminderStore
import java.util.Calendar

/**
 * ReminderScheduler (parity batch 2026-10-31, M2) — roz ke waqt
 * ka alarm, JobScheduler ka wahi pattern (exact mile to exact,
 * warna inexact fallback; WorkManager nahi). Action alag hai
 * (ACTION_REMINDER), taaki reminder kabhi routine-run se na
 * takraye — fire par sirf notification hota hai, koi routine,
 * koi action, koi message/call kabhi nahi chalta.
 */
object ReminderScheduler {

    const val ACTION_REMINDER = "com.flora.app.ACTION_REMINDER_DUE"

    private fun pendingIntent(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra("title", reminder.title)
        }
        return PendingIntent.getBroadcast(
            context,
            ("reminder:" + reminder.title).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, reminder: Reminder) {
        if (!reminder.enabled) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = pendingIntent(context, reminder)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (e: Exception) {
        }
    }

    fun cancel(context: Context, reminder: Reminder) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            am.cancel(pendingIntent(context, reminder))
        } catch (e: Exception) {
        }
    }

    suspend fun rescheduleAll(context: Context) {
        for (reminder in ReminderStore.load(context)) {
            if (reminder.enabled) schedule(context, reminder)
        }
    }
}
