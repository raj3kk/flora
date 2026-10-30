package com.flora.app.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.flora.app.MainActivity

/**
 * P1 appearance bundle — Floating Charm (SPEC-FREEZE).
 *
 * Ek chhota tairta hua orb jo har screen ke upar dikhta hai.
 * OverlayMicService ke pattern ka mirror hai (wahi permission
 * gate, wahi overlay type, wahi foreground-notification
 * pattern) — mic button se ALAG cheez hai: ye mic nahi hai,
 * ye sirf Flora chat kholta hai. Live-wallpaper (item 2) se
 * bhi alag hai: ye wallpaper engine nahi, overlay window hai.
 *
 * - ON sirf user ke toggle par hota hai (Appearance screen,
 *   default OFF). Overlay permission user khud Settings se
 *   dega — Flora kabhi khud ON nahi kar sakti.
 * - Tap → Flora chat khulti hai (koi action execute nahi
 *   hota — sirf navigation). Drag → charm kahin bhi le jao.
 * - Visible stop: toggle OFF / notification se "Band karo" /
 *   permission revoke → charm hat jata hai. Service
 *   START_NOT_STICKY hai — system ne maar diya to khud
 *   wapas nahi aayega, toggle dobara karna hoga.
 * - Charm ka rang default Flora rang hai (avatar theming
 *   charm par is batch me nahi hai — future kaam).
 */
class FloraCharmService : Service() {

    companion object {
        const val ACTION_STOP = "com.flora.app.action.CHARM_STOP"
        private const val CHANNEL_ID = "flora_charm"
        private const val NOTIF_ID = 1003
    }

    private var windowManager: WindowManager? = null
    private var charmView: View? = null

    private class CharmOrbView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE6FFFFFF.toInt()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(width, height) / 2f
            if (r <= 0f) return
            paint.shader = RadialGradient(
                cx - r * 0.35f, cy - r * 0.4f, r * 1.2f,
                intArrayOf(0xFFFFC978.toInt(), 0xFF7C4A03.toInt()),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, paint)
            canvas.drawCircle(cx - r * 0.35f, cy - r * 0.4f, r * 0.14f, specPaint)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "Flora floating charm",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        } catch (e: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, FloraCharmService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flora charm chalu hai")
            .setContentText("Tairta orb dikhega — tap par Flora chat khulegi. Band karne ke liye yahan dabao.")
            .setSmallIcon(android.R.drawable.btn_star_big_on)
            .setOngoing(true)
            .addAction(0, "Band karo", stopIntent)
            .build()
        try {
            startForeground(NOTIF_ID, notification)
        } catch (e: Exception) {
        }
        showCharm()
        return START_NOT_STICKY
    }

    private fun showCharm() {
        if (charmView != null) return
        if (!android.provider.Settings.canDrawOverlays(this)) return
        try {
            val wm = getSystemService(WindowManager::class.java) ?: return
            val density = resources.displayMetrics.density
            val size = (56 * density).toInt()
            val params = WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 16
                y = 300
            }
            val orb = CharmOrbView(this)
            var downX = 0f
            var downY = 0f
            var downTime = 0L
            var moved = false
            orb.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        downTime = System.currentTimeMillis()
                        moved = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - downX).toInt()
                        val dy = (event.rawY - downY).toInt()
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                            moved = true
                            params.x -= dx
                            params.y += dy
                            downX = event.rawX
                            downY = event.rawY
                            try {
                                wm.updateViewLayout(orb, params)
                            } catch (e: Exception) {
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved && System.currentTimeMillis() - downTime < 400) {
                            try {
                                val launch = Intent(this, MainActivity::class.java).apply {
                                    addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    )
                                }
                                startActivity(launch)
                            } catch (e: Exception) {
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            wm.addView(orb, params)
            windowManager = wm
            charmView = orb
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            charmView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
        }
        charmView = null
        windowManager = null
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}
