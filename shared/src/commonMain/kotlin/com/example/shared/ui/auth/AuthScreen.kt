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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import com.example.shared.model.LegalDocumentLinks
import com.example.shared.model.PendingLegalChanges
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
 * (Supabase) está mockada — ver TODO(fase-rede).
 *
 * Diferenças intencionais vs. o original Android:
 * - Ícones: drawables (R.drawable.ic_ita_*) viraram Icons.Filled do
 *   material-icons-core; o logo oficial ainda não foi migrado para
 *   composeResources (ver TODO(fase-recursos) no cabeçalho).
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

    // Register fields
    val regName: String = "",
    val regCpfCnpj: String = "",
    val regWhatsapp: String = "",
    val regPassword: String = "",
    val isRegPasswordVisible: Boolean = false,
    val regPin: String = "",
    val regPinConfirm: String = "",
    val isRegPinVisible: Boolean = false,
    val isTermsAccepted: Boolean = false,
    val isPrivacyAccepted: Boolean = false,
    val isLoadingLegalDocuments: Boolean = false,

    // TODO(fase-rede): hoje é mock fixo; buscar do backend (fetchPendingLegalChanges).
    val pendingLegalChanges: PendingLegalChanges? = mockPendingLegalChanges(),

    // Error messages per field or general
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,

    // Dialog states
    val showForgotPasswordDialog: Boolean = false,
    val recoveryEmail: String = "",
    val isSendingRecovery: Boolean = false
)

// TODO(fase-rede): trocar pelo retorno real do backend. Versões alinhadas ao
// que está publicado hoje no site (Termos v6.7).
private fun mockPendingLegalChanges(): PendingLegalChanges = PendingLegalChanges(
    needsTerms = true,
    needsPrivacy = true,
    currentTermsVersion = "6.7",
    currentPrivacyVersion = "6.7",
    termsChanges = emptyList(),
    privacyChanges = emptyList()
)

// android.util.Patterns não existe no commonMain — validação manual.
// Regex.matches() exige match da string inteira, então não precisa de âncora $.
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
private fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

/**
 * Equivalente KMP do AuthViewModel: estado observável via mutableStateOf e
 * os mesmos handlers/validações do original. Ações de rede estão mockadas
 * (sucesso imediato após validação) — a integração real com o Supabase vem
 * na fase-rede.
 */
private class AuthState {
    var uiState by mutableStateOf(AuthUiState())
        private set

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

    // Register updates
    fun onRegNameChange(value: String) {
        uiState = uiState.copy(regName = value, errorMessage = null)
    }

    fun onRegCpfCnpjChange(value: String) {
        uiState = uiState.copy(regCpfCnpj = Masks.formatCpfCnpj(value), errorMessage = null)
    }

    fun onRegWhatsappChange(value: String) {
        uiState = uiState.copy(regWhatsapp = Masks.formatPhone(value), errorMessage = null)
    }

    fun onRegPasswordChange(value: String) {
        uiState = uiState.copy(regPassword = value, errorMessage = null)
    }

    fun toggleRegPasswordVisibility() {
        uiState = uiState.copy(isRegPasswordVisible = !uiState.isRegPasswordVisible)
    }

    fun onRegPinChange(value: String) {
        uiState = uiState.copy(regPin = value.filter { it.isDigit() }.take(4), errorMessage = null)
    }

    fun onRegPinConfirmChange(value: String) {
        uiState = uiState.copy(regPinConfirm = value.filter { it.isDigit() }.take(4), errorMessage = null)
    }

    fun toggleRegPinVisibility() {
        uiState = uiState.copy(isRegPinVisible = !uiState.isRegPinVisible)
    }

    fun onTermsAcceptedChange(accepted: Boolean) {
        uiState = uiState.copy(isTermsAccepted = accepted, errorMessage = null)
    }

    fun onPrivacyAcceptedChange(accepted: Boolean) {
        uiState = uiState.copy(isPrivacyAccepted = accepted, errorMessage = null)
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
        // TODO(fase-rede): SupabaseClient.requestPasswordRecovery(email).
        uiState = uiState.copy(
            isSendingRecovery = false,
            showForgotPasswordDialog = false,
            successMessage = "E-mail de recuperação enviado. Verifique sua caixa de entrada.",
            errorMessage = null
        )
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

        // TODO(fase-rede): SupabaseClient.signIn(email, password) + salvar sessão
        // (UserSessionRepository). Hoje: mock — qualquer e-mail/senha válidos entram.
        uiState = state.copy(isLoading = false, errorMessage = null)
        onSuccess()
    }

