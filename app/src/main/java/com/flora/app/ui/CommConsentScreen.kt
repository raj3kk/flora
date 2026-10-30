package com.flora.app.ui

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Switch
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
import com.flora.app.data.AutoLogStore
import com.flora.app.data.FloraPreferences
import com.flora.app.data.NotifItem
import com.flora.app.data.NotificationStore
import com.flora.app.data.PostEntry
import com.flora.app.data.PostHistoryStore
import com.flora.app.planner.TaskListStore
import kotlinx.coroutines.launch

/**
 * P2 consent-gated communication bundle (ONE item, 5
 * sub-actions) — app-side consent UI + gating.
 *
 * SILENT FORMS DROPPED (NEVER): silent auto-reply aur
 * silent auto-posting is bundle me kabhi nahi aayenge.
 * P2 ka koi action khud message nahi bhejta — hamesha
 * user ke app me prefilled composer khulta hai aur Send
 * user dabata hai (wa.me / mailto / share intent). Isliye
 * history me status kabhi "bhej diya" nahi kehta.
 *
 * COVERED — dobara NAHI banaya gaya:
 * - ReplyDraft ask-first (draft only, Send user dabata hai).
 * - WhatsApp read (notification list se aakhri WhatsApp
 *   messages — FloraApp me pehle se hai).
 * - wa.me user-confirmed sends (FloraApp me pehle se hai) —
 *   P2 sirf naya alert composer deta hai, send mechanism
 *   wahi covered raasta hai.
 *
 * ITEM 36 SE BOUNDARY: item 36 ka whatsapp-template-send
 * server relay hai (SD-P2 drafts, server domain) — P2 usse
 * kuch nahi banata: na template API, na Business Cloud API,
 * na koi server function. P2 ke saare WhatsApp sends app-side
 * user-confirmed (wa.me / share intent) hain.
 *
 * CONSENT MODEL: har sub-action ka apna flag hai (default
 * OFF). Flag OFF ho to action button kaam nahi karta —
 * pehle consent switch ON karni hogi (badlav Auto Log me).
 * Email/social actions par 8.12 ke master consent flags bhi
 * lagu hain (dono ON hone chahiye).
 *
 * EMAIL HONEST SCOPE: "Email read" = on-device notification
 * snippets (user ke email app se — Gmail/Outlook/mail),
 * read-only; poora Gmail API inbox is batch me NAHI hai
 * (uske liye alag Gmail OAuth spec + gate chahiye hoga).
 * "Email reply" = mailto prefilled composer (user Send
 * dabata hai).
 */
