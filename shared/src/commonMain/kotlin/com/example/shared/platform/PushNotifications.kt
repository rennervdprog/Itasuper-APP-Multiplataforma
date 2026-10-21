package com.example.shared.platform

/**
 * Notificações push — expect/actual.
 *
 * Android: Firebase Cloud Messaging (mesmo projeto e mesmo backend do app
 * nativo). iOS: stub — push nativo no iOS fica para quando houver conta
 * Apple Developer (APNs).
 */
expect object PushNotifications {

    /**
     * Inicializa o provedor com o contexto da aplicação.
     * No Android passe `android.content.Context`; no iOS é ignorado.
     * No Android também inicializa o Firebase manualmente (ver o actual):
     * este app roda com applicationId provisório, sem google-services.json
     * próprio.
     */
    fun init(appContext: Any?)

    /**
     * Registra o app para receber push (inicializa o provedor da plataforma).
     * Seguro chamar mesmo sem credenciais/configuração: o token só fica indisponível.
     */
    fun register()

    /** Token atual de push da plataforma, ou null se indisponível/não registrado. */
    suspend fun getToken(): String?
}
