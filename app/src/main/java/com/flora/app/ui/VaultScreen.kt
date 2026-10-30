package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flora.app.data.AutoLogStore
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * Item 36 §1 — User Credential Vault (view / revoke / delete),
 * Lane A app part.
 *
 * Vault PIN-vault pattern par hai: KeyStore-encrypted, sirf
 * isi phone par. Ye screen sirf dikhati + revoke/delete karti
 * hai — CONNECT yahan nahi hota (connect SD-1 Connectors
 * screen / Builder Connect se hota hai, jahan user ne save ko
 * haan kaha tha). Casual chat/voice me bola gaya koi secret
 * yahan kabhi nahi aayega — save sirf authorized flows se
 * (in-app save / secure paste / OAuth) hota hai.
 *
 * HARD LINES (UI text me bhi):
 * - Delete ke baad credential kahin se recover nahi hoga —
 *   ye by design hai, "recover kar do" ka vaada kabhi nahi.
 * - Vault kabhi web password field me type nahi karta
 *   (FormFiller guard barkaraar) — ye browser password
 *   manager nahi hai.
 * - Payment auto-confirm kabhi nahi — money par hamesha
 *   user ka explicit approval chahiye.
 * - Vault ka content kabhi poora screen/log/chat par nahi
 *   dikhega — sirf masked roop (•••• + aakhri 4).
 * - Har revoke/delete Auto Log me jata hai.
 */
@Composable
fun VaultScreen(autoLogStore: AutoLogStore, onBack: () -> Unit) {
    val appContext = LocalContext.current
    val prefs = remember { FloraPreferences(appContext) }
    val scope = rememberCoroutineScope()

    var note by remember { mutableStateOf("") }
    var tgMask by remember { mutableStateOf("") }
    var tdMask by remember { mutableStateOf("") }
    var lnMask by remember { mutableStateOf("") }
    var armedService by remember { mutableStateOf("") }

    fun masked(token: String): String =
        if (token.length <= 4) "••••" else "••••" + token.takeLast(4)

    fun log(msg: String) {
        scope.launch {
            try {
                autoLogStore.add(msg)
            } catch (e: Exception) {
            }
        }
    }

    fun refresh() {
        scope.launch {
            try {
                val tg = prefs.sdTelegramToken()
                tgMask = if (tg.isNotBlank()) masked(tg) else ""
                val td = prefs.sdTodoistToken()
                tdMask = if (td.isNotBlank()) masked(td) else ""
                val ln = prefs.sdLinearToken()
                lnMask = if (ln.isNotBlank()) masked(ln) else ""
            } catch (e: Exception) {
                note = "Vault status padhne me dikkat aayi — dobara kholo."
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun revoke(service: String) {
        if (armedService != service) {
            armedService = service
            note = "Pakka $service ka credential mitana hai? Ye wapas nahi aayega — confirm karne ke liye dobara dabao."
            return
        }
        armedService = ""
        scope.launch {
            try {
                when (service) {
                    "Telegram" -> prefs.clearSdTelegramToken()
                    "Todoist" -> prefs.clearSdTodoistToken()
                    "Linear" -> prefs.clearSdLinearToken()
                }
                log("Vault: $service credential revoke/delete hua (unrecoverable by design)")
                note = "$service ka credential mita diya gaya — dobara chahiye to phir se connect karna hoga."
                refresh()
            } catch (e: Exception) {
                note = "$service ka credential mit nahi paya — dobara koshish karo."
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row {
            TextButton(onClick = onBack) { Text("Wapas") }
        }
        Text("Vault — tumhare apne saved credentials")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Har entry tabhi bani jab tumne connect karte waqt save ko haan kaha tha. " +
                "Koi entry bina consent ke nahi banti, aur koi credential kabhi poora yahan, chat me, ya log me nahi dikhega.",
        )
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Telegram bot: ${if (tgMask.isBlank()) "save nahi hai" else "connected ($tgMask)"}")
        if (tgMask.isNotBlank()) {
            Button(onClick = { revoke("Telegram") }) {
                Text(if (armedService == "Telegram") "Pakka mitao" else "Revoke / delete")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Todoist: ${if (tdMask.isBlank()) "save nahi hai" else "connected ($tdMask)"}")
        if (tdMask.isNotBlank()) {
            Button(onClick = { revoke("Todoist") }) {
                Text(if (armedService == "Todoist") "Pakka mitao" else "Revoke / delete")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Linear: ${if (lnMask.isBlank()) "save nahi hai" else "connected ($lnMask)"}")
        if (lnMask.isNotBlank()) {
            Button(onClick = { revoke("Linear") }) {
                Text(if (armedService == "Linear") "Pakka mitao" else "Revoke / delete")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Hard lines: vault kabhi web password field me type nahi karta (FormFiller guard) — " +
                "ye browser password manager nahi hai. Payment kabhi vault se auto-confirm nahi hoga. " +
                "Kisi service ne fresh login/2FA manga to Flora ek baar imaandaari se dobara mangegi — " +
                "\"kabhi login nahi mangega\" ka vaada kabhi nahi.",
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
