package com.flora.app.tools

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.flora.app.data.AutoLogStore
import com.flora.app.viewing.LiveScreenViewing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * NoAccess Batch D — thermal throttle (user order 2026-10-30).
 * Spec: docs/FLORA_NOACCESS_FUNCTIONS_PLAN.md §7.
 *
 * Design (verify kiya gaya):
 * - Listener sirf throttle session ke andar register hota hai (engage
 *   par) aur disengage/stop par unregister hota hai — app start par
 *   koi background listener nahi (FloraApplication ka "no background
 *   work on its own" rule barkaraar).
 * - Throttle sirf Flora ka apna workload chhuta hai: live-viewing
 *   polling pause (session zinda, indicator rehta hai) + apni window
 *   ki brightness (FloraApp effect se; system setting kabhi nahi).
 *   Doosre apps kill nahi hote, system settings silent nahi badalti.
 * - Har action visible hai (chat + TTS + notification) + Auto Log me
 *   hai + "thermal throttle band karo" se stop hota hai.
 * - Auto-resume: status MODERATE ya neeche aate hi listener khud
 *   disengage karta hai (viewing resume, brightness restore).
 * - Notification id 2005 — 2004 PlannerEngine me used hai (pre-audit
 *   catch), isliye 2005 liya gaya hai.
 */
object ThermalThrottle {

    private const val CHANNEL_ID = "flora_thermal"
    private const val NOTIF_ID = 2005

    /** Compose state — FloraApp brightness effect isko padhta hai. */
    val throttledState: MutableState<Boolean> = mutableStateOf(false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var logStore: AutoLogStore? = null
    private var powerManager: PowerManager? = null
    private var listener: PowerManager.OnThermalStatusChangedListener? = null

    fun isThrottled(): Boolean = throttledState.value

    private fun log(context: Context, text: String) {
        try {
            if (logStore == null) logStore = AutoLogStore(context)
            val store = logStore ?: return
            scope.launch {
                try {
                    store.add(text)
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun showNotification(context: Context, statusWord: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Flora cooling",
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Flora cooling mode ON")
                .setContentText(
                    "Phone $statusWord hai — live viewing pause hai, brightness kam hai. " +
                        "Band karne ke liye 'thermal throttle band karo' bolo.",
                )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build()
            nm.notify(NOTIF_ID, notif)
        } catch (e: Exception) {
        }
    }

    private fun cancelNotification(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIF_ID)
        } catch (e: Exception) {
        }
    }

    /** Cooling mode ON. Idempotent — pehle se ON ho to dobara register nahi. */
    fun engage(context: Context, statusWord: String) {
        if (throttledState.value) return
        throttledState.value = true
        try {
            LiveScreenViewing.setThermalPaused(true)
        } catch (e: Exception) {
        }
        showNotification(context, statusWord)
        log(context, "Thermal: cooling mode ON ($statusWord) — viewing pause, brightness kam")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm != null) {
                    val l = PowerManager.OnThermalStatusChangedListener { status ->
                        if (status <= PowerManager.THERMAL_STATUS_MODERATE) {
                            disengage(context, "thanda ho gaya")
                        }
                    }
                    pm.addThermalStatusListener(context.mainExecutor, l)
                    powerManager = pm
                    listener = l
                }
            } catch (e: Exception) {
            }
        }
    }

    fun disengage(context: Context, reason: String) {
        if (!throttledState.value) return
        throttledState.value = false
        try {
            val l = listener
            if (l != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    powerManager?.removeThermalStatusListener(l)
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
        listener = null
        powerManager = null
        try {
            LiveScreenViewing.setThermalPaused(false)
        } catch (e: Exception) {
        }
        cancelNotification(context)
        log(context, "Thermal: cooling mode OFF ($reason) — viewing resume, brightness normal")
    }
}
