package com.flora.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import com.flora.app.data.AppDeleteExportBackend
import com.flora.app.data.DataDeleteExport
import kotlinx.coroutines.launch

/**
 * P3-04 — Data delete + export screen (fixer wiring).
 *
 * - Delete: confirm checkbox ke baad hi button chalta hai; bina
 *   confirm ke backend khud inkaar karta hai (guard). Delete
 *   wapas nahi hota — ye screen par saaf likha hai.
 * - Retention 30 din: stores ka prune alag se hai; ye full-delete
 *   user ka faisla hai, retention se upar.
 * - Chat memory EXEMPT: delete-all chat memory ko kabhi nahi chhuta.
 * - Server-side rows (licence/UTR) app se delete nahi hote — ye
 *   note screen par hai, Part 2 server function ke bina invent
 *   nahi kiya gaya.
 * - Export: manifest + counts; share sirf user-tap par (chooser),
 *   auto-send kabhi nahi. Poora content dump nahi — wo backup ka
 *   kaam hai.
 */
@Composable
fun DataDeleteExportScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backend = remember { AppDeleteExportBackend(context) }
    var confirmed by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var manifestText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text("Data delete + export")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Delete wapas nahi hota. Scope: journal, docs, routines, " +
                "jobs, memories, reminders, triggers, watchlist, " +
                "post-history, macros, fav-songs, SOS, notifications, " +
                "pending-tasks, projects, project-memory. Chat memory " +
                "is delete se exempt hai — wo nahi mitegi. Server-side " +
                "rows (licence/UTR) app se delete nahi hote."
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
            Text("Main samajhta hun — delete wapas nahi hoga.")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            enabled = confirmed,
            onClick = {
                scope.launch {
                    val res = backend.deleteAll(confirmed)
                    resultText = if (res.ok) {
                        "Delete ho gaya: ${res.reason}"
                    } else {
                        "Delete poora nahi hua: ${res.reason}"
                    }
                }
            }
        ) { Text("Sab data delete karo") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                val bundle = backend.exportAll()
                manifestText = "Export manifest (" +
                    DataDeleteExport.RETENTION_DAYS + " din retention):\n" +
                    bundle.storeCounts.entries.sortedBy { it.key }
                        .joinToString("\n") { "${it.key}: ${it.value}" }
            }
        }) { Text("Export manifest banao") }
        if (manifestText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(manifestText)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, manifestText)
                }
                context.startActivity(Intent.createChooser(send, "Export manifest share karo"))
            }) { Text("Manifest share karo") }
        }
        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultText)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClose) { Text("Band karo") }
    }
}
