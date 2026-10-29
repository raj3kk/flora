package com.flora.app.viewing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Display
import androidx.core.app.NotificationCompat
import com.flora.app.automation.FloraAutomationService
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lane A item 37 — Live Screen Viewing pipeline (spec v2).
 *
 * Pipeline (spec §2.1): maujooda AccessibilityService ka screenshot
 * API (API 30+) se polling loop — request -> callback bitmap ->
 * process -> agli request. Service file ko chheda NAHI gaya hai:
 * ye controller service ke public instance se screenshot API
 * call karta hai, aur viewing ka apna alag guard + alag state
 * rakhta hai (shared-service guard separation, spec §7).
 *
 * Binding numbers (spec §3.3): default 2 fps, max 5 fps
 * (har 200-500ms ek frame); capture ~200-400ms, poora loop
 * ~300-600ms. "Live" = lagatar periodic screenshots, KOI
 * VIDEO STREAM NAHI, koi 1ms ka vaada nahi.
 *
 * Threading (spec §9, declaration order: service -> polling
 * handler -> sampler -> hooks):
 * - Polling handler: dedicated HandlerThread par screenshot
 *   callbacks aate hain (main thread par frame processing kabhi nahi).
 * - Perception worker: apna CoroutineScope (SupervisorJob +
 *   Dispatchers.IO). lifecycleScope ABANDONED hai — wo raasta
 *   yahan dobara nahi aaya hai.
 * - Main thread: sirf UI kaam (notification) — frame kabhi nahi.
 *
 * Overlap (spec §0): polling loop sampler ke pichhle result ka
 * intezaar NAHI karta — har tick interval par naya request
 * bhejta hai (ek request in-flight ho to tick skip hoti hai,
 * queue kabhi nahi banti), aur callback aane par frame worker
 * scope me process hota hai bina loop ko roke.
 *
 * Guards (spec §8, isi kram me — fail-closed):
 * user request -> planner confirm -> service ON check ->
 * API 30+ guard -> indicator (notification) -> polling ->
 * sampler/hooks -> stop. Koi step skip nahi, koi bypass nahi.
 * Service OFF/kill ho to polling rukti hai + dobara auto-start
 * kabhi nahi — dobara dekhne ke liye naya user request chahiye.
 *
 * Transparency + safety (spec §6, kabhi nahi hatenge):
 * - Indicator: persistent notification zaroori hai — notification
 *   na dikh sake to polling shuru hi nahi hoti.
 * - Auto Log: [onLog] par sirf events jaate hain (start/stop/
 *   hook ka naam + outcome + counts) — frame ka content, padha
 *   hua text, ya perception result kabhi log me nahi jaata.
 * - Storage: frames disk par kabhi nahi likhe jaate; har bitmap
 *   process ke baad release hota hai, memory me ek se zyada
 *   frame hold nahi hota.
 * - On-device only: koi cloud call nahi, paise me free.
 * - Secure window ka exact behavior phone protocol se verify
 *   hoga (UNVERIFIED) — koi bypass kabhi nahi.
 * - Password field se koi extraction nahi (hooks content ko
 *   chhoote hi nahi — ye junctions no-op hain).
 *
 * Viewing-only module: is file me koi action code nahi hai —
 * sirf dekhna (capture + sampler + hooks). Planner se jod
 * (spec §12 note: "screen dekho" step ka exact contract is
 * spec me nahi hai) abhi khula point hai — viewing standalone
 * start/stop + status + log ke saath bani hai.
 */

data class ViewingSessionSnapshot(
    val isViewing: Boolean,
    val purpose: String,
    val fps: Int,
    val intervalMs: Long,
    val requested: Long,
    val callbacks: Long,
    val processed: Long,
    val droppedStale: Long,
    val skippedTicks: Long,
    val errors: Long,
    val startedAtMs: Long,
)

sealed class ViewingStartResult {
    data class Started(val snapshot: ViewingSessionSnapshot) : ViewingStartResult()
    data class Refused(val failure: ViewingGuardFailure) : ViewingStartResult()
}

object LiveScreenViewing {

    private const val CHANNEL_ID = "flora_viewing"
    private const val NOTIF_ID = 2007
    private const val SCREENSHOT_TIMEOUT_MS = 8000L

    @Volatile
    var isViewing: Boolean = false
        private set

