package com.flora.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.data.LicenceGate
import com.flora.app.data.LicenceStatus
import kotlinx.coroutines.launch

/**
 * Licence screen (Wave B, LFMS) — licence ke do raaste, ek hi screen par.
 *
 * 1. Website payment + admin approve: user id se judti hai
 *    (admin-approve website-path) — app khud payment approve
 *    kabhi nahi karti.
 * 2. Admin se mili licence key (FLORA-XXXX-XXXX-XXXX): neeche
 *    key daal kar verify karo — key server par user se judi
 *    hai, server hi status/binding/expiry ka faisla karta hai.
 * Dono par wahi niyam: 1 user = 1 licence = 1 device active
 * at one time; first claim bind karta hai; 15 din poore par
 * server transfer karta hai (purana device free); 15 din se
 * pehle device_locked; admin rebind turant (audit ke saath).
 * App khud rebind ki koshish kabhi nahi karti — sirf deviceId
 * bhejti hai, faisla server ka hai.
 * Licence key masked dikhti hai (sirf last 4) — click par
 * reveal + copy (jab mann tab copy, plaintext-with-controls
 * design: full key sirf authed verify/create se milti hai).
 * App me UTR save karna sirf LOCAL note hai — asli UTR submit
 * website pay page se hota hai (neeche button in-app browser
 * me pay page kholta hai).
 */
