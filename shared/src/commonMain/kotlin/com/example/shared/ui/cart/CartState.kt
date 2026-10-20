package com.example.shared.ui.cart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.shared.model.CartItem
import com.example.shared.model.Coupon
import com.example.shared.model.DeliveryAddressInput
import com.example.shared.model.DeliveryQuote
import com.example.shared.model.DeliveryQuoteFailure
import com.example.shared.model.DeliveryQuoteResult
import com.example.shared.model.Order
import com.example.shared.model.PixDirectPaymentUiState
import com.example.shared.model.PixPaymentUiState
import com.example.shared.model.Product
import com.example.shared.model.SelectedAddonItem
import com.example.shared.model.SavedAddress
import com.example.shared.model.toSnapshot
import com.example.shared.network.SupabaseApi
import com.example.shared.platform.SecureStorage
import kotlin.math.abs
import kotlinx.datetime.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Formatação de moeda do carrinho — delega pra fonte única
 * (com.example.shared.util.formatBRL). Mantido aqui porque as telas do
 * carrinho/checkout já importam deste pacote.
 */
fun formatBRL(valor: Double): String = com.example.shared.util.formatBRL(valor)

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
 * Estado do carrinho e do checkout. Substitui o CartRepository +
 * OrdersViewModel do Android: estado observável via mutableStateOf, sem
 * ViewModel Android. Tudo real via SupabaseApi (pedido, cotação oficial,
 * cupom com guardas, carteira/fidelidade pós-pedido) — sem dados mockados.
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

    // ------------------------------------------------------------------
    // Perfil de entrega da loja + cotação oficial — porte do CartRepository
    // do app original. A taxa exibida vem EXCLUSIVAMENTE da cotação central
    // (ou da taxa fixa oficial da loja); sem cotação, a taxa é 0 e o
    // checkout não confirma (igual ao app atual).
    // ------------------------------------------------------------------

    /** Perfis de entrega lembrados por loja (tipo de taxa + taxa oficial). */
    private data class StoreDeliveryProfile(val feeType: String, val officialFee: Double?)
    private val deliveryProfiles = mutableMapOf<String, StoreDeliveryProfile>()

    /** Perfil configurado pela loja, registrado quando o item entrou na sacola. */
    var storeDeliveryFeeType by mutableStateOf("")
    var storeOfficialDeliveryFee by mutableStateOf<Double?>(null)

    /** Valor e coordenadas liberados exclusivamente pela cotação central. */
    var officialDeliveryFee by mutableStateOf<Double?>(null)
    var officialDeliveryDistanceKm by mutableStateOf<Double?>(null)
    var officialDeliveryQuoteKey by mutableStateOf<String?>(null)
    var deliveryLatitude by mutableStateOf<Double?>(null)
    var deliveryLongitude by mutableStateOf<Double?>(null)

    /** Última cotação (sucesso ou falha) + estado da sincronização. */
    var deliveryQuote by mutableStateOf<DeliveryQuote?>(null)
    var deliveryQuoteFailure by mutableStateOf<DeliveryQuoteFailure?>(null)
    var isQuotingDelivery by mutableStateOf(false)
    private var deliveryQuoteRequestKey: String? = null
    private var inFlightDeliveryQuoteKey: String? = null

    val hasOfficialDeliveryQuote: Boolean
        get() = deliveryType == DeliveryType.PICKUP ||
            (officialDeliveryFee != null && officialDeliveryQuoteKey != null)

    /** Taxa fixa oficial da loja (quando o tipo NÃO é por km). */
    fun fixedDeliveryFee(): Double? {
        if (deliveryType != DeliveryType.DELIVERY) return null
        storeOfficialDeliveryFee?.takeIf {
            !storeDeliveryFeeType.equals("km", ignoreCase = true)
        }?.let { return it }
        return null
    }

    /** Registra o perfil real carregado na página da loja (vitrine). */
    fun rememberStoreDeliveryProfile(storeId: String, feeType: String, officialFee: Double?) {
        if (storeId.isBlank()) return
        deliveryProfiles[storeId] = StoreDeliveryProfile(feeType, officialFee)
        if (this.storeId == storeId && items.isNotEmpty()) {
            val isFixed = !feeType.equals("km", ignoreCase = true) && officialFee != null
            storeDeliveryFeeType = feeType
            storeOfficialDeliveryFee = officialFee
            if (isFixed && deliveryType == DeliveryType.DELIVERY) {
                officialDeliveryFee = officialFee
                officialDeliveryQuoteKey = "fixed:$storeId"
            }
        }
    }

    /** Aplica a taxa VIP já configurada pela loja fixa; não depende de endereço. */
    fun setOfficialFixedDeliveryFee(storeId: String, fee: Double) {
        if (this.storeId != storeId || deliveryType != DeliveryType.DELIVERY) return
        val requestKey = "fixed:$storeId"
        if (officialDeliveryFee == fee && officialDeliveryQuoteKey == requestKey) return
        officialDeliveryFee = fee.coerceAtLeast(0.0)
        officialDeliveryDistanceKm = null
        officialDeliveryQuoteKey = requestKey
    }

    fun setOfficialDeliveryQuote(quote: DeliveryQuote, requestKey: String) {
        val destination = quote.destination ?: return
        if (!quote.isSuccessfulDelivery) return
        deliveryLatitude = destination.latitude
        deliveryLongitude = destination.longitude
        officialDeliveryFee = quote.pricing.deliveryFee.coerceAtLeast(0.0)
        officialDeliveryDistanceKm = quote.distance.km.takeIf { it.isFinite() }
        officialDeliveryQuoteKey = requestKey
    }

    fun clearOfficialDeliveryQuote(clearCoordinates: Boolean = false) {
        if (clearCoordinates) {
            deliveryLatitude = null
            deliveryLongitude = null
        }
        officialDeliveryFee = null
        officialDeliveryDistanceKm = null
        officialDeliveryQuoteKey = null
    }

    fun setDeliveryCoordinates(latitude: Double?, longitude: Double?) {
        deliveryLatitude = latitude
        deliveryLongitude = longitude
    }

    /**
     * Sincroniza a cotação oficial com o endereço atual (porte do
     * synchronizeDeliveryQuote do OrdersViewModel): retirada e taxa fixa
     * resolvem na hora; taxa por km exige endereço completo e chama a edge
     * quote-delivery, com dedupe pela chave do pedido de cotação.
     */
    fun synchronizeDeliveryQuote(address: DeliveryAddressInput?) {
        val sid = storeId
        if (items.isEmpty() || sid.isNullOrBlank()) return

        if (deliveryType == DeliveryType.PICKUP) {
            if (officialDeliveryQuoteKey != null) clearOfficialDeliveryQuote(clearCoordinates = true)
            if (deliveryQuote?.fulfillment != "pickup" || deliveryQuoteFailure != null || isQuotingDelivery) {
                deliveryQuote = DeliveryQuote.pickup()
                deliveryQuoteFailure = null
                isQuotingDelivery = false
                deliveryQuoteRequestKey = "pickup"
            }
            inFlightDeliveryQuoteKey = null
            return
        }

        val fixedFee = fixedDeliveryFee()
        if (fixedFee != null) {
            val fixedKey = "fixed:$sid"
            setOfficialFixedDeliveryFee(sid, fixedFee)
            if (deliveryQuote != null || deliveryQuoteFailure != null || isQuotingDelivery || deliveryQuoteRequestKey != fixedKey) {
                deliveryQuote = null
                deliveryQuoteFailure = null
                isQuotingDelivery = false
                deliveryQuoteRequestKey = fixedKey
            }
            inFlightDeliveryQuoteKey = null
            return
        }

        if (address == null || !address.isComplete()) {
            if (officialDeliveryQuoteKey != null) clearOfficialDeliveryQuote()
            if (deliveryQuote != null || deliveryQuoteFailure != null || isQuotingDelivery) {
                deliveryQuote = null
                deliveryQuoteFailure = null
                isQuotingDelivery = false
                deliveryQuoteRequestKey = null
            }
            inFlightDeliveryQuoteKey = null
            return
        }

        val requestKey = address.requestKey(sid, subtotal)
        val hasCurrentQuote = deliveryQuote?.isSuccessfulDelivery == true &&
            deliveryQuoteRequestKey == requestKey &&
            officialDeliveryQuoteKey == requestKey
        if (hasCurrentQuote) return
        if (deliveryQuoteFailure != null && deliveryQuoteRequestKey == requestKey) return
        if (inFlightDeliveryQuoteKey == requestKey) return

        if (officialDeliveryQuoteKey != null) clearOfficialDeliveryQuote()
        inFlightDeliveryQuoteKey = requestKey
        deliveryQuote = null
        deliveryQuoteFailure = null
        isQuotingDelivery = true
        deliveryQuoteRequestKey = requestKey

        val token = try { secureStorage.get(KEY_ACCESS_TOKEN) } catch (_: Throwable) { null }.orEmpty()
        val quotedStoreId = sid
        val quotedSubtotal = subtotal
        scope.launch {
            val result = try {
                SupabaseApi.quoteDelivery(quotedStoreId, quotedSubtotal, address, token)
            } catch (_: Throwable) {
                DeliveryQuoteResult(failure = DeliveryQuoteFailure("quote_unreachable"))
            }
            if (inFlightDeliveryQuoteKey != requestKey) return@launch
            inFlightDeliveryQuoteKey = null
            val stillCurrent = storeId == quotedStoreId &&
                subtotal == quotedSubtotal &&
                deliveryType == DeliveryType.DELIVERY
            if (!stillCurrent) return@launch

            if (result.isSuccess && result.quote != null) {
                deliveryQuote = result.quote
                deliveryQuoteFailure = null
                isQuotingDelivery = false
                deliveryQuoteRequestKey = requestKey
                setOfficialDeliveryQuote(result.quote, requestKey)
            } else {
                deliveryQuote = null
                deliveryQuoteFailure = result.failure ?: DeliveryQuoteFailure()
                isQuotingDelivery = false
                deliveryQuoteRequestKey = requestKey
                clearOfficialDeliveryQuote()
            }
        }
    }

    /** Invalida a cotação atual e força uma nova sincronização. */
    fun invalidateDeliveryQuote(address: DeliveryAddressInput?) {
        inFlightDeliveryQuoteKey = null
        clearOfficialDeliveryQuote()
        deliveryQuote = null
        deliveryQuoteFailure = null
        isQuotingDelivery = false
        deliveryQuoteRequestKey = null
        synchronizeDeliveryQuote(address)
    }

    /**
     * Revalida a sacola contra o catálogo atual (porte do
     * validateAgainstCatalog): remove itens indisponíveis/esgotados e
     * atualiza os dados do produto. Retorna true se algo foi removido.
     */
    fun validateAgainstCatalog(products: List<Product>): Boolean {
        if (items.isEmpty()) return false
        val available = products.filter { it.isAvailable }.associateBy { it.id }
        val before = totalItemCount
        val validatedItems = items.mapNotNull { item ->
            // Itens montados (pizza/pastel meio a meio) não são produtos do
            // catálogo: o preço é o da montagem — nunca trocar nem remover.
            if (item.product.id.startsWith("custom_")) return@mapNotNull item
            val currentProduct = available[item.product.id] ?: return@mapNotNull null
            item.copy(product = currentProduct)
        }
        items = validatedItems
        if (validatedItems.isEmpty()) {
            storeId = null
            appliedCoupon = null
            couponDiscount = 0.0
        }
        clearOfficialDeliveryQuote()
        return totalItemCount < before
    }

    var couponInput by mutableStateOf("")
    /** Cupom aplicado (objeto completo — o checkout registra o uso pelo id). */
    var appliedCoupon by mutableStateOf<Coupon?>(null)
    val appliedCouponCode: String?
        get() = appliedCoupon?.code

    /**
     * Cupom pendente vindo da seção "Lojas com Cupom" da Home (porte do
     * CartRepository do app original): a Home grava código + loja ao tocar no
     * card e a tela do carrinho consome uma única vez, só se for da mesma loja.
     */
    var pendingCouponCode by mutableStateOf<String?>(null)
    var pendingCouponStoreId by mutableStateOf<String?>(null)
    var couponDiscount by mutableStateOf(0.0)
        private set
    var couponError by mutableStateOf<String?>(null)
        private set

    var selectedAddressId by mutableStateOf<String?>(null)

    /** Endereços salvos REAIS do cliente (o checkout preenche via API). */
    var savedAddresses by mutableStateOf<List<SavedAddress>>(emptyList())
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
        get() = if (deliveryType == DeliveryType.PICKUP || items.isEmpty()) 0.0
        else officialDeliveryFee ?: 0.0

    /** Frete grátis acompanha a taxa central — inclusive após nova cotação por km. */
    val effectiveCouponDiscount: Double
        get() = if (appliedCoupon?.discountType.equals("free_shipping", ignoreCase = true) &&
            deliveryType == DeliveryType.DELIVERY
        ) {
            effectiveDeliveryFee
        } else {
            couponDiscount
        }

    val total: Double
        get() = (subtotal + effectiveDeliveryFee - effectiveCouponDiscount).coerceAtLeast(0.0)

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
     * Bloqueio de farmácia em 3 camadas (igual ao CartRepository do original):
     * produto com receita obrigatória, produto controlado ou modo de venda
     * fora do checkout da plataforma NUNCA entra na sacola.
     */
    fun isPharmacyBlocked(product: Product): Boolean =
        product.requiresPrescription || product.isControlled ||
            product.pharmacySaleMode != "platform_checkout"

    /**
     * Adiciona um produto na sacola (usado pela vitrine da loja).
     * Retorna false quando o produto é bloqueado (farmácia) — nada muda.
     * Se a sacola tem itens de outra loja, esvazia antes (comportamento de
     * fallback; a vitrine pede confirmação via StoreSwitchBottomSheet usando
     * isSwitchingStore).
     */
    fun addProduct(
        product: Product,
        quantity: Int = 1,
        notes: String = "",
        addons: List<SelectedAddonItem> = emptyList(),
        storeName: String = ""
    ): Boolean {
        if (isPharmacyBlocked(product)) return false
        if (items.isNotEmpty() && storeId != null && storeId != product.storeId) {
            clearCart()
        }
        // Perfil de entrega lembrado da loja (igual ao original): taxa fixa
        // oficial entra na hora com a chave "fixed:<loja>".
        val rememberedProfile = deliveryProfiles[product.storeId]
        val effectiveFeeType = rememberedProfile?.feeType.orEmpty()
        val effectiveOfficialFee = rememberedProfile?.officialFee
        if (storeId == null) {
            storeId = product.storeId
            if (storeName.isNotBlank()) this.storeName = storeName
        }
        if (storeDeliveryFeeType.isBlank() && effectiveFeeType.isNotBlank()) {
            storeDeliveryFeeType = effectiveFeeType
        }
        if (storeOfficialDeliveryFee == null && effectiveOfficialFee != null) {
            storeOfficialDeliveryFee = effectiveOfficialFee
        }
        val feeTypeNow = storeDeliveryFeeType.ifBlank { effectiveFeeType }
        if (feeTypeNow.equals("fixed", ignoreCase = true) &&
            deliveryType == DeliveryType.DELIVERY && officialDeliveryQuoteKey == null
        ) {
            (storeOfficialDeliveryFee ?: effectiveOfficialFee)?.let { fee ->
                officialDeliveryFee = fee
                officialDeliveryQuoteKey = "fixed:${product.storeId}"
            }
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
        return true
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
        storeId = null
        storeName = ""
        appliedCoupon = null
        couponDiscount = 0.0
        couponInput = ""
        couponError = null
        pendingCouponCode = null
        pendingCouponStoreId = null
        storeDeliveryFeeType = ""
        storeOfficialDeliveryFee = null
        clearOfficialDeliveryQuote(clearCoordinates = true)
        deliveryQuote = null
        deliveryQuoteFailure = null
        isQuotingDelivery = false
        deliveryQuoteRequestKey = null
        inFlightDeliveryQuoteKey = null
        selectedAddressId = null
    }

    /** Troca entrega/retirada (porte do setDeliveryType do original). */
    fun selectDeliveryType(type: DeliveryType) {
        val restoreFixed = type == DeliveryType.DELIVERY &&
            storeDeliveryFeeType.equals("fixed", ignoreCase = true) &&
            storeOfficialDeliveryFee != null
        deliveryType = type
        officialDeliveryFee = if (restoreFixed) storeOfficialDeliveryFee else null
        officialDeliveryDistanceKm = null
        officialDeliveryQuoteKey = if (restoreFixed) "fixed:$storeId" else null
    }

    /**
     * Aplica o cupom com TODAS as guardas do app original: sessão, expiração,
     * limite de usos, valor mínimo, uso anterior (coupon_uses) e primeiro
     * pedido — em falha de validação, o cupom NÃO é aceito por engano.
     */
    fun applyCoupon() {
        couponError = null
        val code = couponInput.trim().uppercase()
        if (code.isBlank()) {
            couponError = "Digite o código do cupom."
            return
        }
        val userId = try { secureStorage.get(KEY_USER_ID) } catch (_: Throwable) { null }
        val accessToken = try { secureStorage.get(KEY_ACCESS_TOKEN) } catch (_: Throwable) { null }
        if (userId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            couponError = "Entre novamente para validar o cupom."
            return
        }
        scope.launch {
            try {
                val coupon = SupabaseApi.fetchCoupon(code, storeId)
                if (coupon == null) {
                    couponError = "Cupom '$code' não encontrado ou não aplicável a esta loja."
                    return@launch
                }
                if (isCouponExpired(coupon.expiresAt)) {
                    couponError = "Este cupom expirou."
                    return@launch
                }
                if (coupon.maxUses != null && coupon.usedCount >= coupon.maxUses) {
                    couponError = "Este cupom atingiu o limite de usos."
                    return@launch
                }
                if (subtotal < coupon.minOrderValue) {
                    couponError = "Valor mínimo do pedido para este cupom é ${formatBRL(coupon.minOrderValue)}."
                    return@launch
                }
                val alreadyUsed = SupabaseApi.hasCouponUsage(coupon.id, userId, accessToken)
                if (alreadyUsed == null) {
                    couponError = "Não foi possível validar o cupom agora. Tente novamente."
                    return@launch
                }
                if (alreadyUsed) {
                    couponError = "Você já utilizou este cupom."
                    return@launch
                }
                if (coupon.firstOrderOnly) {
                    val hasPreviousOrder = SupabaseApi.hasNonCancelledOrders(userId, accessToken)
                    if (hasPreviousOrder == null) {
                        couponError = "Não foi possível validar seu primeiro pedido agora. Tente novamente."
                        return@launch
                    }
                    if (hasPreviousOrder) {
                        couponError = "Este cupom é válido apenas para o primeiro pedido."
                        return@launch
                    }
                }
                val discount = when (coupon.discountType.lowercase()) {
                    "percentage" -> (subtotal * (coupon.discountValue / 100.0)).coerceAtMost(subtotal)
                    "free_shipping" -> 0.0
                    else -> coupon.discountValue.coerceAtMost(subtotal)
                }.coerceAtLeast(0.0)

                appliedCoupon = coupon
                couponDiscount = discount
            } catch (_: Exception) {
                couponError = "Erro ao validar cupom. Tente novamente."
            }
        }
    }

    fun removeCoupon() {
        appliedCoupon = null
        couponDiscount = 0.0
        couponInput = ""
        couponError = null
    }

    /** Cupom expirado? (sem data = não expira; data ilegível = não bloqueia). */
    private fun isCouponExpired(expiresAt: String?): Boolean {
        val raw = expiresAt?.trim().orEmpty()
        if (raw.isBlank()) return false
        return try {
            val normalized = raw.replace(" ", "T").let {
                if (it.endsWith("Z") || Regex(".*[+-]\\d{2}:\\d{2}$").containsMatchIn(it)) it else it + "Z"
            }
            kotlinx.datetime.Instant.parse(normalized) < Clock.System.now()
        } catch (_: Throwable) {
            false
        }
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
     * Envia o pedido real via Supabase — porte completo do checkoutOrder +
     * OrderRepository.placeOrder do app original: validações (farmácia,
     * sessão, troco, endereço, cotação), confirmação final da taxa fixa,
     * snapshot da cotação no pedido e benefícios/cupom processados DEPOIS
     * do insert (fidelidade → carteira → registro do cupom).
     */
    fun placeOrder(
        walletDiscount: Double = 0.0,
        loyaltyPointsUsed: Int = 0,
        loyaltyDiscount: Double = 0.0,
        addressInput: DeliveryAddressInput? = null,
        scheduledForIso: String = "",
        releaseAtIso: String = "",
        isPreorder: Boolean = false
    ) {
        if (items.isEmpty() || isPlacingOrder) return
        orderError = null

        if (items.any { isPharmacyBlocked(it.product) }) {
            orderError = "Há um produto que exige validação da farmácia. Remova o item para finalizar pelo checkout comum do ItaSuper."
            return
        }
        val userId = try { secureStorage.get(KEY_USER_ID) } catch (_: Throwable) { null }
        val accessToken = try { secureStorage.get(KEY_ACCESS_TOKEN) } catch (_: Throwable) { null }
        if (userId.isNullOrBlank() || accessToken.isNullOrBlank()) {
            orderError = "Sua sessão expirou. Entre novamente para finalizar o pedido."
            return
        }
        if (paymentMethod.isBlank()) {
            orderError = "Selecione a forma de pagamento."
            return
        }
        val normalizedPayment = normalizePaymentMethod(paymentMethod)
        val checkoutTotal = (total - loyaltyDiscount - walletDiscount).coerceAtLeast(0.0)
        val changeRaw = changeForAmount.replace(",", ".").trim()
        val changeDouble = changeRaw.toDoubleOrNull()
        if (normalizedPayment == "dinheiro" && needsChange) {
            if (changeRaw.isBlank() || changeDouble == null) {
                orderError = "Informe o valor do troco para pagamento em dinheiro."
                return
            }
            if (changeDouble < checkoutTotal) {
                orderError = "O valor para troco deve ser igual ou maior que o total do pedido (${formatBRL(checkoutTotal)})."
                return
            }
        }

        // Endereço atual da entrega: o informado pelo checkout ou o endereço
        // salvo selecionado.
        val currentAddress = addressInput ?: savedAddresses.firstOrNull { it.id == selectedAddressId }?.let {
            DeliveryAddressInput(
                street = it.street,
                number = it.number,
                complement = it.complement,
                neighborhood = it.neighborhood,
                city = it.city,
                state = it.state,
                cep = it.cep
            )
        }
        val fixedFee = fixedDeliveryFee()
        if (deliveryType == DeliveryType.DELIVERY) {
            if (currentAddress == null || !currentAddress.isComplete()) {
                orderError = "Preencha rua, número, bairro e CEP válido para a entrega."
                return
            }
            if (fixedFee == null) {
                val requestKey = currentAddress.requestKey(storeId, subtotal)
                when {
                    isQuotingDelivery -> {
                        orderError = "Estamos calculando a entrega. Aguarde um instante para confirmar o pedido."
                        return
                    }
                    deliveryQuote?.isSuccessfulDelivery != true ||
                        deliveryQuoteRequestKey != requestKey ||
                        officialDeliveryQuoteKey != requestKey -> {
                        orderError = deliveryQuoteFailure?.userMessage()
                            ?: "Revise o endereço para calcular a taxa oficial de entrega."
                        return
                    }
                }
            }
        }

        isPlacingOrder = true
        val quotedStoreId = storeId ?: ""
        val quotedStoreName = storeName
        val quotedItems = items
        val quotedSubtotal = subtotal
        val quotedCouponDiscount = effectiveCouponDiscount
        val quotedCouponId = appliedCoupon?.id?.takeIf { it.isNotBlank() }
        scope.launch {
            try {
                // Cotação efetiva: retirada = pickup; taxa fixa = confirmação
                // final na central; por km = a cotação já sincronizada.
                val effectiveQuote: DeliveryQuote = when {
                    deliveryType == DeliveryType.PICKUP -> DeliveryQuote.pickup()
                    fixedFee != null -> {
                        val confirmation = SupabaseApi.quoteDelivery(
                            quotedStoreId, quotedSubtotal, currentAddress!!, accessToken
                        )
                        val confirmedQuote = confirmation.quote
                        if (!confirmation.isSuccess || confirmedQuote == null) {
                            deliveryQuoteFailure = confirmation.failure ?: DeliveryQuoteFailure()
                            orderError = confirmation.failure?.userMessage()
                                ?: "Não foi possível confirmar o endereço de entrega. Revise os dados e tente novamente."
                            isPlacingOrder = false
                            return@launch
                        }
                        val confirmedFee = confirmedQuote.pricing.deliveryFee
                        if (kotlin.math.abs(confirmedFee - fixedFee) > 0.009) {
                            setOfficialFixedDeliveryFee(quotedStoreId, confirmedFee)
                            orderError = "A taxa de entrega foi atualizada. Revise o total e confirme o pedido novamente."
                            isPlacingOrder = false
                            return@launch
                        }
                        confirmedQuote
                    }
                    else -> deliveryQuote ?: DeliveryQuote.pickup()
                }
                val destination = effectiveQuote.destination
                val isPickupOrder = deliveryType == DeliveryType.PICKUP
                val finalDeliveryFee = if (isPickupOrder) 0.0 else effectiveQuote.pricing.deliveryFee
                // A carteira é debitada depois do insert; o total inicial não a antecipa.
                val totalBeforeWallet = (quotedSubtotal + finalDeliveryFee - quotedCouponDiscount - loyaltyDiscount)
                    .coerceAtLeast(0.0)
                val initialStatus = when {
                    normalizedPayment == "pix" -> "aguardando_pagamento"
                    normalizedPayment == "pix_direto" -> "aguardando_comprovante"
                    isPreorder -> "scheduled"
                    else -> "pendente"
                }
                val draft = Order(
                    id = "",
                    storeId = quotedStoreId,
                    storeName = quotedStoreName,
                    items = quotedItems,
                    subtotal = quotedSubtotal,
                    deliveryFee = finalDeliveryFee,
                    discount = quotedCouponDiscount,
                    walletDiscount = 0.0,
                    loyaltyPointsUsed = loyaltyPointsUsed,
                    loyaltyDiscount = loyaltyDiscount,
                    total = totalBeforeWallet,
                    paymentMethod = normalizedPayment,
                    deliveryAddress = if (isPickupOrder) "Retirada na loja" else destination?.normalizedAddress.orEmpty(),
                    neighborhood = if (isPickupOrder) "RETIRADA" else destination?.neighborhood.orEmpty(),
                    deliveryCep = if (isPickupOrder) "" else destination?.cep.orEmpty(),
                    deliveryCity = if (isPickupOrder) "" else destination?.city.orEmpty(),
                    deliveryState = if (isPickupOrder) "" else destination?.state.orEmpty(),
                    clientLatitude = if (isPickupOrder) null else destination?.latitude,
                    clientLongitude = if (isPickupOrder) null else destination?.longitude,
                    deliveryFeeAbsorbedByStore = if (isPickupOrder) 0.0 else effectiveQuote.pricing.platformFeeStoreAbsorbed,
                    deliveryQuoteSnapshot = if (isPickupOrder) null else effectiveQuote.toSnapshot(),
                    scheduledFor = scheduledForIso,
                    releaseAt = releaseAtIso,
                    status = initialStatus,
                    createdAt = ""
                )
                val result = SupabaseApi.submitOrder(
                    order = draft,
                    clientId = userId,
                    accessToken = accessToken,
                    needsChange = normalizedPayment == "dinheiro" && needsChange,
                    changeFor = if (normalizedPayment == "dinheiro" && needsChange) changeDouble else null
                )
                if (!result.isSuccess || result.orderId.isNullOrBlank()) {
                    orderError = result.errorMessage ?: "Não foi possível enviar o pedido. Tente novamente."
                    isPlacingOrder = false
                    return@launch
                }
                val newOrderId = result.orderId
                // Benefícios e cupom são processados após o pedido (igual ao original).
                if (loyaltyPointsUsed > 0 && loyaltyDiscount > 0) {
                    SupabaseApi.redeemLoyaltyPoints(newOrderId, quotedStoreId, loyaltyPointsUsed, accessToken)
                }
                var confirmedWalletDiscount = 0.0
                if (walletDiscount > 0) {
                    val applied = SupabaseApi.applyWalletDiscount(newOrderId, userId, walletDiscount, accessToken)
                    if (applied.isSuccess) confirmedWalletDiscount = walletDiscount
                }
                if (quotedCouponId != null) {
                    SupabaseApi.registerCouponUse(quotedCouponId, userId, newOrderId, accessToken)
                }
                placedOrder = draft.copy(
                    id = newOrderId,
                    createdAt = result.createdAt ?: "",
                    walletDiscount = confirmedWalletDiscount,
                    total = (totalBeforeWallet - confirmedWalletDiscount).coerceAtLeast(0.0)
                )
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
        val address = savedAddresses.firstOrNull { it.id == selectedAddressId }
        return address?.let {
            listOf(it.displayLine, it.neighborhood, it.city)
                .filter { part -> part.isNotBlank() }
                .joinToString(" - ")
        } ?: "Endereço não informado"
    }
}

@Composable
fun rememberCartState(): CartHolder = remember { CartHolder() }

