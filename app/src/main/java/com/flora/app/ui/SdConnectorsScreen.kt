package com.flora.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flora.app.data.AutoLogStore
import com.flora.app.data.BuilderService
import com.flora.app.data.FloraPreferences
import com.flora.app.tools.BuilderOutcome
import com.flora.app.tools.LinearClient
import com.flora.app.tools.SdResult
import com.flora.app.tools.TelegramClient
import com.flora.app.tools.TodoistClient
import com.flora.app.tools.VercelClient
import kotlinx.coroutines.launch

/**
 * SD-P1 Batch 2 (SD-1 app clients + UI, Lane A) — Telegram bot,
 * Todoist, Linear, Vercel (connection check), seedha app se.
 *
 * Per-connector pattern (plan §3 SD-1): user khud connect kare
 * (apni key ka secure paste — poori key kabhi screen/chat/log
 * par nahi, sirf aakhri 4 akshar masked roop me); har action
 * se pehle user ka button-tap hi consent hai; har action ka
 * natija Auto Log me jata hai; disconnect/revoke hamesha
 * maujood hai (revoke par stored credential mita diya jata
 * hai); not-connected / API down par imaandaar message, app
 * local mode me chalti hai.
 *
 * DRIVE IS SCREEN PAR SIRF DISABLED NOTE HAI: Drive item 14
 * Drive OAuth (user-gated) ka intezar kar raha hai — uske bina
 * Drive ka koi action is batch me nahi hai.
 *
 * VERCEL NOTE: is screen par sirf connection check hai (B3
 * Builder Connect wala token slot hi istemal hota hai — alag
 * SD-1 Vercel token slot nahi banaya gaya, taaki do token
 * ek-dusre se takrayen nahi). Deploy trigger is batch me HELD
 * hai: placeholder file ka deploy user ki live site badal
 * sakta hai, isliye deploy Builder screen ke maujooda flow
 * se hi hoga — yahan se koi deploy trigger nahi chalega.
 */
