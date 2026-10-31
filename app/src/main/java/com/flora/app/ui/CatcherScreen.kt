package com.flora.app.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.flora.app.data.AutoLogStore
import com.flora.app.data.CatcherEvents
import com.flora.app.data.CatcherHealth
import com.flora.app.data.CatcherHooks
import com.flora.app.data.CatcherReport
import com.flora.app.data.SupportRoute
import com.flora.app.data.CrashLogReader
import com.flora.app.data.LicenceStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Catcher screen (in-app Catcher, G2): ek hi full copy-able problem
 * report — crash + error events + Auto Log tail + system state +
 * licence state + user note. Copy/Share/Save sirf user tap par;
 * koi auto-upload kabhi nahi.
 *
 * Params caller-supplied hain (koi naya server call nahi): licence
 * SettingsScreen ke maujooda state se aayega, fcmStatus bhi wahi se.
 * Saare states unhe use karne wale LaunchedEffect/Buttons se PEHLE
 * declare hain (order trap se bacha).
 */
@Composable
fun CatcherScreen(
    autoLogStore: AutoLogStore,
    licence: LicenceStatus?,
    autoMode: Boolean,
    fcmStatus: String = "",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    var note by remember { mutableStateOf("") }
    var crashText by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<String>>(emptyList()) }
    var countsText by remember { mutableStateOf("") }
    var prevLogText by remember { mutableStateOf("") }
    var savedMsg by remember { mutableStateOf("") }
    var report by remember { mutableStateOf("") }
    val autoItems by autoLogStore.items.collectAsState(initial = emptyList())
    // States upar declare hain — LaunchedEffect unke BAAD hai.
    LaunchedEffect(Unit) {
        val c = withContext(Dispatchers.IO) { CrashLogReader.readCrashLog(context.filesDir) }
        val e = withContext(Dispatchers.IO) { CatcherEvents.readTail() }
        val counts = withContext(Dispatchers.IO) { CatcherEvents.countsByCategory(context.filesDir) }
        val prevExists = withContext(Dispatchers.IO) { CatcherEvents.prevLogExists(context.filesDir) }
        crashText = c
        events = e
        countsText = counts.entries.sortedByDescending { it.value }
            .joinToString(", ") { "${it.key}: ${it.value}" }
        prevLogText = if (prevExists) {
            "Pichhli log generation maujood hai (catcher_events.prev.log)"
        } else {
            "Koi pichhli log generation nahi hai"
        }
    }
    LaunchedEffect(crashText, events, autoItems, note, licence, autoMode, fcmStatus) {
        val fmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        val autoLines = autoItems.takeLast(30).map { (text, ts) ->
            val whenText = if (ts > 0) fmt.format(Date(ts)) else ""
            "$whenText — $text"
        }
        val licLines = if (licence == null) {
            emptyList()
        } else {
            buildList {
                add("Licence: ${licence.plan} / ${licence.status} / valid=${licence.valid}")
                licence.honestMessage()?.let { add(it) }
            }
        }
        report = CatcherReport.buildReport(
            appLine = appLine(context),
            androidLine = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), " +
                "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
            timeLine = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            licenceLines = licLines,
            autoMode = autoMode,
            fcmStatus = fcmStatus,
            note = note,
            crashText = crashText ?: "",
            events = events,
            autoLogLines = autoLines,
            systemLines = systemLines(context),
        )
    }
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        Text("Problem report (Catcher)")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Ye report is phone ka diagnostic record hai — crash, error " +
                "events, Auto Log aur system state. Isme password, key, OTP " +
                "ya poora UTR kabhi nahi aata."
        )
        if (countsText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Events by category: $countsText")
        }
        // CEO batch C4-2: init-health line + prev-log indicator —
        // khaali screen ki jagah Catcher ki haalat hamesha dikhe.
        Spacer(modifier = Modifier.height(8.dp))
        Text(CatcherHealth.healthLine())
        if (prevLogText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(prevLogText)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Ye kar raha tha tab hua…") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Flora problem report", report))
            savedMsg = "Report copy ho gaya — chat me paste kar do."
        }) { Text("Report copy karo") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, report)
            }
            context.startActivity(Intent.createChooser(send, "Problem report share karo"))
        }) { Text("Report share karo") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                // CEO batch C4-1: report-save asli storage IO hai —
                // fail chupchaap nahi, Catcher me STORAGE entry + user
                // ko saaf failure message (pehle fail par bhi "save ho
                // gaya" likha jata tha, jo galat tha).
                val ok = withContext(Dispatchers.IO) {
                    try {
                        File(context.filesDir, "catcher-report.txt").writeText(report)
                        true
                    } catch (e: Exception) {
                        CatcherHooks.storageError("catcher-report-save", "io-fail")
                        false
                    }
                }
                savedMsg = if (ok) {
                    "Report save ho gaya (catcher-report.txt)."
                } else {
                    "Report save nahi ho paya — storage error, dobara koshish karo."
                }
            }
        }) { Text("Report save karo") }
        Spacer(modifier = Modifier.height(8.dp))
        // CEO batch SupportRoute UI entry: pehle se wired
        // SupportRoute intent kholta hai — sirf mail composer,
        // bhejna user-tap par, auto-send kabhi nahi. Draft =
        // user ki apni note line (sanitized, 500 chars cap);
        // note khaali ho to user mail app me khud likhega.
        Button(onClick = {
            val send = SupportRoute.sendIntent(note)
            context.startActivity(Intent.createChooser(send, "Support ko mail karo"))
        }) { Text("Support ko mail karo") }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Mail composer khulega — bhejna tumhare tap par hoga.")
        if (savedMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(savedMsg)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Report preview:")
        Spacer(modifier = Modifier.height(4.dp))
        val ct = crashText
        if (ct == null) {
            Text("Report ban raha hai…")
        } else {
            Text(report)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Note: Ye report kahin auto-upload nahi hoti — copy, share " +
                "aur save sirf tumhare tap par hota hai."
        )
    }
}

@Suppress("DEPRECATION")
private fun appLine(context: Context): String {
    return try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "Flora ${info.versionName ?: "unknown"} (${PackageInfoCompat.getLongVersionCode(info)})"
    } catch (e: Exception) {
        "Flora unknown"
    }
}

private fun systemLines(context: Context): List<String> {
    val out = mutableListOf<String>()
    try {
        val mic = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        out.add("Mic permission: ${if (mic) "haan" else "na"}")
        val notif = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        out.add("Notification permission: ${if (notif) "haan" else "na"}")
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? android.view.accessibility.AccessibilityManager
        val acc = am?.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )?.any { it.resolveInfo.serviceInfo.packageName == context.packageName } ?: false
        out.add("Accessibility (Flora service): ${if (acc) "ON" else "OFF"}")
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batt = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        out.add("Battery optimization ignored: ${if (batt) "haan" else "na"}")
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.getNetworkCapabilities(cm.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        out.add("Network: ${if (net) "connected" else "na"}")
        out.add("Auto-start: phone settings se check karein (Vivo: auto-start + battery unrestricted)")
    } catch (e: Exception) {
        out.add("System state padhne me error aaya.")
    }
    return out
}
