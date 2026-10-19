package com.example.shared.model

/**
 * Porte de PixPaymentUiState do app Android (OrdersViewModel.kt).
 * Estado da geração do PIX online após o pedido.
 */
data class PixPaymentUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val pixCode: String? = null,
    val qrCodeBase64: String? = null,
    val errorMessage: String? = null
)

/**
 * Porte de PixDirectPaymentUiState do app Android (OrdersViewModel.kt).
 * Estado do pagamento via PIX Direto (chave da loja + comprovante).
 */
data class PixDirectPaymentUiState(
    val order: Order? = null,
    val storeName: String = "",
    val pixKey: String = "",
    val pixKeyType: String = "",
    val beneficiary: String = "",
    val instructions: String = "",
    val isUploading: Boolean = false,
    val proofSent: Boolean = false,
    val errorMessage: String? = null
)