    fun handleRegister(onSuccess: () -> Unit) {
        val state = uiState

        val name = state.regName.trim()
        if (name.isBlank() || !name.contains(" ")) {
            uiState = state.copy(errorMessage = "Informe seu nome completo")
            return
        }
        if (!Masks.isValidCpfOrCnpj(state.regCpfCnpj)) {
            uiState = state.copy(errorMessage = "CPF ou CNPJ inválido")
            return
        }
        if (!Masks.isValidPhone(state.regWhatsapp)) {
            uiState = state.copy(errorMessage = "Informe um WhatsApp válido com DDD")
            return
        }
        if (state.regPassword.length < 6) {
            uiState = state.copy(errorMessage = "Senha: mínimo 6 caracteres")
            return
        }
        if (state.regPin.length != 4) {
            uiState = state.copy(errorMessage = "Defina um PIN de entrega com 4 dígitos numéricos")
            return
        }
        if (state.regPin != state.regPinConfirm) {
            uiState = state.copy(errorMessage = "Os PINs informados não coincidem")
            return
        }

        val legalDocuments = state.pendingLegalChanges
        if (legalDocuments == null) {
            uiState = state.copy(errorMessage = "Aguarde o carregamento dos Termos e da Política de Privacidade.")
            return
        }
        if ((legalDocuments.needsTerms && !state.isTermsAccepted) ||
            (legalDocuments.needsPrivacy && !state.isPrivacyAccepted)
        ) {
            uiState = state.copy(errorMessage = "Você precisa aceitar os Termos de Uso e a Política de Privacidade para continuar.")
            return
        }

        // TODO(fase-rede): SupabaseClient.signUp + insertProfile + recordLegalAcceptance
        // + salvar sessão (UserSessionRepository.register). Hoje: mock — validações
        // passando, a conta é "criada".
        uiState = state.copy(isLoading = false, errorMessage = null)
        onSuccess()
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    val authState = remember { AuthState() }
    val uiState = authState.uiState
    val snackbarHostState = remember { SnackbarHostState() }
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
                    // TODO(fase-recursos): trocar pelo logo oficial (compose resources)
                    // quando os drawables da marca forem migrados do app Android.
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ItaSuperPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = "ItaSuper",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
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
                Icon(Icons.Filled.Email, contentDescription = null, tint = ItaSuperPrimary)
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
                Icon(Icons.Filled.Lock, contentDescription = null, tint = ItaSuperPrimary)
            },
            trailingIcon = {
                IconButton(onClick = authState::toggleLoginPasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.isLoginPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
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
                imageVector = Icons.Filled.CheckCircle,
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
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Nome Completo
        OutlinedTextField(
            value = uiState.regName,
            onValueChange = authState::onRegNameChange,
            label = { Text("Nome completo *") },
            placeholder = { Text("Ex: Maria Silva") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = ItaSuperPrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("reg_name_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )

        // CPF / CNPJ com máscara e validação
        OutlinedTextField(
            value = uiState.regCpfCnpj,
            onValueChange = authState::onRegCpfCnpjChange,
            label = { Text("CPF ou CNPJ *") },
            placeholder = { Text("000.000.000-00") },
            leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = ItaSuperPrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("reg_cpf_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )

        // WhatsApp com DDD
        OutlinedTextField(
            value = uiState.regWhatsapp,
            onValueChange = authState::onRegWhatsappChange,
            label = { Text("WhatsApp com DDD *") },
            placeholder = { Text("(21) 99999-9999") },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = ItaSuperPrimary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("reg_whatsapp_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )

        // Senha
        OutlinedTextField(
            value = uiState.regPassword,
            onValueChange = authState::onRegPasswordChange,
            label = { Text("Senha *") },
            placeholder = { Text("Mínimo 6 caracteres") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = ItaSuperPrimary) },
            trailingIcon = {
                IconButton(onClick = authState::toggleRegPasswordVisibility) {
                    Icon(
                        imageVector = if (uiState.isRegPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Alternar visibilidade"
                    )
                }
            },
            visualTransformation = if (uiState.isRegPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("reg_password_input"),
            shape = RoundedCornerShape(18.dp),
            colors = authFieldColors()
        )

        // Box de PIN de entrega (4 dígitos numéricos)
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
                    Icon(Icons.Filled.Dialpad, contentDescription = null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
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

                // PIN input
                OutlinedTextField(
                    value = uiState.regPin,
                    onValueChange = authState::onRegPinChange,
                    label = { Text("PIN de entrega (4 dígitos) *") },
                    placeholder = { Text("Ex: 1234") },
                    leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = ItaSuperPrimary) },
                    visualTransformation = if (uiState.isRegPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = authState::toggleRegPinVisibility) {
                            Icon(
                                imageVector = if (uiState.isRegPinVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = "Alternar visibilidade PIN"
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("reg_pin_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = authFieldColors()
                )

                // PIN Confirmation
                OutlinedTextField(
                    value = uiState.regPinConfirm,
                    onValueChange = authState::onRegPinConfirmChange,
                    label = { Text("Confirme o PIN de entrega *") },
                    placeholder = { Text("Repita o PIN de 4 dígitos") },
                    leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null, tint = ItaSuperPrimary) },
                    visualTransformation = if (uiState.isRegPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("reg_pin_confirm_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = authFieldColors()
                )

                // Visual Pin Match Indicator
                if (uiState.regPin.length == 4 && uiState.regPinConfirm.length == 4) {
                    val isMatch = uiState.regPin == uiState.regPinConfirm
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isMatch) Icons.Filled.CheckCircle else Icons.Filled.Warning,
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
        } else {
            Text(
                "Os Termos e a Política de Privacidade precisam ser carregados antes do cadastro.",
                style = MaterialTheme.typography.bodySmall.copy(color = ItaSuperError)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Submit Button
        Button(
            onClick = onRegisterClick,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("register_submit_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Criar conta",
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
                imageVector = Icons.Filled.Warning,
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
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = ItaSuperPrimary) },
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
