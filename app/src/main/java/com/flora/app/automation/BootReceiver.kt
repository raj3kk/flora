package com.flora.app.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flora.app.data.FloraPreferences
import com.flora.app.voice.BackgroundMicService
import com.flora.app.wakeword.WakeWordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Boot receiver (v2.12): phone restart hone ke baad Wake Word apne
 * aap wapas chalu ho — SIRF tab jab user ne pehle Wake Word ON
 * kiya hua tha. Koi permission, koi setting, koi data is receiver
 * se nahi chheda jata; OFF tha to kuch nahi hota.
 *
 * v2.15: continuous mic ON tha to BackgroundMicService bhi wapas
 * chalu karne ki koshish hoti hai (standby mode me — app khulte
 * hi activity khud sunegi). Boot se FGS start kuch phones par
 * block ho sakta hai, isliye try/catch hai; fail ho to agle
 * app-open par mic ON karte hi service wapas aa jayegi.
 *
 * v2.22 (chunk 4): routine jobs ke alarms bhi dobara lagte
 * hain — warna reboot ke baad roz ke waqt wale routine
 * chupchaap band ho jate.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = FloraPreferences(context.applicationContext)
                if (prefs.wakeEnabled()) {
                    context.startForegroundService(
                        Intent(context, WakeWordService::class.java)
                    )
                }
                if (prefs.continuousMic()) {
                    try {
                        context.startForegroundService(
                            Intent(context, BackgroundMicService::class.java)
                        )
                    } catch (e: Exception) {
                    }
                }
                // Batch 3: call ya notification announce ON ho to
                // AnnounceService wapas chalu — boot par FGS start
                // kuch phones par block ho sakta hai, isliye try/catch
                // (fail ho to toggle dobara service wapas layega).
                if (prefs.callAnnounce() || prefs.notifyAnnounce()) {
                    try {
                        context.startForegroundService(
                            Intent(context, AnnounceService::class.java)
                        )
                    } catch (e: Exception) {
                    }
                }
                try {
                    JobScheduler.rescheduleAll(context.applicationContext)
                } catch (e: Exception) {
                }
                // Parity batch 2026-10-31, M2: reminders ke alarms
                // bhi dobara lagte hain (routine jobs jaisa hi).
                try {
                    ReminderScheduler.rescheduleAll(context.applicationContext)
                } catch (e: Exception) {
                }
                // Parity batch 2026-10-31, M5: event triggers ke
                // alarms bhi dobara lagte hain.
                try {
                    TriggerScheduler.rescheduleAll(context.applicationContext)
                } catch (e: Exception) {
                }
                // B4/F16: reboot ke baad plan resume hook — sirf
                // WorkManager reschedule (mic/wake restart upar wahi
                // hai). Plan reschedule yahan nahi, worker me hai.
                try {
                    com.flora.app.planner.PlanResumeScheduler.enqueue(context.applicationContext)
                } catch (e: Exception) {
                }
            } catch (e: Exception) {
                // Boot par fail: user ke agle app-open par Wake Word
                // Settings se wapas ON ho jayega, kaam rukta nahi.
            } finally {
                pending.finish()
            }
        }
    }
}
