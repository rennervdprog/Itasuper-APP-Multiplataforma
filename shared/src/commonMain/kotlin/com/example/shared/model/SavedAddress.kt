package com.example.shared.model

/** Endereço de entrega persistido em `saved_addresses`, equivalente ao seletor do Capacitor. */
data class SavedAddress(
    val id: String = "",
    val label: String = "Casa",
    val street: String,
    val number: String,
    val complement: String = "",
    val neighborhood: String,
    val city: String = "",
    val state: String = "",
    val referencePoint: String = "",
    val cep: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pinConfirmed: Boolean = false,
    val isDefault: Boolean = false
) {
    val displayLine: String
        get() = listOf(street, number).filter { it.isNotBlank() }.joinToString(", ")
}
