package com.example.shared.ui.legal

import androidx.compose.runtime.Composable

/**
 * Equivalente multiplataforma do `androidx.activity.compose.BackHandler` usado no
 * `LegalConsentGate` do app Android.
 *
 * No original, o `BackHandler` só existe para o caso de o diálogo ainda não ter o
 * foco da janela — o próprio `Dialog` já consome o botão voltar
 * (`dismissOnBackPress`). No iOS não há botão voltar do sistema (no-op).
 */
@Composable
expect fun LegalConsentBackHandler(enabled: Boolean, onBack: () -> Unit)
