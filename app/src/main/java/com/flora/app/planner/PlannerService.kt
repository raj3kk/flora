package com.flora.app.planner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flora.app.data.AutoLogStore

/**
 * B4 execution runtime (F03/F16).
 *
 * PlannerService: live foreground service — engine ka scope
 * process ke saath zinda rehta hai jab tak service foreground
 * me hai. Service khud engine nahi banata — PlannerEngineFactory
 * ka shared instance hi chalta hai (UI + service ek engine).
 *
 * PlanResumeWorker (M13/M15/G5): WorkManager live executor
 * NAHI hai — sirf durable resume guarantee. Boot/app-reopen
 * par ye worker RUNNING plans ko PAUSED par lata hai (beech
 * me atka step last VERIFIED step se resume hoga, duplicate
 * side-effect nahi — M1 marker se). Resume khud user ke
 * reopen + "Resume" par hota hai (background se FGS force
 * nahi — Android 12+ background FGS start block hota hai,
 * isliye imaandaar raasta: paused notice + reopen par resume).
 *
 * BootReceiver me sirf PlanResumeScheduler.enqueue() hook
 * joda gaya hai — mic/wake restart wahi rehta hai (F16).
 */
class PlannerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                PlanNotifier.cancel(applicationContext)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                PlanNotifier.showProgress(applicationContext, "active", "Plan service chal rahi hai", 0, 0)
                startForeground(2004, buildForegroundNotification())
                return START_STICKY
            }
        }
    }

    private fun buildForegroundNotification(): android.app.Notification {
        val channelId = "flora_plan"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Flora Plan", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("Flora Plan chal raha hai")
            .setContentText("Plan steps background me chal rahe hain — Auto Log me har step dikhega.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.flora.app.planner.STOP"

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, PlannerService::class.java))
            } catch (e: Exception) {
            }
        }

        fun stop(context: Context) {
            try {
                context.startService(Intent(context, PlannerService::class.java).apply { action = ACTION_STOP })
            } catch (e: Exception) {
            }
        }
    }
}

class PlanResumeWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val repository = PlanRepository(applicationContext)
            val autoLog = AutoLogStore(applicationContext)
            val interrupted = repository.interruptedPlans()
            for (plan in interrupted) {
                if (plan.status == PlanStatus.RUNNING) {
                    repository.updatePlanMeta(
                        plan.planId, status = PlanStatus.PAUSED,
                        lastError = "app/boot ke baad ruka — last verified step se resume hoga",
                    )
                    try {
                        autoLog.add("[plan:${plan.planId} step:-1] reboot/app-close ke baad PAUSED (resume: last verified step se)")
                    } catch (e: Exception) {
                    }
                }
                PlanNotifier.showProgress(
                    applicationContext, plan.planId,
                    "Plan ruka hua hai — kholo to last verified step se resume hoga", 0, 0,
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object PlanResumeScheduler {
    fun enqueue(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<PlanResumeWorker>().build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork("flora-plan-resume", androidx.work.ExistingWorkPolicy.REPLACE, request)
        } catch (e: Exception) {
        }
    }
}
