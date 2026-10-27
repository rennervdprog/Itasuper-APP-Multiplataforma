package com.example.shared.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.shared.network.SupabaseApi
import com.example.shared.session.UserSessionRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.shared.model.ClaimAddressInput
import com.example.shared.model.ClaimCheckResult
import com.example.shared.model.ClaimStatus
import com.example.shared.model.CLAIM_FALLBACK_PRIVACY_VERSION
import com.example.shared.model.CLAIM_FALLBACK_TERMS_VERSION
import com.example.shared.model.SIGNUP_DRAFT_KEY
import com.example.shared.model.SIGNUP_STEPS
import com.example.shared.model.SignupErrors
import com.example.shared.model.SignupStep
import com.example.shared.model.SignupValues
import com.example.shared.model.decodeSignupDraft
import com.example.shared.model.encodeSignupDraft
import com.example.shared.model.normalizeClaimPhone
import com.example.shared.model.passwordRulesPassedCount
import com.example.shared.model.validateSignupStep
import com.russhwolf.settings.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.example.shared.generated.resources.Res
import com.example.shared.generated.resources.ic_ita_alert
import com.example.shared.generated.resources.ic_ita_check
import com.example.shared.generated.resources.ic_ita_check_circle
import com.example.shared.generated.resources.ic_ita_eye
import com.example.shared.generated.resources.ic_ita_eye_off
import com.example.shared.generated.resources.ic_ita_key
import com.example.shared.generated.resources.ic_ita_keypad
import com.example.shared.generated.resources.ic_ita_lock
import com.example.shared.generated.resources.ic_ita_mail
import com.example.shared.generated.resources.ic_ita_phone
import com.example.shared.generated.resources.ic_ita_profile
import com.example.shared.generated.resources.itasuper_brand_icon
import org.jetbrains.compose.resources.painterResource
import com.example.shared.model.LegalDocumentLinks
import com.example.shared.model.PendingLegalChanges
import com.example.shared.model.SavedAddress
import com.example.shared.theme.ItaSuperBorder
import com.example.shared.theme.ItaSuperError
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSuccess
import com.example.shared.theme.ItaSuperTextSecondary
import com.example.shared.util.Masks

/**
 * Tela de autenticação (login + cadastro) — migração KMP do AuthScreen do
 * app Android (fase 3d). Layout 1:1 com o original ("estilo entregador":
 * fundo branco, logo + badge "PARA CLIENTES" + título grande); o estado que
 * era do AuthViewModel virou [AuthState] (remember/mutableStateOf) e a rede
 * é real: login/cadastro via Supabase, sessão no [UserSessionRepository].
 *
 * Diferenças intencionais vs. o original Android:
 * - Ícones e logo: mesmos drawables do original via composeResources
 *   (`Res.drawable.ic_ita_*`, `itasuper_brand_icon`).
 * - Abertura de Termos/Privacidade: LocalUriHandler em vez de
 *   Intent(ACTION_VIEW) — equivalente KMP, sem android.net.Uri.
 */
sealed class AuthMode {
    data object Login : AuthMode()
    data object Register : AuthMode()
}

data class AuthUiState(
    val authMode: AuthMode = AuthMode.Login,
    // Login fields
    val loginEmail: String = "",
    val loginPassword: String = "",
    val isLoginPasswordVisible: Boolean = false,

    // Register fields (Fase 3: cadastro em 3 passos — Conta → Endereço → PIN)
    val regEmail: String = "",
    val regName: String = "",
    val regWhatsapp: String = "",
    val regPassword: String = "",
    val isRegPasswordVisible: Boolean = false,
    val regCep: String = "",
    val regNumber: String = "",
    val regStreet: String = "",
    val regNeighborhood: String = "",
    val regComplement: String = "",
    val regReferencePoint: String = "",
    val regPin: String = "",
    val regPinConfirm: String = "",
    val isRegPinVisible: Boolean = false,
    val regPinAcknowledged: Boolean = false,
    val isTermsAccepted: Boolean = false,
    val isPrivacyAccepted: Boolean = false,
    val isLoadingLegalDocuments: Boolean = false,

    // Passo atual do cadastro e erros por campo do passo.
    val signupStep: SignupStep = SignupStep.ACCOUNT,
    val signupFieldErrors: SignupErrors = emptyMap(),
    val isCepLoading: Boolean = false,
    val cepNotFound: Boolean = false,

    // Reivindicação de conta (Fase 3 / 6D-B — "Reivindicar no cadastro").
    val claimStatus: ClaimStatus? = null,
    val claimGuestId: String = "",
    val claimLastStoreName: String? = null,
    val claimLastOrderId: String? = null,
    val claimCode: String = "",
    val claimOtpSent: Boolean = false,
    val showClaimOtp: Boolean = false,
    val claimFallback: Boolean = false,
    val claimResendIn: Int = 0,
    val claimBusy: Boolean = false,

    // Carregado do backend ao entrar no modo cadastro (fetchPendingLegalChanges).
    val pendingLegalChanges: PendingLegalChanges? = null,

    // Error messages per field or general
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,

    // Dialog states
    val showForgotPasswordDialog: Boolean = false,
    val recoveryEmail: String = "",
    val isSendingRecovery: Boolean = false
)

// android.util.Patterns não existe no commonMain — validação manual.
// Regex.matches() exige match da string inteira, então não precisa de âncora $.
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
private fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

/**
 * Equivalente KMP do AuthViewModel: estado observável via mutableStateOf e
 * os mesmos handlers/validações do original, com rede real via SupabaseApi e
 * sessão persistida pelo UserSessionRepository.
 */
/** Chaves do SecureStorage para a sessão do usuário (mesmos valores do UserSessionRepository). */
const val KEY_USER_ID = "session_user_id"
const val KEY_ACCESS_TOKEN = "session_access_token"
const val KEY_REFRESH_TOKEN = "session_refresh_token"
const val KEY_USER_EMAIL = "session_user_email"

private class AuthState {
    var uiState by mutableStateOf(AuthUiState())
        private set

    private val scope = MainScope()

    /**
     * Carrega os documentos legais vigentes para o cadastro (mesmo backend
     * legal do app original/web). Para usuário novo, o backend precisa
     * retornar aceite requerido — sem isso o cadastro não anda.
     */
    fun loadRegistrationLegalDocuments() {
        if (uiState.isLoadingLegalDocuments || uiState.pendingLegalChanges != null) return
        uiState = uiState.copy(isLoadingLegalDocuments = true)
        scope.launch {
            val pending = try {
                SupabaseApi.fetchPendingLegalChanges(null, null)
            } catch (_: Throwable) {
                null
            }
            if (pending == null || !pending.requiresAcceptance) {
                uiState = uiState.copy(
                    isLoadingLegalDocuments = false,
                    errorMessage = "Não foi possível carregar os Termos e a Política de Privacidade. Verifique sua conexão."
                )
            } else {
                uiState = uiState.copy(
                    isLoadingLegalDocuments = false,
                    pendingLegalChanges = pending,
                    isTermsAccepted = false,
                    isPrivacyAccepted = false
                )
            }
        }
    }

    fun setAuthMode(mode: AuthMode) {
        uiState = uiState.copy(authMode = mode, errorMessage = null, successMessage = null)
    }

