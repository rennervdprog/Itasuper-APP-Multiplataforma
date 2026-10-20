package com.example.shared.ui.cart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.shared.model.CartItem
import com.example.shared.model.Order
import com.example.shared.model.PixDirectPaymentUiState
import com.example.shared.model.PixPaymentUiState
import com.example.shared.model.Product
import com.example.shared.model.SelectedAddonItem
import com.example.shared.network.SupabaseApi
import com.example.shared.platform.SecureStorage
import com.example.shared.ui.mock.mockSavedAddresses
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Formatação manual de moeda para o commonMain (sem String.format).
 * Ex: 12.9 -> "R$ 12,90" | 1234.5 -> "R$ 1.234,50"
 */
fun formatBRL(valor: Double): String {
    val sinal = if (valor < 0) "-" else ""
    val totalCentavos = (abs(valor) * 100 + 0.5).toLong()
    val inteiro = totalCentavos / 100
    val centavos = (totalCentavos % 100).toInt()
    val intStr = inteiro.toString().reversed().chunked(3).joinToString(".").reversed()
    val centStr = if (centavos < 10) "0$centavos" else "$centavos"
    return "${sinal}R$ $intStr,$centStr"
}

/**
 * Normaliza a forma de pagamento de exibição para o vocabulário do backend.
 * (Porte de OrdersViewModel.checkoutOrder do app original — o backend/PDV
 * consome "pix", "pix_machine", "pix_direto", "cartao", "dinheiro".)
 */
fun normalizePaymentMethod(display: String): String = when (display.trim().lowercase()) {
    "pix", "pix online" -> "pix"
    "pix na maquininha" -> "pix_machine"
    "pix direto" -> "pix_direto"
    "cartão", "cartao", "cartão na entrega" -> "cartao"
    "dinheiro", "dinheiro na entrega" -> "dinheiro"
    else -> display.trim().lowercase()
}

/**
 * Status inicial do pedido conforme a forma de pagamento.
 * (Porte de OrdersViewModel.checkoutOrder do app original — o status
 * "recebido" não existe na máquina de estados do backend.)
 */
fun initialOrderStatus(normalizedPaymentMethod: String): String = when (normalizedPaymentMethod) {
    "pix" -> "aguardando_pagamento"
    "pix_direto" -> "aguardando_comprovante"
    else -> "pendente"
}

enum class DeliveryType { DELIVERY, PICKUP }

/**
 * Estado do carrinho (mockado). Substitui o OrdersViewModel do Android:
 * estado observável via mutableStateOf, sem ViewModel Android.
 *
 * TODO(fase-rede): carregar/salvar o carrinho real via Ktor/SupabaseClient,
 * cotar a entrega de verdade e validar o cupom no backend.
 */
/** Chaves do SecureStorage para a sessão (iguais às do AuthScreen). */
private const val KEY_USER_ID = "session_user_id"
private const val KEY_ACCESS_TOKEN = "session_access_token"

