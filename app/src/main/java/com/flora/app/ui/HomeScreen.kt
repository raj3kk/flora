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
import androidx.compose.ui.unit.dp
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * Home (v2.22, P5-F22 phone half): relay box ka code phone par
 * save karo. SAFETY (sakht): mains ka kaam sirf trained
 * electrician kare, MCB OFF karke; har channel max 5A, poora
 * board 10A se kam. India me green wire EARTH hoti hai (phase
 * nahi) — is baat par kabhi samjhauta nahi.
 *
 * IMAANDARI: server sync (S4) abhi baaki hai — code save hona
 * control nahi hai. Awaaz se light/pankha tabhi chalega jab
 * server sync ban jayega; tab tak screen sirf taiyaari hai.
 */
@Composable
fun HomeScreen(prefs: FloraPreferences) {
    val scope = rememberCoroutineScope()
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
                        saved = c
                        code = ""
                        note = "Code phone par save ho gaya. Server sync (S4) aate hi control chalega — tab tak awaaz se light/pankha nahi chalega."
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
        Text(
            "Note: ye screen control nahi hai. Asli on/off server sync " +
                "(S4) ke baad aayega — uske bina koi relay nahi chalega, " +
                "aur chhupke se kuch on/off kabhi nahi hoga."
        )
    }
}