    @Volatile
    private var purpose: String = ""

    @Volatile
    private var fps: Int = ViewingRate.DEFAULT_FPS

    @Volatile
    private var intervalMs: Long = ViewingRate.intervalMs(ViewingRate.DEFAULT_FPS)

    @Volatile
    private var startedAtMs: Long = 0L

    @Volatile
    private var requested: Long = 0L

    @Volatile
    private var callbacks: Long = 0L

    @Volatile
    private var processed: Long = 0L

    @Volatile
    private var skippedTicks: Long = 0L

    @Volatile
    private var errors: Long = 0L

    @Volatile
    private var frameSeq: Long = 0L

    @Volatile
    private var requestInFlight: Boolean = false

    @Volatile
    private var requestStartedMs: Long = 0L

    private var appContext: Context? = null
    private var onLog: (String) -> Unit = {}

    // Apna scope — lifecycleScope nahi (ABANDONED raasta).
    private val viewingScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var pollJob: Job? = null

    // Polling handler — dedicated thread, main thread nahi.
    private var pollThread: HandlerThread? = null
    private var pollHandler: Handler? = null

    private val screenshotExecutor: Executor = Executor { cmd ->
        pollHandler?.post(cmd)
    }

    // Sampler — polling handler ke baad, hooks se pehle declare.
    private val sampler: ViewingSampler = ViewingSampler()
    private val pollGate: ViewingPollGate = ViewingPollGate()

    // Hooks — sampler ke baad declare (junctions only, engine nahi).
    private val hooks: List<ViewingPerceptionHook> = ViewingHookSet.defaults

    fun snapshot(): ViewingSessionSnapshot = ViewingSessionSnapshot(
        isViewing = isViewing,
        purpose = purpose,
        fps = fps,
        intervalMs = intervalMs,
        requested = requested,
        callbacks = callbacks,
        processed = processed,
        droppedStale = sampler.droppedStale,
        skippedTicks = skippedTicks,
        errors = errors,
        startedAtMs = startedAtMs,
    )

    /**
     * Guard order §8: user -> planner -> service ON -> API 30+ ->
     * indicator. Sab pass hon to polling shuru, warna Refused.
     */
    fun start(
        context: Context,
        sessionPurpose: String,
        requestedFps: Int,
        userConfirmed: Boolean,
        plannerConfirmed: Boolean,
        logCallback: (String) -> Unit = {},
    ): ViewingStartResult {
        if (isViewing) {
            return ViewingStartResult.Refused(ViewingGuardFailure.ALREADY_RUNNING)
        }
        val serviceOn = FloraAutomationService.instance != null
        val indicatorReady = hasNotificationPermission(context)
        val failure = ViewingGuards.evaluate(
            userConfirmed = userConfirmed,
            plannerConfirmed = plannerConfirmed,
            serviceOn = serviceOn,
            apiLevel = Build.VERSION.SDK_INT,
            indicatorReady = indicatorReady,
        )
        if (failure != null) {
            return ViewingStartResult.Refused(failure)
        }
        val appCtx = context.applicationContext
        if (!showNotification(appCtx)) {
            return ViewingStartResult.Refused(
                ViewingGuardFailure.INDICATOR_UNAVAILABLE
            )
        }
        appContext = appCtx
        onLog = logCallback
        purpose = sessionPurpose
        fps = ViewingRate.clamp(requestedFps)
        intervalMs = ViewingRate.intervalMs(fps)
        requested = 0L
        callbacks = 0L
        processed = 0L
        skippedTicks = 0L
        errors = 0L
        frameSeq = 0L
        requestInFlight = false
        pollGate.reset()
        val thread = HandlerThread("flora-viewing-poll")
        thread.start()
        pollThread = thread
        pollHandler = Handler(thread.looper)
        startedAtMs = System.currentTimeMillis()
        isViewing = true
        onLog("viewing-start purpose=" + sessionPurpose + " fps=" + fps)
        pollJob = viewingScope.launch {
            while (isViewing) {
                pollTick()
                delay(intervalMs)
            }
        }
        return ViewingStartResult.Started(snapshot())
    }

