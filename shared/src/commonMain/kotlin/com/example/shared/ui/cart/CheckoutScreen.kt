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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.shared.model.DeliveryAddressInput
import com.example.shared.model.LoyaltyConfig
import com.example.shared.model.SavedAddress
import com.example.shared.model.Store
import com.example.shared.model.normalizeBrazilianUf
import com.example.shared.model.preorderReleaseAtMillis
import com.example.shared.network.SupabaseApi
import com.example.shared.session.UserSessionRepository
import kotlinx.coroutines.launch
import com.example.shared.theme.ItaSuperBorder
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSuccess
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

// Checkout REAL (Fase E do mapa do app original): endereços salvos da API
// (+ endereço do perfil mesclado), ViaCEP de verdade, cotação oficial via
// quote-delivery sincronizada pelo carrinho, política de localização GPS,
// carteira/fidelidade reais e envio do pedido com snapshot da cotação.
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
    val session by UserSessionRepository.session.collectAsState()
    val scope = rememberCoroutineScope()
    val isPickup = cart.deliveryType == DeliveryType.PICKUP
    // Igual ao original: deliveryQuoteReady = RETIRADA ||
    // (!isQuotingDelivery && hasOfficialDeliveryQuote) — a cotação oficial
    // (quote-delivery) é sincronizada pelo carrinho.
    val deliveryQuoteReady = isPickup ||
        (!cart.isQuotingDelivery && cart.hasOfficialDeliveryQuote)

    // Benefícios REAIS (carteira + fidelidade) — porte do
    // loadCheckoutBenefits/recalculateBenefits do app original: carrega por
    // loja quando o carrinho muda de loja; fórmulas idênticas.
    var walletBalance by remember { mutableStateOf(0.0) }
    var loyaltyConfig by remember { mutableStateOf<LoyaltyConfig?>(null) }
    var loyaltyPointsAvailable by remember { mutableStateOf(0) }
    var benefitsStoreId by remember { mutableStateOf<String?>(null) }
    var loyaltyPointsToUse by remember { mutableStateOf(0) }
    var useWallet by remember { mutableStateOf(false) }

    LaunchedEffect(cart.storeId, session.userId) {
        val sid = cart.storeId
        if (sid.isNullOrBlank() || session.userId.isBlank() || session.accessToken.isBlank()) {
            benefitsStoreId = sid
            walletBalance = 0.0
            loyaltyConfig = null
            loyaltyPointsAvailable = 0
            return@LaunchedEffect
        }
        if (benefitsStoreId == sid) return@LaunchedEffect
        val sameStore = benefitsStoreId == sid
        benefitsStoreId = sid
        if (!sameStore) {
            useWallet = false
            loyaltyPointsToUse = 0
        }
        val wallet = try {
            SupabaseApi.fetchWalletBalance(session.userId, session.accessToken)
        } catch (_: Throwable) {
            null
        }
        walletBalance = (wallet?.balance ?: 0.0).coerceAtLeast(0.0)
        val config = try {
            SupabaseApi.fetchLoyaltyConfig(sid, session.accessToken)
        } catch (_: Throwable) {
            null
        }
        loyaltyConfig = config
        loyaltyPointsAvailable = if (config != null) {
            try {
                SupabaseApi.fetchLoyaltyBalance(session.userId, sid, session.accessToken).points
            } catch (_: Throwable) {
                0
            }
        } else {
            0
        }
    }

    val loyaltyConfigActive = loyaltyConfig?.takeIf { it.isEnabled && benefitsStoreId == cart.storeId }
    val loyaltyMinPointsRedeem = loyaltyConfigActive?.minPointsRedeem ?: 0
    val loyaltyDiscountPerPoint = loyaltyConfigActive?.discountPerPoint ?: 0.0
    val loyaltyMaxPointsUsable = if (loyaltyConfigActive == null || loyaltyDiscountPerPoint <= 0.0) {
        0
    } else {
        val maxByPercent = cart.subtotal * (loyaltyConfigActive.maxDiscountPercent / 100.0)
        val maxByBalance = loyaltyPointsAvailable * loyaltyDiscountPerPoint
        kotlin.math.floor(
            minOf(maxByPercent, maxByBalance, cart.subtotal) / loyaltyDiscountPerPoint
        ).toInt().coerceAtLeast(0)
    }
    // Pontos efetivos: limitados ao máximo e zerados abaixo do mínimo.
    val loyaltyPointsEffective = loyaltyPointsToUse.coerceAtMost(loyaltyMaxPointsUsable).takeIf {
        loyaltyConfigActive != null && it >= loyaltyConfigActive.minPointsRedeem
    } ?: 0
    val loyaltyDiscount = loyaltyPointsEffective * loyaltyDiscountPerPoint
    val walletDiscount = if (useWallet) minOf(walletBalance, (cart.total - loyaltyDiscount).coerceAtLeast(0.0)) else 0.0
    val checkoutTotal = (cart.total - loyaltyDiscount - walletDiscount).coerceAtLeast(0.0)
    val showBenefits = loyaltyMinPointsRedeem > 0 || walletBalance > 0

    // Endereços salvos do usuário (tabela saved_addresses via Supabase),
    // com o endereço do perfil mesclado no topo — porte do
    // mergeProfileAddress/loadSavedAddresses do app original.
    var savedAddresses by remember { mutableStateOf<List<SavedAddress>>(emptyList()) }
    val selectedAddress = savedAddresses.firstOrNull { it.id == cart.selectedAddressId }
    var addressEditedByCustomer by remember { mutableStateOf(false) }
    var usingGpsAddress by remember { mutableStateOf(false) }

    fun profileSavedAddress(): SavedAddress? {
        val s = session
        val hasAnyAddressData = listOf(
            s.addressStreet, s.addressNumber, s.addressNeighborhood, s.addressCity, s.addressCep
        ).any { it.isNotBlank() }
        if (!hasAnyAddressData) return null
        return SavedAddress(
            id = "__profile_address__",
            label = "Endereço do perfil",
            street = s.addressStreet,
            number = s.addressNumber,
            complement = s.addressComplement,
            neighborhood = s.addressNeighborhood,
            city = s.addressCity,
            state = normalizeBrazilianUf(s.addressState),
            referencePoint = s.addressReferencePoint,
            cep = s.addressCep.filter(Char::isDigit).take(8),
            isDefault = true
        )
    }

    fun mergeProfileAddress(remoteAddresses: List<SavedAddress>): List<SavedAddress> {
        val profile = profileSavedAddress() ?: return remoteAddresses
        fun normalized(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
        val withoutDuplicate = remoteAddresses.filterNot { address ->
            normalized(address.street) == normalized(profile.street) &&
                normalized(address.number) == normalized(profile.number) &&
                address.cep.filter(Char::isDigit) == profile.cep
        }
        return listOf(profile) + withoutDuplicate
    }

    fun reloadAddresses() {
        val s = session
        if (s.userId.isBlank() || s.accessToken.isBlank()) return
        scope.launch {
            try {
                val remote = SupabaseApi.fetchSavedAddresses(s.userId, s.accessToken)
                val merged = mergeProfileAddress(remote)
                savedAddresses = merged
                cart.savedAddresses = merged
            } catch (_: Exception) {
                // Mantém a lista atual em caso de erro (sem mocks)
            }
        }
    }

    // Carrega endereços reais do backend (com o JWT da sessão, como no original)
    LaunchedEffect(session.userId) {
        if (session.userId.isNotBlank()) reloadAddresses()
    }

    LaunchedEffect(savedAddresses) {
        if (usingGpsAddress || addressEditedByCustomer) return@LaunchedEffect
        if (cart.selectedAddressId == null || savedAddresses.none { it.id == cart.selectedAddressId }) {
            val def = savedAddresses.firstOrNull { it.isDefault } ?: savedAddresses.firstOrNull()
            if (def != null) cart.selectAddress(def.id)
        }
    }

    // Editor de endereço (campos = endereço atual do checkout, como no
    // original: a seleção de um endereço salvo preenche os campos).
    var showAddressEditor by remember { mutableStateOf(false) }
    var cep by remember { mutableStateOf("") }
    var cepError by remember { mutableStateOf<String?>(null) }
    var street by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var complement by remember { mutableStateOf("") }
    var showGpsAddressConfirmation by remember { mutableStateOf(false) }
    var addressFormError by remember { mutableStateOf<String?>(null) }

    fun applyAddressFields(address: SavedAddress?) {
        cep = address?.cep ?: session.addressCep
        street = address?.street ?: session.addressStreet
        number = address?.number ?: session.addressNumber
        neighborhood = address?.neighborhood ?: session.addressNeighborhood
        city = address?.city?.ifBlank { session.addressCity } ?: session.addressCity
        state = normalizeBrazilianUf(address?.state?.ifBlank { session.addressState } ?: session.addressState)
        complement = listOfNotNull(address?.complement, address?.referencePoint)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { session.addressComplement }
        if (address != null) {
            cart.setDeliveryCoordinates(address.latitude, address.longitude)
        }
    }

    // Seleção aplicada → campos + possível divergência de GPS (política de
    // localização: GPS fresco a >300m ou cidade/rua diferentes pergunta).
    LaunchedEffect(selectedAddress?.id, session.activeLocationUpdatedAt) {
        if (addressEditedByCustomer || usingGpsAddress) return@LaunchedEffect
        applyAddressFields(selectedAddress)
        if (!isPickup && selectedAddress != null) {
            showGpsAddressConfirmation = activeLocationDiffersFromAddress(session, selectedAddress)
        }
    }

    fun currentAddressInput(): DeliveryAddressInput = DeliveryAddressInput(
        street = street,
        number = number,
        complement = complement,
        neighborhood = neighborhood,
        city = city,
        state = state,
        cep = cep
    )

    // Sincroniza a cotação oficial sempre que o endereço/subtotal/loja muda.
    LaunchedEffect(
        cart.storeId, cart.subtotal, cart.deliveryType,
        street, number, neighborhood, city, state, cep
    ) {
        cart.synchronizeDeliveryQuote(currentAddressInput())
    }

    // Falha de cotação reabre o editor (igual ao original: o endereço
    // digitado precisa ser corrigível).
    LaunchedEffect(cart.deliveryQuoteFailure) {
        if (cart.deliveryQuoteFailure != null && !isPickup) {
            showAddressEditor = true
        }
    }

    fun openAddressEditor() {
        cepError = null
        showAddressEditor = true
    }

    /** Salva o endereço atual na API (createSavedAddress do original). */
    fun saveCurrentAddress() {
        val s = session
        if (s.userId.isBlank() || s.accessToken.isBlank()) return
        if (street.isBlank() || number.isBlank() || neighborhood.isBlank() ||
            city.isBlank() || normalizeBrazilianUf(state).isBlank() ||
            cep.filter(Char::isDigit).length != 8
        ) {
            addressFormError = "Complete rua, número, bairro, cidade, UF e CEP válido antes de salvar o endereço."
            return
        }
        val useGpsCoordinates = usingGpsAddress
        scope.launch {
            val saved = try {
                SupabaseApi.createSavedAddress(
                    userId = s.userId,
                    accessToken = s.accessToken,
                    address = SavedAddress(
                        label = "Novo endereço",
                        street = street.trim(),
                        number = number.trim(),
                        complement = complement.trim(),
                        neighborhood = neighborhood.trim(),
                        city = city.trim(),
                        state = normalizeBrazilianUf(state),
                        cep = cep.filter(Char::isDigit),
                        latitude = if (useGpsCoordinates) s.activeLocationLatitude else null,
                        longitude = if (useGpsCoordinates) s.activeLocationLongitude else null,
                        pinConfirmed = useGpsCoordinates &&
                            s.activeLocationLatitude != null && s.activeLocationLongitude != null,
                        isDefault = savedAddresses.isEmpty()
                    )
                )
            } catch (_: Throwable) {
                null
            }
            if (saved == null) {
                addressFormError = "Não foi possível salvar este endereço."
            } else {
                addressFormError = null
                addressEditedByCustomer = false
                showAddressEditor = false
                reloadAddresses()
            }
        }
    }

    /** Busca o CEP no ViaCEP real (porte do searchAddressByCep do original). */
    fun searchAddressByCep() {
        addressEditedByCustomer = true
        val cepInput = cep.trim()
        if (cepInput.isBlank()) {
            cepError = "Informe o CEP."
            return
        }
        cepError = null
        scope.launch {
            val result = try {
                SupabaseApi.fetchAddressByCep(cepInput)
            } catch (_: Throwable) {
                null
            }
            if (result != null) {
                cep = result.cep.filter(Char::isDigit).take(8)
                    .ifBlank { cepInput.filter(Char::isDigit).take(8) }
                if (result.street.isNotBlank()) street = result.street
                if (result.neighborhood.isNotBlank()) neighborhood = result.neighborhood
                if (result.city.isNotBlank()) city = result.city
                if (result.state.isNotBlank()) state = result.state.uppercase()
                cepError = null
                cart.invalidateDeliveryQuote(currentAddressInput())
            } else {
                cepError = "CEP não encontrado. Preencha o endereço manualmente."
            }
        }
    }

    /** "Usar localização atual": preenche o editor SÓ com dados do GPS. */
    fun useGpsAddressForCheckout() {
        val s = session
        if (!isActiveLocationFresh(s)) {
            showGpsAddressConfirmation = false
            addressFormError = "Atualize sua localização atual antes de usá-la no checkout."
            return
        }
        val gpsCep = s.activeLocationCep.filter(Char::isDigit).take(8)
        addressEditedByCustomer = true
        cart.setDeliveryCoordinates(s.activeLocationLatitude, s.activeLocationLongitude)
        cep = gpsCep
        street = s.activeLocationStreet
        number = s.activeLocationNumber
        neighborhood = s.activeLocationNeighborhood
        city = s.activeLocationCity
        state = normalizeBrazilianUf(s.activeLocationState)
        complement = ""
        showAddressEditor = true
        showGpsAddressConfirmation = false
        usingGpsAddress = true
        cepError = null
        addressFormError = if (gpsCep.length == 8) {
            null
        } else {
            "Não identificamos o CEP da localização atual. Preencha o CEP para calcular a entrega."
        }
        cart.invalidateDeliveryQuote(currentAddressInput())
    }

    /** "Usar endereço salvo": reaplica a seleção e invalida a cotação. */
    fun useSavedAddressForCheckout() {
        addressEditedByCustomer = false
        usingGpsAddress = false
        applyAddressFields(selectedAddress)
        showGpsAddressConfirmation = false
        cart.invalidateDeliveryQuote(currentAddressInput())
    }

    /**
     * "Confirmar localização e usar neste pedido" — porte do
     * confirmGpsAddressForCheckout: valida o endereço do GPS e a cotação
     * antes de fechar o editor (sem salvar o endereço).
     */
    fun confirmGpsAddress() {
        if (!usingGpsAddress) return
        val address = currentAddressInput()
        if (!address.isComplete()) {
            addressFormError = if (number.isBlank()) {
                "Informe o número do imóvel para confirmar a localização."
            } else {
                "Aguarde o endereço ser preenchido para confirmar a localização."
            }
            return
        }
        val quoteReady = cart.hasOfficialDeliveryQuote || cart.deliveryQuote?.isSuccessfulDelivery == true
        if (!quoteReady) {
            addressFormError = if (cart.isQuotingDelivery) {
                "Aguarde a confirmação da entrega antes de prosseguir."
            } else {
                "Não foi possível confirmar a entrega para este endereço. Revise os dados e tente novamente."
            }
            return
        }
        addressEditedByCustomer = false
        addressFormError = null
        showAddressEditor = false
        showGpsAddressConfirmation = false
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
                Button(onClick = { useGpsAddressForCheckout() }) {
                    Text("Usar localização atual")
                }
            },
            dismissButton = {
                TextButton(onClick = { useSavedAddressForCheckout() }) {
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
                                            onClick = {
                                                // Porte do selectSavedAddress: aplica
                                                // o endereço e invalida a cotação.
                                                addressEditedByCustomer = false
                                                usingGpsAddress = false
                                                cart.selectAddress(address.id)
                                                applyAddressFields(address)
                                                cart.invalidateDeliveryQuote(currentAddressInput())
                                            }
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
                                    onValueChange = { cep = it; cepError = null; addressEditedByCustomer = true },
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
                                    onValueChange = { street = it; addressEditedByCustomer = true },
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
                                    onValueChange = { number = it; addressEditedByCustomer = true },
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
                                    onValueChange = { neighborhood = it; addressEditedByCustomer = true },
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
                                    onValueChange = { city = it; addressEditedByCustomer = true },
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
                                onValueChange = { state = it; addressEditedByCustomer = true },
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
                                onValueChange = { complement = it; addressEditedByCustomer = true },
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
                            if (addressFormError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = addressFormError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            TextButton(
                                onClick = { saveCurrentAddress() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(if (usingGpsAddress) "Salvar para próximas compras" else "Salvar como endereço")
                            }
                            if (usingGpsAddress) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { confirmGpsAddress() },
                                    enabled = !cart.isQuotingDelivery,
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
                                cart.hasOfficialDeliveryQuote && cart.deliveryQuote == null && !cart.isQuotingDelivery -> {
                                    Text(
                                        text = "Taxa fixa confirmada pela regra da loja",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ItaSuperSuccess,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                cart.isQuotingDelivery -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ItaSuperPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Calculando a taxa de entrega...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                cart.deliveryQuote?.isSuccessfulDelivery == true -> {
                                    val quote = cart.deliveryQuote!!
                                    val km = quote.distance.km
                                    val kmText = if (km.isFinite()) {
                                        val rounded = kotlin.math.round(km * 10).toLong()
                                        "${rounded / 10},${rounded % 10}"
                                    } else {
                                        "0,0"
                                    }
                                    Text(
                                        text = if (quote.pricing.freeDeliveryApplied) {
                                            "Frete grátis aplicado pela regra da loja"
                                        } else {
                                            "Entrega confirmada para $kmText km"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ItaSuperSuccess,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                cart.deliveryQuoteFailure != null -> {
                                    Text(
                                        text = cart.deliveryQuoteFailure!!.userMessage(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                else -> {
                                    Text(
                                        text = if (usingGpsAddress && number.isBlank()) {
                                            "Digite o número do imóvel para calcular a entrega."
                                        } else if (usingGpsAddress) {
                                            "Confira o endereço e aguarde a confirmação da entrega para prosseguir."
                                        } else {
                                            "Preencha CEP, rua, número e bairro para calcular a entrega."
                                        },
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
                                cart.isQuotingDelivery -> Text("Calculando...", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text("Fidelidade ($loyaltyPointsEffective pontos)", color = ItaSuperSuccess)
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
                    onClick = {
                        // Agendamento + liberação de pré-pedido em ISO UTC,
                        // igual ao checkoutOrder do app original.
                        val isPreorderOrder = isPreorder && !cart.scheduleNow
                        val scheduledIso = if (isPreorderOrder) {
                            val slot = cart.scheduledSlot
                            if (slot != null) {
                                val parts = slot.split(":")
                                val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                val tz = TimeZone.currentSystemDefault()
                                val date = Clock.System.now().toLocalDateTime(tz).date
                                    .plus(scheduleDayOffset, DateTimeUnit.DAY)
                                LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, hour, minute)
                                    .toInstant(tz).toString()
                            } else {
                                ""
                            }
                        } else {
                            ""
                        }
                        val releaseIso = if (isPreorderOrder && normalizePaymentMethod(cart.paymentMethod) != "pix") {
                            store?.preorderReleaseAtMillis()?.let {
                                Instant.fromEpochMilliseconds(it).toString()
                            } ?: ""
                        } else {
                            ""
                        }
                        cart.placeOrder(
                            walletDiscount = walletDiscount,
                            loyaltyPointsUsed = loyaltyPointsEffective,
                            loyaltyDiscount = loyaltyDiscount,
                            addressInput = currentAddressInput(),
                            scheduledForIso = scheduledIso,
                            releaseAtIso = releaseIso,
                            isPreorder = isPreorderOrder
                        )
                    },
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
                            text = if (!isPickup && cart.isQuotingDelivery) {
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
