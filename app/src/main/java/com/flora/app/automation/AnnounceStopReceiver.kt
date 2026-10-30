package com.flora.app.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Batch 3 — notification par "Band karo" ka receiver.
 * Sirf AnnounceService rokta hai — toggles nahi badalta, isliye
 * ye session stop hai: toggle badalne ya phone restart (toggle ON
 * ho to) par service wapas chalu ho jayegi. Poori tarah band
 * karne ke liye Call announce + Notification announce dono OFF
 * karo. Stop hamesha kaam karega, chahe toggle koi bhi ho.
 */
class AnnounceStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return
        try {
            context.stopService(Intent(context, AnnounceService::class.java))
        } catch (e: Exception) {
        }
    }
}
