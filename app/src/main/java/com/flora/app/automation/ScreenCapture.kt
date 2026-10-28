package com.flora.app.automation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream

/**
 * Live screen frame capture (v2.22).
 * User system dialog se permission deta hai (MediaProjection) —
 * uske bina ek pixel bhi nahi milta. Frame sirf user ke sawaal
 * ke liye vision ko bheja jaata hai (user ki Gemini key se,
 * opt-in). Continuous chhup kar screen dekhna kabhi nahi hota;
 * MediaProjection ka system indicator capture ke waqt dikhta hai.
 *
 * v2.22 ROOT FIX — "session nahi mil raha / baar baar permission"
 * ki jad: pehle session composable ke `remember` me tha aur
 * screen chhodte hi DisposableEffect use STOP kar deta tha — aur
 * ek baar stop hui projection ka purana token dobara kaam nahi
 * karta, isliye har wapas aane par naya permission dialog khulta
 * tha. Ab session app-level holder me zinda rehta hai (screen
 * badalne par bhi), band sirf user ke "session band karo" par ya
 * projection ke sach me marne par hota hai. Doosri baat: pehle
 * isOpen() sirf reference check karta tha — mari hui projection
 * bhi non-null dikhti thi. Ab MediaProjection.Callback se asli
 * maut ka pata chalta hai.
 * IMAANDARI NOTE: app ka process mar jaye (battery optimization
 * Vivo par aggressive hai) to Android dobara permission maangega
 * hi — ye OS ka niyam hai, koi app isse bach nahi sakti. Isliye
 * battery-unrestricted + auto-start on rakhna zaroori hai.
 */
object ScreenCapture {

    /** App-level holder: screen badalne par session zinda rahe. */
    object LiveSessionHolder {
        var session: Session? = null
        fun clear() {
            try { session?.stop() } catch (e: Exception) { }
            session = null
        }
    }

    class Session(activity: Activity, resultCode: Int, data: Intent) {
        private val w: Int
        private val h: Int
        private val dpi: Int
        @Volatile private var dead = false
        private val projection: MediaProjection? =
            (activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as MediaProjectionManager).getMediaProjection(resultCode, data)

        init {
            val metrics = activity.resources.displayMetrics
            w = metrics.widthPixels
            h = metrics.heightPixels
            dpi = metrics.densityDpi
            try {
                projection?.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() { dead = true }
                }, null)
            } catch (e: Exception) { }
        }

        /** Sach me zinda hai ya nahi (reference nahi, callback se). */
        fun isOpen(): Boolean = projection != null && !dead

        suspend fun captureFrame(): ByteArray? = withContext(Dispatchers.IO) {
            val proj = projection ?: return@withContext null
            if (dead) return@withContext null
            var display: VirtualDisplay? = null
            var handlerThread: HandlerThread? = null
            try {
                val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
                display = proj.createVirtualDisplay(
                    "FloraLive", w, h, dpi,
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
            dead = true
            try { projection?.stop() } catch (e: Exception) { }
        }
    }

    /** Session kholo; null = permission/projection nahi mili. */
    fun startSession(activity: Activity, resultCode: Int, data: Intent): Session? {
        return try {
            val s = Session(activity, resultCode, data)
            if (s.isOpen()) s else null
        } catch (e: Exception) {
            null
        }
    }
}
