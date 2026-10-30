package com.flora.app.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.flora.app.data.EventTrigger
import com.flora.app.data.TriggerStore
import java.util.Calendar

/**
 * TriggerScheduler (parity batch 2026-10-31, M5) — roz ke waqt
 * ka alarm, JobScheduler ka wahi pattern (exact mile to exact,
 * warna inexact fallback; WorkManager nahi). Action alag hai,
 * taaki trigger kabhi routine-job alarm se na takraye.
 */
object TriggerScheduler {

    const val ACTION_TRIGGER = "com.flora.app.ACTION_TRIGGER_DUE"

    private fun pendingIntent(context: Context, trigger: EventTrigger): PendingIntent {
        val intent = Intent(context, TriggerAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER
            putExtra("name", trigger.name)
        }
        return PendingIntent.getBroadcast(
            context,
            ("trigger:" + trigger.name).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, trigger: EventTrigger) {
        if (!trigger.enabled) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, trigger.hour)
            set(Calendar.MINUTE, trigger.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = pendingIntent(context, trigger)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (e: Exception) {
        }
    }

    fun cancel(context: Context, trigger: EventTrigger) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            am.cancel(pendingIntent(context, trigger))
        } catch (e: Exception) {
        }
    }

    suspend fun rescheduleAll(context: Context) {
        for (trigger in TriggerStore.load(context)) {
            if (trigger.enabled) schedule(context, trigger)
        }
    }
}
