package com.example.shared.platform

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Adaptação mínima do `ItaSuperFirebaseMessagingService` do app nativo
 * (rennervdprog/Itasuper-APP-NATIVO) para o módulo shared (Fase 5).
 *
 * Recebe mensagens FCM do backend de pedidos. A exibição da notificação
 * (NotificationManager) e o envio do token ao backend ficam como TODO.
 */
class ItaSuperFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM recebido (TODO fase 5: registrar token no backend)")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "ItaSuper"
        val body = message.notification?.body ?: data["body"] ?: "Há uma atualização no seu pedido."
        Log.d(TAG, "Push recebido: $title — $body (TODO fase 5: exibir notificação do pedido)")
    }

    private companion object {
        const val TAG = "ItaSuperPush"
    }
}
