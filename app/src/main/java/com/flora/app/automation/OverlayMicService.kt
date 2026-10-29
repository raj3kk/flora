package com.flora.app.automation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.flora.app.MainActivity

/**
 * Overlay mic button (v2.22, Phase 1) — Flora kisi bhi app ke
 * upar ek chhota mic button dikhati hai. Button dabate hi Flora
 * aage aati hai aur sunna shuru karti hai (system speech
 * recognizer, MainActivity ke mic raaste se).
 *
 * - SYSTEM_ALERT_WINDOW permission Android ka niyam hai: user
 *   ek baar khud dega (voice branch settings khol deti hai).
 *   Bina permission ke button kabhi nahi dikhega, crash nahi.
 * - Foreground service + hamesha dikhne wali notification:
 *   chhupke kuch nahi — notification se "Band karo" bhi hai.
 * - Button sirf Flora kholta hai; koi audio ye service khud
 *   record nahi karti, koi screen capture nahi karti.
 */
class OverlayMicService : Service() {

    companion object {
        const val ACTION_STOP = "com.flora.app.action.OVERLAY_STOP"

        /**
         * N1/N8: island actions. Island isi service ke FGS +
         * notification (1002) par chalta hai — notch ke liye
         * doosra FGS kabhi nahi banta. SHOW par button hat
         * jata hai, HIDE par wapas aata hai (R-N-1: ek waqt
         * par ek hi visual entry).
         */
        const val ACTION_NOTCH_SHOW = "com.flora.app.action.NOTCH_SHOW"
        const val ACTION_NOTCH_HIDE = "com.flora.app.action.NOTCH_HIDE"
        const val EXTRA_NOTCH_STATE = "notch_state"
        private const val CHANNEL_ID = "flora_overlay"
        private const val NOTIF_ID = 1002
    }

    private var windowManager: WindowManager? = null
    private var buttonView: View? = null
    private var islandView: View? = null
    private var islandShown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "Flora overlay mic",
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
            Intent(this, OverlayMicService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Flora overlay mic chalu hai")
            .setContentText("Kisi bhi screen par mic button dikhega. Band karne ke liye yahan dabao.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(0, "Band karo", stopIntent)
            .build()
        try {
            startForeground(NOTIF_ID, notification)
        } catch (e: Exception) {
        }
        when (intent?.action) {
            ACTION_NOTCH_SHOW -> {
                val stateName = intent?.getStringExtra(EXTRA_NOTCH_STATE)
                val state = try {
                    com.flora.app.voice.IslandState.valueOf(stateName ?: "")
                } catch (e: Exception) {
                    com.flora.app.voice.IslandState.CONVERSATION_OPEN
                }
                showIsland(state)
            }
            ACTION_NOTCH_HIDE -> hideIsland()
            else -> {
                if (!islandShown) addButton()
            }
        }
        return START_STICKY
    }

    /**
     * N1: top-center pill host (Gravity.TOP | CENTER_HORIZONTAL,
     * FLAG_NOT_FOCUSABLE mandatory). Button hata kar island
     * lagao — dono ek saath kabhi nahi (R-N-1). Island view
     * khud NotchState se state/amplitude leta hai, isliye
     * dobara SHOW sirf state update hai, naya view nahi.
     */
    private fun showIsland(state: com.flora.app.voice.IslandState) {
        if (!android.provider.Settings.canDrawOverlays(this)) return
        if (islandView != null) {
            islandShown = true
            return
        }
        try {
            val wm = getSystemService(WindowManager::class.java) ?: return
            removeButton()
            val density = resources.displayMetrics.density
            val width = (resources.displayMetrics.widthPixels -
                (32 * density).toInt()).coerceAtLeast((200 * density).toInt())
            val height = (72 * density).toInt()
            val params = WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = (24 * density).toInt()
            }
            val island = NotchIslandView(this, state)
            wm.addView(island, params)
            windowManager = wm
            islandView = island
            islandShown = true
        } catch (e: Exception) {
        }
    }

    private fun hideIsland() {
        try {
            islandView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
        }
        islandView = null
        islandShown = false
        addButton()
    }

    private fun removeButton() {
        try {
            buttonView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
        }
        buttonView = null
    }

    private fun addButton() {
        if (buttonView != null) return
        if (!android.provider.Settings.canDrawOverlays(this)) return
        try {
            val wm = getSystemService(WindowManager::class.java) ?: return
            val size = (56 * resources.displayMetrics.density).toInt()
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
            val button = ImageView(this).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setBackgroundColor(0xFFE91E63.toInt())
                setPadding(24, 24, 24, 24)
                setOnClickListener {
                    try {
                        val launch = Intent(this@OverlayMicService, MainActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                            putExtra("flora_mic", true)
                        }
                        startActivity(launch)
                    } catch (e: Exception) {
                    }
                }
            }
            wm.addView(button, params)
            windowManager = wm
            buttonView = button
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        try {
            islandView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
        }
        islandView = null
        islandShown = false
        try {
            buttonView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
        }
        buttonView = null
        windowManager = null
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }
}
