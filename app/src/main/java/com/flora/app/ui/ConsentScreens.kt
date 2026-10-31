package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flora.app.data.ConsentCatalog
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * P3-05 — Privacy/Terms + consent screens (fixer wiring).
 *
 * ConsentCatalog ki 5 permissions, har ek ka reason + on/off:
 * mic-24x7, screen-view, camera, notification-read, contacts.
 * Sab default OFF hain (prefs me absent = OFF). Koi permission
 * chupchaap ON nahi hoti — har toggle user ke haath me hai, aur
 * toggle turant prefs me save hota hai.
 *
 * Note: ye consent toggles hain, system permission grant nahi —
 * system permission (mic/camera/notification) Permissions screen
 * / wizard se milti hai; yahan OFF ka matlab Flora us permission
 * ka istemal nahi karegi, chahe system grant maujood ho.
 */
@Composable
fun ConsentScreen(prefs: FloraPreferences, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var states by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var loaded by remember { mutableStateOf(false) }

    // States upar declare hain — LaunchedEffect unke BAAD hai.
    LaunchedEffect(Unit) {
        val map = mutableMapOf<String, Boolean>()
        for (entry in ConsentCatalog.ENTRIES) {
            map[entry.key] = try {
                prefs.consentEnabled(entry.key)
            } catch (e: Exception) {
                false
            }
        }
        states = map
        loaded = true
    }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text("Privacy + consent")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Har permission ka reason neeche hai. Sab default OFF " +
                "hain — ON sirf tumhare toggle par hoga."
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!loaded) {
            Text("Consent load ho raha hai…")
        } else {
            for (entry in ConsentCatalog.ENTRIES) {
                val on = states[entry.key] ?: entry.defaultOn
                Text(entry.title)
                Text(entry.reason)
                Row {
                    Text(if (on) "ON" else "OFF")
                    Switch(
                        checked = on,
                        onCheckedChange = { value ->
                            states = states + (entry.key to value)
                            scope.launch {
                                try {
                                    prefs.setConsentEnabled(entry.key, value)
                                } catch (e: Exception) {
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Button(onClick = onClose) { Text("Band karo") }
    }
}