    // Login updates
    fun onLoginEmailChange(value: String) {
        uiState = uiState.copy(loginEmail = value, errorMessage = null)
    }

    fun onLoginPasswordChange(value: String) {
        uiState = uiState.copy(loginPassword = value, errorMessage = null)
    }

    fun toggleLoginPasswordVisibility() {
        uiState = uiState.copy(isLoginPasswordVisible = !uiState.isLoginPasswordVisible)
    }

    // Register updates (Fase 3: 3 passos)
    fun onRegNameChange(value: String) {
        uiState = uiState.copy(regName = value, errorMessage = null)
        clearSignupFieldError("fullName")
        saveSignupDraft()
    }

    fun onRegEmailChange(value: String) {
        uiState = uiState.copy(regEmail = value, errorMessage = null)
        clearSignupFieldError("email")
        saveSignupDraft()
    }

    fun onRegWhatsappChange(value: String) {
        uiState = uiState.copy(
            regWhatsapp = Masks.formatPhone(value),
            errorMessage = null,
            // Trocar o número invalida o claim anterior (outro telefone).
            claimStatus = null,
            claimGuestId = "",
            claimLastStoreName = null,
            claimLastOrderId = null,
            claimOtpSent = false,
            showClaimOtp = false,
            claimFallback = false,
            claimResendIn = 0
        )
        claimCheckedPhone = null
        clearSignupFieldError("whatsapp")
        saveSignupDraft()
    }

    fun onRegPasswordChange(value: String) {
        uiState = uiState.copy(regPassword = value, errorMessage = null)
        clearSignupFieldError("password")
        saveSignupDraft()
    }

    fun toggleRegPasswordVisibility() {
        uiState = uiState.copy(isRegPasswordVisible = !uiState.isRegPasswordVisible)
    }

    fun onRegCepChange(value: String) {
        val formatted = Masks.formatCep(value)
        uiState = uiState.copy(regCep = formatted, cepNotFound = false, errorMessage = null)
        clearSignupFieldError("cep")
        saveSignupDraft()
        val digits = formatted.filter { it.isDigit() }
        if (digits.length != 8) return
        lookupCep(digits)
    }

    private var cepLookupId = 0
    private var cepCity: String = ""
    private var cepState: String = ""

    /** Busca o endereço pelo CEP (ViaCEP → BrasilAPI) e preenche rua/bairro se vazios. */
    private fun lookupCep(digits: String) {
        val lookupId = ++cepLookupId
        uiState = uiState.copy(isCepLoading = true)
        scope.launch {
            val found = try {
                SupabaseApi.fetchAddressByCep(digits)
            } catch (_: Throwable) {
                null
            }
            if (lookupId != cepLookupId) return@launch // resposta de CEP antigo: ignora.
            val state = uiState
            if (found == null) {
                uiState = state.copy(isCepLoading = false, cepNotFound = true)
            } else {
                cepCity = found.city
                cepState = found.state
                uiState = state.copy(
                    isCepLoading = false,
                    cepNotFound = false,
                    regStreet = if (state.regStreet.isBlank()) found.street else state.regStreet,
                    regNeighborhood = if (state.regNeighborhood.isBlank()) found.neighborhood else state.regNeighborhood
                )
                clearSignupFieldError("street")
                clearSignupFieldError("neighborhood")
            }
            saveSignupDraft()
        }
    }

    fun onRegNumberChange(value: String) {
        uiState = uiState.copy(regNumber = value, errorMessage = null)
        clearSignupFieldError("number")
        saveSignupDraft()
    }

    fun onRegStreetChange(value: String) {
        uiState = uiState.copy(regStreet = value, errorMessage = null)
        clearSignupFieldError("street")
        saveSignupDraft()
    }

    fun onRegNeighborhoodChange(value: String) {
        uiState = uiState.copy(regNeighborhood = value, errorMessage = null)
        clearSignupFieldError("neighborhood")
        saveSignupDraft()
    }

    fun onRegComplementChange(value: String) {
        uiState = uiState.copy(regComplement = value, errorMessage = null)
        saveSignupDraft()
    }

    fun onRegReferencePointChange(value: String) {
        uiState = uiState.copy(regReferencePoint = value, errorMessage = null)
        saveSignupDraft()
    }

    fun onRegPinChange(value: String) {
        uiState = uiState.copy(regPin = value.filter { it.isDigit() }.take(4), errorMessage = null)
        clearSignupFieldError("deliveryPin")
        saveSignupDraft()
    }

    fun onRegPinConfirmChange(value: String) {
        uiState = uiState.copy(regPinConfirm = value.filter { it.isDigit() }.take(4), errorMessage = null)
        clearSignupFieldError("confirmPin")
        saveSignupDraft()
    }

    fun toggleRegPinVisibility() {
        uiState = uiState.copy(isRegPinVisible = !uiState.isRegPinVisible)
    }

    fun onRegPinAcknowledgedChange(acknowledged: Boolean) {
        uiState = uiState.copy(regPinAcknowledged = acknowledged, errorMessage = null)
        clearSignupFieldError("pinAcknowledged")
        saveSignupDraft()
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        uiState = uiState.copy(isTermsAccepted = accepted, errorMessage = null)
        clearSignupFieldError("acceptedTerms")
        saveSignupDraft()
    }

    fun onPrivacyAcceptedChange(accepted: Boolean) {
        uiState = uiState.copy(isPrivacyAccepted = accepted, errorMessage = null)
        clearSignupFieldError("acceptedPrivacy")
        saveSignupDraft()
    }

    // ---- Cadastro em 3 passos: navegação, validação e rascunho ----

    private fun signupValues(): SignupValues = SignupValues(
        email = uiState.regEmail,
        whatsapp = uiState.regWhatsapp,
        fullName = uiState.regName,
        password = uiState.regPassword,
        cep = uiState.regCep,
        number = uiState.regNumber,
        street = uiState.regStreet,
        neighborhood = uiState.regNeighborhood,
        complement = uiState.regComplement,
        referencePoint = uiState.regReferencePoint,
        deliveryPin = uiState.regPin,
        confirmPin = uiState.regPinConfirm,
        pinAcknowledged = uiState.regPinAcknowledged,
        acceptedTerms = uiState.isTermsAccepted,
        acceptedPrivacy = uiState.isPrivacyAccepted
    )

    private fun applySignupValues(values: SignupValues, step: SignupStep) {
        uiState = uiState.copy(
            regEmail = values.email,
            regWhatsapp = values.whatsapp,
            regName = values.fullName,
            regPassword = values.password,
            regCep = values.cep,
            regNumber = values.number,
            regStreet = values.street,
            regNeighborhood = values.neighborhood,
            regComplement = values.complement,
            regReferencePoint = values.referencePoint,
            regPin = values.deliveryPin,
            regPinConfirm = values.confirmPin,
            regPinAcknowledged = values.pinAcknowledged,
            isTermsAccepted = values.acceptedTerms,
            isPrivacyAccepted = values.acceptedPrivacy,
            signupStep = step,
            signupFieldErrors = emptyMap()
        )
    }

    /** Valida o passo atual; falso = erros no estado e foco no 1º campo inválido. */
    private fun validateCurrentSignupStep(): Boolean {
        val state = uiState
        val legal = state.pendingLegalChanges
        val errors = validateSignupStep(
            step = state.signupStep,
            values = signupValues(),
            passwordRulesPassed = passwordRulesPassedCount(state.regPassword) == 5,
            needsTerms = legal?.needsTerms ?: true,
            needsPrivacy = legal?.needsPrivacy ?: true
        )
        uiState = state.copy(signupFieldErrors = errors)
        return errors.isEmpty()
    }

