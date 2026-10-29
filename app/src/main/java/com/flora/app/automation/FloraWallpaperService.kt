package com.flora.app.automation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.WallpaperManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import kotlin.math.sin

/**
 * Flora live wallpaper (v2.22, Phase 5, G9) — phone ki
 * home screen par halka sa dhadakta hua Flora orb.
 * Sirf drawing hai: koi data, mic, camera ya network nahi.
 * Lagana user ke haath me hai (system live-wallpaper
 * chooser se) — app chhupke wallpaper nahi badal sakti.
 */
/**
 * Live-wallpaper chooser — EK raasta (Phase 9, A2):
 * voice branch ("live wallpaper lagao") aur Settings row
 * dono yahi intent kholte hain, taaki drift na ho. User
 * khud chooser me Flora wallpaper chunta hai — app
 * chhupke wallpaper kabhi nahi badalti. Fail par false
 * (caller user ko batayega + yahan Log bhi hai).
 */
object WallpaperChooser {
    fun open(context: Context): Boolean {
        return try {
            val intent = Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
            ).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(
                        context.applicationContext,
                        FloraWallpaperService::class.java
                    )
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.applicationContext.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w("FloraWallpaper", "live wallpaper chooser nahi khula", e)
            false
        }
    }
}

class FloraWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = FloraEngine()

    private inner class FloraEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var running = false
        private var t = 0.0

        private val drawLoop = object : Runnable {
            override fun run() {
                if (!running) return
                drawFrame()
                t += 0.05
                handler.postDelayed(this, 50)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            running = visible
            if (visible) handler.post(drawLoop) else handler.removeCallbacks(drawLoop)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            running = false
            handler.removeCallbacks(drawLoop)
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas == null) return
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                canvas.drawColor(Color.rgb(20, 10, 25))
                val cx = w / 2f
                val cy = h / 2f
                val base = minOf(w, h) * 0.18f
                // Baahar ka halka glow + andar ka orb (dhadkan).
                paint.color = Color.argb(60, 233, 30, 99)
                canvas.drawCircle(cx, cy, base * (1.25f + 0.1f * sin(t).toFloat()), paint)
                paint.color = Color.rgb(233, 30, 99)
                canvas.drawCircle(cx, cy, base * (1.0f + 0.08f * sin(t).toFloat()), paint)
                paint.color = Color.WHITE
                canvas.drawCircle(cx - base * 0.3f, cy - base * 0.3f, base * 0.18f, paint)
            } catch (e: Exception) {
                // Phase 9, A2: pehle ye catch khaali tha (chupchaap
                // frame chhoot jata tha) — ab Log par flag hai.
                Log.w("FloraWallpaper", "frame draw nahi hua", e)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        // Phase 9, A2: pehle ye catch khaali tha —
                        // ab Log par flag hai.
                        Log.w("FloraWallpaper", "canvas unlock nahi hua", e)
                    }
                }
            }
        }
    }
}
