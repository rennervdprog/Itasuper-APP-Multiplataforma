package com.example.shared.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Fase 4 (G7/G8/G9) — rastreio: refresh, rastreio guest e "Reivindicar
 * minha conta" na tela de acompanhamento.
 *
 * Espelha a web (produção):
 * - `guest-order-status` (GET anônima): `?order_id=<uuid>&last4=<4 dígitos>`.
 *   200 devolve `{order, store, items, customer_name, delivery_pin}`;
 *   erro vem no corpo (`{error}`): `not_found` (pedido inexistente, não
 *   guest ou últimos-4 errados), `rate_limited` (10+ erradas/10 min por
 *   pedido ou 30+/10 min por IP → tentar de novo em 10 min),
 *   `invalid_params`, `internal_error`. A página da web atualiza sozinha
 *   a cada 20 s (`setInterval(load, 20_000)`).
 * - `claim-guest-account` (POST anônima): `{order_id, last4, email,
 *   password}`. 200 `{ok: true}`; erros: `invalid_email`, `weak_password`
 *   (mín. 6), `not_found`, `already_has_account` (409), `email_in_use`
 *   (409). A prova de posse é o próprio rastreio + últimos 4 — a mesma
 *   que já libera o rastreio e o PIN, então não abre nada novo.
 *
 * Funções puras, sem rede — testáveis sem chamar as edges.
 * O parse usa só o corpo, nunca o código HTTP (o `restGetRaw`/`restPostRaw`
 * do app devolve ok + corpo; as edges devolvem o erro no corpo mesmo em
 * 4xx — mesmo padrão do `ClaimAccount.kt` da Fase 3).
 */

/** Intervalo do refresh do rastreio — igual ao da web e ao polling de posição (20 s). */
const val TRACKING_POLL_MS = 20_000L

data class GuestOrderItem(
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val productName: String = ""
)

data class GuestOrderStore(
    val name: String = "",
    val slug: String = "",
    val pixDiretoEnabled: Boolean = false,
    val pixKey: String = "",
    val pixKeyType: String = "",
    val pixBeneficiary: String = "",
    val pixInstructions: String = "",
    val whatsappNumber: String? = null
)

data class GuestOrder(
    val id: String = "",
    val status: String = "",
    val totalPrice: Double = 0.0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val paymentMethod: String = "",
    val addressDetails: String = "",
    val neighborhood: String = "",
    val createdAt: String = "",
    val storeId: String = "",
    val isGuest: Boolean = false,
    val deliveryPin: String = "",
    val pixExpiresAt: String = ""
)

data class GuestOrderStatus(
    val order: GuestOrder,
    val store: GuestOrderStore?,
    val items: List<GuestOrderItem>,
    val customerName: String? = null,
    val deliveryPin: String? = null
)

data class GuestOrderStatusResult(
    val status: GuestOrderStatus? = null,
    val error: String? = null
)

data class ClaimGuestAccountResult(
    val ok: Boolean = false,
    val error: String? = null
)

/** Código do pedido + últimos 4 extraídos do que o cliente colou (UUID ou link `/p/<uuid>?t=1234`). */
data class TrackingLinkParse(
    val orderId: String,
    val last4: String? = null
)

private val trackingJson = Json { ignoreUnknownKeys = true }

private fun JsonObject.tstr(key: String): String? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return (el as? JsonPrimitive)?.content
}

private fun JsonObject.tdouble(key: String): Double? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return (el as? JsonPrimitive)?.doubleOrNull
}

private fun JsonObject.tint(key: String): Int? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return (el as? JsonPrimitive)?.intOrNull
}

