package com.example.shared.ui.permissions

import androidx.compose.runtime.Composable

/**
 * Stub iOS de [PlatformPermissions].
 *
 * A solicitação real de permissões no iOS (CLLocationManager para localização,
 * UNUserNotificationCenter para notificações, via cinterop) fica para uma fase
 * futura. Por enquanto os launchers tratam o pedido como negado para o fluxo
 * da UI não travar — mesmo comportamento do botão "Agora não"/"Pular".
 */

actual object PlatformPermissions {

    actual fun init(appContext: Any?) {
        // iOS não precisa de contexto da aplicação para esta API.
    }

    actual fun hasLocationPermission(): Boolean {
        // TODO(fase-rede): consultar CLLocationManager.authorizationStatus via cinterop.
        return false
    }

    actual fun hasNotificationPermission(): Boolean {
        // TODO(fase-rede): consultar UNUserNotificationCenter via cinterop.
        return false
    }
}

@Composable
actual fun rememberLocationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit = {
    // TODO(fase-rede): solicitar autorização via CLLocationManager (cinterop).
    println("ItaSuper: solicitação de localização não implementada no iOS ainda")
    onResult(false)
}

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit = {
    // TODO(fase-rede): solicitar via UNUserNotificationCenter (cinterop).
    println("ItaSuper: solicitação de notificações não implementada no iOS ainda")
    onResult(false)
}
