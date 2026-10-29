package com.flora.app.viewing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Lane A item 37 — notification Stop action ka receiver.
 * Notification par Stop dabane se viewing turant rukti hai
 * (spec §6.3: user stop hamesha, turant). Sirf viewing rokta
 * hai — isme koi aur kaam nahi hai.
 */
class ViewingStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        LiveScreenViewing.stop("notification-stop")
    }
}
