package com.flora.app.automation

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.sin

/**
 * Flora live wallpaper (v2.22, Phase 5, G9) — phone ki
 * home screen par halka sa dhadakta hua Flora orb.
 * Sirf drawing hai: koi data, mic, camera ya network nahi.
 * Lagana user ke haath me hai (system live-wallpaper
 * chooser se) — app chhupke wallpaper nahi badal sakti.
 */
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
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }
}
