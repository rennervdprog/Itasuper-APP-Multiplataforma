package com.example.shared.ui.cart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.shared.model.CartItem
import com.example.shared.model.Order
import com.example.shared.ui.mock.mockCartItems
import com.example.shared.ui.mock.mockSavedAddresses
import kotlin.math.abs

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

enum class DeliveryType { DELIVERY, PICKUP }

/**
 * Estado do carrinho (mockado). Substitui o OrdersViewModel do Android:
 * estado observável via mutableStateOf, sem ViewModel Android.
 *
 * TODO(fase-rede): carregar/salvar o carrinho real via Ktor/SupabaseClient,
 * cotar a entrega de verdade e validar o cupom no backend.
 */
class CartHolder {
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

    var couponInput by mutableStateOf("")
    var appliedCouponCode by mutableStateOf<String?>(null)
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

    init {
        seedMock()
    }

    private fun seedMock() {
        // Dados de exemplo para navegar pelo fluxo sem rede.
        storeId = "s1"
        storeName = "Pastelão Carioca"
        items = mockCartItems()
        deliveryFee = 4.90
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
        // TODO(fase-rede): validar o cupom no backend (tabela coupons).
        if (code == "PROMO10") {
            val discount = (subtotal * 0.10).coerceAtMost(10.0)
            appliedCouponCode = code
            couponDiscount = discount
        } else {
            couponError = "Cupom inválido ou expirado."
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

    fun placeOrder() {
        if (items.isEmpty() || isPlacingOrder) return
        orderError = null
        if (deliveryType == DeliveryType.DELIVERY && selectedAddressId == null) {
            orderError = "Selecione um endereço de entrega."
            return
        }
        // TODO(fase-rede): enviar o pedido real via Ktor (edge function create-order),
        // gerar o PIX real e retornar o pedido confirmado pelo servidor.
        placedOrder = Order(
            id = "m1o2c3k4-5e6f-7890",
            storeId = storeId ?: "",
            storeName = storeName,
            items = items,
            subtotal = subtotal,
            deliveryFee = effectiveDeliveryFee,
            discount = couponDiscount,
            total = total,
            paymentMethod = paymentMethod,
            deliveryAddress = deliveryAddressSummary(),
            status = "recebido",
            createdAt = "2026-10-17T19:00:00"
        )
    }

    fun dismissSuccess() {
        placedOrder = null
        clearCart()
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