class CartHolder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val secureStorage = SecureStorage()

    var storeId by mutableStateOf<String?>(null)
        private set
    var storeName by mutableStateOf("")
        private set
    var items by mutableStateOf<List<CartItem>>(emptyList())
        private set
    var deliveryType by mutableStateOf(DeliveryType.DELIVERY)
        private set
    var deliveryFee by mutableStateOf(0.0)
        private set

    /**
     * Busca a taxa de entrega oficial da loja via RPC.
     * Chamado quando a loja é definida no carrinho.
     */
    fun refreshDeliveryFee() {
        val sid = storeId ?: return
        scope.launch {
            try {
                val fee = SupabaseApi.fetchOfficialCustomerDeliveryFee(sid)
                if (fee != null) {
                    deliveryFee = fee
                }
            } catch (_: Exception) {
                // Mantém 0.0 em caso de erro (sem mocks)
            }
        }
    }

    var couponInput by mutableStateOf("")
    var appliedCouponCode by mutableStateOf<String?>(null)

    /**
     * Cupom pendente vindo da seção "Lojas com Cupom" da Home (porte do
     * CartRepository do app original): a Home grava código + loja ao tocar no
     * card e a tela do carrinho consome uma única vez, só se for da mesma loja.
     */
    var pendingCouponCode by mutableStateOf<String?>(null)
    var pendingCouponStoreId by mutableStateOf<String?>(null)
        private set
    var couponDiscount by mutableStateOf(0.0)
        private set
    var couponError by mutableStateOf<String?>(null)
        private set

    var selectedAddressId by mutableStateOf<String?>(null)
    var paymentMethod by mutableStateOf("PIX Online")
    var needsChange by mutableStateOf(false)
    var changeForAmount by mutableStateOf("")
    var scheduleNow by mutableStateOf(true)
    var scheduledSlot by mutableStateOf<String?>(null)

    var isPlacingOrder by mutableStateOf(false)
        private set
    var placedOrder by mutableStateOf<Order?>(null)
        private set
    var orderError by mutableStateOf<String?>(null)
        private set

    var pixPayment by mutableStateOf<PixPaymentUiState?>(null)
        private set
    var pixDirectPayment by mutableStateOf<PixDirectPaymentUiState?>(null)
        private set

    val subtotal: Double
        get() = items.sumOf { it.totalPrice }

    val totalItemCount: Int
        get() = items.sumOf { it.quantity }

    val effectiveDeliveryFee: Double
        get() = if (deliveryType == DeliveryType.PICKUP || items.isEmpty()) 0.0 else deliveryFee

    val effectiveCouponDiscount: Double
        get() = couponDiscount

    val total: Double
        get() = (subtotal + effectiveDeliveryFee - couponDiscount).coerceAtLeast(0.0)

    // Carrinho começa vazio — sem dados mockados (regra do Renner: nada de dado fake).
    // Os itens são adicionados pela vitrine via addProduct().


    /**
     * Indica se adicionar este produto trocaria de loja (sacola com itens de
     * outra loja) — a vitrine usa isso para exibir o StoreSwitchBottomSheet
     * antes de esvaziar, em vez do auto-clear silencioso do addProduct.
     */
    fun isSwitchingStore(product: Product): Boolean =
        items.isNotEmpty() && storeId != null && storeId != product.storeId

    /**
     * Adiciona um produto na sacola (usado pela vitrine da loja).
     * Se a sacola tem itens de outra loja, esvazia antes (comportamento de
     * fallback; a vitrine pede confirmação via StoreSwitchBottomSheet usando
     * isSwitchingStore; TODO(fase-rede): confirmar com o usuário).
     */
    fun addProduct(
        product: Product,
        quantity: Int = 1,
        notes: String = "",
        addons: List<SelectedAddonItem> = emptyList(),
        storeName: String = ""
    ) {
        if (items.isNotEmpty() && storeId != null && storeId != product.storeId) {
            clearCart()
        }
        if (storeId == null) {
            storeId = product.storeId
            if (storeName.isNotBlank()) this.storeName = storeName
            refreshDeliveryFee()
        }
        val existing = items.indexOfFirst {
            it.product.id == product.id && it.notes == notes && it.selectedAddons == addons
        }
        items = if (existing >= 0) {
            items.toMutableList().also {
                it[existing] = it[existing].copy(quantity = it[existing].quantity + quantity)
            }
        } else {
            items + CartItem(product = product, quantity = quantity, notes = notes, selectedAddons = addons)
        }
    }

    fun updateQuantity(productId: String, newQuantity: Int) {
        items = if (newQuantity <= 0) {
            items.filterNot { it.product.id == productId }
        } else {
            items.map { if (it.product.id == productId) it.copy(quantity = newQuantity) else it }
        }
    }

    fun clearCart() {
        items = emptyList()
        appliedCouponCode = null
        couponDiscount = 0.0
        couponInput = ""
        couponError = null
    }

    fun selectDeliveryType(type: DeliveryType) {
        deliveryType = type
    }

    fun applyCoupon() {
        couponError = null
        val code = couponInput.trim().uppercase()
        if (code.isBlank()) {
            couponError = "Digite um código de cupom."
            return
        }
        scope.launch {
            try {
                val coupon = SupabaseApi.fetchCoupon(code, storeId)
                if (coupon == null) {
                    couponError = "Cupom inválido ou expirado."
                    return@launch
                }
                // Verifica valor mínimo do pedido
                if (subtotal < coupon.minOrderValue) {
                    couponError = "Pedido mínimo de R$ ${coupon.minOrderValue} para este cupom."
                    return@launch
                }
                // Calcula desconto
                val discount = when (coupon.discountType) {
                    "percentage" -> (subtotal * coupon.discountValue / 100.0)
                    else -> coupon.discountValue
                }.coerceAtMost(subtotal)
                
                appliedCouponCode = coupon.code
                couponDiscount = discount
            } catch (_: Exception) {
                couponError = "Erro ao validar cupom. Tente novamente."
            }
        }
    }

    fun removeCoupon() {
        appliedCouponCode = null
        couponDiscount = 0.0
        couponInput = ""
        couponError = null
    }

    fun selectAddress(id: String) {
        selectedAddressId = id
    }

    fun selectPaymentMethod(method: String) {
        paymentMethod = method
        if (method != "Dinheiro") {
            needsChange = false
            changeForAmount = ""
        }
    }

    fun toggleNeedsChange(value: Boolean) {
        needsChange = value
    }

    /**
     * Envia o pedido real via Supabase. Sem mock (regra: nada de dado fake).
     * Exige sessão ativa (login); sem sessão, pede login (igual ao app original).
     */
    fun placeOrder() {
        if (items.isEmpty() || isPlacingOrder) return
        orderError = null
        if (deliveryType == DeliveryType.DELIVERY && selectedAddressId == null) {
            orderError = "Selecione um endereço de entrega."
            return
        }
        // Verifica a sessão salva (login obrigatório para pedir)
        val userId = try { secureStorage.get(KEY_USER_ID) } catch (_: Throwable) { null }
        val accessToken = try { secureStorage.get(KEY_ACCESS_TOKEN) } catch (_: Throwable) { null }
        if (userId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            orderError = "Faça login para finalizar o pedido."
            return
        }

        isPlacingOrder = true
        scope.launch {
            try {
                val normalizedPayment = normalizePaymentMethod(paymentMethod)
                val order = Order(
                    id = "",
                    storeId = storeId ?: "",
                    storeName = storeName,
                    items = items,
                    subtotal = subtotal,
                    deliveryFee = effectiveDeliveryFee,
                    discount = couponDiscount,
                    total = total,
                    paymentMethod = normalizedPayment,
                    deliveryAddress = deliveryAddressSummary(),
                    status = initialOrderStatus(normalizedPayment),
                    createdAt = ""
                )
                val changeFor = changeForAmount.replace(",", ".").trim().toDoubleOrNull()
                val result = SupabaseApi.submitOrder(
                    order = order,
                    clientId = userId,
                    accessToken = accessToken,
                    needsChange = needsChange,
                    changeFor = changeFor
                )
                if (result.isSuccess) {
                    placedOrder = order.copy(
                        id = result.orderId ?: "",
                        createdAt = result.createdAt ?: ""
                    )
                } else {
                    orderError = result.errorMessage ?: "Não foi possível enviar o pedido. Tente novamente."
                }
            } catch (_: Throwable) {
                orderError = "Não foi possível enviar o pedido. Verifique sua conexão e tente novamente."
            }
            isPlacingOrder = false
        }
    }

    fun dismissSuccess() {
        placedOrder = null
        clearCart()
    }

    // ------------------------------------------------------------------
    // PIX — porte de OrdersViewModel.generatePixPayment / openPixDirectPayment
    // / uploadPixDirectProof do app original.
    // ------------------------------------------------------------------

    fun generatePixPayment(order: Order) {
        val uid = try { secureStorage.get(KEY_USER_ID) } catch (_: Throwable) { null }
        val token = try { secureStorage.get(KEY_ACCESS_TOKEN) } catch (_: Throwable) { null }
        val payerName = try { secureStorage.get("session_user_name") } catch (_: Throwable) { null }.orEmpty()
        val payerDoc = try { secureStorage.get("session_user_cpf") } catch (_: Throwable) { null }.orEmpty()
        if (uid.isNullOrBlank() || token.isNullOrBlank()) {
            orderError = "Sua sessão expirou. Entre novamente para pagar com PIX."
            return
        }
        pixPayment = PixPaymentUiState(order = order, isLoading = true)
        scope.launch {
            val response = SupabaseApi.generatePixForOrder(order, payerName, payerDoc, token)
            pixPayment = if (response.isSuccess) {
                PixPaymentUiState(
                    order = order,
                    pixCode = response.pixCode,
                    qrCodeBase64 = response.qrCodeBase64
                )
            } else {
                PixPaymentUiState(
                    order = order,
                    errorMessage = response.errorMessage ?: "Não foi possível gerar o PIX."
                )
            }
        }
    }

    fun dismissPixPayment() {
        pixPayment = null
    }

    fun openPixDirectPayment(order: Order) {
        pixDirectPayment = PixDirectPaymentUiState(order = order)
        scope.launch {
            val store = try { SupabaseApi.fetchStoreById(order.storeId) } catch (_: Throwable) { null }
            if (store == null || !store.pixDirectEnabled || store.pixDirectKey.isBlank()) {
                pixDirectPayment = PixDirectPaymentUiState(
                    order = order,
                    errorMessage = "A chave PIX direto desta loja não está disponível."
                )
                return@launch
            }
            pixDirectPayment = PixDirectPaymentUiState(
                order = order,
                storeName = store.name,
                pixKey = store.pixDirectKey,
                pixKeyType = store.pixDirectKeyType,
                beneficiary = store.pixDirectBeneficiary,
                instructions = store.pixDirectInstructions
            )
        }
    }

    fun uploadPixDirectProof(bytes: ByteArray, mimeType: String, extension: String) {
        val state = pixDirectPayment ?: return
        val order = state.order ?: return
        val token = try { secureStorage.get(KEY_ACCESS_TOKEN) } catch (_: Throwable) { null }
        if (token.isNullOrBlank()) {
            pixDirectPayment = state.copy(errorMessage = "Sua sessão expirou. Entre novamente para enviar o comprovante.")
            return
        }
        pixDirectPayment = state.copy(isUploading = true, errorMessage = null)
        scope.launch {
            val result = SupabaseApi.uploadPixDirectProof(
                orderId = order.id,
                storeId = order.storeId,
                bytes = bytes,
                mimeType = mimeType,
                extension = extension,
                accessToken = token
            )
            val current = pixDirectPayment ?: state
            pixDirectPayment = result.fold(
                onSuccess = { current.copy(isUploading = false, proofSent = true) },
                onFailure = { e ->
                    current.copy(
                        isUploading = false,
                        errorMessage = e.message ?: "Não foi possível enviar o comprovante."
                    )
                }
            )
        }
    }

    fun dismissPixDirectPayment() {
        pixDirectPayment = null
    }

    fun deliveryAddressSummary(): String {
        if (deliveryType == DeliveryType.PICKUP) return "Retirada na loja: $storeName"
        val address = mockSavedAddresses().firstOrNull { it.id == selectedAddressId }
        return address?.let {
            listOf(it.displayLine, it.neighborhood, it.city)
                .filter { part -> part.isNotBlank() }
                .joinToString(" - ")
        } ?: "Endereço não informado"
    }
}

@Composable
fun rememberCartState(): CartHolder = remember { CartHolder() }