    private fun clearSignupFieldError(field: String) {
        if (uiState.signupFieldErrors[field] == null) return
        uiState = uiState.copy(signupFieldErrors = uiState.signupFieldErrors - field)
    }

    /** Salva o rascunho no aparelho (Settings) — some ao concluir o cadastro. */
    private fun saveSignupDraft() {
        try {
            Settings().putString(
                SIGNUP_DRAFT_KEY,
                encodeSignupDraft(signupValues(), uiState.signupStep)
            )
        } catch (_: Throwable) {
            // Rascunho é cortesia: falhar não trava o cadastro.
        }
    }

    /** Restaura o rascunho salvo ao entrar no cadastro (uma vez por sessão). */
    fun restoreSignupDraft() {
        if (draftRestored) return
        draftRestored = true
        try {
            val raw = Settings().getStringOrNull(SIGNUP_DRAFT_KEY) ?: return
            val (values, step) = decodeSignupDraft(raw) ?: return
            if (values.email.isBlank() && values.whatsapp.isBlank() && values.fullName.isBlank()) return
            applySignupValues(values, step)
        } catch (_: Throwable) {
            // Rascunho corrompido: começa do zero.
        }
    }

    private fun clearSignupDraft() {
        try {
            Settings().remove(SIGNUP_DRAFT_KEY)
        } catch (_: Throwable) {
            // Melhor esforço.
        }
    }

    /** Vai para um passo: voltar é direto; avançar revalida os intermediários. */
    fun goToSignupStep(target: SignupStep) {
        val current = uiState.signupStep
        if (target == current) return
        if (target.number < current.number) {
            uiState = uiState.copy(signupStep = target, signupFieldErrors = emptyMap(), errorMessage = null)
            saveSignupDraft()
            return
        }
        // Avançar: revalida o passo atual e os intermediários.
        scope.launch {
            var step = current
            while (step.number < target.number) {
                val advanced = advanceFromStep(step)
                if (!advanced) return@launch
                step = uiState.signupStep
            }
            uiState = uiState.copy(signupStep = target, signupFieldErrors = emptyMap())
            saveSignupDraft()
        }
    }

    /** "Continuar" do passo 1 ou 2. */
    fun advanceSignupStep() {
        scope.launch { advanceFromStep(uiState.signupStep) }
    }

    /** Avança um passo; falso = ficou (validação, claim pendente ou conta real). */
    private suspend fun advanceFromStep(step: SignupStep): Boolean {
        if (!validateCurrentSignupStep()) return false
        return when (step) {
            SignupStep.ACCOUNT -> {
                val result = runClaimCheck() ?: return false
                when (result.status) {
                    ClaimStatus.NEW -> {
                        uiState = uiState.copy(signupStep = SignupStep.ADDRESS, signupFieldErrors = emptyMap())
                        saveSignupDraft()
                        true
                    }
                    ClaimStatus.GUEST -> {
                        // Código a caminho: fica no passo 1 mostrando a tela do código.
                        uiState = uiState.copy(showClaimOtp = true)
                        false
                    }
                    ClaimStatus.REAL -> {
                        uiState = uiState.copy(
                            errorMessage = "Esse número já tem conta. Entre ou recupere a senha."
                        )
                        false
                    }
                    null -> false
                }
            }
            SignupStep.ADDRESS -> {
                uiState = uiState.copy(signupStep = SignupStep.PIN, signupFieldErrors = emptyMap())
                saveSignupDraft()
                true
            }
            SignupStep.PIN -> false // o passo 3 é o submit (handleRegister).
        }
    }

    // ---- Reivindicação de conta (claim-check / send-otp / verify-otp) ----

    /** Telefone do último claim-check (cache: evita re-chamar a edge no mesmo número). */
    private var claimCheckedPhone: String? = null
    private var draftRestored: Boolean = false

    /**
     * `claim-check` no passo 1. Null = erro de rede/resposta — mensagem no estado.
     * Reusa o resultado em cache quando o número não mudou (mesmo que a web).
     */
    private suspend fun runClaimCheck(): ClaimCheckResult? {
        val state = uiState
        val phone = normalizeClaimPhone(state.regWhatsapp) ?: return null
        val cached = state.claimStatus
        if (claimCheckedPhone == phone && cached != null) {
            return ClaimCheckResult(
                status = cached,
                guestUserId = state.claimGuestId.ifBlank { null },
                lastStoreName = state.claimLastStoreName,
                lastOrderId = state.claimLastOrderId
            )
        }
        uiState = state.copy(claimBusy = true, errorMessage = null)
        val result = try {
            SupabaseApi.claimCheck(phone)
        } catch (_: Throwable) {
            null
        }
        if (result == null || result.status == null) {
            uiState = uiState.copy(
                claimBusy = false,
                errorMessage = "Não foi possível verificar o número. Tente novamente."
            )
            return null
        }
        uiState = uiState.copy(
            claimBusy = false,
            claimStatus = result.status,
            claimGuestId = result.guestUserId.orEmpty(),
            claimLastStoreName = result.lastStoreName,
            claimLastOrderId = result.lastOrderId,
            claimOtpSent = false,
            showClaimOtp = false,
            claimFallback = false,
            claimResendIn = 0
        )
        claimCheckedPhone = phone
        if (result.status == ClaimStatus.GUEST) {
            sendClaimOtp(phone, result.guestUserId.orEmpty())
        }
        return result
    }

    /**
     * `claim-send-otp`: envia o código de 6 dígitos no WhatsApp do guest.
     * Se a loja não tiver WhatsApp conectado, mostra o fallback (Opção A).
     */
    private suspend fun sendClaimOtp(phone: String, guestUserId: String) {
        if (guestUserId.isBlank()) return
        uiState = uiState.copy(claimBusy = true, errorMessage = null)
        val result = try {
            SupabaseApi.claimSendOtp(phone, guestUserId)
        } catch (_: Throwable) {
            null
        }
        if (result == null) {
            uiState = uiState.copy(
                claimBusy = false,
                errorMessage = "Não foi possível enviar o código. Tente novamente."
            )
            return
        }
        if (result.error != null && !result.ok) {
            when (result.error) {
                "store_whatsapp_unavailable" -> {
                    uiState = uiState.copy(
                        claimBusy = false,
                        claimFallback = true,
                        showClaimOtp = false
                    )
                }
                "resend_too_soon" -> {
                    uiState = uiState.copy(
                        claimBusy = false,
                        errorMessage = "Aguarde ${result.retryAfterSeconds ?: 60}s para reenviar o código."
                    )
                }
                "rate_limited" -> {
                    uiState = uiState.copy(
                        claimBusy = false,
                        errorMessage = "Muitos códigos enviados. Tente novamente em 1 hora."
                    )
                }
                else -> {
                    uiState = uiState.copy(
                        claimBusy = false,
                        errorMessage = "Não foi possível enviar o código. Tente novamente."
                    )
                }
            }
            return
        }
        uiState = uiState.copy(
            claimBusy = false,
            claimOtpSent = true,
            showClaimOtp = true,
            claimFallback = false,
            claimResendIn = result.resendAfterSeconds ?: 60
        )
    }

