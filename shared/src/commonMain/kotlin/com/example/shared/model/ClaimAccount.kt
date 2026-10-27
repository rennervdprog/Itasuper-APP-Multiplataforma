package com.example.shared.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Fase 3 (G9 / 6D-B) — "Reivindicar no cadastro" (Opção A2 da web).
 *
 * Cliente que já pediu como guest cria a conta com o mesmo WhatsApp e
 * converte o cadastro existente (mesmo user_id) em vez de criar outro.
 * As 3 edges são anônimas (service_role) e devolvem JSON com o status no
 * corpo mesmo em 4xx — por isso o parse usa só o corpo, nunca o código
 * HTTP (o `restPostRaw` do app também só devolve ok + corpo).
 *
 * Funções puras, sem rede — testáveis sem chamar as edges.
 *
 * Regras copiadas da web (AuthPage.tsx + claim-*.ts):
 * - `claim-check` ao avançar do passo 1: `guest` (tem cadastro fantasma),
 *   `real` (já tem conta de verdade) ou `new` (número novo). Só conta
 *   como real quem NÃO tem e-mail sintético de guest — `profiles.role`
 *   NÃO distingue guest de real (lição da 6D-B).
 * - `claim-send-otp`: código de 6 dígitos via WhatsApp da loja do último
 *   pedido; se a loja não tiver WhatsApp conectado, volta
 *   `store_whatsapp_unavailable` + fallback "tracking" (Opção A).
 * - `claim-verify-otp` no submit final: verifica E converte numa só
 *   chamada (não há verificação avulsa antes).
 */

enum class ClaimStatus {
    GUEST,
    REAL,
    NEW;

    companion object {
        fun fromRaw(raw: String?): ClaimStatus? = when (raw?.trim()?.lowercase()) {
            "guest" -> GUEST
            "real" -> REAL
            "new" -> NEW
            else -> null
        }
    }
}

data class ClaimCheckResult(
    val status: ClaimStatus?,
    val guestUserId: String? = null,
    val lastStoreId: String? = null,
    val lastStoreName: String? = null,
    val lastOrderId: String? = null,
    val error: String? = null
)

data class ClaimSendOtpResult(
    val ok: Boolean = false,
    val expiresAt: String? = null,
    val resendAfterSeconds: Int? = null,
    val error: String? = null,
    val fallback: String? = null,
    val retryAfterSeconds: Int? = null
)

data class ClaimVerifyOtpResult(
    val ok: Boolean = false,
    val userId: String? = null,
    val error: String? = null,
    val attemptsRemaining: Int? = null
)

/** Endereço do cadastro, no formato que o `claim-verify-otp` espera. */
data class ClaimAddressInput(
    val cep: String,
    val street: String,
    val number: String,
    val complement: String,
    val neighborhood: String,
    val referencePoint: String
)

private val claimJson = Json { ignoreUnknownKeys = true }

private fun JsonObject.str(key: String): String? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return (el as? JsonPrimitive)?.content
}

private fun JsonObject.intOrNull(key: String): Int? = str(key)?.toIntOrNull()

private fun parseClaimObject(text: String): JsonObject? {
    if (text.isBlank()) return null
    return try {
        claimJson.parseToJsonElement(text).jsonObject
    } catch (_: Exception) {
        null
    }
}

/** Parser puro da resposta do `claim-check`. null = resposta inesperada. */
fun parseClaimCheckResponse(text: String): ClaimCheckResult? {
    val obj = parseClaimObject(text) ?: return null
    val error = obj.str("error")
    if (error != null) return ClaimCheckResult(status = null, error = error)
    val status = ClaimStatus.fromRaw(obj.str("status")) ?: return null
    return ClaimCheckResult(
        status = status,
        guestUserId = obj.str("guest_user_id"),
        lastStoreId = obj.str("last_store_id"),
        lastStoreName = obj.str("last_store_name"),
        lastOrderId = obj.str("last_order_id")
    )
}

/** Parser puro da resposta do `claim-send-otp`. null = resposta inesperada. */
fun parseClaimSendOtpResponse(text: String): ClaimSendOtpResult? {
    val obj = parseClaimObject(text) ?: return null
    return ClaimSendOtpResult(
        ok = obj.str("ok")?.toBooleanStrictOrNull() ?: false,
        expiresAt = obj.str("expires_at"),
        resendAfterSeconds = obj.intOrNull("resend_after_seconds"),
        error = obj.str("error"),
        fallback = obj.str("fallback"),
        retryAfterSeconds = obj.intOrNull("retry_after_seconds")
    )
}

/** Parser puro da resposta do `claim-verify-otp`. null = resposta inesperada. */
fun parseClaimVerifyOtpResponse(text: String): ClaimVerifyOtpResult? {
    val obj = parseClaimObject(text) ?: return null
    return ClaimVerifyOtpResult(
        ok = obj.str("ok")?.toBooleanStrictOrNull() ?: false,
        userId = obj.str("user_id"),
        error = obj.str("error"),
        attemptsRemaining = obj.intOrNull("attempts_remaining")
    )
}

/** Corpo do `claim-verify-otp` (mesmos campos que a web envia). */
fun buildClaimVerifyOtpBody(
    phone: String,
    guestUserId: String,
    code: String,
    email: String,
    password: String,
    fullName: String,
    address: ClaimAddressInput,
    deliveryPin: String,
    termsVersion: String,
    privacyVersion: String,
    userAgent: String
): String = buildJsonObject {
    put("phone", JsonPrimitive(phone))
    put("guest_user_id", JsonPrimitive(guestUserId))
    put("code", JsonPrimitive(code.filter { it.isDigit() }))
    put("email", JsonPrimitive(email.trim().lowercase()))
    put("password", JsonPrimitive(password))
    put("full_name", JsonPrimitive(fullName.trim()))
    put("address", buildJsonObject {
        put("cep", JsonPrimitive(address.cep))
        put("street", JsonPrimitive(address.street.trim()))
        put("number", JsonPrimitive(address.number.trim()))
        put("complement", JsonPrimitive(address.complement.trim()))
        put("neighborhood", JsonPrimitive(address.neighborhood.trim()))
        put("reference_point", JsonPrimitive(address.referencePoint.trim()))
    })
    put("delivery_pin", JsonPrimitive(deliveryPin))
    put("terms_version", JsonPrimitive(termsVersion))
    put("privacy_version", JsonPrimitive(privacyVersion))
    put("user_agent", JsonPrimitive(userAgent))
}.toString()

/** Versões legais de fallback (iguais às da edge e de `legalVersions.ts`). */
const val CLAIM_FALLBACK_TERMS_VERSION = "6.8"
const val CLAIM_FALLBACK_PRIVACY_VERSION = "6.7"
