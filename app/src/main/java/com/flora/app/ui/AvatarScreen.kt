package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
 * Avatar catalog (v2.22, S2): Flora ka rang-roop chuno. Sab kuch
 * on-device Compose se banta hai — koi download, koi creator
 * asset nahi. Sirf orb ka rang badalta hai; awaaz aur persona
 * wahi rehte hain.
 */
@Composable
fun AvatarScreen(prefs: FloraPreferences, onPick: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("gulabi") }

    LaunchedEffect(Unit) {
        current = try { prefs.avatar() } catch (e: Exception) { "gulabi" }
    }

    val styles = listOf(
        "gulabi" to "Gulabi (default)",
        "neela" to "Neela",
        "hara" to "Hara",
        "baingani" to "Baingani",
        "sunehra" to "Sunehra",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Avatar — Flora ka rang-roop chuno")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Sirf orb ka rang badlega; awaaz aur baat karne ka tareeqa wahi rahega.")
        Spacer(modifier = Modifier.height(12.dp))
        for ((id, name) in styles) {
            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                FloraOrb(busy = false, avatar = id)
                Spacer(modifier = Modifier.padding(4.dp))
                Column {
                    Text(name + if (current == id) " ✓ (chuna hua)" else "")
                    Button(onClick = {
                        scope.launch {
                            try {
                                prefs.setAvatar(id)
                                current = id
                                onPick(id)
                            } catch (e: Exception) { }
                        }
                    }) { Text(if (current == id) "Chuna hua" else "Chuno") }
                }
            }
        }
    }
}