    /** Reenvia o código (respeita o countdown de 60 s da edge). */
    fun resendClaimOtp() {
        val state = uiState
        if (state.claimResendIn > 0 || state.claimBusy) return
        val phone = normalizeClaimPhone(state.regWhatsapp) ?: return
        if (state.claimGuestId.isBlank()) return
        scope.launch { sendClaimOtp(phone, state.claimGuestId) }
    }

    fun tickClaimResendCountdown() {
        if (uiState.claimResendIn > 0) {
            uiState = uiState.copy(claimResendIn = uiState.claimResendIn - 1)
        }
    }

    fun onClaimCodeChange(value: String) {
        uiState = uiState.copy(claimCode = value.filter { it.isDigit() }.take(6), errorMessage = null)
    }

    /**
     * "Confirmar código" na tela do OTP: só confere o formato (6 dígitos).
     * A verificação de verdade acontece no submit final, junto com a
     * conversão da conta (claim-verify-otp) — igual à web.
     */
    fun confirmClaimCode() {
        if (uiState.claimCode.length != 6) {
            uiState = uiState.copy(errorMessage = "Digite o código de 6 dígitos enviado no WhatsApp.")
            return
        }
        uiState = uiState.copy(
            showClaimOtp = false,
            signupStep = SignupStep.ADDRESS,
            signupFieldErrors = emptyMap(),
            errorMessage = null
        )
        saveSignupDraft()
    }

    /** Volta da tela do código para o passo 1 (trocar o número). */
    fun cancelClaimOtp() {
        uiState = uiState.copy(
            showClaimOtp = false,
            claimFallback = false,
            claimCode = "",
            claimOtpSent = false,
            claimStatus = null,
            claimGuestId = "",
            claimLastStoreName = null,
            claimLastOrderId = null
        )
        claimCheckedPhone = null
    }

    // Forgot password dialog
    fun openForgotPasswordDialog() {
        uiState = uiState.copy(
            showForgotPasswordDialog = true,
            recoveryEmail = uiState.loginEmail
        )
    }

    fun closeForgotPasswordDialog() {
        uiState = uiState.copy(showForgotPasswordDialog = false)
    }

    fun onRecoveryEmailChange(value: String) {
        uiState = uiState.copy(recoveryEmail = value)
    }

    fun sendRecoveryEmail() {
        val email = uiState.recoveryEmail.trim()
        if (email.isBlank() || !isValidEmail(email)) {
            uiState = uiState.copy(errorMessage = "Informe um e-mail válido para recuperação")
            return
        }
        if (uiState.isSendingRecovery) return
        uiState = uiState.copy(isSendingRecovery = true, errorMessage = null)
        scope.launch {
            val result = try {
                SupabaseApi.requestPasswordRecovery(email)
            } catch (_: Throwable) {
                null
            }
            if (result != null && result.isSuccess) {
                uiState = uiState.copy(
                    isSendingRecovery = false,
                    showForgotPasswordDialog = false,
                    successMessage = "E-mail de recuperação enviado. Verifique sua caixa de entrada.",
                    errorMessage = null
                )
            } else {
                uiState = uiState.copy(
                    isSendingRecovery = false,
                    errorMessage = result?.errorMessage ?: "Não foi possível enviar o e-mail de recuperação."
                )
            }
        }
    }

    fun clearMessages() {
        uiState = uiState.copy(errorMessage = null, successMessage = null)
    }

