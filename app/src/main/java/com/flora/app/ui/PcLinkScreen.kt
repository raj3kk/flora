package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flora.app.data.FloraPreferences
import kotlinx.coroutines.launch

/**
 * PC link screen (v2.22, Phase 5, G13, phone half) —
 * computer se Flora jodne ka pairing code dikhata hai.
 * Code sirf is phone par banta/saved hai; computer
 * wala client app alag deliverable hai (abhi nahi hai
 * — is screen se PC control ka daava nahi kiya jata).
 */
@Composable
fun PcLinkScreen(prefs: FloraPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Text("PC jodo (taiyaari)")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Taiyaari stage: PC client abhi nahi hai, isliye ye " +
                "code abhi kahin daala nahi ja sakta. Client aate " +
                "hi isi code se phone aur computer judenge. Code " +
                "sirf is phone par hai, kisi server par nahi jata."
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                val existing = prefs.pcPairCode()
                if (existing.isNotEmpty()) {
                    code = existing
                } else {
                    val fresh = (100000..999999).random().toString()
                    prefs.setPcPairCode(fresh)
                    code = fresh
                }
                note = "Pairing code taiyaar hai — par PC client abhi nahi hai, isliye ye code abhi kahin daala nahi ja sakta."
            }
        }) { Text("Pairing code dikhao") }
        Spacer(modifier = Modifier.height(8.dp))
        if (code.isNotEmpty()) {
            Text("Pairing code: $code")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Taiyaari hi hai: PC client abhi nahi bana hai, " +
                    "isliye ye code abhi kisi PC app me daala nahi " +
                    "ja sakta. (PC client is screen ka hissa nahi hai.)"
            )
        }
        Button(onClick = {
            scope.launch {
                val fresh = (100000..999999).random().toString()
                prefs.setPcPairCode(fresh)
                code = fresh
                note = "Naya pairing code ban gaya hai — par PC client abhi nahi hai, isliye ye code abhi kahin daala nahi ja sakta."
            }
        }) { Text("Naya code banao") }
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Note: PC client app abhi alag se banana baaki hai — " +
                "ye screen sirf phone ka hissa hai, poora PC " +
                "control isse nahi chalta."
        )
    }
}