private fun JsonObject.tbool(key: String, default: Boolean = false): Boolean {
    val el = this[key] ?: return default
    if (el is JsonNull) return default
    return (el as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: default
}

private fun parseTrackingObject(text: String): JsonObject? {
    if (text.isBlank()) return null
    return try {
        trackingJson.parseToJsonElement(text).jsonObject
    } catch (_: Exception) {
        null
    }
}

/** Parser puro da resposta do `guest-order-status`. null = resposta inesperada. */
fun parseGuestOrderStatusResponse(text: String): GuestOrderStatusResult? {
    val obj = parseTrackingObject(text) ?: return null
    val error = obj.tstr("error")
    if (error != null) return GuestOrderStatusResult(status = null, error = error)
    val orderObj = obj["order"] as? JsonObject ?: return null
    val orderId = orderObj.tstr("id").orEmpty()
    if (orderId.isBlank()) return null
    val order = GuestOrder(
        id = orderId,
        status = orderObj.tstr("status").orEmpty(),
        totalPrice = orderObj.tdouble("total_price") ?: 0.0,
        subtotal = orderObj.tdouble("subtotal") ?: 0.0,
        deliveryFee = orderObj.tdouble("delivery_fee") ?: 0.0,
        paymentMethod = orderObj.tstr("payment_method").orEmpty(),
        addressDetails = orderObj.tstr("address_details").orEmpty(),
        neighborhood = orderObj.tstr("neighborhood").orEmpty(),
        createdAt = orderObj.tstr("created_at").orEmpty(),
        storeId = orderObj.tstr("store_id").orEmpty(),
        isGuest = orderObj.tbool("is_guest", false),
        deliveryPin = orderObj.tstr("delivery_pin").orEmpty(),
        pixExpiresAt = orderObj.tstr("pix_expires_at").orEmpty()
    )
    val storeObj = obj["store"] as? JsonObject
    val store = storeObj?.let {
        GuestOrderStore(
            name = it.tstr("name").orEmpty(),
            slug = it.tstr("slug").orEmpty(),
            pixDiretoEnabled = it.tbool("pix_direto_enabled", false),
            pixKey = it.tstr("pix_direto_key").orEmpty(),
            pixKeyType = it.tstr("pix_direto_key_type").orEmpty(),
            pixBeneficiary = it.tstr("pix_direto_beneficiary").orEmpty(),
            pixInstructions = it.tstr("pix_direto_instructions").orEmpty(),
            whatsappNumber = it.tstr("whatsapp_number")
        )
    }
    val items = (obj["items"] as? JsonArray)?.mapNotNull { el ->
        val item = el as? JsonObject ?: return@mapNotNull null
        val productName = (item["products"] as? JsonObject)?.tstr("name").orEmpty()
        GuestOrderItem(
            quantity = item.tint("quantity") ?: 0,
            unitPrice = item.tdouble("unit_price") ?: 0.0,
            productName = productName
        )
    }.orEmpty()
    return GuestOrderStatusResult(
        status = GuestOrderStatus(
            order = order,
            store = store,
            items = items,
            customerName = obj.tstr("customer_name"),
            deliveryPin = obj.tstr("delivery_pin")
        ),
        error = null
    )
}

/** Parser puro da resposta do `claim-guest-account`. null = resposta inesperada. */
fun parseClaimGuestAccountResponse(text: String): ClaimGuestAccountResult? {
    val obj = parseTrackingObject(text) ?: return null
    val error = obj.tstr("error")
    if (error != null) return ClaimGuestAccountResult(ok = false, error = error)
    return ClaimGuestAccountResult(
        ok = obj.tbool("ok", false),
        error = null
    )
}

/**
 * Rótulo do status — espelha o `STATUS_LABEL` da página de rastreio da
 * web, com as variantes que o app já usa na tela de pedidos
 * (`aguardando_pagamento`, `scheduled`, `recebido`, `pronto`,
 * `saiu_entrega`/`em_transito`, `finalizado`).
 */
fun guestOrderStatusLabel(status: String): String = when (status.lowercase()) {
    "pendente", "recebido" -> "Recebido pela loja"
    "em_preparo", "preparando" -> "Em preparo"
    "aguardando_pagamento" -> "Aguardando pagamento"
    "aguardando_comprovante" -> "Aguardando pagamento (Pix Direto)"
    "comprovante_enviado" -> "Comprovante enviado — aguardando loja"
    "pix_direto_recusado" -> "Comprovante recusado"
    "pronto_para_entrega", "pronto" -> "Pronto para entrega"
    "saiu_para_entrega", "saiu_entrega", "em_transito" -> "A caminho"
    "scheduled" -> "Agendado"
    "entregue", "finalizado" -> "Entregue"
    "cancelado" -> "Cancelado"
    else -> status.replaceFirstChar { it.uppercase() }
}

/** Estados finais: o refresh do rastreio para aqui (nada mais muda). */
fun isTerminalOrderStatus(status: String): Boolean =
    status.lowercase() in setOf("entregue", "finalizado", "cancelado")

private val uuidRegex =
    Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
private val last4ParamRegex = Regex("[?&]t=(\\d{4})")

/**
 * Extrai o pedido do que o cliente colou: o UUID puro ou o link de
 * rastreio (`/p/<uuid>?t=1234`, com ou sem domínio). null = não há UUID
 * no texto. Os últimos 4 vêm do `t=` do link quando presente.
 */
fun parseTrackingLinkOrId(input: String): TrackingLinkParse? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val uuid = uuidRegex.find(trimmed)?.value ?: return null
    val last4 = last4ParamRegex.find(trimmed)?.groupValues?.getOrNull(1)
    return TrackingLinkParse(orderId = uuid, last4 = last4)
}

private val claimEmailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/**
 * Validação do formulário "Criar minha conta" — espelha a
 * `validateClaimForm` da web. Devolve o código do erro ou null se válido.
 * Ordem igual à da web: e-mail → senha → confirmação.
 */
fun validateClaimGuestAccountForm(email: String, password: String, password2: String): String? {
    if (!claimEmailRegex.matches(email.trim())) return "invalid_email"
    if (password.length < 6) return "weak_password"
    if (password != password2) return "password_mismatch"
    return null
}

/** Mensagens de erro do claim — iguais às da página de rastreio da web. */
fun claimGuestAccountErrorMessage(code: String?): String = when (code) {
    "invalid_email" -> "Digite um e-mail válido."
    "weak_password" -> "A senha deve ter pelo menos 6 caracteres."
    "password_mismatch" -> "As senhas não conferem."
    "not_found" -> "Não encontramos esse pedido com esses dados — confira o link e tente de novo."
    "already_has_account" -> "Esse número já tem conta. Entre com seu e-mail e senha ou recupere a senha."
    "email_in_use" -> "Esse e-mail já está em uso por outra conta. Use outro e-mail."
    else -> "Não foi possível criar sua conta agora. Tente de novo."
}

/** Mensagem de erro do rastreio guest — igual à da página de rastreio da web. */
fun guestTrackingErrorMessage(code: String?): String = when (code) {
    "rate_limited" -> "Muitas tentativas — aguarde alguns minutos e tente de novo."
    else -> "Pedido não encontrado ou link inválido."
}