@Composable
fun CommConsentScreen(autoLogStore: AutoLogStore, onBack: () -> Unit) {
    val appContext = LocalContext.current
    val prefs = remember { FloraPreferences(appContext) }
    val taskStore = remember { TaskListStore(appContext) }
    val scope = rememberCoroutineScope()

    var note by remember { mutableStateOf("") }
    var waAlertConsent by remember { mutableStateOf(false) }
    var waGroupConsent by remember { mutableStateOf(false) }
    var emailReadConsent by remember { mutableStateOf(false) }
    var emailReplyConsent by remember { mutableStateOf(false) }
    var socialConsent by remember { mutableStateOf(false) }
    var masterEmailConsent by remember { mutableStateOf(false) }
    var masterSocialConsent by remember { mutableStateOf(false) }
    var waNumber by remember { mutableStateOf("") }
    var waText by remember { mutableStateOf("") }
    var mailTo by remember { mutableStateOf("") }
    var mailSubject by remember { mutableStateOf("") }
    var mailBody by remember { mutableStateOf("") }
    var postText by remember { mutableStateOf("") }
    var emailNotifs by remember { mutableStateOf(listOf<NotifItem>()) }
    var history by remember { mutableStateOf(listOf<PostEntry>()) }
    var armedAction by remember { mutableStateOf("") }

    fun log(msg: String) {
        scope.launch {
            try {
                autoLogStore.add(msg)
            } catch (e: Exception) {
            }
        }
    }

    fun record(action: String, target: String, snippet: String, status: String = "Bhejne ke liye khola gaya (user confirmation app me)") {
        scope.launch {
            try {
                PostHistoryStore.add(appContext, action, target, snippet, status)
            } catch (e: Exception) {
            }
        }
    }

    fun refresh() {
        scope.launch {
            try {
                waAlertConsent = prefs.p2WaAlert()
                waGroupConsent = prefs.p2WaGroup()
                emailReadConsent = prefs.p2EmailRead()
                emailReplyConsent = prefs.p2EmailReply()
                socialConsent = prefs.p2SocialPost()
                masterEmailConsent = prefs.sd8EmailConsent()
                masterSocialConsent = prefs.sd8SocialConsent()
                history = PostHistoryStore.list(appContext)
            } catch (e: Exception) {
                note = "Communication settings padhne me dikkat aayi — dobara kholo."
            }
        }
    }

    fun setConsent(kind: String, value: Boolean) {
        scope.launch {
            try {
                when (kind) {
                    "waAlert" -> prefs.setP2WaAlert(value)
                    "waGroup" -> prefs.setP2WaGroup(value)
                    "emailRead" -> prefs.setP2EmailRead(value)
                    "emailReply" -> prefs.setP2EmailReply(value)
                    "social" -> prefs.setP2SocialPost(value)
                }
                log("P2: consent $kind = $value")
                refresh()
            } catch (e: Exception) {
                note = "Consent save nahi hui — dobara koshish karo."
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun openWaAlert() {
        val digits = waNumber.filter { it.isDigit() }
        if (digits.length < 10) {
            note = "WhatsApp alert ke liye poora number chahiye (10 digit ya country code ke saath)."
            return
        }
        if (waText.isBlank()) {
            note = "Alert ka text khaali hai."
            return
        }
        try {
            val url = "https://wa.me/$digits?text=" + Uri.encode(waText.trim())
            appContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            record("WhatsApp alert", digits.takeLast(4).let { "••••$it" }, waText.trim())
            log("P2: WhatsApp alert khola gaya (user Send dabayega)")
            note = "WhatsApp khul gaya hai — message likha hua milega, Send tum dabao."
        } catch (e: Exception) {
            note = "WhatsApp khul nahi paya — kya WhatsApp installed hai?"
        }
    }

    fun openGroupReport() {
        scope.launch {
            try {
                val tasks = taskStore.list()
                val dayMs = 24L * 60L * 60L * 1000L
                val now = System.currentTimeMillis()
                val doneToday = tasks.count { it.done && it.createdMs > now - dayMs }
                val doneAll = tasks.count { it.done }
                val report = "Flora group report — aaj poore hue tasks: $doneToday, kul poore tasks: $doneAll, kul tasks: ${tasks.size}." +
                    if (doneToday > 0) {
                        " Aaj poore: " + tasks.filter { it.done && it.createdMs > now - dayMs }.take(10).joinToString(", ") { it.title.take(30) }
                    } else {
                        ""
                    }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    intent.setPackage("com.whatsapp")
                    appContext.startActivity(intent)
                } catch (e: Exception) {
                    intent.setPackage(null)
                    appContext.startActivity(Intent.createChooser(intent, "Report kahan bhejna hai?").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                record("WhatsApp group report", "WhatsApp (group chuna user ne)", report)
                log("P2: group report share khola gaya (user group chunega + Send dabayega)")
                note = "Report share khul gaya hai — WhatsApp me group chuno aur Send dabao."
            } catch (e: Exception) {
                note = "Group report ban nahi payi — dobara koshish karo."
            }
        }
    }

    fun loadEmailNotifs() {
        scope.launch {
            try {
                val all = NotificationStore.load(appContext)
                val mailPkgs = setOf(
                    "com.google.android.gm",
                    "com.microsoft.office.outlook",
                    "com.yahoo.mobile.client.android.mail",
                    "com.samsung.android.email",
                )
                emailNotifs = all.filter { it.pkg in mailPkgs || "mail" in it.pkg.lowercase() }.take(20)
                record("Email read", "On-device notifications", "Email snippets dekhe (${emailNotifs.size})", "Dekha gaya (read-only, notification snippet)")
                log("P2: email notification snippets dekhe (${emailNotifs.size})")
                note = if (emailNotifs.isEmpty()) {
                    "Email app ki koi notification save nahi mili — notification access ON ho to aane wali mails yahan dikhengi."
                } else {
                    "Email snippets (${emailNotifs.size}) — ye sirf notification text hai, poora inbox nahi."
                }
            } catch (e: Exception) {
                note = "Email snippets padhne me dikkat aayi."
            }
        }
    }

    fun openMailReply() {
        if (mailTo.isBlank() || "@" !in mailTo) {
            note = "Email reply ke liye sahi email address chahiye."
            return
        }
        if (mailBody.isBlank()) {
            note = "Email ka text khaali hai."
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + mailTo.trim())).apply {
                putExtra(Intent.EXTRA_SUBJECT, mailSubject.trim())
                putExtra(Intent.EXTRA_TEXT, mailBody.trim())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            record("Email reply", mailTo.trim(), mailBody.trim())
            log("P2: email reply composer khola gaya (user Send dabayega)")
            note = "Email app khul gayi hai — Send tum dabao."
        } catch (e: Exception) {
            note = "Email app khul nahi payi."
        }
    }

    fun openSocialPost() {
        if (postText.isBlank()) {
            note = "Post ka text khaali hai."
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, postText.trim())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val igInstalled = try {
                appContext.packageManager.getLaunchIntentForPackage("com.instagram.android") != null
            } catch (e: Exception) {
                false
            }
            if (igInstalled) {
                intent.setPackage("com.instagram.android")
                appContext.startActivity(intent)
            } else {
                appContext.startActivity(Intent.createChooser(intent, "Post kahan karna hai?").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
            record("Social post", "Instagram/chooser (user ne chuna)", postText.trim())
            log("P2: social post share khola gaya (user app me post karega)")
            note = "Share khul gaya hai — app me post confirm tum karo. Flora khud kabhi silent post nahi karti."
        } catch (e: Exception) {
            note = "Share khul nahi paya."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Row {
            TextButton(onClick = onBack) { Text("Wapas") }
        }
        Text("Communication — consent-gated (silent kabhi nahi)")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Har action ke liye pehle consent ON karni hogi, phir har baar Confirm dabana hoga. " +
                "Flora khud kabhi message/post nahi bhejti — hamesha tumhare app me composer khulta hai aur Send tum dabate ho.",
        )
        if (note.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(note)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("1. WhatsApp alert")
        Row {
            Text("WhatsApp alert ki consent", modifier = Modifier.weight(1f))
            Switch(checked = waAlertConsent, onCheckedChange = { setConsent("waAlert", it) })
        }
        OutlinedTextField(value = waNumber, onValueChange = { waNumber = it }, label = { Text("Number (country code ke saath)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = waText, onValueChange = { waText = it }, label = { Text("Alert text") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            if (!waAlertConsent) {
                note = "Pehle WhatsApp alert ki consent ON karo."
            } else if (armedAction != "waAlert") {
                armedAction = "waAlert"
                note = "Pakka ye alert WhatsApp par kholna hai? Confirm karne ke liye dobara dabao."
            } else {
                armedAction = ""
                openWaAlert()
            }
        }) { Text(if (armedAction == "waAlert") "Pakka kholo" else "Alert kholo (wa.me)") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("2. WhatsApp group report")
        Row {
            Text("Group report ki consent", modifier = Modifier.weight(1f))
            Switch(checked = waGroupConsent, onCheckedChange = { setConsent("waGroup", it) })
        }
        Text("Report aaj/kul poore tasks se banegi. Group tum WhatsApp me chunoge.")
        Button(onClick = {
            if (!waGroupConsent) {
                note = "Pehle group report ki consent ON karo."
            } else if (armedAction != "waGroup") {
                armedAction = "waGroup"
                note = "Pakka group report share karni hai? Confirm karne ke liye dobara dabao."
            } else {
                armedAction = ""
                openGroupReport()
            }
        }) { Text(if (armedAction == "waGroup") "Pakka share karo" else "Group report banao + share karo") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("3. Email read (notification snippets, read-only)")
        Row {
            Text("Email read ki consent", modifier = Modifier.weight(1f))
            Switch(checked = emailReadConsent, onCheckedChange = { setConsent("emailRead", it) })
        }
        Text("Master email consent (8.12): ${if (masterEmailConsent) "ON" else "OFF — email actions ke liye ye bhi ON hona chahiye"}")
        Button(onClick = {
            if (!emailReadConsent || !masterEmailConsent) {
                note = "Email read ke liye dono consent ON hone chahiye (ye wali + 8.12 master)."
            } else {
                loadEmailNotifs()
            }
        }) { Text("Email snippets dekho") }
        emailNotifs.forEach { item ->
            Text("- ${item.title.take(60)}: ${item.text.take(80)}")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("4. Email reply (mailto composer)")
        Row {
            Text("Email reply ki consent", modifier = Modifier.weight(1f))
            Switch(checked = emailReplyConsent, onCheckedChange = { setConsent("emailReply", it) })
        }
        OutlinedTextField(value = mailTo, onValueChange = { mailTo = it }, label = { Text("To (email)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = mailSubject, onValueChange = { mailSubject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = mailBody, onValueChange = { mailBody = it }, label = { Text("Email text") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            if (!emailReplyConsent || !masterEmailConsent) {
                note = "Email reply ke liye dono consent ON hone chahiye (ye wali + 8.12 master)."
            } else if (armedAction != "mailReply") {
                armedAction = "mailReply"
                note = "Pakka ye email composer kholna hai? Confirm karne ke liye dobara dabao."
            } else {
                armedAction = ""
                openMailReply()
            }
        }) { Text(if (armedAction == "mailReply") "Pakka kholo" else "Email composer kholo") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("5. Social post (kabhi silent nahi)")
        Row {
            Text("Social post ki consent", modifier = Modifier.weight(1f))
            Switch(checked = socialConsent, onCheckedChange = { setConsent("social", it) })
        }
        Text("Master social consent (8.12): ${if (masterSocialConsent) "ON" else "OFF — social actions ke liye ye bhi ON hona chahiye"}")
        OutlinedTextField(value = postText, onValueChange = { postText = it }, label = { Text("Post text") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            if (!socialConsent || !masterSocialConsent) {
                note = "Social post ke liye dono consent ON hone chahiye (ye wali + 8.12 master)."
            } else if (armedAction != "social") {
                armedAction = "social"
                note = "Pakka ye post share karni hai? App me tum khud post confirm karoge — dobara dabao."
            } else {
                armedAction = ""
                openSocialPost()
            }
        }) { Text(if (armedAction == "social") "Pakka share karo" else "Post share karo") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Post history (${history.size}) — sirf snippets, poora text nahi:")
        history.forEach { entry ->
            Text("- ${entry.action} • ${entry.target.take(40)} • ${entry.status.take(40)}")
        }
        if (history.isEmpty()) Text("Abhi koi P2 action history me nahi hai.")
        Spacer(modifier = Modifier.height(16.dp))
    }
}
