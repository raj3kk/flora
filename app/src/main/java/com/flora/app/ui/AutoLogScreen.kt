package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flora.app.data.AutoLogStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Auto Log screen (v2.17): Auto mode me Flora ne jo bhi bina poochhe
 * kiya, uski list — date/time ke saath. Saaf karne ka button bhi hai.
 * Log sirf is phone par hai, koi server/backup nahi.
 */
@Composable
fun AutoLogScreen(store: AutoLogStore) {
    val scope = rememberCoroutineScope()
    val items by store.items.collectAsState(initial = emptyList())
    val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Auto Log — Auto mode me hue kaam")
        Spacer(modifier = Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text("Abhi koi auto kaam nahi hua hai.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(items.reversed()) { (text, ts) ->
                    val whenText = if (ts > 0) fmt.format(Date(ts)) else ""
                    Text("$whenText — $text")
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                try { store.clear() } catch (e: Exception) { }
            }
        }) { Text("Log saaf karo") }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Note: 30 din se purani entries app-start par auto-delete hoti hain. Payment kabhi Auto Log me nahi aayegi — payment hamesha alag confirmation se hoti hai.")
    }
}
