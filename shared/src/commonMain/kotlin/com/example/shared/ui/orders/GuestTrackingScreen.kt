package com.example.shared.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.TRACKING_POLL_MS
import com.example.shared.model.claimGuestAccountErrorMessage
import com.example.shared.model.guestOrderStatusLabel
import com.example.shared.model.guestTrackingErrorMessage
import com.example.shared.model.isTerminalOrderStatus
import com.example.shared.model.parseTrackingLinkOrId
import com.example.shared.model.validateClaimGuestAccountForm
import com.example.shared.model.GuestOrderStatus
import com.example.shared.network.SupabaseApi
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.util.formatBRL
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// =============================================================================
// Fase 4 (G7/G9) — rastreio guest + "Reivindicar minha conta".
// Espelha a página pública da web (/p/:orderId?t=<últimos4>):
// - Formulário: código do pedido (UUID ou o link de rastreio colado) +
//   últimos 4 dígitos do WhatsApp. A edge `guest-order-status` valida e
//   devolve pedido + loja + itens + PIN; erro 429 vira o aviso de "muitas
//   tentativas" (o rate limit vive no servidor — o app só mostra).
// - Detalhe: status, PIN, loja (+ "Falar com a loja" no WhatsApp), itens
//   e totais, e a seção "Criar minha conta com este pedido" (edge
//   `claim-guest-account`). Atualiza sozinho a cada 20 s, igual à web.
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestTrackingScreen(
    onBack: () -> Unit,
    onTrackGuest: (orderId: String, last4: String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var last4 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acompanhar pedido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Acompanhe um pedido feito sem login com o código do pedido e os últimos 4 dígitos do WhatsApp usado na compra.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5D5D5D)
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it; error = null },
                label = { Text("Código do pedido ou link de rastreio") },
                placeholder = { Text("Cole o link recebido ou o código") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = last4,
                onValueChange = { last4 = it.filter { c -> c.isDigit() }.take(4); error = null },
                label = { Text("Últimos 4 dígitos do WhatsApp") },
                placeholder = { Text("Ex.: 5555") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD32F2F))
            }
            Button(
                onClick = {
                    val parsed = parseTrackingLinkOrId(code)
                    val cleanLast4 = last4.filter { it.isDigit() }
                    when {
                        parsed == null -> error = "Confira o código do pedido."
                        cleanLast4.length != 4 -> error = "Digite os últimos 4 dígitos do WhatsApp."
                        else -> {
                            isLoading = true
                            error = null
                            scope.launch {
                                try {
                                    val result = SupabaseApi.fetchGuestOrderStatus(parsed.orderId, cleanLast4)
                                    if (result?.status != null) {
                                        onTrackGuest(parsed.orderId, cleanLast4)
                                    } else {
                                        error = guestTrackingErrorMessage(result?.error)
                                    }
                                } catch (_: Throwable) {
                                    error = "Não foi possível buscar o pedido. Tente de novo."
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Acompanhar pedido", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestTrackingDetail(
    orderId: String,
    last4: String,
    onBack: () -> Unit,
    onGoToLogin: () -> Unit,
    onOpenUrl: (String) -> Boolean
) {
    var status by remember { mutableStateOf<GuestOrderStatus?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    suspend fun load(initial: Boolean) {
        if (initial) isLoading = true
        try {
            val result = SupabaseApi.fetchGuestOrderStatus(orderId, last4)
            if (result?.status != null) {
                status = result.status
                error = null
            } else {
                error = result?.error
            }
        } catch (_: Throwable) {
            error = error ?: "error"
        } finally {
            if (initial) isLoading = false
        }
    }

    // Atualiza sozinho a cada 20 s, igual à página da web; para quando o
    // pedido chega a estado final.
    LaunchedEffect(orderId, last4) {
        load(initial = true)
        while (isActive) {
            delay(TRACKING_POLL_MS)
            val current = status
            if (current != null && isTerminalOrderStatus(current.order.status)) break
            load(initial = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seu pedido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ItaSuperPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Carregando pedido...", color = Color(0xFF737373))
                }
            }
            return@Scaffold
        }
        val current = status
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        guestTrackingErrorMessage(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5D5D5D),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onBack) {
                        Text("Voltar para o início", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                    }
                }
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            // Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val orderStatus = current.order.status.lowercase()
                    val statusColor = when (orderStatus) {
                        "entregue", "finalizado" -> Color(0xFF16A34A)
                        "cancelado" -> Color(0xFFD32F2F)
                        else -> ItaSuperPrimary
                    }
                    Icon(
                        when (orderStatus) {
                            "entregue", "finalizado" -> Icons.Filled.CheckCircle
                            "saiu_para_entrega", "saiu_entrega", "em_transito" -> Icons.Filled.LocalShipping
                            else -> Icons.Filled.Schedule
                        },
                        null, tint = statusColor, modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        guestOrderStatusLabel(current.order.status),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        "Pedido #${current.order.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF737373)
                    )
                    current.customerName?.takeIf { it.isNotBlank() }?.let {
                        Text("Olá, $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // PIN de entrega
            val pin = current.deliveryPin ?: current.order.deliveryPin
            val orderStatus = current.order.status.lowercase()
            if (pin.isNotBlank() && orderStatus != "entregue" && orderStatus != "finalizado" && orderStatus != "cancelado") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E6)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VpnKey, null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Seu PIN de entrega", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            pin,
                            style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 8.sp),
                            fontWeight = FontWeight.Black,
                            color = ItaSuperPrimary
                        )
                        Text(
                            "Informe ao entregador para liberar o pedido. Não compartilhe este código com mais ninguém.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D5D5D),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Loja + WhatsApp
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Store, null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            current.store?.name?.ifBlank { "Loja" } ?: "Loja",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (current.order.addressDetails.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = Color(0xFF737373), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                listOf(current.order.addressDetails, current.order.neighborhood)
                                    .filter { it.isNotBlank() }.joinToString(" — "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5D5D5D)
                            )
                        }
                    }
                    current.store?.whatsappNumber?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }?.let { digits ->
                        Button(
                            onClick = { onOpenUrl("https://wa.me/$digits") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Message, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Falar com a loja no WhatsApp", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Itens + totais
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Text("Itens", fontWeight = FontWeight.Bold)
                    current.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                            Text("${item.quantity}× ${item.productName.ifBlank { "Item" }}", style = MaterialTheme.typography.bodyMedium)
                            Text(formatBRL(item.unitPrice * item.quantity), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                        Text(formatBRL(current.order.subtotal), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                        Text("Entrega", style = MaterialTheme.typography.bodyMedium)
                        Text(formatBRL(current.order.deliveryFee), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text(formatBRL(current.order.totalPrice), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Pagamento: ${current.order.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF737373)
                    )
                }
            }

            // Fase 6D Opção A — "Reivindicar minha conta"
            GuestClaimCard(
                orderId = orderId,
                last4 = last4,
                onGoToLogin = onGoToLogin
            )

            Text(
                "Esta página atualiza sozinha a cada 20s.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF737373),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GuestClaimCard(
    orderId: String,
    last4: String,
    onGoToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var password2 by remember { mutableStateOf("") }
    var claiming by remember { mutableStateOf(false) }
    var claimDone by remember { mutableStateOf(false) }
    var claimError by remember { mutableStateOf<String?>(null) }
    var claimAlreadyHasAccount by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            Text("Criar minha conta com este pedido", fontWeight = FontWeight.Bold)
            if (claimDone) {
                Text(
                    "Conta criada! Entre com seu e-mail e senha.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Seu histórico de pedidos, endereços e pontos continuam na mesma conta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D5D5D)
                )
                Button(
                    onClick = onGoToLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Entrar com e-mail e senha", fontWeight = FontWeight.Bold) }
            } else {
                Text(
                    "Crie sua conta com o e-mail e a senha que você quiser usar para entrar. Seus pedidos e endereços continuam na mesma conta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D5D5D)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; claimError = null },
                    label = { Text("Seu e-mail") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; claimError = null },
                    label = { Text("Senha (mínimo 6 caracteres)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = password2,
                    onValueChange = { password2 = it; claimError = null },
                    label = { Text("Confirmar senha") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                claimError?.let {
                    Text(claimGuestAccountErrorMessage(it), style = MaterialTheme.typography.bodySmall, color = Color(0xFFD32F2F))
                }
                if (claimAlreadyHasAccount) {
                    TextButton(onClick = onGoToLogin, modifier = Modifier.fillMaxWidth()) {
                        Text("Ir para o login", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                    }
                }
                Button(
                    onClick = {
                        val formError = validateClaimGuestAccountForm(email, password, password2)
                        if (formError != null) {
                            claimError = formError
                            claimAlreadyHasAccount = false
                            return@Button
                        }
                        claiming = true
                        claimError = null
                        claimAlreadyHasAccount = false
                        scope.launch {
                            try {
                                val result = SupabaseApi.claimGuestAccount(orderId, last4, email, password)
                                if (result?.ok == true) {
                                    claimDone = true
                                } else {
                                    claimError = result?.error ?: "error"
                                    claimAlreadyHasAccount = result?.error == "already_has_account"
                                }
                            } catch (_: Throwable) {
                                claimError = "error"
                            } finally {
                                claiming = false
                            }
                        }
                    },
                    enabled = !claiming,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (claiming) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (claiming) "Criando conta..." else "Criar minha conta", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