    fun handleLogin(onSuccess: () -> Unit) {
        val state = uiState
        val email = state.loginEmail.trim()

        if (email.isBlank() || !isValidEmail(email)) {
            uiState = state.copy(errorMessage = "Informe um e-mail válido")
            return
        }
        if (state.loginPassword.length < 6) {
            uiState = state.copy(errorMessage = "Senha: mínimo 6 caracteres")
            return
        }

        // Login real via Supabase. Sem fallback para mock (regra: nada de dado fake).
        // Erro de rede -> mensagem de erro (igual ao app original).
        uiState = state.copy(isLoading = true, errorMessage = null)
        scope.launch {
            try {
                val result = SupabaseApi.signIn(email, state.loginPassword)
                if (result.isSuccess) {
                    // Sessão real no repositório (persiste + sincroniza o perfil).
                    UserSessionRepository.login(
                        email = email,
                        userId = result.userId ?: "",
                        accessToken = result.accessToken ?: "",
                        refreshToken = result.refreshToken ?: "",
                        expiresAtSeconds = result.expiresAt
                    )
                    uiState = uiState.copy(isLoading = false, errorMessage = null, successMessage = "Login realizado!")
                    onSuccess()
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = result.errorMessage ?: "E-mail ou senha incorretos"
                    )
                }
            } catch (_: Throwable) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Não foi possível conectar. Verifique sua internet e tente novamente."
                )
            }
        }
    }

    /**
     * Submit do passo 3 — "Criar conta" (Fase 3).
     * Revalida os 3 passos (o usuário pode ter voltado e mudado algo) e
     * segue por um dos dois caminhos, iguais aos da web:
     * - guest (claim): `claim-verify-otp` verifica o código E converte a
     *   conta numa só chamada; depois entra com e-mail + senha novos.
     * - novo: `signUp` com metadata (o trigger cria o perfil) + PATCH do
     *   perfil (phone E whatsapp_number) + endereço "Casa" + aceite legal.
     * O e-mail é o do passo 1 — nunca mais placeholder.
     */
    fun handleRegister(onSuccess: () -> Unit) {
        val state = uiState
        val values = signupValues()
        val legal = state.pendingLegalChanges

        // Revalida os 3 passos, na ordem; o 1º inválido ganha o foco.
        for (step in SignupStep.entries) {
            val errors = validateSignupStep(
                step = step,
                values = values,
                passwordRulesPassed = passwordRulesPassedCount(state.regPassword) == 5,
                needsTerms = legal?.needsTerms ?: true,
                needsPrivacy = legal?.needsPrivacy ?: true
            )
            if (errors.isNotEmpty()) {
                uiState = state.copy(signupStep = step, signupFieldErrors = errors)
                return
            }
        }
        if (legal == null) {
            uiState = state.copy(errorMessage = "Aguarde o carregamento dos Termos e da Política de Privacidade.")
            return
        }
        val phone = normalizeClaimPhone(state.regWhatsapp)
        if (phone == null) {
            uiState = state.copy(signupStep = SignupStep.ACCOUNT)
            return
        }
        val email = state.regEmail.trim().lowercase()
        val name = state.regName.trim()

        uiState = state.copy(isLoading = true, errorMessage = null)
        scope.launch {
            try {
                // Garante o claim-check antes de decidir o caminho.
                val claimStatus = uiState.claimStatus ?: runClaimCheck()?.status
                if (claimStatus == null) {
                    uiState = uiState.copy(isLoading = false)
                    return@launch
                }
                if (claimStatus == ClaimStatus.REAL) {
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = "Esse número já tem conta. Entre ou recupere a senha."
                    )
                    return@launch
                }
                if (claimStatus == ClaimStatus.GUEST) {
                    finishGuestClaim(phone, email, name, onSuccess)
                } else {
                    finishNewSignup(phone, email, name, legal, onSuccess)
                }
            } catch (_: Throwable) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = "Não foi possível conectar. Verifique sua internet e tente novamente."
                )
            }
        }
    }

    /** Caminho guest: verifica o código e converte a conta (claim-verify-otp). */
    private suspend fun finishGuestClaim(
        phone: String,
        email: String,
        name: String,
        onSuccess: () -> Unit
    ) {
        val state = uiState
        val guestId = state.claimGuestId
        if (guestId.isBlank()) {
            uiState = state.copy(
                isLoading = false,
                errorMessage = "Não foi possível localizar seu cadastro anterior. Tente novamente."
            )
            return
        }
        if (state.claimCode.length != 6) {
            uiState = state.copy(
                isLoading = false,
                showClaimOtp = true,
                errorMessage = "Digite o código de 6 dígitos enviado no WhatsApp."
            )
            return
        }
        val legal = state.pendingLegalChanges
        val result = try {
            SupabaseApi.claimVerifyOtp(
                phone = phone,
                guestUserId = guestId,
                code = state.claimCode,
                email = email,
                password = state.regPassword,
                fullName = name,
                address = ClaimAddressInput(
                    cep = state.regCep.filter { it.isDigit() },
                    street = state.regStreet,
                    number = state.regNumber,
                    complement = state.regComplement,
                    neighborhood = state.regNeighborhood,
                    referencePoint = state.regReferencePoint
                ),
                deliveryPin = state.regPin,
                termsVersion = legal?.currentTermsVersion ?: CLAIM_FALLBACK_TERMS_VERSION,
                privacyVersion = legal?.currentPrivacyVersion ?: CLAIM_FALLBACK_PRIVACY_VERSION
            )
        } catch (_: Throwable) {
            null
        }
        if (result == null || !result.ok) {
            val message = when (result?.error) {
                "invalid_code" -> {
                    val remaining = result.attemptsRemaining
                    if (remaining != null) "Código incorreto. Você tem mais $remaining tentativa(s)."
                    else "Código incorreto. Confira e tente novamente."
                }
                "too_many_attempts" -> "Muitas tentativas. Peça um novo código."
                "code_expired_or_not_found" -> "Código expirado ou não encontrado. Peça um novo código."
                "email_in_use" -> "Esse e-mail já está em uso por outra conta."
                "already_registered" -> "Esse número já tem conta. Entre ou recupere a senha."
                else -> "Não foi possível concluir o cadastro. Tente novamente."
            }
            uiState = uiState.copy(
                isLoading = false,
                showClaimOtp = result?.error != "email_in_use" && result?.error != "already_registered",
                claimCode = "",
                errorMessage = message
            )
            if (result?.error == "already_registered") {
                uiState = uiState.copy(claimStatus = ClaimStatus.REAL)
            }
            return
        }
        // Conversão feita: entra com o e-mail e a senha novos (igual à web).
        val signIn = try {
            SupabaseApi.signIn(email, state.regPassword)
        } catch (_: Throwable) {
            null
        }
        if (signIn == null || !signIn.isSuccess || signIn.userId.isNullOrBlank() || signIn.accessToken.isNullOrBlank()) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = "Sua conta foi criada, mas não foi possível entrar. Tente entrar com seu e-mail e senha."
            )
            return
        }
        UserSessionRepository.login(
            email = email,
            userId = signIn.userId,
            accessToken = signIn.accessToken,
            refreshToken = signIn.refreshToken ?: "",
            expiresAtSeconds = signIn.expiresAt
        )
        clearSignupDraft()
        uiState = uiState.copy(isLoading = false, errorMessage = null, successMessage = "Conta criada!")
        onSuccess()
    }

    /** Caminho novo: signUp com metadata + PATCH do perfil + endereço + aceite legal. */
    private suspend fun finishNewSignup(
        phone: String,
        email: String,
        name: String,
        legal: PendingLegalChanges,
        onSuccess: () -> Unit
    ) {
        val state = uiState
        val result = try {
            SupabaseApi.signUp(
                email = email,
                password = state.regPassword,
                metadata = mapOf(
                    "role" to "cliente",
                    "whatsapp" to phone,
                    "full_name" to name,
                    "delivery_pin" to state.regPin
                )
            )
        } catch (_: Throwable) {
            null
        }
        if (result == null || !result.isSuccess) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = result?.errorMessage ?: "Não foi possível criar a conta. Tente novamente."
            )
            return
        }
        val userId = result.userId.orEmpty()
        val accessToken = result.accessToken.orEmpty()
        if (userId.isBlank() || accessToken.isBlank()) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = "Sua conta foi criada, mas não foi possível concluir o cadastro. Entre novamente para finalizar."
            )
            return
        }
        // O trigger já criou o perfil no signUp — PATCH completa os dados
        // (phone E whatsapp_number; o POST antigo dava 409 aqui).
        val profileUpdated = try {
            SupabaseApi.updateProfileAfterSignup(
                userId = userId,
                accessToken = accessToken,
                fullName = name,
                email = email,
                phone = phone,
                deliveryPin = state.regPin
            )
        } catch (_: Throwable) {
            false
        }
        if (!profileUpdated) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = "Sua conta foi criada, mas não foi possível concluir o cadastro. Entre novamente para finalizar."
            )
            return
        }
        // Endereço "Casa" padrão (falha não trava o cadastro — igual à web).
        try {
            SupabaseApi.createSavedAddress(
                userId = userId,
                accessToken = accessToken,
                address = SavedAddress(
                    label = "Casa",
                    street = state.regStreet.trim(),
                    number = state.regNumber.trim(),
                    complement = state.regComplement.trim(),
                    neighborhood = state.regNeighborhood.trim(),
                    city = cepCity,
                    state = cepState,
                    referencePoint = state.regReferencePoint.trim(),
                    cep = state.regCep.filter { it.isDigit() },
                    isDefault = true
                )
            )
        } catch (_: Throwable) {
            // Melhor esforço.
        }
        val acceptanceResult = try {
            SupabaseApi.recordLegalAcceptance(
                userId = userId,
                accessToken = accessToken,
                termsVersion = legal.currentTermsVersion,
                privacyVersion = legal.currentPrivacyVersion
            )
        } catch (_: Throwable) {
            null
        }
        if (acceptanceResult == null || !acceptanceResult.success) {
            uiState = uiState.copy(
                isLoading = false,
                errorMessage = acceptanceResult?.errorMessage
                    ?: "Sua conta foi criada, mas o aceite não pôde ser registrado. Entre novamente para finalizar."
            )
            return
        }
        UserSessionRepository.register(
            name = name,
            email = email,
            cpfCnpj = "",
            whatsapp = state.regWhatsapp,
            pin = state.regPin,
            userId = userId,
            accessToken = accessToken,
            refreshToken = result.refreshToken ?: "",
            expiresAtSeconds = result.expiresAt
        )
        clearSignupDraft()
        uiState = uiState.copy(isLoading = false, errorMessage = null, successMessage = "Conta criada!")
        onSuccess()
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val authState = remember { AuthState() }
    val uiState = authState.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    // Ao entrar no modo cadastro, carrega os documentos legais vigentes do
    // backend (igual ao loadRegistrationLegalDocuments do app original) e
    // restaura o rascunho salvo no aparelho, se houver (Fase 3).
    val isRegisterMode = uiState.authMode is AuthMode.Register
    LaunchedEffect(isRegisterMode) {
        if (isRegisterMode) {
            authState.loadRegistrationLegalDocuments()
            authState.restoreSignupDraft()
        }
    }
    // Countdown do reenvio do código de reivindicação (1 s por vez).
    val claimResendIn = uiState.claimResendIn
    LaunchedEffect(claimResendIn) {
        if (claimResendIn > 0) {
            delay(1000)
            authState.tickClaimResendCountdown()
        }
    }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current

    // O erro aparece só no ErrorBanner, dentro do cartão. Ao surgir, rola o
    // formulário para o topo para o usuário ver o motivo da recusa.
    // Quem limpa a mensagem é a própria digitação: todo onChange do AuthState
    // zera errorMessage, assim como a troca de aba.
    val errorMessage = uiState.errorMessage
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) scrollState.animateScrollTo(0)
    }

    val successMessage = uiState.successMessage
    LaunchedEffect(successMessage) {
        successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            authState.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
        ) {
            val isLogin = uiState.authMode is AuthMode.Login

            // Layout estilo entregador: fundo branco, badge + título grande
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 28.dp)
                    .padding(top = 40.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Logo ItaSuper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 28.dp)
                ) {
                    // Logo oficial (igual ao app Android: itasuper_brand_icon).
                    Image(
                        painter = painterResource(Res.drawable.itasuper_brand_icon),
                        contentDescription = "ItaSuper",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("ita")
                            withStyle(SpanStyle(color = Color(0xFFFF6A00), fontWeight = FontWeight.ExtraBold)) {
                                append("super")
                            }
                        },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )
                }

                // Badge "PARA CLIENTES"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFFFF3E4))
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PARA CLIENTES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6A00),
                        letterSpacing = 0.3.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Título
                Text(
                    text = buildAnnotatedString {
                        if (isLogin) {
                            append("Que bom ter\nvocê de volta")
                        } else {
                            append("Crie sua\nconta")
                        }
                        withStyle(SpanStyle(color = Color(0xFFFF6A00))) { append(".") }
                    },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    lineHeight = 36.sp,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isLogin) "Entre para pedir das suas lojas favoritas."
                    else "Peça com rapidez, segurança e praticidade.",
                    fontSize = 14.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 21.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                errorMessage?.let { error ->
                    ErrorBanner(message = error)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isLogin) {
                    LoginForm(
                        uiState = uiState,
                        authState = authState,
                        onLoginClick = {
                            focusManager.clearFocus()
                            authState.handleLogin(onAuthSuccess)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Não tem conta? ",
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Criar conta",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6A00),
                            modifier = Modifier.clickable { authState.setAuthMode(AuthMode.Register) }
                        )
                    }
                } else {
                    RegisterForm(
                        uiState = uiState,
                        authState = authState,
                        onOpenLegalDocument = { url ->
                            runCatching { uriHandler.openUri(url) }
                        },
                        onContinueClick = {
                            focusManager.clearFocus()
                            authState.advanceSignupStep()
                        },
                        onRegisterClick = {
                            focusManager.clearFocus()
                            authState.handleRegister(onAuthSuccess)
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Já tem conta? ",
                            fontSize = 13.5.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Entrar",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF6A00),
                            modifier = Modifier.clickable { authState.setAuthMode(AuthMode.Login) }
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (uiState.showForgotPasswordDialog) {
        ForgotPasswordDialog(
            email = uiState.recoveryEmail,
            isSending = uiState.isSendingRecovery,
            onEmailChange = authState::onRecoveryEmailChange,
            onDismiss = authState::closeForgotPasswordDialog,
            onSend = authState::sendRecoveryEmail
        )
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ItaSuperPrimary,
    unfocusedBorderColor = ItaSuperBorder,
    focusedLabelColor = ItaSuperPrimary
)

@Composable
private fun LoginForm(
    uiState: AuthUiState,
    authState: AuthState,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // E-mail field
        OutlinedTextField(
            value = uiState.loginEmail,
            onValueChange = authState::onLoginEmailChange,
            label = { Text("E-mail") },
            placeholder = { Text("seu.email@exemplo.com") },
            leadingIcon = {
                Icon(painter = painterResource(Res.drawable.ic_ita_mail), contentDescription = null, tint = ItaSuperPrimary)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .testTag("login_email_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )

        // Password field
        OutlinedTextField(
            value = uiState.loginPassword,
            onValueChange = authState::onLoginPasswordChange,
            label = { Text("Senha") },
            placeholder = { Text("Mínimo 6 caracteres") },
            leadingIcon = {
                Icon(painter = painterResource(Res.drawable.ic_ita_lock), contentDescription = null, tint = ItaSuperPrimary)
            },
            trailingIcon = {
                IconButton(onClick = authState::toggleLoginPasswordVisibility) {
                    Icon(
                        painter = painterResource(if (uiState.isLoginPasswordVisible) Res.drawable.ic_ita_eye else Res.drawable.ic_ita_eye_off),
                        contentDescription = "Alternar visibilidade da senha"
                    )
                }
            },
            visualTransformation = if (uiState.isLoginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .testTag("login_password_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )

        // Forgot password link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = authState::openForgotPasswordDialog,
                modifier = Modifier.testTag("forgot_password_button")
            ) {
                Text(
                    text = "Esqueci minha senha",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ItaSuperPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Login Submit Button
        Button(
            onClick = onLoginClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_submit_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ItaSuperPrimary
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Entrar",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_ita_check_circle),
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "Acesso seguro e dados protegidos",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ItaSuperTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun RegisterForm(
    uiState: AuthUiState,
    authState: AuthState,
    onOpenLegalDocument: (String) -> Unit,
    onContinueClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SignupStepHeader(
            currentStep = uiState.signupStep,
            onStepClick = { authState.goToSignupStep(it) }
        )

        when {
            uiState.claimFallback -> ClaimFallbackCard(uiState = uiState, authState = authState)
            uiState.showClaimOtp -> ClaimOtpForm(uiState = uiState, authState = authState)
            uiState.signupStep == SignupStep.ACCOUNT -> SignupAccountStep(uiState = uiState, authState = authState)
            uiState.signupStep == SignupStep.ADDRESS -> SignupAddressStep(uiState = uiState, authState = authState)
            else -> SignupPinStep(uiState = uiState, authState = authState, onOpenLegalDocument = onOpenLegalDocument)
        }

        if (!uiState.showClaimOtp && !uiState.claimFallback) {
            if (uiState.signupStep != SignupStep.ACCOUNT) {
                TextButton(
                    onClick = {
                        val previous = SignupStep.fromNumber(uiState.signupStep.number - 1)
                        authState.goToSignupStep(previous)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voltar", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                }
            }
            Button(
                onClick = if (uiState.signupStep == SignupStep.PIN) onRegisterClick else onContinueClick,
                enabled = !uiState.isLoading && !uiState.claimBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("register_submit_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                if (uiState.isLoading || uiState.claimBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.signupStep == SignupStep.PIN) "Criar conta" else "Continuar",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

/** Cabeçalho dos 3 passos (Conta → Endereço → PIN), igual ao da web. */
@Composable
private fun SignupStepHeader(
    currentStep: SignupStep,
    onStepClick: (SignupStep) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (info in SIGNUP_STEPS) {
            val isCurrent = info.step == currentStep
            val isDone = info.step.number < currentStep.number
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onStepClick(info.step) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> ItaSuperPrimary
                                isDone -> ItaSuperSuccess
                                else -> Color(0xFFE2E8F0)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_ita_check),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = info.step.number.toString(),
                            color = if (isCurrent) Color.White else Color(0xFF64748B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = info.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) ItaSuperPrimary else Color(0xFF64748B)
                    )
                )
            }
        }
    }
    val currentInfo = SIGNUP_STEPS.first { it.step == currentStep }
    Text(
        text = currentInfo.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    )
    Text(
        text = currentInfo.subtitle,
        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
    )
}

@Composable
private fun fieldErrorText(field: String, errors: SignupErrors): (@Composable () -> Unit)? {
    val error = errors[field] ?: return null
    return {
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperError)
        )
    }
}

/** Passo 1 — Conta: e-mail, WhatsApp, nome e senha com as 5 regras. */
@Composable
private fun SignupAccountStep(uiState: AuthUiState, authState: AuthState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = uiState.regEmail,
            onValueChange = authState::onRegEmailChange,
            label = { Text("E-mail *") },
            placeholder = { Text("seu@email.com") },
            leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_ita_mail), contentDescription = null, tint = ItaSuperPrimary) },
            isError = uiState.signupFieldErrors["email"] != null,
            supportingText = fieldErrorText("email", uiState.signupFieldErrors),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_email_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
        OutlinedTextField(
            value = uiState.regWhatsapp,
            onValueChange = authState::onRegWhatsappChange,
            label = { Text("WhatsApp com DDD *") },
            placeholder = { Text("(21) 99999-9999") },
            leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_ita_phone), contentDescription = null, tint = ItaSuperPrimary) },
            isError = uiState.signupFieldErrors["whatsapp"] != null,
            supportingText = fieldErrorText("whatsapp", uiState.signupFieldErrors),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_whatsapp_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
        OutlinedTextField(
            value = uiState.regName,
            onValueChange = authState::onRegNameChange,
            label = { Text("Nome completo *") },
            placeholder = { Text("Ex: Maria Silva") },
            leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_ita_profile), contentDescription = null, tint = ItaSuperPrimary) },
            isError = uiState.signupFieldErrors["fullName"] != null,
            supportingText = fieldErrorText("fullName", uiState.signupFieldErrors),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_name_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
        OutlinedTextField(
            value = uiState.regPassword,
            onValueChange = authState::onRegPasswordChange,
            label = { Text("Senha *") },
            placeholder = { Text("Crie uma senha segura") },
            leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_ita_lock), contentDescription = null, tint = ItaSuperPrimary) },
            trailingIcon = {
                IconButton(onClick = authState::toggleRegPasswordVisibility) {
                    Icon(
                        painter = painterResource(if (uiState.isRegPasswordVisible) Res.drawable.ic_ita_eye else Res.drawable.ic_ita_eye_off),
                        contentDescription = "Alternar visibilidade"
                    )
                }
            },
            visualTransformation = if (uiState.isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = uiState.signupFieldErrors["password"] != null,
            supportingText = fieldErrorText("password", uiState.signupFieldErrors),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_password_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
        PasswordRulesList(password = uiState.regPassword)
    }
}

/** As 5 regras de senha da web, com o estado de cada uma. */
@Composable
private fun PasswordRulesList(password: String) {
    val passed = List(5) { index ->
        when (index) {
            0 -> password.length >= 8
            1 -> password.any { it.isUpperCase() }
            2 -> password.any { it.isLowerCase() }
            3 -> password.any { it.isDigit() }
            else -> password.any { !it.isLetterOrDigit() }
        }
    }
    val labels = listOf(
        "Mínimo 8 caracteres",
        "Uma letra maiúscula",
        "Uma letra minúscula",
        "Um número",
        "Um caractere especial"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Sua senha precisa de:",
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
        )
        for (i in labels.indices) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(if (passed[i]) Res.drawable.ic_ita_check_circle else Res.drawable.ic_ita_alert),
                    contentDescription = null,
                    tint = if (passed[i]) ItaSuperSuccess else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = labels[i],
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (passed[i]) ItaSuperSuccess else Color(0xFF64748B)
                    )
                )
            }
        }
    }
}

