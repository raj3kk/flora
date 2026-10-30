package com.flora.app.ui

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.flora.app.data.AutoLogStore
import com.flora.app.data.CrashLogReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    // B4 (fix-cycle): crash.log (FloraApplication writer, filesDir/crash.log)
    // Auto Log me kabhi nahi aata tha, isliye crash ka saboot UI tak pahunche nahi.
    val appContext = LocalContext.current
    var crashText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        crashText = withContext(Dispatchers.IO) {
            CrashLogReader.readCrashLog(appContext.filesDir)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Auto Log — Auto mode me hue kaam")
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // B4: crash-log section — sabse upar, taaki crash saboot pehle dikhe.
            // (Call site FloraApp.kt me waisa hi hai — sirf is file ka content badla hai.)
            item {
                Text("Crash log — app band hone ka record")
                Spacer(modifier = Modifier.height(4.dp))
                val ct = crashText
                if (ct == null) {
                    Text("Crash log padha ja raha hai…")
                } else if (ct.isEmpty()) {
                    Text("Koi crash record nahi hai.")
                } else {
                    Text(ct)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(onClick = {
                        scope.launch {
                            val full = withContext(Dispatchers.IO) {
                                CrashLogReader.readFullForShare(appContext.filesDir)
                            }
                            if (full.isNotBlank()) {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, full)
                                }
                                appContext.startActivity(Intent.createChooser(send, "Crash log share karo"))
                            }
                        }
                    }) { Text("Crash log share karo") }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Note: Ye sirf is phone ka crash record hai; share aap khud chooser se kar rahe hain, koi auto-upload nahi hai.")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (items.isEmpty()) {
                item { Text("Abhi koi auto kaam nahi hua hai.") }
            } else {
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
