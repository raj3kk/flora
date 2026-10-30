package com.flora.app.ui

import android.annotation.SuppressLint
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.viewinterop.AndroidView
import com.flora.app.backup.BackupFailure
import com.flora.app.backup.DriveClient
import com.flora.app.backup.FloraBackupCrypto
import com.flora.app.backup.FloraBackupManager
import com.flora.app.data.AutoLogStore
import com.flora.app.data.FloraPreferences
import com.flora.app.data.LicenceClient
import com.flora.app.security.GuardianGate
import com.flora.app.security.PinVault
import com.flora.app.tools.BuilderOAuth
import kotlinx.coroutines.launch

/**
 * B6 Backup/Import screen — Settings ke andar dikhta hai
 * (Settings kabhi gated nahi hoti — recovery raasta hamesha
 * khula rehta hai). Har backup/import USER ka kaam hai: koi
 * auto-upload, koi background backup worker nahi.
 *
 * User-facing text me koi technical baat nahi (koi code, koi
 * model naam, koi "key gadbad" nahi) — sirf seedha sach:
 * - Backup PIN kahin save nahi hota; bhool gaye to backup
 *   kabhi nahi khulega (koi recovery backdoor nahi).
 * - Naya PIN sirf naye backup par lagta hai; purana backup
 *   purane PIN se hi khulega.
 * - Restore ka matlab REPLACE hai: purana data hat kar
 *   backup wala aa jayega — confirm dialog me ye saaf likha hai.
 * - Galat PIN par kuch bhi nahi badalta (purana data waisa hi).
 * - Drive par file dikhna hi success proof hai (koi chhupa
 *   folder nahi — "Flora Backups" folder user ke Drive me
 *   dikhta hai).
 * Guardian ON ho to backup/import ACTION se pehle Guardian
 * PIN maanga jaata hai (gated action); backup PIN usse bilkul
 * alag hai aur Guardian ke 15-minute session se kabhi nahi
 * bandhta.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BackupScreen(
    prefs: FloraPreferences,
    licenceClient: LicenceClient,
    pinVault: PinVault,
    guardian: GuardianGate,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val manager = remember { FloraBackupManager(appContext, prefs, licenceClient) }
    val drive = remember { DriveClient(prefs) }
    val autoLog = remember { AutoLogStore(appContext) }

    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var backupPin by remember { mutableStateOf("") }
    var backupPinConfirm by remember { mutableStateOf("") }
    var restorePin by remember { mutableStateOf("") }
    var driveClientId by remember { mutableStateOf("") }
    var driveRedirectUri by remember { mutableStateOf("") }
    var driveConnected by remember { mutableStateOf(false) }
    var driveFiles by remember { mutableStateOf<List<DriveClient.DriveFile>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<DriveClient.DriveFile?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showDriveLogin by remember { mutableStateOf(false) }
    var driveLoginUrl by remember { mutableStateOf("") }
    var driveVerifier by remember { mutableStateOf("") }
    var driveState by remember { mutableStateOf("") }
    var guardianPinInput by remember { mutableStateOf("") }
    var guardianPinNote by remember { mutableStateOf("") }
    var guardianPending by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun deviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    LaunchedEffect(Unit) {
        driveClientId = try { prefs.driveClientId() } catch (e: Exception) { "" }
        driveRedirectUri = try { prefs.driveRedirectUri() } catch (e: Exception) { "" }
        val connected = try { drive.isConnected() } catch (e: Exception) { false }
        driveConnected = connected
        if (connected) {
            val folderId = try { drive.ensureFolder() } catch (e: Exception) { null }
            if (folderId != null) {
                driveFiles = try { drive.listBackups(folderId) } catch (e: Exception) { emptyList() }
            }
        }
    }

    fun refreshDriveState() {
        scope.launch {
            val connected = try { drive.isConnected() } catch (e: Exception) { false }
            driveConnected = connected
            driveClientId = try { prefs.driveClientId() } catch (e: Exception) { "" }
            driveRedirectUri = try { prefs.driveRedirectUri() } catch (e: Exception) { "" }
            if (connected) {
                val folderId = try { drive.ensureFolder() } catch (e: Exception) { null }
                if (folderId != null) {
                    driveFiles = try { drive.listBackups(folderId) } catch (e: Exception) { emptyList() }
                }
            }
        }
    }

    fun runGuarded(action: () -> Unit) {
        scope.launch {
            val gOn = try { prefs.guardianMode() } catch (e: Exception) { false }
            val pOn = try { prefs.pinEnabled() } catch (e: Exception) { false }
            if (guardian.canAccessSensitive(pOn, gOn)) {
                action()
            } else {
                guardianPinNote = "Ye kaam karne se pehle Guardian PIN daalo."
                guardianPending = action
            }
        }
    }

    fun doBackup() {
        val pin = backupPin
        if (pin.length < 4) {
            note = "Backup PIN kam se kam 4 akshar ka rakho, phir dobara karo."
            return
        }
        if (pin != backupPinConfirm) {
            note = "Dono PIN ek jaise nahi hain — dobara milakar likho."
            return
        }
        busy = true
        progressText = "Backup taiyaar ho raha hai..."
        note = ""
        scope.launch {
            try {
                val bytes = manager.buildBackupBlob(pin)
                val fileName = FloraBackupCrypto.backupFileName(System.currentTimeMillis())
                progressText = "Drive par chadh raha hai..."
                val folderId = drive.ensureFolder()
                if (folderId == null) {
                    note = "Drive folder nahi mila — Drive dobara jodo aur phir koshish karo."
                    busy = false
                    progressText = ""
                    return@launch
                }
                val fileId = drive.upload(fileName, bytes, folderId) { sent, total ->
                    if (total > 0) {
                        progressText = "Drive par chadh raha hai... ${(sent * 100 / total)}%"
                    }
                }
                if (fileId == null) {
                    note = "Upload poora nahi hua — net check karke dobara koshish karo."
                    busy = false
                    progressText = ""
                    return@launch
                }
                val listed = drive.existsInListing(folderId, fileId)
                backupPin = ""
                backupPinConfirm = ""
                progressText = ""
                note = if (listed) {
                    "Backup ban gaya aur Drive par dikh raha hai: $fileName"
                } else {
                    "Upload hua hai, par Drive list me turant nahi dikha — Drive app me 'Flora Backups' folder check karo."
                }
                try { autoLog.add("Backup banaya (Drive)") } catch (e: Exception) { }
                refreshDriveState()
            } catch (e: BackupFailure) {
                note = e.message ?: "Backup nahi ban paya."
            } catch (e: Exception) {
                note = "Backup nahi ban paya — dobara koshish karo."
            }
            busy = false
            progressText = ""
        }
    }

    fun doRestore() {
        val file = selectedFile
        if (file == null) {
            note = "Pehle list se ek backup chuno."
            return
        }
        val pin = restorePin
        if (pin.isEmpty()) {
            note = "Backup kholne ke liye uska PIN daalo."
            return
        }
        busy = true
        progressText = "Backup Drive se aa raha hai..."
        note = ""
        scope.launch {
            try {
                val folderId = drive.ensureFolder()
                if (folderId == null) {
                    note = "Drive folder nahi mila — Drive dobara jodo aur phir koshish karo."
                    busy = false
                    progressText = ""
                    return@launch
                }
                val bytes = drive.download(file.id) { received, total ->
                    if (total > 0) {
                        progressText = "Backup aa raha hai... ${(received * 100 / total)}%"
                    }
                }
                if (bytes == null) {
                    note = "Backup download nahi hua — net check karke dobara koshish karo."
                    busy = false
                    progressText = ""
                    return@launch
                }
                progressText = "Backup lag raha hai..."
                val report = manager.restoreFromBlob(pin, bytes, deviceId())
                restorePin = ""
                selectedFile = null
                showRestoreConfirm = false
                val linkText = when {
                    report.linkRestored -> "App link dobara jud gaya hai."
                    report.linkLost -> "App link server se confirm nahi hua, isliye link hata diya gaya hai — Settings se dobara link karo."
                    else -> "App link is backup me nahi tha."
                }
                note = "Restore ho gaya. $linkText " +
                    (report.licenceMessage ?: "") + " " + report.licenceKeyNote +
                    if (report.romanticDowngraded) " Romantic mode backup me tha par is phone par 18+ confirm nahi mila, isliye friendly Flora mode par wapas aa gaya hai — Settings se 18+ confirm karke dobara ON kar sakte ho." else ""
                try { autoLog.add("Backup se restore hua") } catch (e: Exception) { }
                // Item 34 (D-I34-4): fail-closed downgrade chhupa
                // nahi — Auto Log me bhi saaf entry.
                if (report.romanticDowngraded) {
                    try { autoLog.add("Romantic mode restore par downgrade hua (flora) — 18+ confirm nahi mila") } catch (e: Exception) { }
                }
            } catch (e: BackupFailure) {
                note = e.message ?: "Restore nahi hua."
            } catch (e: Exception) {
                note = "Restore nahi hua — dobara koshish karo."
            }
            busy = false
            progressText = ""
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Backup / Restore")
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Apni chats, yaadein, journal, docs, projects, settings aur routines ka " +
                "backup Google Drive par rakho — naye phone par wapas laane ke liye. " +
                "Backup ek PIN se band hota hai: ye PIN kahin save nahi hota, isliye " +
                "bhool gaye to backup kabhi nahi khulega. Naya PIN sirf naye backup " +
                "par lagta hai; purana backup purane PIN se hi khulega."
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { refreshDriveState() }) { Text("Drive status check karo") }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (driveConnected) {
                "Drive juda hua hai."
            } else {
                "Drive abhi juda nahi hai — neeche se jodo, tabhi backup banega."
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Google Drive jodo (sirf backup files ke liye permission)")
        Text(
            "Google Cloud me Desktop app type ka OAuth client banao, phir uska " +
                "Client ID aur Redirect URI yahan daalo. Flora sirf apni backup " +
                "files dekh aur bana sakti hai — tumhari baaki Drive files nahi."
        )
        OutlinedTextField(
            value = driveClientId,
            onValueChange = { driveClientId = it },
            label = { Text("Google Client ID") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = driveRedirectUri,
            onValueChange = { driveRedirectUri = it },
            label = { Text("Redirect URI") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            scope.launch {
                prefs.setDriveClientId(driveClientId)
                prefs.setDriveRedirectUri(driveRedirectUri)
                if (driveClientId.isBlank() || driveRedirectUri.isBlank()) {
                    note = "Client ID aur Redirect URI dono daalo."
                    return@launch
                }
                val start = drive.authorizeStart(driveClientId.trim(), driveRedirectUri.trim())
                driveVerifier = start.verifier
                driveState = start.state
                driveLoginUrl = start.url
                showDriveLogin = true
            }
        }) { Text("Google se jodo") }
        Button(onClick = {
            scope.launch {
                prefs.clearDriveTokens()
                prefs.setDriveFolderId("")
                driveConnected = false
                driveFiles = emptyList()
                note = "Drive hata diya gaya hai."
            }
        }) { Text("Drive hatao") }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Naya backup banao")
        OutlinedTextField(
            value = backupPin,
            onValueChange = { backupPin = it },
            label = { Text("Backup PIN") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = backupPinConfirm,
            onValueChange = { backupPinConfirm = it },
            label = { Text("Backup PIN dobara") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = !busy && driveConnected,
            onClick = { runGuarded { doBackup() } }
        ) { Text("Backup banao (Drive par)") }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Backup wapas laao (Restore)")
        Text(
            "Restore ka matlab REPLACE hai: is phone ka purana data hat kar " +
                "backup wala aa jayega. Ye kadam soch kar uthao."
        )
        if (driveFiles.isEmpty()) {
            Text("Koi backup list me nahi dikha — pehle 'Drive status check karo' dabao.")
        } else {
            for (file in driveFiles) {
                Row {
                    Button(onClick = { selectedFile = file }) {
                        Text(if (selectedFile?.id == file.id) "✓ ${file.name}" else file.name)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
        OutlinedTextField(
            value = restorePin,
            onValueChange = { restorePin = it },
            label = { Text("Backup ka PIN") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            enabled = !busy && driveConnected && selectedFile != null,
            onClick = { showRestoreConfirm = true }
        ) { Text("Restore karo") }
        if (progressText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(progressText)
        }
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(note)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onClose) { Text("Wapas Settings par") }
    }

    if (guardianPending != null) {
        AlertDialog(
            onDismissRequest = {
                guardianPending = null
                guardianPinInput = ""
            },
            title = { Text("Guardian PIN") },
            text = {
                Column {
                    Text(guardianPinNote)
                    OutlinedTextField(
                        value = guardianPinInput,
                        onValueChange = { guardianPinInput = it },
                        label = { Text("PIN") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pending = guardianPending
                    val pin = guardianPinInput
                    guardianPending = null
                    guardianPinInput = ""
                    scope.launch {
                        if (pinVault.verify(pin)) {
                            guardian.markPinVerified()
                            pending?.invoke()
                        } else {
                            guardianPinNote = "PIN galat hai — dobara koshish karo."
                            guardianPending = pending
                        }
                    }
                }) { Text("Aage badho") }
            },
            dismissButton = {
                TextButton(onClick = {
                    guardianPending = null
                    guardianPinInput = ""
                }) { Text("Radd karo") }
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore pakka karein?") },
            text = {
                Text(
                    "Is phone ka purana data REPLACE ho jayega — chats, yaadein, " +
                        "journal, docs, projects aur settings ki jagah backup wala " +
                        "data aa jayega. Galat PIN par kuch bhi nahi badlega."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    runGuarded { doRestore() }
                }) { Text("Haan, restore karo") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Radd karo") }
            }
        )
    }

    if (showDriveLogin && driveLoginUrl.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDriveLogin = false },
            title = { Text("Google se jodo") },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.allowFileAccessFromFileURLs = false
                            settings.allowUniversalAccessFromFileURLs = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.safeBrowsingEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    val redirect = driveRedirectUri.trim()
                                    if (redirect.isEmpty() || !url.startsWith(redirect)) {
                                        return false
                                    }
                                    val code = BuilderOAuth.parseCodeFromRedirect(
                                        url, redirect, driveState
                                    )
                                    if (code != null) {
                                        showDriveLogin = false
                                        scope.launch {
                                            val ok = drive.exchangeCode(
                                                code, driveVerifier,
                                                driveClientId.trim(), redirect
                                            )
                                            note = if (ok) {
                                                "Drive jud gaya hai."
                                            } else {
                                                "Drive jud nahi paya — dobara koshish karo."
                                            }
                                            refreshDriveState()
                                        }
                                    } else {
                                        showDriveLogin = false
                                        note = "Google ne permission nahi di — dobara koshish karo."
                                    }
                                    return true
                                }
                            }
                            loadUrl(driveLoginUrl)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(480.dp)
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDriveLogin = false }) { Text("Band karo") }
            }
        )
    }
}
