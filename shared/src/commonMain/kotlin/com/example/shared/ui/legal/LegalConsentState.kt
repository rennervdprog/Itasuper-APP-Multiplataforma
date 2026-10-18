package com.example.shared.ui.legal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.shared.model.LegalAcceptanceMode
import com.example.shared.model.PendingLegalChanges
import com.example.shared.model.UserSession
import com.example.shared.network.SupabaseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Estado do gate de aceite legal — porte de `LegalConsentViewModel`
 * (`Itasuper-APP-NATIVO/.../ui/legal/LegalConsentViewModel.kt`).
 *
 * Adaptações KMP:
 * - Sem `androidx.lifecycle.ViewModel` no commonMain: classe simples com
 *   `mutableStateOf` + `CoroutineScope` próprio (padrão já usado no
 *   `AppNavigator` do projeto).
 * - Rede via `SupabaseApi` (Ktor) em vez de `SupabaseClient` (OkHttp):
 *   `fetchLegalAcceptedVersions` + `fetchPendingLegalChanges` + `recordLegalAcceptance`.
 * - `UserSessionRepository.synchronizeProfileFromRemote()` não existe no KMP
 *   ainda: marcado com TODO(fase-rede) no [accept].
 */
data class LegalConsentUiState(
    val isChecking: Boolean = false,
    val isAccepting: Boolean = false,
    val pending: PendingLegalChanges? = null,
    val acceptedTerms: Boolean = false,
    val acceptedPrivacy: Boolean = false,
    val errorMessage: String? = null,
    val showRefusalPanel: Boolean = false
) {
    val requiresAcceptance: Boolean
        get() = pending?.requiresAcceptance == true

    /** Aviso prévio: os Termos prometem 30 dias em que nada muda, então não pode bloquear. */
    val isNotice: Boolean
        get() = pending?.mode == LegalAcceptanceMode.NOTICE

    /** Somente o modo binding prende o usuário na tela. */
    val isBlocking: Boolean
        get() = pending?.isBlocking == true

    val canDismiss: Boolean
        get() = requiresAcceptance && !isBlocking && !isAccepting

    val canAccept: Boolean
        get() = pending != null &&
            (!pending.needsTerms || acceptedTerms) &&
            (!pending.needsPrivacy || acceptedPrivacy) &&
            !isAccepting
}

class LegalConsentState {
    var uiState by mutableStateOf(LegalConsentUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var checkedUserId: String? = null

    fun checkForUpdates(session: UserSession) {
        if (!session.isLoggedIn || session.userId.isBlank() || session.accessToken.isBlank()) {
            checkedUserId = null
            uiState = LegalConsentUiState()
            return
        }
        if (checkedUserId == session.userId || uiState.isChecking) return

        checkedUserId = session.userId
        uiState = uiState.copy(isChecking = true, errorMessage = null)
        scope.launch {
            val (termsAccepted, privacyAccepted) =
                SupabaseApi.fetchLegalAcceptedVersions(session.userId, session.accessToken)
            val pending = SupabaseApi.fetchPendingLegalChanges(
                termsAccepted = termsAccepted,
                privacyAccepted = privacyAccepted?.ifBlank { termsAccepted },
                accessToken = session.accessToken
            )
            uiState = if (pending != null && shouldShow(pending)) {
                LegalConsentUiState(
                    pending = pending,
                    acceptedTerms = !pending.needsTerms,
                    acceptedPrivacy = !pending.needsPrivacy
                )
            } else {
                LegalConsentUiState()
            }
        }
    }

    /** Aviso já adiado nesta sessão não reaparece; aceite obrigatório sempre aparece. */
    private fun shouldShow(pending: PendingLegalChanges): Boolean {
        if (!pending.requiresAcceptance) return false
        if (pending.isBlocking) return true
        return !LegalNoticeStorage.isDismissed(pending.versionKey)
    }

    fun setTermsAccepted(accepted: Boolean) {
        uiState = uiState.copy(acceptedTerms = accepted, errorMessage = null)
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        uiState = uiState.copy(acceptedPrivacy = accepted, errorMessage = null)
    }

    fun setRefusalPanelVisible(visible: Boolean) {
        uiState = uiState.copy(showRefusalPanel = visible, errorMessage = null)
    }

    /** Adia o aviso prévio. Nunca dispensa um aceite obrigatório. */
    fun dismissNotice() {
        val state = uiState
        val pending = state.pending ?: return
        if (!state.canDismiss) return
        LegalNoticeStorage.dismiss(pending.versionKey)
        uiState = LegalConsentUiState()
    }

    fun accept(session: UserSession) {
        val state = uiState
        val pending = state.pending ?: return
        if (!state.canAccept) return

        uiState = state.copy(isAccepting = true, errorMessage = null)
        scope.launch {
            val result = SupabaseApi.recordLegalAcceptance(
                userId = session.userId,
                accessToken = session.accessToken,
                termsVersion = pending.currentTermsVersion,
                privacyVersion = pending.currentPrivacyVersion,
                // Aceitar antes da vigência é permitido e registrado como tal.
                acceptanceMode = pending.mode
            )
            uiState = if (result.success) {
                LegalNoticeStorage.dismiss(pending.versionKey)
                // TODO(fase-rede): sincronizar o perfil remoto após o aceite
                // (o app Android chama UserSessionRepository.synchronizeProfileFromRemote()).
                LegalConsentUiState()
            } else {
                state.copy(
                    isAccepting = false,
                    errorMessage = result.errorMessage ?: "Não foi possível registrar seu aceite."
                )
            }
        }
    }
}

/** Estado do gate com ciclo de vida da composição (substitui o ViewModel do Android). */
@Composable
fun rememberLegalConsentState(): LegalConsentState = remember { LegalConsentState() }
