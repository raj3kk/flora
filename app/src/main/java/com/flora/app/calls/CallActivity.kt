package com.flora.app.calls

import android.os.Bundle
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
 * Incoming call par Flora ke options (v2.22) — pehle koi call UI
 * hi nahi tha, isliye default dialer hone par bhi user ko koi
 * option nahi dikhta tha. Service lambdas deti hai (answer/route/
 * disconnect service ke paas hi ho sakte hain), Activity sirf
 * button dikhati hai aur dabane par lambda chalati hai.
 */
object CallHolder {
    var number: String = "unknown"
    var onAnswerFlora: (() -> Unit)? = null
    var onAnswerSelf: (() -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null
    var onRemoved: (() -> Unit)? = null
    fun clear() {
        onAnswerFlora = null
        onAnswerSelf = null
        onDisconnect = null
        onRemoved = null
    }
}

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallHolder.onRemoved = { runOnUiThread { finish() } }
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Call aa rahi hai")
                    Spacer(Modifier.height(8.dp))
                    Text(CallHolder.number)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { CallHolder.onAnswerFlora?.invoke() }) {
                        Text("Flora se baat karao")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { CallHolder.onAnswerSelf?.invoke() }) {
                        Text("Khud baat karo")
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        CallHolder.onDisconnect?.invoke()
                        finish()
                    }) {
                        Text("Kaato")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) CallHolder.onRemoved = null
        super.onDestroy()
    }
}
