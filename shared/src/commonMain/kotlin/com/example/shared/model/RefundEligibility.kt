package com.example.shared.model

import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

/**
 * Regra visual compartilhada. O Supabase continua sendo a autoridade final:
 * exige PIX Direto confirmado, pedido concluído, ownership e prazo de 24 horas aberto.
 */
object RefundEligibility {
    @OptIn(ExperimentalTime::class)
    fun canOpenPixDiretoCase(
        paymentMethod: String,
        status: String,
        refundRequestExpiresAt: String,
        nowEpochMillis: Long = Instant.now().toEpochMilliseconds()
    ): Boolean {
        if (paymentMethod != "pix_direto" || status.lowercase() !in setOf("entregue", "finalizado")) {
            return false
        }

        val expiresAtEpochMillis = runCatching {
            Instant.parse(refundRequestExpiresAt).toEpochMilliseconds()
        }.getOrNull() ?: return false

        return expiresAtEpochMillis > nowEpochMillis
    }

    const val INELIGIBLE_MESSAGE =
        "Somente pedidos pagos por PIX Direto confirmado e dentro de 24 horas da conclusão podem abrir um caso de reembolso pela plataforma."

    const val EXPIRED_MESSAGE =
        "O prazo de 24 horas após a conclusão do pedido para solicitar reembolso expirou."
}
