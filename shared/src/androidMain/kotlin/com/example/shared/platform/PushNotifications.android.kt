package com.example.shared.platform

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual object PushNotifications {

    actual fun register() {
        // Força a inicialização preguiçosa do FirebaseMessaging. Sem o
        // google-services.json configurado no app, o token fica indisponível
        // (getToken retorna null) — TODO fase 5: adicionar google-services.
        try {
            FirebaseMessaging.getInstance()
        } catch (_: Exception) {
            // Firebase não configurado: push desabilitado silenciosamente.
        }
    }

    actual suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        try {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (continuation.isActive) continuation.resume(token)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        } catch (_: Exception) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
