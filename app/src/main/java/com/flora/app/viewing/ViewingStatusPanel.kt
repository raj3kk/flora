package com.flora.app.viewing

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Lane A item 37 — in-app viewing status panel (spec §6.1).
 *
 * Notification ke saath in-app status doosra zaroori indicator
 * hai. Ye chhota panel [LiveScreenViewing.snapshot] se status
 * dikhata hai aur Stop button deta hai. Ise FloraApp/Live
 * screen me drop-in karke dikhaya ja sakta hai — placement
 * coordinator ke faisle par hai (6723-line FloraApp file me
 * andha edit is commit me nahi kiya gaya hai).
 *
 * Panel khud koi polling shuru nahi karta — sirf status +
 * stop dikhata hai. Frame ka content yahan kabhi nahi aata.
 */
@Composable
fun ViewingStatusPanel() {
    val snap = LiveScreenViewing.snapshot()
    Column {
        if (snap.isViewing) {
            Text("Flora screen dekh rahi hai (" + snap.fps + " fps)")
            Text(
                "Frames: " + snap.processed + " processed, " +
                    snap.droppedStale + " stale drop"
            )
            Button(onClick = { LiveScreenViewing.stop("in-app-stop") }) {
                Text("Viewing roko")
            }
        } else {
            Text("Screen viewing band hai")
        }
    }
}
