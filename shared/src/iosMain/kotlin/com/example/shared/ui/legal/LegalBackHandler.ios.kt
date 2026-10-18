package com.example.shared.ui.legal

import androidx.compose.runtime.Composable

@Composable
actual fun LegalConsentBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS não tem botão voltar do sistema; o próprio Dialog já trata o fechamento.
}
