package com.example.shared.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.SavedAddress
import com.example.shared.model.Store
import com.example.shared.model.preorderReleaseAtMillis
import com.example.shared.network.SupabaseApi
import com.example.shared.platform.SecureStorage
import com.example.shared.theme.ItaSuperBorder
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSuccess
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// TODO(fase-rede): endereço (edição/busca CEP real, confirmação via GPS), cotação
// de entrega real, pagamento real (Woovi), fidelidade/carteira reais, agendamento
// real e envio do pedido precisam do SupabaseClient via Ktor.
// Layout espelha 1:1 o CheckoutScreen do app Android (Itasuper-APP-NATIVO):
// mesma estrutura, textos, dps e sps.
// Diferenças documentadas vs. Android:
// - DatePickerDialog/TimePickerDialog não existem no CMP: o "Escolher horário"
//   abre chips de dia (Hoje/Amanhã) + grade de horários em Compose puro.
// - Ícones de drawable (ic_ita_pix, ic_ita_card, ic_ita_cash, ic_ita_check,
//   ic_ita_store, ic_ita_pin, ic_ita_search) não existem no KMP: substituídos
//   por Icons.Filled equivalentes.
// - Diálogo de sucesso do pedido: no KMP o App.kt exibe a OrderSuccessScreen
//   em tela cheia quando cart.placedOrder != null (padrão já estabelecido).

