package com.flora.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.flora.app.assistant.VoiceEngine
import com.flora.app.data.NotifItem
import com.flora.app.data.NotificationStore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notifications screen (v2.22, P2-F10, READ ONLY): phone par
 * aayi notifications ki list — sirf dekhna, koi action nahi.
 * Access user khud Settings se dega; OFF ho to list khali
 * rahegi aur button Settings kholega. List sirf is phone par
 * hai, ek tap par saaf ho jati hai.
 */
@Composable
fun NotificationsScreen(voice: VoiceEngine) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<NotifItem>>(emptyList()) }
    var enabled by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }

    fun refresh() {
        enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
        scope.launch {
            items = NotificationStore.load(context).reversed()
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun appLabel(pkg: String): String {
        return try {
            val info = context.packageManager.getApplicationInfo(pkg, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            pkg
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Notifications — sirf dekhna, koi action nahi.")
            Text(
                if (enabled) "Access ON hai — aane wali notifications yahan dikhengi."
                else "Access OFF hai — list khali rahegi. Neeche wale button se Settings me Flora ko notification access do (tum khud doge)."
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) {
                    note = "Settings nahi khul payi — phone ke Settings > Notifications se kholo."
                }
            }) { Text("Notification access settings kholo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    NotificationStore.clear(context)
                    items = emptyList()
                    note = "List saaf ho gayi."
                    voice.speak("List saaf ho gayi.")
                }
            }) { Text("List saaf karo") }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { refresh() }) { Text("Refresh karo") }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text("Abhi koi notification save nahi hai.")
            }
        }
        items(items) { item ->
            val time = try {
                SimpleDateFormat("dd MMM HH:mm", Locale.US).format(Date(item.timeMs))
            } catch (e: Exception) {
                ""
            }
            Text("${appLabel(item.pkg)} • $time")
            if (item.title.isNotBlank()) Text(item.title)
            if (item.text.isNotBlank()) Text(item.text)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
