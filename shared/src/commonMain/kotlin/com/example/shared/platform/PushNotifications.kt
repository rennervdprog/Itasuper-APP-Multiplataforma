package com.example.shared.platform

/**
 * Notificações push — expect/actual (Fase 5).
 *
 * Android: usa Firebase Cloud Messaging (mesmo backend do app nativo).
 * iOS: stub — loga TODO (push nativo no iOS fica para uma fase futura).
 */
expect object PushNotifications {

    /**
     * Registra o app para receber push (inicializa o provedor da plataforma).
     * Seguro chamar mesmo sem credenciais/configuração: o token só fica indisponível.
     */
    fun register()

    /** Token atual de push da plataforma, ou null se indisponível/não registrado. */
    suspend fun getToken(): String?
}
