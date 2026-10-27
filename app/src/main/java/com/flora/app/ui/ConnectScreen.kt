package com.flora.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Connect screen (v0.6): user-confirmed sharing, dialling, and SMS
 * drafts — all through the phone's own apps. Flora NEVER posts,
 * calls, or texts silently: Share opens the system share sheet,
 * Dial opens the dialer, SMS opens the SMS app with a draft.
 * The user presses Post / Call / Send themselves, every time.
 * Incoming-call announcement and auto-reply are NOT in v0.6.
 */
@Composable
fun ConnectScreen() {
    val context = LocalContext.current
    var shareText by remember { mutableStateOf("") }
    var callNumber by remember { mutableStateOf("") }
    var smsNumber by remember { mutableStateOf("") }
    var smsText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("Share — aap khud app chun kar post karenge")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = shareText, onValueChange = { shareText = it },
                label = { Text("Share karne wala text") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (shareText.isBlank()) {
                    note = "Pehle share karne wala text likhiye."
                } else {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Kahan share karna hai?"))
                }
            }) { Text("Share sheet kholo") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Call — dialer khulega, call aap khud lagayenge")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = callNumber, onValueChange = { callNumber = it },
                label = { Text("Mobile number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (callNumber.isBlank()) {
                    note = "Pehle number likhiye."
                } else {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${callNumber.trim()}"))
                    )
                }
            }) { Text("Dialer kholo") }
            Spacer(modifier = Modifier.height(16.dp))
            Text("SMS draft — SMS app khulegi, Send aap khud dabayenge")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = smsNumber, onValueChange = { smsNumber = it },
                label = { Text("Mobile number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = smsText, onValueChange = { smsText = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (smsNumber.isBlank()) {
                    note = "Pehle number likhiye."
                } else {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${smsNumber.trim()}")).apply {
                            putExtra("sms_body", smsText)
                        }
                    )
                }
            }) { Text("SMS draft kholo") }
            if (note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Flora kabhi chupke se post, call ya SMS nahi karti — har action aapke haath se hota hai.")
        }
    }
}
