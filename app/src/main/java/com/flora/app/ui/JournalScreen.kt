package com.flora.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flora.app.data.JournalStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Journal + mood (v1.6): aaj kaisa lag raha hai + ek line likho.
 * Sirf is phone par save hota hai, kahin nahi bheja jaata.
 */
@Composable
fun JournalScreen(store: JournalStore) {
    val entries by store.entries.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var mood by remember { mutableStateOf("Theek") }
    var text by remember { mutableStateOf("") }
    val moods = listOf("Khush", "Theek", "Udaas", "Gussa", "Thaka")
    val fmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("hi", "IN")) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Journal — aaj ka mood + ek line, sirf is phone par.")
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                moods.forEach { m ->
                    Button(onClick = { mood = m }) {
                        Text(if (mood == m) "✓ $m" else m)
                    }
                    Spacer(modifier = Modifier.padding(2.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Aaj kaisa raha? Ek line likhiye") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val t = text.trim()
                if (t.isNotEmpty()) {
                    scope.launch { store.add(mood, t) }
                    text = ""
                }
            }) { Text("Journal save karo") }
            Spacer(modifier = Modifier.height(12.dp))
        }
        itemsIndexed(entries) { index, e ->
            Text("${e.mood} • ${if (e.ts > 0) fmt.format(Date(e.ts)) else ""}")
            Text(e.text)
            Row {
                Button(onClick = { scope.launch { store.remove(index) } }) { Text("Delete") }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
