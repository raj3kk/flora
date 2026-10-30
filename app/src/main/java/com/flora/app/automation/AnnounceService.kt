package com.flora.app.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.flora.app.MainActivity
import com.flora.app.data.FloraPreferences

/**
 * Call + notification announce — reliability + visibility (Batch 3).
 *
 * Ye service announce ka "visible" hissa hai: jab tak call ya
 * notification announce ON hai, ye foreground notification dikhti
 * rahegi ("Flora announce chalu hai") — user ko hamesha pata
 * rahega ki Flora call/notification sun rahi hai, aur ek tap
 * ("Band karo") par service turant ruk jayegi.
 *
 * - Khud kuch announce NAHI karti: announce ab bhi receivers /
 *   listener se hota hai (CallAnnouncementReceiver,
 *   FloraNotificationListener). Ye sirf visible presence + stop
 *   control hai.
 * - START_NOT_STICKY (charm mirror): system ne maar diya to khud
 *   wapas nahi aayegi — boot par (toggle ON ho to) BootReceiver
 *   wapas chalu karega, ya toggle dobara.
 * - "Band karo" = session stop: toggle ON rehne par bhi service
 *   ruk jayegi; poori tarah band karne ke liye Call announce +
 *   Notification announce dono OFF karo. Toggle badalne par
 *   syncFromPrefs phir se chalu/stop karega.
 * - Force-stop / Vivo kill par announce NAHI hoga — ye honest
 *   limit spec §0 me likhi hai, chhupayi nahi gayi.
 * - Koi naya permission nahi: FOREGROUND_SERVICE +
 *   FOREGROUND_SERVICE_SPECIAL_USE pehle se manifest me hain.
 */
class AnnounceService : Service() {

    companion object {
        const val NOTIF_ID = 2008
        private const val CHANNEL_ID = "flora_announce"

        fun start(context: Context) {
            try {
                context.startForegroundService(
                    Intent(context, AnnounceService::class.java)
                )
            } catch (e: Exception) {
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, AnnounceService::class.java))
            } catch (e: Exception) {
            }
        }

        /** Toggle badalne par: koi announce ON ho to start, dono OFF hon to stop. */
        suspend fun syncFromPrefs(context: Context) {
            try {
                val prefs = FloraPreferences(context.applicationContext)
                if (prefs.callAnnounce() || prefs.notifyAnnounce()) {
                    start(context)
                } else {
                    stop(context)
                }
            } catch (e: Exception) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Flora announce",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        } catch (e: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val stopIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, AnnounceStopReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flora announce chalu hai")
            .setContentText("Call/notification announce sun raha hai — band karne ke liye yahan dabao.")
            .setContentIntent(openIntent)
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setOngoing(true)
            .addAction(0, "Band karo", stopIntent)
            .build()
        try {
            startForeground(NOTIF_ID, notification)
        } catch (e: Exception) {
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}
