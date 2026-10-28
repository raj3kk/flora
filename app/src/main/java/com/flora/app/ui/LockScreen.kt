package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flora.app.security.PinVault

/**
 * PIN lock screen (v2.22, Phase 5, G8) — PIN se
 * chhootne wala lock, kabhi na chhootne wala nahi.
 * - PIN sahi ho to onUnlock() (chat par wapas).
 * - Back dabao to bhi chat par wapas — user kabhi
 *   is screen me phansa nahi rahega.
 * - PIN set hi na ho to guidance dikhti hai (Settings
 *   me PIN set karo), lock ka naatak nahi.
 */
@Composable
fun LockScreen(
    pinVault: PinVault,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val hasPin = remember { pinVault.hasPin() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Flora Lock")
        Spacer(modifier = Modifier.height(8.dp))
        if (!hasPin) {
            Text(
                "Abhi koi PIN set nahi hai. Lock lagane ke liye " +
                    "pehle Settings me ja kar PIN set karo, phir " +
                    "yahan wapas aao."
            )
        } else {
            Text("Flora kholne ke liye PIN daalo.")
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = pin,
                onValueChange = { pin = it.filter { c -> c.isDigit() }.take(6) },
                label = { Text("PIN") },
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (pinVault.verify(pin)) {
                    pin = ""
                    note = ""
                    onUnlock()
                } else {
                    note = "Galat PIN — dobara daalo."
                }
            }) { Text("Khol do") }
        }
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onBack) { Text("Wapas (Chat)") }
    }
}
