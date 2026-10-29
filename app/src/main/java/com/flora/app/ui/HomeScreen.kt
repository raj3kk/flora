package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.flora.app.core.FeatureFlags
import com.flora.app.data.FloraPreferences
import com.flora.app.tools.HomeSyncClient
import kotlinx.coroutines.launch

/**
 * Home (v2.22, P5-F22 phone half): relay box ka code phone par
 * save karo. SAFETY (sakht): mains ka kaam sirf trained
 * electrician kare, MCB OFF karke; har channel max 5A, poora
 * board 10A se kam. India me green wire EARTH hoti hai (phase
 * nahi) — is baat par kabhi samjhauta nahi.
 *
 * IMAANDARI: box registration sync MAUJOOD hai ("Server se
 * sync karo" button se box code server tak jata hai) — code
 * save hona control nahi hai. Relay on/off ka server hissa
 * abhi baaki hai, isliye awaaz se light/pankha abhi nahi
 * chalega; tab tak screen sirf taiyaari + registration hai.
 */
@Composable
fun HomeScreen(prefs: FloraPreferences) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var code by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        saved = try { prefs.homeBoxCode() } catch (e: Exception) { "" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Home — relay box (taiyaari)")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Safety pehle: mains ka kaam sirf trained electrician, " +
                "MCB OFF karke. Har channel max 5A, poora board 10A se kam. " +
                "India me green wire EARTH hai, phase nahi."
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (saved.isNotEmpty()) {
            Text("Juda hua box code: $saved (sirf phone par save hai)")
        } else {
            Text("Abhi koi box juda nahi hai.")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = code, onValueChange = { code = it },
            label = { Text("Box ka join code") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val c = code.trim()
            if (c.isEmpty()) {
                note = "Pehle box ka code likho."
            } else {
                scope.launch {
                    try {
                        prefs.setHomeBoxCode(c)
                        // Cycle 1, APP-C1-009: naya code abhi sync
                        // nahi hua — awaaz wala jawab "sync baaki
                        // hai" isi flag se bolega.
                        prefs.setHomeServerSynced(false)
                        saved = c
                        code = ""
                        note = "Code phone par save ho gaya. 'Server se sync karo' se registration sync karo — relay control ka server hissa aane tak awaaz se light/pankha nahi chalega."
                    } catch (e: Exception) {
                        note = "Code save nahi ho paya."
                    }
                }
            }
        }) { Text("Box jodo (code save karo)") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                try {
                    prefs.setHomeBoxCode("")
                    prefs.setHomeServerSynced(false)
                    saved = ""
                    note = "Box hata diya gaya."
                } catch (e: Exception) {
                    note = "Hata nahi paya."
                }
            }
        }) { Text("Box hatao") }
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Server sync (Phase 8 rewrite): user JWT + device_id +
        // box_code -> home-sync function service-role upsert.
        // Function deploy hone se pehle, ya unlinked ho, ya home
        // flag OFF ho — har case me null = imaandaar message,
        // jhootha "jud gaya" kabhi nahi.
        Button(onClick = {
            scope.launch {
                val c = saved
                if (c.isEmpty()) {
                    note = "Pehle box ka code save karo, phir sync karo."
                } else if (!FeatureFlags.isEnabled("home")) {
                    note = "Ye feature abhi band hai."
                } else {
                    note = "Server se sync ho raha hai..."
                    val res = HomeSyncClient.sync(prefs, c, ctx)
                    // Cycle 1, APP-C1-009: safal sync par flag —
                    // awaaz wala jawab ab "sync baaki hai" nahi
                    // kahega. Fail par flag waisa hi (purana safal
                    // sync server row ko nahi mitata).
                    if (res != null) {
                        try { prefs.setHomeServerSynced(true) } catch (e: Exception) { }
                    }
                    note = if (res != null) {
                        "Server ko code mil gaya hai. Relay control tab bhi box ke server hissa banne ke baad hi chalega."
                    } else {
                        "Server sync nahi ho paya — website account link karo, phir function deploy hone ke baad dobara karo. Code phone par surakshit hai."
                    }
                }
            }
        }) { Text("Server se sync karo") }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Note: ye screen control nahi hai. Box registration " +
                "sync maujood hai, par asli on/off relay control " +
                "ke server hisse ke baad aayega — uske bina koi " +
                "relay nahi chalega, aur chhupke se kuch on/off " +
                "kabhi nahi hoga."
        )
    }
}