@Composable
fun LicenceScreen(
    prefs: FloraPreferences,
    licenceClient: LicenceClient,
    licence: LicenceStatus?,
    onLicence: (LicenceStatus?) -> Unit,
    onGoSettings: () -> Unit,
    onOpenWebsitePay: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var serverUpi by remember { mutableStateOf<String?>(null) }
    var utr by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }
    var licenceKeyInput by remember { mutableStateOf("") }
    var savedKey by remember { mutableStateOf("") }
    var keyRevealed by remember { mutableStateOf(false) }
    var keyVerifying by remember { mutableStateOf(false) }

    fun maskedKey(key: String): String {
        if (key.length <= 4) return "••••"
        return "••••-" + key.takeLast(4)
    }

    fun copyKey(text: String) {
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Flora licence", text))
            Toast.makeText(context, "Licence copy ho gaya", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            note = "Copy nahi ho paya."
        }
    }

    fun refresh() {
        if (refreshing) return
        refreshing = true
        scope.launch {
            try {
                val userId = prefs.effectiveUserId()
                val androidId = Settings.Secure.getString(
                    context.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: "flora-device"
                val deviceId = prefs.deviceId(androidId)
                val status = licenceClient.verify(userId, deviceId)
                onLicence(status)
                // Licence v2: kharab status par uska imaandaar
                // message hi note banega (generic "check ho gayi"
                // nahi — warna user ko pata hi nahi chalega ki
                // Pro kyon band hai).
                note = status.honestMessage() ?: "Licence server se check ho gayi."
            } catch (e: Exception) {
                note = "Server se check nahi ho paya — internet dekh kar dobara karo."
            }
            refreshing = false
        }
    }

    fun verifyKey() {
        if (keyVerifying) return
        val input = licenceKeyInput.trim()
        if (input.isEmpty()) {
            note = "Pehle licence key daalo."
            return
        }
        keyVerifying = true
        scope.launch {
            try {
                val androidId = Settings.Secure.getString(
                    context.contentResolver, Settings.Secure.ANDROID_ID
                ) ?: "flora-device"
                val deviceId = prefs.deviceId(androidId)
                val status = licenceClient.verifyWithKey(input, deviceId)
                onLicence(status)
                if (status.serverChecked && status.valid && status.status == "active") {
                    prefs.setLicenceKey(input)
                    savedKey = input
                    note = "Licence key verify ho gayi — plan: ${status.plan}."
                } else {
                    note = status.honestMessage()
                        ?: if (!status.serverChecked) "Server se check nahi ho paya — internet dekh kar dobara karo."
                        else "Ye key verify nahi hui — key dobara check karo."
                }
            } catch (e: Exception) {
                note = "Server se check nahi ho paya — internet dekh kar dobara karo."
            }
            keyVerifying = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            serverUpi = licenceClient.fetchServerUpi()
        } catch (e: Exception) {
            serverUpi = ""
        }
        try { utr = prefs.lastUtr() } catch (e: Exception) { }
        try {
            val k = prefs.licenceKey()
            licenceKeyInput = k
            savedKey = k
        } catch (e: Exception) { }
        refresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text("Licence / Pro — ₹199/month")
        licence?.let {
            Text("Status: ${it.plan} (${it.status})" + if (it.serverChecked) " — server se verified ✓" else " — local mode (server check nahi hua)")
            // Licence v2 status ka imaandaar message + expiry
            // (khali ho to kuch extra nahi dikhega).
            it.honestMessage()?.let { hm -> Text(hm) }
            if (it.expiresAt.isNotEmpty()) {
                Text("Expiry: ${it.expiresAt}")
            }
        } ?: Text("Status: pata nahi — Refresh dabao.")
        FreeRemainingText(prefs, licence)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Licence kaise banegi (do raaste):")
        Text("1. Website par signup karo (email + password).")
        Text("2. Website pay page par ₹199 UPI se pay karo aur UTR WAHI submit karo (neeche button) — asli submit wahi hota hai.")
        Text("3. Ya app se UPI app khol kar pay karo, phir UTR website pay page par submit karo.")
        Text("4. Admin UTR + amount + note match karke approve karega, tabhi licence active hogi.")
        Text("App me UTR save karna sirf tumhara LOCAL note hai — usse submit nahi hota, approve ka intezar us par mat karo.")
        Text("Admin se mili licence key ho to neeche key daal kar verify karo (us par bhi wahi niyam lagenge).")
        Text("Website se khareeda Pro isi app me dikhane ke liye Settings me 'Website account link' karo.")
        Text("Device lock: ek licence ek time me ek device par chalti hai. Pehle jis device par claim hui, wahi bind rahegi. 15 din poore hone par licence naye device par transfer ho jati hai (purana device free ho jata hai); 15 din se pehle nayi device par device lock lagega (transfer ki date dikhegi). Admin panel se turant rebind bhi ho sakta hai — app khud rebind ki koshish nahi karti, faisla server ka hai.")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { refresh() }) {
            Text(if (refreshing) "Check ho raha hai…" else "Licence refresh karo")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onOpenWebsitePay() }) {
            Text("Website se UTR submit karo")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                val vpa = serverUpi
                if (vpa == null) {
                    note = "UPI load ho raha hai — thodi der baad dobara dabao."
                } else if (vpa.isBlank()) {
                    // Wave B, D1: server UPI hi EK source hai —
                    // local VPA yahan kabhi nahi dikhega.
                    note = "UPI abhi set nahi hai — owner ne server par UPI ID set nahi kiya hai."
                } else {
                    val userId = prefs.effectiveUserId()
                    val uri = Uri.parse(
                        "upi://pay?pa=$vpa&pn=Flora&am=199&cu=INR&tn=FloraPro_$userId"
                    )
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        note = "UPI app khul rahi hai — payment ke baad UTR website pay page par submit kijiye (app me save karna sirf local note hai)."
                    } catch (e: Exception) {
                        note = "UPI app nahi khul payi — koi UPI app install honi chahiye."
                    }
                }
            }
        }) { Text("₹199 UPI se pay karo") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = utr, onValueChange = { utr = it },
            label = { Text("UTR number (payment ke baad, local note)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                prefs.setLastUtr(utr.trim())
                note = "UTR local note me save ho gaya — asli submit website pay page se hota hai, approve ka intezar us par mat karo."
            }
        }) { Text("UTR save karo (local note)") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = licenceKeyInput, onValueChange = { licenceKeyInput = it },
            label = { Text("Licence key (FLORA-XXXX-XXXX-XXXX)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { verifyKey() }) {
            Text(if (keyVerifying) "Verify ho raha hai…" else "Licence key verify karo")
        }
        if (savedKey.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Saved key: " + if (keyRevealed) savedKey else maskedKey(savedKey))
            Row {
                Button(onClick = { keyRevealed = !keyRevealed }) {
                    Text(if (keyRevealed) "Chhupao" else "Dikhao")
                }
                Button(onClick = { copyKey(savedKey) }) {
                    Text("Copy karo")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onGoSettings) { Text("Website account link (Settings me)") }
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Free me: 10 minute/day baat + app me maujood saare tools. Pro me: unlimited baat + FCM push. Live wallpaper / conversation mode abhi available nahi hain — Pro me hone ka daava nahi kiya jata.")
    }
}

/**
 * Wave B scope addition (A3): free-limit "kitna bacha hai" —
 * gate aur display EK hi counter se (LicenceGate ka local
 * talk-seconds counter; server usage-count aaj ka total wapas
 * nahi deta, wo sirf count karta hai — isliye do sach nahi
 * banenge). Pro par "unlimited" dikhega.
 */
@Composable
fun FreeRemainingText(prefs: FloraPreferences, licence: LicenceStatus?) {
    var remaining by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(licence) {
        remaining = try {
            if (licence?.plan == "pro") Long.MAX_VALUE
            else LicenceGate.freeRemainingSeconds(prefs)
        } catch (e: Exception) {
            null
        }
    }
    val rem = remaining
    if (licence?.plan == "pro") {
        Text("Pro: unlimited talk")
    } else if (rem != null) {
        Text("Free: aaj ${rem / 60} min ${rem % 60} sec baat bachi hai (10 min/day limit).")
    }
}
