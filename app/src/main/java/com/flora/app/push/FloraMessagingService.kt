package com.flora.app.push

// FCM receive + token (v2.2, wired): Firebase project flora-f22a7,
// Android app package com.flora.app (google-services.json app/ me).
// Behaviour: push sirf ek saaf notification dikhata hai — koi silent
// action, koi silent automation, koi data collection nahi. Token sirf
// is phone par DataStore me save hota hai; kabhi log/chat/backup me
// nahi jata. Server ko token sync tabhi hoga jab push_tokens table
// aur server-side send function taiyaar ho (alag step).

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.flora.app.MainActivity
import com.flora.app.data.FloraPreferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FloraMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            try {
                FloraPreferences(applicationContext).setFcmToken(token)
            } catch (e: Exception) {
                // Token save fail: agle app-open par dobara fetch hoga.
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: "Flora"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Flora se naya message hai."
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "flora_push"
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Flora notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(1001, notification)
    }
}