    /**
     * Stop — turant: polling rukti hai, indicator hat-ta hai,
     * Auto Log me stop event jaata hai. Dobara polling sirf
     * naye user request (naye start call) par hogi — auto-start
     * kabhi nahi.
     */
    fun stop(reason: String) {
        if (!isViewing) return
        isViewing = false
        pollJob?.cancel()
        pollJob = null
        pollThread?.quitSafely()
        pollThread = null
        pollHandler = null
        requestInFlight = false
        appContext?.let { cancelNotification(it) }
        onLog("viewing-stop reason=" + reason)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return context.checkSelfPermission(
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showNotification(context: Context): Boolean {
        return try {
            val nm =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Flora screen viewing",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
            val stopIntent = Intent(context, ViewingStopReceiver::class.java)
            val stopPending = PendingIntent.getBroadcast(
                context, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Flora screen dekh rahi hai")
                .setContentText("Rokne ke liye Stop dabao ya 'ruk jao' bolo.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop", stopPending
                )
                .build()
            nm.notify(NOTIF_ID, notif)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun cancelNotification(context: Context) {
        try {
            val nm =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)
        } catch (e: Exception) {
        }
    }

    /**
     * Ek polling tick: service gayab ho to fail-closed stop;
     * request in-flight ho to tick skip (queue kabhi nahi);
     * timeout ho to in-flight saaf karke agli tick par retry.
     */
    private fun pollTick() {
        val service = FloraAutomationService.instance
        if (service == null) {
            stop("service-off")
            return
        }
        val now = System.currentTimeMillis()
        if (requestInFlight) {
            if (now - requestStartedMs > SCREENSHOT_TIMEOUT_MS) {
                requestInFlight = false
                errors++
                onLog("viewing-error screenshot-timeout")
            } else {
                skippedTicks++
                return
            }
        }
        if (!pollGate.shouldRequest(now, intervalMs)) {
            skippedTicks++
            return
        }
        pollGate.recordRequest(now)
        requestInFlight = true
        requestStartedMs = now
        requested++
        try {
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                screenshotExecutor,
                object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(
                        result: android.accessibilityservice.AccessibilityService.ScreenshotResult
                    ) {
                        requestInFlight = false
                        callbacks++
                        handleScreenshot(result)
                    }

                    override fun onFailure(errorCode: Int) {
                        requestInFlight = false
                        errors++
                        onLog("viewing-error screenshot-failed code=" + errorCode)
                    }
                }
            )
        } catch (e: Exception) {
            requestInFlight = false
            errors++
            onLog("viewing-error screenshot-exception")
        }
    }

    /**
     * Callback bitmap: hardware buffer se software copy banao,
     * hardware buffer turant band karo, sampler se latest-frame
     * check karo, phir worker scope me hooks chalao (loop ko
     * roke bina — overlap). Har bitmap process ke baad release
     * hoti hai; memory me ek se zyada frame hold nahi hota.
     * Frame ka content kabhi log/store nahi hota.
     */
    private fun handleScreenshot(
        result: android.accessibilityservice.AccessibilityService.ScreenshotResult
    ) {
        val hw = result.hardwareBuffer ?: run {
            errors++
            return
        }
        val raw = Bitmap.wrapHardwareBuffer(hw, result.colorSpace)
        if (raw == null) {
            try {
                hw.close()
            } catch (e: Exception) {
            }
            errors++
            return
        }
        val soft = raw.copy(Bitmap.Config.ARGB_8888, false)
        try {
            hw.close()
        } catch (e: Exception) {
        }
        if (soft == null) {
            errors++
            return
        }
        val seq = ++frameSeq
        if (!sampler.accept(seq)) {
            soft.recycle()
            return
        }
        val frame = ViewingFrame(
            seq = seq,
            timestampMs = System.currentTimeMillis(),
            width = soft.width,
            height = soft.height,
            bitmap = soft,
        )
        viewingScope.launch {
            try {
                if (!isViewing) return@launch
                if (!sampler.isLatest(seq)) return@launch
                for (hook in hooks) {
                    try {
                        val outcome = hook.onFrame(frame)
                        onLog(
                            "viewing-hook hook=" + hook.hookName +
                                " outcome=" + outcome.name
                        )
                    } catch (e: Exception) {
                        onLog(
                            "viewing-hook hook=" + hook.hookName +
                                " outcome=FAILED"
                        )
                    }
                }
                processed++
            } finally {
                if (!soft.isRecycled) {
                    soft.recycle()
                }
            }
        }
    }
}
