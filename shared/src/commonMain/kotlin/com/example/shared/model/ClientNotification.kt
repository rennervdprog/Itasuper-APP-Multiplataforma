package com.example.shared.model

/** Registro sincronizado de uma atualização de pedido enviada ao cliente. */
data class ClientNotification(
    val id: String,
    val orderId: String? = null,
    val type: String = "order_update",
    val title: String,
    val body: String,
    val payload: Map<String, String> = emptyMap(),
    val readAt: String? = null,
    val createdAt: String = ""
) {
    val isRead: Boolean get() = !readAt.isNullOrBlank()
}
