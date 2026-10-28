package com.flora.app.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Auto mode indicator (v2.17): Auto mode ON hai to ek ongoing
 * notification hamesha dikhegi — taaki Auto mode kabhi chhupke na
 * chale. OFF hote hi notification hat jayegi.
 */
object AutoModeNotifier {

    private const val CHANNEL_ID = "flora_auto"
    private const val NOTIF_ID = 2003

    fun show(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Flora Auto mode",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Flora Auto mode ON")
                .setContentText("Call/SMS/app bina har baar poochhe chalenge. Band karne ke liye 'auto mode band' bolo.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build()
            nm.notify(NOTIF_ID, notif)
        } catch (e: Exception) {
        }
    }

    fun cancel(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)
        } catch (e: Exception) {
        }
    }
}