/** Passo 2 — Endereço: CEP com lookup, rua, número, bairro, complemento e referência. */
@Composable
private fun SignupAddressStep(uiState: AuthUiState, authState: AuthState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = uiState.regCep,
            onValueChange = authState::onRegCepChange,
            label = { Text("CEP *") },
            placeholder = { Text("00000-000") },
            trailingIcon = {
                if (uiState.isCepLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ItaSuperPrimary, strokeWidth = 2.dp)
                }
            },
            isError = uiState.signupFieldErrors["cep"] != null,
            supportingText = {
                val error = uiState.signupFieldErrors["cep"]
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperError))
                } else if (uiState.cepNotFound) {
                    Text(
                        "CEP não encontrado. Preencha os dados manualmente ou confira o CEP.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB45309))
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_cep_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
        OutlinedTextField(
            value = uiState.regStreet,
            onValueChange = authState::onRegStreetChange,
            label = { Text("Rua *") },
            placeholder = { Text("Ex: Rua das Flores") },
            isError = uiState.signupFieldErrors["street"] != null,
            supportingText = fieldErrorText("street", uiState.signupFieldErrors),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_street_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = uiState.regNumber,
                onValueChange = authState::onRegNumberChange,
                label = { Text("Número *") },
                placeholder = { Text("123") },
                isError = uiState.signupFieldErrors["number"] != null,
                supportingText = fieldErrorText("number", uiState.signupFieldErrors),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier
                    .weight(1f)
                    .testTag("reg_number_input"),
                shape = RoundedCornerShape(18.dp),
                colors = authFieldColors()
            )
            OutlinedTextField(
                value = uiState.regComplement,
                onValueChange = authState::onRegComplementChange,
                label = { Text("Complemento") },
                placeholder = { Text("Apto, casa...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier
                    .weight(1f)
                    .testTag("reg_complement_input"),
                shape = RoundedCornerShape(18.dp),
                colors = authFieldColors()
            )
        }
        OutlinedTextField(
            value = uiState.regNeighborhood,
            onValueChange = authState::onRegNeighborhoodChange,
            label = { Text("Bairro *") },
            placeholder = { Text("Ex: Centro") },
            isError = uiState.signupFieldErrors["neighborhood"] != null,
            supportingText = fieldErrorText("neighborhood", uiState.signupFieldErrors),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_neighborhood_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
        OutlinedTextField(
            value = uiState.regReferencePoint,
            onValueChange = authState::onRegReferencePointChange,
            label = { Text("Ponto de referência") },
            placeholder = { Text("Ex: Próximo à praça") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reg_reference_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
    }
}

/** Passo 3 — PIN: card do PIN + ciência + Termos/Privacidade. */
@Composable
private fun SignupPinStep(
    uiState: AuthUiState,
    authState: AuthState,
    onOpenLegalDocument: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ItaSuperHighlightBg)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = painterResource(Res.drawable.ic_ita_keypad), contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PIN de Entrega (4 dígitos)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = "O PIN garante a segurança no momento da entrega do seu pedido.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                OutlinedTextField(
                    value = uiState.regPin,
                    onValueChange = authState::onRegPinChange,
                    label = { Text("PIN de entrega (4 dígitos) *") },
                    placeholder = { Text("Ex: 1234") },
                    leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_ita_key), contentDescription = null, tint = ItaSuperPrimary) },
                    visualTransformation = if (uiState.isRegPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = authState::toggleRegPinVisibility) {
                            Icon(
                                painter = painterResource(if (uiState.isRegPinVisible) Res.drawable.ic_ita_eye else Res.drawable.ic_ita_eye_off),
                                contentDescription = "Alternar visibilidade PIN"
                            )
                        }
                    },
                    isError = uiState.signupFieldErrors["deliveryPin"] != null,
                    supportingText = fieldErrorText("deliveryPin", uiState.signupFieldErrors),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_pin_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = authFieldColors()
                )
                OutlinedTextField(
                    value = uiState.regPinConfirm,
                    onValueChange = authState::onRegPinConfirmChange,
                    label = { Text("Confirme o PIN de entrega *") },
                    placeholder = { Text("Repita o PIN de 4 dígitos") },
                    leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_ita_check), contentDescription = null, tint = ItaSuperPrimary) },
                    visualTransformation = if (uiState.isRegPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = uiState.signupFieldErrors["confirmPin"] != null,
                    supportingText = fieldErrorText("confirmPin", uiState.signupFieldErrors),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_pin_confirm_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = authFieldColors()
                )
                if (uiState.regPin.length == 4 && uiState.regPinConfirm.length == 4) {
                    val isMatch = uiState.regPin == uiState.regPinConfirm
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(if (isMatch) Res.drawable.ic_ita_check_circle else Res.drawable.ic_ita_alert),
                            contentDescription = null,
                            tint = if (isMatch) ItaSuperSuccess else ItaSuperError,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMatch) "PINs coincidem!" else "Os PINs informados não coincidem",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isMatch) ItaSuperSuccess else ItaSuperError,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = uiState.regPinAcknowledged,
                onCheckedChange = authState::onRegPinAcknowledgedChange,
                colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary),
                modifier = Modifier.testTag("pin_ack_checkbox")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Entendi: usarei este PIN em todas as minhas entregas para confirmar o recebimento.",
                    style = MaterialTheme.typography.bodySmall
                )
                uiState.signupFieldErrors["pinAcknowledged"]?.let { error ->
                    Text(error, style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperError))
                }
            }
        }

        val legalDocuments = uiState.pendingLegalChanges
        if (uiState.isLoadingLegalDocuments) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ItaSuperPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Carregando Termos e Política de Privacidade...", style = MaterialTheme.typography.bodySmall)
            }
        } else if (legalDocuments != null) {
            if (legalDocuments.needsTerms) {
                LegalRegisterCheckbox(
                    checked = uiState.isTermsAccepted,
                    onCheckedChange = authState::onTermsAcceptedChange,
                    prefix = "Li e aceito os ",
                    title = "Termos de Uso",
                    version = legalDocuments.currentTermsVersion,
                    url = LegalDocumentLinks.TERMS_URL,
                    onOpen = onOpenLegalDocument,
                    testTag = "terms_checkbox"
                )
            }
            if (legalDocuments.needsPrivacy) {
                LegalRegisterCheckbox(
                    checked = uiState.isPrivacyAccepted,
                    onCheckedChange = authState::onPrivacyAcceptedChange,
                    prefix = "Li e aceito a ",
                    title = "Política de Privacidade",
                    version = legalDocuments.currentPrivacyVersion,
                    url = LegalDocumentLinks.PRIVACY_URL,
                    onOpen = onOpenLegalDocument,
                    testTag = "privacy_checkbox"
                )
            }
            val termsError = uiState.signupFieldErrors["acceptedTerms"]
                ?: uiState.signupFieldErrors["acceptedPrivacy"]
            termsError?.let { error ->
                Text(error, style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperError))
            }
        } else {
            Text(
                "Os Termos e a Política de Privacidade precisam ser carregados antes do cadastro.",
                style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperError)
            )
        }
    }
}

