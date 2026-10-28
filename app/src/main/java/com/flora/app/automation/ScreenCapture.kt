package com.flora.app.automation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream

/**
 * Live screen frame capture (v1.2, single frame on demand).
 * User system dialog se permission deta hai (MediaProjection) —
 * uske bina ek pixel bhi nahi milta. Frame milte hi projection
 * band kar di jaati hai; continuous recording/streaming nahi hoti.
 * Frame sirf us sawaal ke liye use hota hai jo user ne abhi poocha
 * hai, aur phone par JPEG banne ke baad hi vision ko bheja jaata
 * hai (user ki Gemini key se, opt-in).
 */
object ScreenCapture {

    /** Persistent session (v2.20): ek khuli projection se
     *  baar-baar frame — one-shot wala purana captureFrame
     *  hata diya gaya hai (wohi "frame nahi mila" bug tha). */
    class Session(private val activity: Activity, resultCode: Int, data: Intent) {
        private val projection: android.media.projection.MediaProjection? =
            (activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager).getMediaProjection(resultCode, data)

        fun isOpen(): Boolean = projection != null

        suspend fun captureFrame(): ByteArray? = withContext(Dispatchers.IO) {
            val proj = projection ?: return@withContext null
            var display: VirtualDisplay? = null
            var handlerThread: HandlerThread? = null
            try {
                val metrics = activity.resources.displayMetrics
                val w = metrics.widthPixels
                val h = metrics.heightPixels
                val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
                display = proj.createVirtualDisplay(
                    "FloraLive", w, h, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface, null, null
                )
                val done = CompletableDeferred<ByteArray?>()
                handlerThread = HandlerThread("FloraCapture").apply { start() }
                reader.setOnImageAvailableListener({ r ->
                    try {
                        val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                        val plane = image.planes[0]
                        val buffer = plane.buffer
                        val rowPadding = plane.rowStride - plane.pixelStride * w
                        val bitmap = Bitmap.createBitmap(
                            w + rowPadding / plane.pixelStride, h, Bitmap.Config.ARGB_8888
                        )
                        bitmap.copyPixelsFromBuffer(buffer)
                        image.close()
                        val cropped = Bitmap.createBitmap(bitmap, 0, 0, w, h)
                        val out = ByteArrayOutputStream()
                        cropped.compress(Bitmap.CompressFormat.JPEG, 80, out)
                        done.complete(out.toByteArray())
                    } catch (e: Exception) {
                        done.complete(null)
                    }
                }, Handler(handlerThread.looper))
                withTimeoutOrNull(10_000) { done.await() }
            } catch (e: Exception) {
                null
            } finally {
                try { display?.release() } catch (e: Exception) { }
                handlerThread?.quitSafely()
            }
        }

        fun stop() {
            try { projection?.stop() } catch (e: Exception) { }
        }
    }

    /** Continuous session kholo; null = permission/projection nahi mili. */
    fun startSession(activity: Activity, resultCode: Int, data: Intent): Session? {
        return try {
            val s = Session(activity, resultCode, data)
            if (s.isOpen()) s else null
        } catch (e: Exception) {
            null
        }
    }
}
