package com.flora.app.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
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
import com.flora.app.data.LicenceStatus
import kotlinx.coroutines.launch

/**
 * Licence screen (v2.15) — "licence kaise banega, kahan
 * dalenge" ka seedha jawab, ek hi screen par.
 *
 * Licence koi code nahi hai jo app me type karna ho — ye
 * server par tumhare account (user id) se judta hai:
 * 1. Website par signup karo (email + password).
 * 2. App me ₹199 UPI se pay karo (neeche button).
 * 3. UTR yahan save karo.
 * 4. Admin UTR + amount + note match karke approve karega —
 *    tabhi server par licence active hogi. App khud kabhi
 *    payment approve nahi karti (money rule).
 * Licence status yahan live server se dikhta hai; device
 * binding (v2 rule): ek licence ek time me ek device par —
 * pehle jis device par claim hui, wahi bind rahegi;
 * auto-rebind kabhi nahi, rebind sirf admin panel se hoga.
 */
@Composable
fun LicenceScreen(
    prefs: FloraPreferences,
    licenceClient: LicenceClient,
    licence: LicenceStatus?,
    onLicence: (LicenceStatus?) -> Unit,
    onGoSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf("") }
    var adminVpa by remember { mutableStateOf("") }
    var utr by remember { mutableStateOf("") }
    var refreshing by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        try { adminVpa = prefs.adminUpiVpa() } catch (e: Exception) { }
        try { utr = prefs.lastUtr() } catch (e: Exception) { }
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
        Spacer(modifier = Modifier.height(8.dp))
        Text("Licence kaise banegi (koi code type nahi karna):")
        Text("1. Website par signup karo (email + password).")
        Text("2. Neeche ₹199 UPI se pay karo — UPI app khulegi.")
        Text("3. Payment ke baad UTR yahan save karo.")
        Text("4. Admin UTR + amount + note match karke approve karega, tabhi licence active hogi.")
        Text("Website se khareeda Pro isi app me dikhane ke liye Settings me 'Website account link' karo.")
        Text("Device lock: ek licence ek time me ek device par chalti hai. Pehle jis device par claim hui, wahi bind rahegi — doosre phone par apne aap transfer nahi hogi; rebind sirf admin panel se hoga.")
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { refresh() }) {
            Text(if (refreshing) "Check ho raha hai…" else "Licence refresh karo")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                val vpa = prefs.adminUpiVpa()
                if (vpa.isBlank()) {
                    note = "Owner ne abhi admin UPI VPA set nahi kiya hai — Settings me VPA save hone ke baad hi pay khulega."
                } else {
                    val userId = prefs.effectiveUserId()
                    val uri = Uri.parse(
                        "upi://pay?pa=$vpa&pn=Flora&am=199&cu=INR&tn=FloraPro_$userId"
                    )
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        note = "UPI app khul rahi hai — payment ke baad UTR neeche save kijiye."
                    } catch (e: Exception) {
                        note = "UPI app nahi khul payi — koi UPI app install honi chahiye."
                    }
                }
            }
        }) { Text("₹199 UPI se pay karo") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = utr, onValueChange = { utr = it },
            label = { Text("UTR number (payment ke baad)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                prefs.setLastUtr(utr.trim())
                note = "UTR save ho gaya (local note). Admin verify karke hi licence active karega — server verification alag step hai."
            }
        }) { Text("UTR save karo") }
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
