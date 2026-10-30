package com.flora.app.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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
        const val ACTION_SHOW_CALL_OVERLAY = "com.flora.app.action.SHOW_CALL_OVERLAY"
        const val ACTION_HIDE_CALL_OVERLAY = "com.flora.app.action.HIDE_CALL_OVERLAY"
        const val EXTRA_OVERLAY_NAME = "extra_overlay_name"
        const val EXTRA_OVERLAY_NUMBER = "extra_overlay_number"

        /** Batch 4 overlay fallback: FSI na mile to call UI overlay se. */
        fun showCallOverlay(context: Context, name: String?, number: String) {
            try {
                val i = Intent(context, AnnounceService::class.java).apply {
                    action = ACTION_SHOW_CALL_OVERLAY
                    putExtra(EXTRA_OVERLAY_NAME, name)
                    putExtra(EXTRA_OVERLAY_NUMBER, number)
                }
                context.startForegroundService(i)
            } catch (e: Exception) {
            }
        }

        fun hideCallOverlay(context: Context) {
            try {
                val i = Intent(context, AnnounceService::class.java).apply {
                    action = ACTION_HIDE_CALL_OVERLAY
                }
                context.startService(i)
            } catch (e: Exception) {
            }
        }

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
        when (intent?.action) {
            ACTION_SHOW_CALL_OVERLAY -> showCallOverlayView(
                intent.getStringExtra(EXTRA_OVERLAY_NAME),
                intent.getStringExtra(EXTRA_OVERLAY_NUMBER).orEmpty()
            )
            ACTION_HIDE_CALL_OVERLAY -> hideCallOverlayView()
        }
        return START_NOT_STICKY
    }

    private var windowManager: WindowManager? = null
    private var callOverlayView: View? = null

    private fun showCallOverlayView(name: String?, number: String) {
        if (callOverlayView != null) return
        if (!android.provider.Settings.canDrawOverlays(this)) return
        try {
            val wm = getSystemService(WindowManager::class.java) ?: return
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
                setBackgroundColor(0xE6000000.toInt())
            }
            val message = if (!name.isNullOrBlank()) {
                "Boss, $name calling you"
            } else {
                "Boss, unknown number calling you"
            }
            val messageView = TextView(this).apply {
                setText(message)
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 20f
            }
            layout.addView(messageView)
            if (number.isNotBlank()) {
                val dialButton = Button(this).apply {
                    setText("Wapas call karo")
                    setOnClickListener {
                        try {
                            val dial = Intent(
                                Intent.ACTION_DIAL, Uri.parse("tel:$number")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(dial)
                        } catch (e: Exception) {
                        }
                        hideCallOverlayView()
                    }
                }
                layout.addView(dialButton)
            }
            val dismissButton = Button(this).apply {
                setText("Dismiss")
                setOnClickListener { hideCallOverlayView() }
            }
            layout.addView(dismissButton)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
            }
            wm.addView(layout, params)
            windowManager = wm
            callOverlayView = layout
        } catch (e: Exception) {
        }
    }

    private fun hideCallOverlayView() {
        try {
            callOverlayView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
        }
        callOverlayView = null
        windowManager = null
    }

    override fun onDestroy() {
        hideCallOverlayView()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}
