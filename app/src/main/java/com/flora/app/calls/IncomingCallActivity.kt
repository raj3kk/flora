package com.flora.app.calls

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Maya-parity self-activation UI (Batch 4) — dialer-role CallActivity
 * se ALAG: ye announce ka UI hai, call control ka nahi.
 *
 * Ringing par Flora khud saamne aati hai (FSI notification se):
 * lock screen par bhi ye activity khulti hai (showWhenLocked +
 * turnScreenOn — manifest attrs + neeche code calls, CallActivity
 * pattern ka mirror). UI me sirf naam dikhta hai ("Boss, X
 * calling you") ya unknown variant — digits kabhi display nahi
 * hote (bolne me bhi nahi, Batch 1 rule).
 *
 * Buttons: [Wapas call karo] sirf ACTION_DIAL kholta hai (number
 * bhara hua, user khud call dabayega — auto-dial kabhi nahi);
 * [Dismiss] sirf ye UI band karta hai. Answer/reject buttons
 * isme NAHI hain — answering ke liye dialer role chahiye, ye
 * announce ka kaam nahi hai. Call khatam hote hi receiver
 * IncomingCallHolder.onCallEnded se ise band kar deta hai.
 * Number sirf dial ke liye extra me aata hai — kabhi display,
 * log ya server par nahi jaata.
 */
object IncomingCallHolder {
    var onCallEnded: (() -> Unit)? = null
    fun clear() {
        onCallEnded = null
    }
}

class IncomingCallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val name = intent.getStringExtra(EXTRA_CALLER_NAME)
        val number = intent.getStringExtra(EXTRA_CALLER_NUMBER).orEmpty()
        val message = if (!name.isNullOrBlank()) {
            "Boss, $name calling you"
        } else {
            "Boss, unknown number calling you"
        }
        IncomingCallHolder.onCallEnded = { runOnUiThread { finish() } }
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(message)
                    Spacer(Modifier.height(24.dp))
                    if (number.isNotBlank()) {
                        Button(onClick = {
                            try {
                                val dial = Intent(
                                    Intent.ACTION_DIAL, Uri.parse("tel:$number")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(dial)
                            } catch (e: Exception) {
                            }
                            finish()
                        }) {
                            Text("Wapas call karo")
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Button(onClick = { finish() }) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) IncomingCallHolder.clear()
        super.onDestroy()
    }
}
