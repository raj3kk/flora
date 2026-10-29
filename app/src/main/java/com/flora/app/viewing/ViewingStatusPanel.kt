package com.flora.app.viewing

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Lane A item 37 — in-app viewing status panel (spec §6.1).
 *
 * Notification ke saath in-app status doosra zaroori indicator
 * hai. Ye chhota panel [LiveScreenViewing.snapshot] se status
 * dikhata hai aur Stop button deta hai. Follow-up placement:
 * FloraApp ke main Column me (har screen par dikhega) — Live
 * screen par NAHI, wo purani screen-capture wali screen hai.
 *
 * snapshot() volatile vars par hai, Compose state par nahi —
 * isliye panel apna snapshot har second khud refresh karta hai,
 * taaki Start/Stop ke baad status 1 second ke andar sahi dikhe
 * bina kisi aur recomposition trigger ke.
 *
 * Panel khud koi polling shuru nahi karta — Start button sirf
 * diya hua [onStart] callback chalata hai (asli start FloraApp
 * me hota hai, guard ke saath). Frame ka content yahan kabhi
 * nahi aata.
 */
@Composable
fun ViewingStatusPanel(onStart: (() -> Unit)? = null) {
    var snap by remember { mutableStateOf(LiveScreenViewing.snapshot()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            snap = LiveScreenViewing.snapshot()
        }
    }
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
            if (onStart != null) {
                Button(onClick = onStart) {
                    Text("Screen viewing shuru karo")
                }
            }
        }
    }
}