/**
 * Tela do código de 6 dígitos (claim guest): aparece no lugar do passo 1
 * quando o claim-check achou um cadastro fantasma. A verificação de verdade
 * acontece no submit final, junto com a conversão (claim-verify-otp).
 */
@Composable
private fun ClaimOtpForm(uiState: AuthUiState, authState: AuthState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ItaSuperHighlightBg)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Você já pediu aqui antes!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = buildString {
                        append("Encontramos um cadastro com esse WhatsApp")
                        uiState.claimLastStoreName?.let { append(" (último pedido na $it)") }
                        append(". Enviamos um código de 6 dígitos no seu WhatsApp para confirmar que é você.")
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                )
            }
        }
        OutlinedTextField(
            value = uiState.claimCode,
            onValueChange = authState::onClaimCodeChange,
            label = { Text("Código de 6 dígitos *") },
            placeholder = { Text("000000") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("claim_code_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )
        Button(
            onClick = authState::confirmClaimCode,
            enabled = uiState.claimCode.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("claim_code_confirm_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
        ) {
            Text(
                text = "Confirmar código",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = authState::resendClaimOtp,
                enabled = uiState.claimResendIn == 0 && !uiState.claimBusy
            ) {
                Text(
                    text = if (uiState.claimResendIn > 0) "Reenviar código em ${uiState.claimResendIn}s" else "Reenviar código",
                    color = ItaSuperPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = authState::cancelClaimOtp) {
                Text("Trocar número", color = ItaSuperPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Fallback (Opção A): a loja do último pedido não tem WhatsApp conectado. */
@Composable
private fun ClaimFallbackCard(uiState: AuthUiState, authState: AuthState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ItaSuperHighlightBg)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Não conseguimos enviar o código",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = buildString {
                        append("A loja do seu último pedido")
                        uiState.claimLastStoreName?.let { append(" ($it)") }
                        append(" não está com o WhatsApp conectado no momento. Você pode reivindicar sua conta pela página de acompanhamento do pedido ou tentar de novo mais tarde.")
                    },
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569))
                )
            }
        }
        Button(
            onClick = authState::resendClaimOtp,
            enabled = !uiState.claimBusy,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
        ) {
            Text(
                text = "Tentar enviar novamente",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            )
        }
        TextButton(
            onClick = authState::cancelClaimOtp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Trocar número", color = ItaSuperPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LegalRegisterCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    prefix: String,
    title: String,
    version: String,
    url: String,
    onOpen: (String) -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary),
                modifier = Modifier.testTag(testTag)
            )
            Column(modifier = Modifier.padding(top = 10.dp).weight(1f)) {
                Text(
                    text = "$prefix$title versão $version.",
                    style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperTextSecondary)
                )
                Text(
                    text = "Ler $title",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ItaSuperPrimary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .clickable { onOpen(url) }
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_ita_alert),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    email: String,
    isSending: Boolean,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Recuperar Senha",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Informe seu e-mail cadastrado no ItaSuper. Enviaremos um link de recuperação.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("E-mail para recuperação") },
                    leadingIcon = { Icon(painter = painterResource(Res.drawable.ic_ita_mail), contentDescription = null, tint = ItaSuperPrimary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_password_email_input"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ItaSuperPrimary)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSend,
                enabled = !isSending,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                modifier = Modifier.testTag("send_recovery_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Enviar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
