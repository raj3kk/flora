package com.flora.app.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.flora.app.data.Job
import com.flora.app.data.JobStore
import java.util.Calendar

/**
 * JobScheduler (v2.22, chunk 4) — roz ke waqt ka alarm.
 * Exact alarm mile to exact, warna inexact fallback (koi
 * permission error nahi, koi crash nahi). WorkManager nahi
 * use hua (v2.19 me dead dep ke roop me hata diya gaya tha).
 */
object JobScheduler {

    const val ACTION_JOB = "com.flora.app.ACTION_JOB_DUE"

    private fun pendingIntent(context: Context, job: Job): PendingIntent {
        val intent = Intent(context, JobAlarmReceiver::class.java).apply {
            action = ACTION_JOB
            putExtra("routine", job.routineName)
        }
        return PendingIntent.getBroadcast(
            context,
            job.routineName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, job: Job) {
        if (!job.enabled) return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, job.hour)
            set(Calendar.MINUTE, job.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = pendingIntent(context, job)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (e: Exception) {
        }
    }

    fun cancel(context: Context, job: Job) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        try {
            am.cancel(pendingIntent(context, job))
        } catch (e: Exception) {
        }
    }

    suspend fun rescheduleAll(context: Context) {
        for (job in JobStore.load(context)) {
            if (job.enabled) schedule(context, job)
        }
    }
}
