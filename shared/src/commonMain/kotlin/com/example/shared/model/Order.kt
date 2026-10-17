package com.example.shared.model

data class Order(
    val id: String,
    val storeId: String,
    val storeName: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val deliveryFee: Double,
    val discount: Double = 0.0,
    val walletDiscount: Double = 0.0,
    val loyaltyPointsUsed: Int = 0,
    val loyaltyDiscount: Double = 0.0,
    val total: Double,
    val paymentMethod: String,
    val deliveryAddress: String,
    val status: String = "Em preparação",
    val createdAt: String,
    val confirmedAt: String = "",
    /** ISO do prazo canônico de abertura de caso PIX Direto emitido pelo servidor. */
    val refundRequestExpiresAt: String = "",
    /** ISO do prazo de 20 minutos para envio do comprovante PIX Direto. */
    val pixExpiresAt: String = "",
    val deliveryPin: String = "",
    val neighborhood: String = "",
    val deliveryCep: String = "",
    val deliveryCity: String = "",
    val deliveryState: String = "",
    val clientLatitude: Double? = null,
    val clientLongitude: Double? = null,
    val deliveryFeeAbsorbedByStore: Double = 0.0,
    val deliveryQuoteSnapshot: DeliveryQuoteSnapshot? = null,
    val driverId: String = "",
    val deliveryConfirmedByClient: Boolean = false,
    /** ISO do horário escolhido pelo cliente; vazio em pedidos imediatos. */
    val scheduledFor: String = "",
    /** ISO da abertura que libera o pré-pedido para a loja; vazio quando não aplicável. */
    val releaseAt: String = ""
)
