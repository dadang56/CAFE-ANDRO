package com.lavana.dapoer.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lavana.dapoer.MainActivity
import com.lavana.dapoer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "dapoer_lavana_orders"
private const val PREFS_NAME = "lavana_prefs"

/**
 * Nomor unik untuk tiap notifikasi supaya tidak saling menimpa di status bar.
 * Disimpan sebagai counter sederhana per proses (cukup untuk kebutuhan app ini).
 */
private var notificationIdCounter = 1000

class DapoerFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        registerFcmToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Dapoer Lavana"
        val body = message.notification?.body ?: message.data["body"] ?: return
        showNotification(applicationContext, title, body)
    }

    private fun showNotification(context: Context, title: String, body: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status Pesanan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi status pesanan Dapoer Lavana"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            manager.notify(notificationIdCounter++, notification)
        } catch (e: SecurityException) {
            // Izin POST_NOTIFICATIONS belum diberikan (Android 13+) -- abaikan.
        }
    }
}

/**
 * Ambil token FCM perangkat saat ini dan simpan/perbarui di Supabase, terhubung
 * ke identitas yang sedang login (Customer/Admin/Driver). Dipanggil setelah
 * login berhasil dan saat sesi dipulihkan otomatis di MainActivity.
 */
fun registerCurrentFcmToken(context: Context) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            registerFcmToken(context, task.result)
        }
    }
}

private fun registerFcmToken(context: Context, token: String) {
    val staff = CartManager.currentStaff
    val ownerId: String
    val role: String
    if (staff != null) {
        ownerId = staff.id ?: return
        role = staff.role
    } else {
        ownerId = SupabaseClient.currentUserId ?: return
        role = "Customer"
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            SupabaseClient.db["fcm_tokens"].upsert(
                FcmToken(ownerId = ownerId, role = role, token = token),
                onConflict = "token"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Hapus token FCM perangkat ini dari Supabase saat logout, supaya perangkat
 * bekas tidak terus menerima notifikasi milik akun lama.
 */
fun unregisterCurrentFcmToken() {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val token = task.result
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    SupabaseClient.db["fcm_tokens"].delete {
                        filter { eq("token", token) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
