package com.example.shared.model

/** Saldo disponível na carteira ItaSuper do cliente autenticado. */
data class WalletBalance(
    val balance: Double = 0.0
)

/** Regras de fidelidade habilitadas por loja. */
data class LoyaltyConfig(
    val storeId: String = "",
    val isEnabled: Boolean = false,
    val minPointsRedeem: Int = 50,
    val discountPerPoint: Double = 0.10,
    val maxDiscountPercent: Double = 20.0,
    val pointsPerReal: Int = 1
)

/** Pontos de fidelidade disponíveis do cliente em uma loja. */
data class LoyaltyBalance(
    val points: Int = 0
)
