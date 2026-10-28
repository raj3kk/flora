package com.flora.app.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flora.app.data.FloraPreferences
import com.flora.app.wakeword.WakeWordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Boot receiver (v2.12): phone restart hone ke baad Wake Word apne
 * aap wapas chalu ho — SIRF tab jab user ne pehle Wake Word ON
 * kiya hua tha. Koi permission, koi setting, koi data is receiver
 * se nahi chheda jata; OFF tha to kuch nahi hota.
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
            } catch (e: Exception) {
                // Boot par fail: user ke agle app-open par Wake Word
                // Settings se wapas ON ho jayega, kaam rukta nahi.
            } finally {
                pending.finish()
            }
        }
    }
}
