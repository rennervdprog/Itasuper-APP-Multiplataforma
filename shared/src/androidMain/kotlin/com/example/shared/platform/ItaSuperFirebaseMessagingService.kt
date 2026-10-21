package com.example.shared.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Serviço FCM — porte completo do ItaSuperFirebaseMessagingService +
 * PushNotificationManager.showOrderNotification do app nativo
 * (rennervdprog/Itasuper-APP-NATIVO).
 *
 * Antes este serviço só escrevia no log: token novo não era registrado no
 * backend e mensagem recebida com o app aberto nunca virava notificação.
 * Agora: onNewToken dispara o registro do aparelho e onMessageReceived
 * exibe a notificação no canal "itasuper_orders".
 */
class ItaSuperFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Igual ao original: token novo (ou renovado) é registrado no backend
        // vinculado à conta logada.
        PushRegistration.syncAsync()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "ItaSuper"
        val body = message.notification?.body ?: data["body"] ?: "Há uma atualização no seu pedido."
        showOrderNotification(title, body, data)
    }

    /** Porte de PushNotificationManager.showOrderNotification do original. */
    private fun showOrderNotification(title: String, body: String, data: Map<String, String>) {
        createOrderNotificationChannel()
        if (!hasNotificationPermission()) return

        val orderId = data["order_id"]
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            orderId?.let { putExtra(EXTRA_ORDER_ID, it) }
        } ?: return
        val pendingIntent = PendingIntent.getActivity(
            this,
            orderId?.hashCode() ?: title.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ORDER_CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(title.ifBlank { "ItaSuper" })
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(this)
            .notify(orderId?.hashCode() ?: title.hashCode(), notification)
    }

    private fun createOrderNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ORDER_CHANNEL_ID,
            "Atualizações de pedidos",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos sobre preparo, entrega e conclusão dos seus pedidos"
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val ORDER_CHANNEL_ID = "itasuper_orders"
        const val EXTRA_ORDER_ID = "order_id"
    }
}
