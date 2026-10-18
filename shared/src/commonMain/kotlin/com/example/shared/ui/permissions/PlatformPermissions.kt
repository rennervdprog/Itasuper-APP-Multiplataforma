package com.example.shared.ui.permissions

import androidx.compose.runtime.Composable

/**
 * Permissões de localização e notificações — expect/actual.
 *
 * A UI dos diálogos/onboarding de permissão vive no commonMain (pura, idêntica
 * ao app Android); só a interação com o sistema operacional varia:
 *
 * - Android: `ActivityResultContracts.RequestMultiplePermissions` /
 *   `RequestPermission` + checagem via `Context.checkSelfPermission`
 *   (mesma lógica do `PermissionUtils` do app nativo).
 * - iOS: stub — loga TODO e trata como negado (a solicitação real via
 *   CLLocationManager/UNUserNotificationCenter com cinterop fica para uma
 *   fase futura).
 */
expect object PlatformPermissions {

    /**
     * Inicializa com o contexto da aplicação.
     * No Android passe `android.content.Context`; no iOS é ignorado.
     */
    fun init(appContext: Any?)

    /** true se ACCESS_FINE_LOCATION ou ACCESS_COARSE_LOCATION já foi concedida. */
    fun hasLocationPermission(): Boolean

    /** true se POST_NOTIFICATIONS já foi concedida (no Android < 33 sempre true). */
    fun hasNotificationPermission(): Boolean
}

/**
 * Launcher de permissão de localização.
 *
 * @param onResult chamado com true quando a localização fina ou aproximada foi
 * concedida (inclui re-checagem após o retorno do sistema, como no original).
 * @return função que dispara o pedido de permissão do sistema.
 */
@Composable
expect fun rememberLocationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit

/**
 * Launcher de permissão de notificações.
 *
 * @param onResult chamado com o resultado bruto do sistema.
 * @return função que dispara o pedido de permissão do sistema.
 */
@Composable
expect fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit
