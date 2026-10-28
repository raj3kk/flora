package com.flora.app.ui

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * v2.12 ka permission gate — v2.13 me SetupWizard ne isko replace
 * kar diya hai (3 steps, per-item tap, settings fallback). Ye file
 * ab use nahi hoti; push helper me delete support nahi hai, isliye
 * file repo me padi hai — dobara use mat karo.
 */
 * Permission gate (v2.12): app pehli baar khulte hi sab zaroori
 * permissions ek saath, ek screen par, kaaran ke saath maange
 * jaate hain — phir hi aage badha jata hai. Deny ki hui
 * permission sirf uska feature band karti hai, poora app nahi.
 * Accessibility, dialer role aur battery setting is gate me
 * nahi hain — wo hamesha user ki marzi se, Settings me, alag
 * se milenge (kabhi chhupke ya zabardasti nahi).
 */
data class GatePermission(val permission: String, val label: String, val why: String)

val FLORA_GATE_PERMISSIONS = listOf(
    GatePermission(
        Manifest.permission.RECORD_AUDIO,
        "Mic",
        "Baat karne aur 'Hey Flora' sunne ke liye",
    ),
    GatePermission(
        Manifest.permission.CAMERA,
        "Camera",
        "Scan aur photo ke liye",
    ),
    GatePermission(
        Manifest.permission.CALL_PHONE,
        "Call",
        "Awaaz se call lagane ke liye (sirf tumhare ek confirmation ke baad)",
    ),
    GatePermission(
        Manifest.permission.SEND_SMS,
        "SMS",
        "Awaaz se SMS bhejne ke liye (sirf tumhare ek confirmation ke baad)",
    ),
    GatePermission(
        Manifest.permission.POST_NOTIFICATIONS,
        "Notifications",
        "Push aur Wake Word ki notification dikhane ke liye — bina iske koi notification nahi aayega",
    ),
    GatePermission(
        Manifest.permission.READ_PHONE_STATE,
        "Phone state",
        "Aane wali call ka number bol kar batane ke liye",
    ),
)

@Composable
fun PermissionGateScreen(
    granted: (String) -> Boolean,
    onRequestAll: () -> Unit,
    onProceed: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Pehle permissions — phir Flora")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Flora ko ye permissions chahiye taaki mic, camera, " +
                    "call, SMS aur notifications sahi se chalein. Jo " +
                    "permission deny karoge, sirf uska feature nahi " +
                    "chalega — baaki app chalegi. Baad me Settings se " +
                    "koi bhi permission dobara de sakte ho."
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        items(FLORA_GATE_PERMISSIONS) { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.label)
                    Text(item.why)
                }
                Text(if (granted(item.permission)) "Mila hua ✓" else "Nahi mila")
            }
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRequestAll, modifier = Modifier.fillMaxWidth()) {
                Text("Sab permission maango")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onProceed, modifier = Modifier.fillMaxWidth()) {
                Text("Aage badho")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Note: Accessibility, dialer role aur battery setting " +
                    "is gate me nahi hain — wo Settings me, tumhari " +
                    "marzi se, alag se milenge."
            )
        }
    }
}
