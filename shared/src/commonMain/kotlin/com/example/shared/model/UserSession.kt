package com.example.shared.model

data class UserSession(
    val isLoggedIn: Boolean = false,
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessTokenExpiresAt: Long = 0L,
    val isSessionRestored: Boolean = false,
    val cpfCnpj: String = "",
    val whatsapp: String = "",
    val deliveryPin: String = "",
    val addressStreet: String = "",
    val addressNumber: String = "",
    val addressNeighborhood: String = "",
    val addressCity: String = "",
    val addressCep: String = "",
    val addressState: String = "",
    val addressComplement: String = "",
    val addressReferencePoint: String = "",
    // Localização temporária do aparelho: usada para catálogo, distância e confirmação de entrega.
    // Não substitui o endereço cadastrado da conta.
    val activeLocationStreet: String = "",
    val activeLocationNumber: String = "",
    val activeLocationNeighborhood: String = "",
    val activeLocationCep: String = "",
    val activeLocationCity: String = "",
    val activeLocationState: String = "",
    val activeLocationLatitude: Double? = null,
    val activeLocationLongitude: Double? = null,
    val activeLocationUpdatedAt: Long = 0L,
    val pixKeyType: String = "",
    val pixKey: String = ""
)
