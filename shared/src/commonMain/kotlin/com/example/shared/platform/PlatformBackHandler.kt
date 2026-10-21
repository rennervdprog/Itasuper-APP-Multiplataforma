package com.example.shared.platform

import androidx.compose.runtime.Composable

/**
 * Equivalente multiplataforma do `androidx.activity.compose.BackHandler`:
 * no Android, intercepta o botão/gesto Voltar do sistema; no iOS não há
 * botão voltar do sistema (no-op). Mesmo padrão do LegalConsentBackHandler.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
