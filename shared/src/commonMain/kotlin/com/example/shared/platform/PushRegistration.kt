package com.example.shared.platform

import com.example.shared.network.SupabaseApi
import com.example.shared.session.UserSessionRepository
import com.example.shared.ui.permissions.PlatformPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Registro do aparelho para push — porte de
 * PushNotificationManager.registerCurrentDevice/registerFcmToken do app
 * original, que NUNCA tinha sido portado: o token FCM existia no aparelho,
 * mas ninguém o enviava ao servidor, então nenhum push chegava.
 *
 * Regras iguais às do original:
 * - só registra com permissão de notificações concedida;
 * - só registra com sessão logada (o servidor vincula o token ao usuário);
 * - device_info no formato "itasuper:native:android:<id do aparelho>",
 *   com o id gerado uma vez e persistido no SecureStorage.
 *
 * Chamado na abertura/retomada do app, após o login e quando o Firebase
 * renova o token (onNewToken). Idempotente: registrar de novo só reafirma
 * o mesmo token.
 */
object PushRegistration {

    private const val DEVICE_ID_KEY = "push_device_id"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val storage by lazy { SecureStorage() }

    /** Dispara a sincronização sem bloquear o chamador. */
    fun syncAsync() {
        scope.launch { syncWithBackend() }
    }

    suspend fun syncWithBackend() {
        try {
            val session = UserSessionRepository.session.value
            if (!session.isLoggedIn || session.accessToken.isBlank()) return
            if (!PlatformPermissions.hasNotificationPermission()) return
            val token = PushNotifications.getToken()
            if (token.isNullOrBlank()) return
            val deviceInfo = "itasuper:native:android:${deviceId()}"
            SupabaseApi.registerPushDevice(session.accessToken, token, deviceInfo)
        } catch (_: Throwable) {
            // Push é best-effort: qualquer falha aqui nunca pode derrubar o app.
        }
    }

    private fun deviceId(): String {
        storage.get(DEVICE_ID_KEY)?.takeIf { it.isNotBlank() }?.let { return it }
        val generated = buildString {
            repeat(4) {
                if (isNotEmpty()) append('-')
                append(Random.nextLong(0, 0xFFFF).toString(16).padStart(4, '0'))
            }
        }
        storage.save(DEVICE_ID_KEY, generated)
        return generated
    }
}

/**
 * Destino pendente vindo do toque numa notificação push — porte do
 * pendingDestination/pendingOrderId do PushNotificationManager original.
 * A MainActivity alimenta (extras do intent) e o App consome uma vez.
 */
object PushNavigation {

    private val _pendingOrderId = MutableStateFlow<String?>(null)
    val pendingOrderId: StateFlow<String?> = _pendingOrderId.asStateFlow()

    fun request(orderId: String?) {
        _pendingOrderId.value = orderId
    }

    /** Lê e limpa o pedido pendente (null quando não há navegação pendente). */
    fun consume(): String? {
        val value = _pendingOrderId.value
        _pendingOrderId.value = null
        return value
    }
}