@Composable
fun SdConnectorsScreen(autoLogStore: AutoLogStore, onBack: () -> Unit) {
    val appContext = LocalContext.current
    val prefs = remember { FloraPreferences(appContext) }
    val scope = rememberCoroutineScope()

    var note by remember { mutableStateOf("") }
    var tgConnected by remember { mutableStateOf(false) }
    var tgMask by remember { mutableStateOf("") }
    var tgPaste by remember { mutableStateOf("") }
    var tgChatId by remember { mutableStateOf("") }
    var tgMsg by remember { mutableStateOf("") }
    var tdConnected by remember { mutableStateOf(false) }
    var tdMask by remember { mutableStateOf("") }
    var tdPaste by remember { mutableStateOf("") }
    var tdContent by remember { mutableStateOf("") }
    var tdTasks by remember { mutableStateOf(listOf<TodoistClient.TodoTask>()) }
    var lnConnected by remember { mutableStateOf(false) }
    var lnMask by remember { mutableStateOf("") }
    var lnPaste by remember { mutableStateOf("") }
    var lnTeamId by remember { mutableStateOf("") }
    var lnTitle by remember { mutableStateOf("") }
    var lnIssues by remember { mutableStateOf(listOf<LinearClient.Issue>()) }
    var vcConnected by remember { mutableStateOf(false) }
    var vcName by remember { mutableStateOf("") }

    fun masked(token: String): String =
        if (token.length <= 4) "••••" else "••••" + token.takeLast(4)

    fun log(msg: String) {
        scope.launch {
            try {
                autoLogStore.add(msg)
            } catch (e: Exception) {
            }
        }
    }

    fun refreshFlags() {
        scope.launch {
            try {
                val tg = prefs.sdTelegramToken()
                tgConnected = tg.isNotBlank()
                tgMask = if (tg.isNotBlank()) masked(tg) else ""
                tgChatId = prefs.sdTelegramChatId()
                val td = prefs.sdTodoistToken()
                tdConnected = td.isNotBlank()
                tdMask = if (td.isNotBlank()) masked(td) else ""
                val ln = prefs.sdLinearToken()
                lnConnected = ln.isNotBlank()
                lnMask = if (ln.isNotBlank()) masked(ln) else ""
                lnTeamId = prefs.sdLinearTeamId()
                vcConnected = prefs.builderConnected(BuilderService.VERCEL)
            } catch (e: Exception) {
                note = "Status padhne me dikkat aayi — dobara kholo."
            }
        }
    }

    LaunchedEffect(Unit) { refreshFlags() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row {
            TextButton(onClick = onBack) { Text("Wapas") }
        }
        Text("SD-1 Connectors — app se seedha")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Telegram, Todoist, Linear: apni key paste karke connect karo. " +
                "Key sirf isi phone par KeyStore-encrypted rehti hai, kabhi server/log/chat par nahi jati. " +
                "Har action se pehle tumhara button-tap hi consent hai, aur har action Auto Log me likha jata hai.",
        )
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Telegram bot")
        Text(if (tgConnected) "Connected ($tgMask)" else "Connected nahi hai")
        OutlinedTextField(
            value = tgPaste, onValueChange = { tgPaste = it },
            label = { Text("Bot token paste karo") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Button(onClick = {
                scope.launch {
                    val token = tgPaste.trim()
                    if (token.isEmpty()) {
                        note = "Telegram token khaali hai."
                    } else if (prefs.setSdTelegramToken(token)) {
                        tgPaste = ""
                        log("SD-1: Telegram connected")
                        note = "Telegram connected."
                        refreshFlags()
                    } else {
                        note = "Telegram token save nahi hua — dobara koshish karo."
                    }
                }
            }) { Text("Connect") }
            TextButton(onClick = {
                scope.launch {
                    prefs.clearSdTelegramToken()
                    log("SD-1: Telegram disconnected (token mitaya gaya)")
                    note = "Telegram disconnected — token mita diya gaya."
                    refreshFlags()
                }
            }) { Text("Disconnect") }
        }
        OutlinedTextField(
            value = tgChatId, onValueChange = { tgChatId = it },
            label = { Text("Chat id (jahan message jaye)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = tgMsg, onValueChange = { tgMsg = it },
            label = { Text("Test message") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            scope.launch {
                val token = prefs.sdTelegramToken()
                if (token.isBlank()) {
                    note = "Pehle Telegram connect karo."
                } else {
                    prefs.setSdTelegramChatId(tgChatId.trim())
                    when (val r = TelegramClient.sendMessage(token, tgChatId.trim(), tgMsg.trim())) {
                        is SdResult.Ok -> {
                            log("SD-1: Telegram message bheja gaya")
                            note = "Telegram message bhej diya gaya."
                        }
                        is SdResult.Failure -> note = "Telegram message nahi gaya: ${r.reason}"
                    }
                }
            }
        }) { Text("Test message bhejo") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Todoist")
        Text(if (tdConnected) "Connected ($tdMask)" else "Connected nahi hai")
        OutlinedTextField(
            value = tdPaste, onValueChange = { tdPaste = it },
            label = { Text("Todoist API token paste karo") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Button(onClick = {
                scope.launch {
                    val token = tdPaste.trim()
                    if (token.isEmpty()) {
                        note = "Todoist token khaali hai."
                    } else if (prefs.setSdTodoistToken(token)) {
                        tdPaste = ""
                        log("SD-1: Todoist connected")
                        note = "Todoist connected."
                        refreshFlags()
                    } else {
                        note = "Todoist token save nahi hua — dobara koshish karo."
                    }
                }
            }) { Text("Connect") }
            TextButton(onClick = {
                scope.launch {
                    prefs.clearSdTodoistToken()
                    log("SD-1: Todoist disconnected (token mitaya gaya)")
                    note = "Todoist disconnected — token mita diya gaya."
                    refreshFlags()
                }
            }) { Text("Disconnect") }
        }
        OutlinedTextField(
            value = tdContent, onValueChange = { tdContent = it },
            label = { Text("Naya task") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Button(onClick = {
                scope.launch {
                    val token = prefs.sdTodoistToken()
                    if (token.isBlank()) {
                        note = "Pehle Todoist connect karo."
                    } else {
                        when (val r = TodoistClient.createTask(token, tdContent.trim())) {
                            is SdResult.Ok -> {
                                tdContent = ""
                                log("SD-1: Todoist task banaya: ${r.value.content.take(60)}")
                                note = "Todoist task ban gaya."
                            }
                            is SdResult.Failure -> note = "Todoist task nahi bana: ${r.reason}"
                        }
                    }
                }
            }) { Text("Task banao") }
            TextButton(onClick = {
                scope.launch {
                    val token = prefs.sdTodoistToken()
                    if (token.isBlank()) {
                        note = "Pehle Todoist connect karo."
                    } else {
                        when (val r = TodoistClient.listTasks(token)) {
                            is SdResult.Ok -> {
                                tdTasks = r.value
                                log("SD-1: Todoist tasks dekhe (${r.value.size})")
                                note = "Todoist tasks: ${r.value.size} mile."
                            }
                            is SdResult.Failure -> note = "Todoist tasks nahi mile: ${r.reason}"
                        }
                    }
                }
            }) { Text("Tasks dekho") }
        }
        tdTasks.forEach { task ->
            Row {
                Text(task.content.take(60), modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    scope.launch {
                        val token = prefs.sdTodoistToken()
                        when (val r = TodoistClient.completeTask(token, task.id)) {
                            is SdResult.Ok -> {
                                log("SD-1: Todoist task complete: ${task.content.take(60)}")
                                tdTasks = tdTasks.filter { it.id != task.id }
                                note = "Task complete ho gaya."
                            }
                            is SdResult.Failure -> note = "Task complete nahi hua: ${r.reason}"
                        }
                    }
                }) { Text("Complete") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Linear")
        Text(if (lnConnected) "Connected ($lnMask)" else "Connected nahi hai")
        OutlinedTextField(
            value = lnPaste, onValueChange = { lnPaste = it },
            label = { Text("Linear API key paste karo") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Button(onClick = {
                scope.launch {
                    val token = lnPaste.trim()
                    if (token.isEmpty()) {
                        note = "Linear key khaali hai."
                    } else if (prefs.setSdLinearToken(token)) {
                        lnPaste = ""
                        log("SD-1: Linear connected")
                        note = "Linear connected."
                        refreshFlags()
                    } else {
                        note = "Linear key save nahi hui — dobara koshish karo."
                    }
                }
            }) { Text("Connect") }
            TextButton(onClick = {
                scope.launch {
                    prefs.clearSdLinearToken()
                    log("SD-1: Linear disconnected (key mitayi gayi)")
                    note = "Linear disconnected — key mita di gayi."
                    refreshFlags()
                }
            }) { Text("Disconnect") }
        }
        OutlinedTextField(
            value = lnTeamId, onValueChange = { lnTeamId = it },
            label = { Text("Team id (issue banane ke liye)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = lnTitle, onValueChange = { lnTitle = it },
            label = { Text("Naya issue title") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            Button(onClick = {
                scope.launch {
                    val token = prefs.sdLinearToken()
                    if (token.isBlank()) {
                        note = "Pehle Linear connect karo."
                    } else {
                        prefs.setSdLinearTeamId(lnTeamId.trim())
                        when (val r = LinearClient.createIssue(token, lnTeamId.trim(), lnTitle.trim())) {
                            is SdResult.Ok -> {
                                lnTitle = ""
                                log("SD-1: Linear issue banaya: ${r.value.title.take(60)}")
                                note = "Linear issue ban gaya."
                            }
                            is SdResult.Failure -> note = "Linear issue nahi bana: ${r.reason}"
                        }
                    }
                }
            }) { Text("Issue banao") }
            TextButton(onClick = {
                scope.launch {
                    val token = prefs.sdLinearToken()
                    if (token.isBlank()) {
                        note = "Pehle Linear connect karo."
                    } else {
                        when (val r = LinearClient.listIssues(token)) {
                            is SdResult.Ok -> {
                                lnIssues = r.value
                                log("SD-1: Linear issues dekhe (${r.value.size})")
                                note = "Linear issues: ${r.value.size} mile."
                            }
                            is SdResult.Failure -> note = "Linear issues nahi mile: ${r.reason}"
                        }
                    }
                }
            }) { Text("Issues dekho") }
        }
        lnIssues.forEach { issue ->
            Text(issue.title.take(80))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Vercel deploy")
        Text(if (vcConnected) "Builder Connect se Vercel connected hai" else "Vercel connected nahi hai — pehle Builder Connect me Vercel connect karo")
        Button(onClick = {
            scope.launch {
                when (val r = VercelClient(prefs).whoAmI()) {
                    is BuilderOutcome.Ok -> {
                        vcName = r.value
                        log("SD-1: Vercel connection check ok (${r.value})")
                        note = "Vercel connection theek hai (${r.value})."
                    }
                    is BuilderOutcome.Failure -> note = "Vercel connection check fail: ${r.detail}"
                }
            }
        }) { Text("Connection check") }
        if (vcName.isNotEmpty()) Text("Vercel user: $vcName")
        Text(
            "Deploy trigger is screen se HELD hai: placeholder file ka deploy tumhari live site badal sakta hai. " +
                "Deploy Builder screen ke maujooda flow se hi hoga.",
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Google Drive — abhi band hai")
        Text(
            "Drive item 14 Drive OAuth (user-gated) ka intezar kar raha hai. " +
                "Uske bina Drive ka koi action yahan nahi chalega — ye button jaanboojh kar disabled hai.",
        )
        Button(onClick = {}, enabled = false) { Text("Drive (band hai)") }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