private fun formatHourMinute(hour: Int, minute: Int): String {
    val hh = if (hour < 10) "0$hour" else "$hour"
    val mm = if (minute < 10) "0$minute" else "$minute"
    return "$hh:$mm"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckoutScreen(
    cart: CartHolder,
    onNavigateBack: () -> Unit
) {
    // Loja real via Supabase (sem mock). Carrega assincronamente; enquanto carrega,
    // as opções de pagamento que dependem da loja ficam indisponíveis.
    var store by remember { mutableStateOf<Store?>(null) }
    var storeLoadFailed by remember { mutableStateOf(false) }
    LaunchedEffect(cart.storeId) {
        val id = cart.storeId
        if (id != null) {
            try {
                store = SupabaseApi.fetchStoreById(id)
            } catch (e: Exception) {
                storeLoadFailed = true
            }
        }
    }
    val isPickup = cart.deliveryType == DeliveryType.PICKUP
    // No Android: deliveryQuoteReady = RETIRADA || (!isQuotingDelivery && hasOfficialDeliveryQuote).
    // No mock a cotação oficial está sempre pronta quando há taxa (fase-rede: vir do backend).
    val isQuotingDelivery = false
    val deliveryQuoteReady = isPickup || (!isQuotingDelivery && cart.deliveryFee > 0.0)

    // Benefícios (fidelidade + carteira): estrutura fiel ao Android; no mock não há
    // dados, então a seção fica oculta como no original (fase-rede: preencher via Ktor).
    val loyaltyPointsAvailable = 0
    val loyaltyMinPointsRedeem = 0
    val loyaltyDiscountPerPoint = 0.0
    val loyaltyMaxPointsUsable = 0
    var loyaltyPointsToUse by remember { mutableStateOf(0) }
    val walletBalance = 0.0
    var useWallet by remember { mutableStateOf(false) }
    val showBenefits = loyaltyMinPointsRedeem > 0 || walletBalance > 0
    val loyaltyDiscount = loyaltyPointsToUse * loyaltyDiscountPerPoint
    val walletDiscount = if (useWallet) minOf(walletBalance, (cart.total - loyaltyDiscount).coerceAtLeast(0.0)) else 0.0
    val checkoutTotal = (cart.total - loyaltyDiscount - walletDiscount).coerceAtLeast(0.0)

    // Endereços salvos do usuário (tabela saved_addresses via Supabase).
    var savedAddresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    val selectedAddress = savedAddresses.firstOrNull { it.id == cart.selectedAddressId }
    
    // Carrega endereços reais do backend
    LaunchedEffect(Unit) {
        try {
            val secureStorage = SecureStorage()
            val userId = secureStorage.get("session_user_id") ?: ""
            if (userId.isNotBlank()) {
                val addresses = SupabaseApi.fetchSavedAddresses(userId)
                savedAddresses = addresses
            }
        } catch (_: Exception) {
            // Mantém lista vazia em caso de erro (sem mocks)
        }
    }
    
    LaunchedEffect(savedAddresses) {
        if (cart.selectedAddressId == null || savedAddresses.none { it.id == cart.selectedAddressId }) {
            val def = savedAddresses.firstOrNull { it.isDefault } ?: savedAddresses.firstOrNull()
            if (def != null) cart.selectAddress(def.id)
        }
    }

    // Editor de endereço.
    var showAddressEditor by remember { mutableStateOf(false) }
    var cep by remember { mutableStateOf("") }
    var cepError by remember { mutableStateOf<String?>(null) }
    var street by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var complement by remember { mutableStateOf("") }
    // GPS real não disponível no mock (fase-rede): mantém a estrutura do Android.
    val usingGpsAddress = false
    var showGpsAddressConfirmation by remember { mutableStateOf(false) }

    fun openAddressEditor() {
        val addr = selectedAddress
        cep = addr?.cep ?: ""
        street = addr?.street ?: ""
        number = addr?.number ?: ""
        neighborhood = addr?.neighborhood ?: ""
        city = addr?.city ?: ""
        state = addr?.state ?: ""
        complement = addr?.complement ?: ""
        cepError = null
        showAddressEditor = true
    }

    fun saveCurrentAddress() {
        val currentId = selectedAddress?.id
        val id = currentId ?: "local_${Clock.System.now().toEpochMilliseconds()}"
        val updated = SavedAddress(
            id = id,
            label = selectedAddress?.label ?: "Novo endereço",
            street = street,
            number = number,
            complement = complement,
            neighborhood = neighborhood,
            city = city,
            state = state,
            cep = cep,
            pinConfirmed = usingGpsAddress,
            isDefault = selectedAddress?.isDefault ?: savedAddresses.isEmpty()
        )
        savedAddresses = if (savedAddresses.any { it.id == id }) {
            savedAddresses.map { if (it.id == id) updated else it }
        } else {
            savedAddresses + updated
        }
        cart.selectAddress(id)
        showAddressEditor = false
    }

    fun searchAddressByCep() {
        // Mock da busca de CEP (fase-rede: ViaCEP de verdade).
        val digits = cep.filter { it.isDigit() }
        if (digits == "28970000") {
            street = "Rua das Palmeiras"
            neighborhood = "Centro"
            city = "Araruama"
            state = "RJ"
            cepError = null
        } else {
            cepError = "Não encontramos endereços para este CEP."
        }
    }

    // Agendamento em Compose puro (substitui DatePickerDialog/TimePickerDialog).
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var scheduleDayOffset by remember { mutableStateOf(0) } // 0 = hoje, 1 = amanhã
    val scheduleDateLabel = remember(scheduleDayOffset) {
        val date = now.date.plus(scheduleDayOffset, DateTimeUnit.DAY)
        "${date.dayOfMonth}/${date.monthNumber}"
    }
    val scheduleSlots = remember(scheduleDayOffset) {
        buildList {
            if (scheduleDayOffset == 0) {
                var minutes = now.hour * 60 + now.minute + 30
                minutes = ((minutes + 29) / 30) * 30
                while (minutes <= 22 * 60 && size < 8) {
                    add(formatHourMinute(minutes / 60, minutes % 60))
                    minutes += 30
                }
            } else {
                var minutes = 11 * 60
                while (minutes <= 22 * 60) {
                    add(formatHourMinute(minutes / 60, minutes % 60))
                    minutes += 30
                }
            }
        }
    }
    val isPreorder = store?.preorderEnabled == true
    val preorderReleaseLabel = remember(store) {
        // Mesma regra do Android (Preorder.kt): millis da liberação do pré-pedido.
        val releaseMillis = store?.preorderReleaseAtMillis()
        releaseMillis?.let {
            val dt = Instant.fromEpochMilliseconds(it)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            formatHourMinute(dt.hour, dt.minute)
        }
    }

    if (showGpsAddressConfirmation && !isPickup) {
        AlertDialog(
            onDismissRequest = { showGpsAddressConfirmation = false },
            title = { Text("Confirmar local de entrega") },
            text = {
                Text(
                    "Sua localização atual parece diferente do endereço cadastrado. Deseja manter o endereço salvo ou confirmar os dados da localização atual?"
                )
            },
            confirmButton = {
                Button(onClick = { showGpsAddressConfirmation = false }) {
                    Text("Usar localização atual")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsAddressConfirmation = false }) {
                    Text("Usar endereço salvo")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Finalizar Pedido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFA)
                ),
                // O Scaffold raiz do App() ja aplica o inset da status bar;
                // deixar o default (systemBars) aqui duplicaria o espaco no topo.
                windowInsets = WindowInsets(0, 0, 0, 0)
)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Delivery Address / Pickup Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPickup) Icons.Filled.Storefront else Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPickup) "Local de Retirada" else "Endereço de Entrega",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!isPickup && savedAddresses.isNotEmpty()) {
                            Text(
                                text = "Endereços salvos",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            savedAddresses.forEach { address ->
                                val selected = address.id == cart.selectedAddressId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) Color(0xFFFFF0E6) else Color.White)
                                        .border(if (selected) 2.dp else 1.dp, if (selected) ItaSuperPrimary else Color(0xFFE2E2E2), RoundedCornerShape(16.dp))
                                        .selectable(
                                            selected = selected,
                                            onClick = { cart.selectAddress(address.id) }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Endereço selecionado",
                                            tint = ItaSuperPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .border(1.dp, ItaSuperBorder, CircleShape)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = address.label,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = listOf(address.displayLine, address.neighborhood)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (address.pinConfirmed) {
                                            Text(
                                                text = "Localização confirmada",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ItaSuperSuccess
                                            )
                                        }
                                    }
                                    if (!address.isDefault) {
                                        TextButton(onClick = {
                                            savedAddresses = savedAddresses.map {
                                                it.copy(isDefault = it.id == address.id)
                                            }
                                        }) {
                                            Text("Padrão")
                                        }
                                    }
                                    TextButton(onClick = {
                                        savedAddresses = savedAddresses.filterNot { it.id == address.id }
                                    }) {
                                        Text("Excluir", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (isPickup) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFFF0E6))
                                    .border(1.dp, Color(0xFFF7CBAE), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = cart.storeName.ifBlank { "Estabelecimento Parceiro" },
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperPrimary
                                    )
                                    Text(
                                        text = "Você deverá retirar o pedido diretamente na loja após confirmação de preparo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (!showAddressEditor) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                if (selectedAddress != null) {
                                    val addr = selectedAddress
                                    Text(
                                        text = listOf(addr.street, addr.number)
                                            .filter { it.isNotBlank() }
                                            .joinToString(", "),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = listOf(addr.neighborhood, addr.city)
                                            .filter { it.isNotBlank() }
                                            .joinToString(" - "),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (addr.complement.isNotBlank()) {
                                        Text(
                                            text = addr.complement,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    TextButton(
                                        onClick = { openAddressEditor() },
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Alterar endereço")
                                    }
                                } else {
                                    TextButton(onClick = { openAddressEditor() }) {
                                        Text("Cadastrar endereço")
                                    }
                                }
                            }
                        } else {
                            // Cadastro ou edição do endereço.
                            if (usingGpsAddress) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFFF3E8))
                                        .border(1.dp, Color(0xFFF7CBAE), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Localização atual selecionada",
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperPrimary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (number.isBlank()) {
                                            "1. Digite o número do imóvel. 2. Aguarde a confirmação da entrega. 3. Role até o final e toque em Confirmar e Enviar Pedido."
                                        } else {
                                            "Confira o número do imóvel. Depois aguarde a confirmação da entrega e toque em Confirmar e Enviar Pedido no final da tela."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = cep,
                                    onValueChange = { cep = it; cepError = null },
                                    label = { Text("CEP") },
                                    placeholder = { Text("00000-000") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_cep_input")
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { searchAddressByCep() },
                                    enabled = cep.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .height(54.dp)
                                        .testTag("checkout_search_cep_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Buscar CEP",
                                        tint = Color.White
                                    )
                                }
                            }

                            if (cepError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cepError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Street & Number
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = street,
                                    onValueChange = { street = it },
                                    label = { Text("Rua / Logradouro") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = ItaSuperBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(2f)
                                        .testTag("checkout_street_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = number,
                                    onValueChange = { number = it },
                                    label = { Text("Nº") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = ItaSuperBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_number_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Neighborhood & City
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = neighborhood,
                                    onValueChange = { neighborhood = it },
                                    label = { Text("Bairro") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = ItaSuperBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_neighborhood_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text("Cidade") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = ItaSuperBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("checkout_city_input")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("UF (opcional)") },
                                placeholder = { Text("Ex: RJ") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ItaSuperPrimary,
                                    unfocusedBorderColor = ItaSuperBorder,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_state_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = complement,
                                onValueChange = { complement = it },
                                label = { Text("Complemento / Ponto de Referência (opcional)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ItaSuperPrimary,
                                    unfocusedBorderColor = ItaSuperBorder,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_complement_input")
                            )
                            TextButton(
                                onClick = { saveCurrentAddress() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (usingGpsAddress) "Salvar para próximas compras" else "Salvar como endereço")
                            }
                            if (usingGpsAddress) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { saveCurrentAddress() },
                                    enabled = !isQuotingDelivery,
                                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("confirm_gps_address_button")
                                ) {
                                    Text(
                                        text = "Confirmar localização e usar neste pedido",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!isPickup) {
                            Spacer(modifier = Modifier.height(10.dp))
                            when {
                                deliveryQuoteReady -> {
                                    Text(
                                        text = "Taxa fixa confirmada pela regra da loja",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ItaSuperSuccess,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                isQuotingDelivery -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ItaSuperPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Calculando a taxa de entrega...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                // TODO(fase-rede): ramos de cotação real (distância em km,
                                // frete grátis aplicado, falha de cotação) como no Android.
                                else -> {
                                    Text(
                                        text = "Preencha CEP, rua, número e bairro para calcular a entrega.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Payment Method Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text(
                            text = "Forma de pagamento",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val paymentOptions = buildList {
                            val settings = store?.settings
                            if (settings?.acceptPixOnline == true) add("PIX Online")
                            if (settings?.acceptPixMachine == true) add("PIX na Maquininha")
                            val s = store
                            if (s?.pixDirectEnabled == true && s.pixDirectKey.isNotBlank()) add("PIX Direto")
                            if (settings?.acceptCard == true) add("Cartão")
                            if (settings?.acceptCash == true) add("Dinheiro")
                        }

                        LaunchedEffect(paymentOptions.joinToString("|"), cart.paymentMethod) {
                            if (paymentOptions.isNotEmpty() && cart.paymentMethod !in paymentOptions) {
                                cart.selectPaymentMethod(paymentOptions.first())
                            }
                        }

                        if (paymentOptions.isEmpty()) {
                            Text(
                                text = "A loja não possui uma forma de pagamento disponível no momento.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            paymentOptions.forEach { method ->
                                val isSelected = cart.paymentMethod == method
                                PaymentSelectionCard(
                                    method = method,
                                    selected = isSelected,
                                    onClick = { cart.selectPaymentMethod(method) }
                                )
                            }
                        }

                        // Dinheiro exato é permitido; o valor só é solicitado quando houver troco.
                        if (cart.paymentMethod == "Dinheiro") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (cart.needsChange) Color(0xFFFFF0E6) else Color(0xFFF7F7F7))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = cart.needsChange,
                                    onCheckedChange = { cart.toggleNeedsChange(it) },
                                    colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary)
                                )
                                Text(
                                    text = "Preciso de troco",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (cart.needsChange) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = cart.changeForAmount,
                                    onValueChange = { cart.changeForAmount = it },
                                    label = { Text("Troco para quanto?") },
                                    placeholder = { Text("Ex: 50,00 ou 100,00") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = ItaSuperBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("checkout_cash_change_input")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Informe o valor em cédulas para que o entregador leve o troco correto.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Agendamento. A data é opcional; no pré-pedido, sem seleção significa liberação na abertura.
            // No Android usa DatePickerDialog/TimePickerDialog; no CMP os horários são
            // escolhidos em chips de dia (Hoje/Amanhã) + grade de horários em Compose puro.
            if (store != null) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                            Text(
                                text = if (isPickup) "Agendar retirada" else "Agendar entrega",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isPreorder && preorderReleaseLabel != null) {
                                Text(
                                    text = "A loja está fechada, mas aceita pré-pedidos. O pedido será liberado às $preorderReleaseLabel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ItaSuperPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        cart.scheduleNow = true
                                        cart.scheduledSlot = null
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (cart.scheduleNow) ItaSuperPrimary else Color.White,
                                        contentColor = if (cart.scheduleNow) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (isPreorder) "Na abertura" else "Agora")
                                }
                                Button(
                                    onClick = { cart.scheduleNow = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!cart.scheduleNow) ItaSuperPrimary else Color.White,
                                        contentColor = if (!cart.scheduleNow) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Escolher horário")
                                }
                            }
                            if (!cart.scheduleNow) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Hoje" to 0, "Amanhã" to 1).forEach { (label, offset) ->
                                        val selected = scheduleDayOffset == offset
                                        Button(
                                            onClick = { scheduleDayOffset = offset },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected) ItaSuperPrimary else Color.White,
                                                contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(label, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                if (scheduleSlots.isEmpty()) {
                                    Text(
                                        text = "Sem horários disponíveis para este dia.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        scheduleSlots.forEach { time ->
                                            val label = "$scheduleDateLabel às $time"
                                            val selected = cart.scheduledSlot == label
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (selected) Color(0xFFFFF0E6) else Color.White)
                                                    .border(
                                                        if (selected) 2.dp else 1.dp,
                                                        if (selected) ItaSuperPrimary else Color(0xFFE2E2E2),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { cart.scheduledSlot = label }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = time,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            cart.scheduledSlot?.let { scheduledAt ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Agendado para $scheduledAt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ItaSuperPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // 4. Carteira e fidelidade (estrutura fiel ao Android; oculta no mock sem dados).
            if (showBenefits) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                            Text("Benefícios ItaSuper", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(10.dp))
                            if (loyaltyMinPointsRedeem > 0) {
                                val canRedeem = loyaltyMaxPointsUsable >= loyaltyMinPointsRedeem
                                Text(
                                    text = "Fidelidade nesta loja",
                                    fontWeight = FontWeight.SemiBold,
                                    color = ItaSuperPrimary
                                )
                                Text(
                                    text = "Você tem $loyaltyPointsAvailable pontos disponíveis.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (canRedeem) {
                                    val selectedPoints = loyaltyPointsToUse.takeIf { it > 0 } ?: loyaltyMinPointsRedeem
                                    val selectedDiscount = selectedPoints * loyaltyDiscountPerPoint
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            enabled = selectedPoints > loyaltyMinPointsRedeem,
                                            onClick = { loyaltyPointsToUse = (selectedPoints - 10).coerceAtLeast(0) }
                                        ) { Text("− 10") }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$selectedPoints pontos", fontWeight = FontWeight.Bold)
                                            Text("= ${formatBRL(selectedDiscount)}", color = ItaSuperSuccess, style = MaterialTheme.typography.bodySmall)
                                        }
                                        TextButton(
                                            enabled = selectedPoints < loyaltyMaxPointsUsable,
                                            onClick = { loyaltyPointsToUse = (selectedPoints + 10).coerceAtMost(loyaltyMaxPointsUsable) }
                                        ) { Text("+ 10") }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = { loyaltyPointsToUse = selectedPoints },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(
                                            if (loyaltyPointsToUse > 0) "Atualizar resgate" else "Aplicar $selectedPoints pontos",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (loyaltyPointsToUse > 0) {
                                        TextButton(
                                            onClick = { loyaltyPointsToUse = 0 },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Remover pontos aplicados") }
                                    }
                                } else {
                                    Text(
                                        text = "Você precisa de pelo menos $loyaltyMinPointsRedeem pontos para resgatar. Este pedido acumula novos pontos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            if (walletBalance > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (useWallet) Color(0xFFFFF0E6) else Color.White)
                                        .border(if (useWallet) 2.dp else 1.dp, if (useWallet) ItaSuperPrimary else Color(0xFFE2E2E2), RoundedCornerShape(16.dp))
                                        .selectable(
                                            selected = useWallet,
                                            onClick = { useWallet = !useWallet }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(if (useWallet) Color(0xFFFFE8DA) else Color(0xFFF5F5F5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AttachMoney,
                                            contentDescription = null,
                                            tint = if (useWallet) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Usar crédito da carteira", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (useWallet) "Usando ${formatBRL(walletDiscount)}" else "Saldo disponível: ${formatBRL(walletBalance)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (useWallet) ItaSuperSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (useWallet) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Carteira selecionada",
                                            tint = ItaSuperPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Error message banner
            if (cart.orderError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cart.orderError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Order Summary
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text(
                            text = "Resumo final do pedido",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal (${cart.totalItemCount} itens)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatBRL(cart.subtotal))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Taxa de entrega", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            when {
                                isPickup -> Text("Grátis (Retirada)", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                isQuotingDelivery -> Text("Calculando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                !deliveryQuoteReady -> Text("A confirmar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                cart.effectiveDeliveryFee == 0.0 -> Text("Grátis", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                else -> Text(formatBRL(cart.effectiveDeliveryFee))
                            }
                        }

                        if (cart.effectiveCouponDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cupom", color = ItaSuperSuccess)
                                Text("- ${formatBRL(cart.effectiveCouponDiscount)}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (loyaltyDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fidelidade ($loyaltyPointsToUse pontos)", color = ItaSuperSuccess)
                                Text("- ${formatBRL(loyaltyDiscount)}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (walletDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Carteira ItaSuper", color = ItaSuperSuccess)
                                Text("- ${formatBRL(walletDiscount)}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = ItaSuperBorder, modifier = Modifier.padding(vertical = 14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFEEDF))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total a pagar", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Text("Revise os dados antes de confirmar", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                text = formatBRL(checkoutTotal),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = ItaSuperPrimary
                            )
                        }
                    }
                }
            }

            // Submit Order Button
            item {
                Button(
                    onClick = { cart.placeOrder() },
                    enabled = !cart.isPlacingOrder && deliveryQuoteReady,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("confirm_place_order_button")
                ) {
                    if (cart.isPlacingOrder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Enviando pedido para ${cart.storeName.ifBlank { "a loja" }}...",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = if (!isPickup && isQuotingDelivery) {
                                "Calculando entrega..."
                            } else {
                                "Confirmar e Enviar Pedido • ${formatBRL(checkoutTotal)}"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun PaymentSelectionCard(
    method: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // No Android os ícones são drawables (ic_ita_pix/ic_ita_card/ic_ita_cash);
    // no KMP usam-se Icons.Filled equivalentes.
    val icon = when {
        method.contains("PIX", ignoreCase = true) -> Icons.Filled.Pix
        method.contains("Cartão", ignoreCase = true) -> Icons.Filled.CreditCard
        else -> Icons.Filled.AttachMoney
    }
    val description = when {
        method.equals("PIX Online", ignoreCase = true) -> "Pagamento confirmado no aplicativo"
        method.equals("PIX Direto", ignoreCase = true) -> "Envie o comprovante depois do pedido"
        method.equals("PIX na Maquininha", ignoreCase = true) -> "Pague na entrega"
        method.equals("Cartão", ignoreCase = true) -> "Pague na entrega"
        else -> "Informe o valor para troco"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFFFFF3EC) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .background(if (selected) ItaSuperPrimary else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        HorizontalDivider(color = Color(0xFFEAEAEA))
    }
}
