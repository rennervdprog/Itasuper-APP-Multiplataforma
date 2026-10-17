package com.example.shared.model

enum class LegalDocumentKind {
    TERMS,
    PRIVACY
}

object LegalDocumentLinks {
    const val TERMS_URL = "https://itasuper.com.br/termos-de-uso"
    const val PRIVACY_URL = "https://itasuper.com.br/politica-de-privacidade"
    const val SUPPORT_WHATSAPP_URL = "https://wa.me/5522992796291"
}

/**
 * Modo em que o backend publicou a mudança.
 *
 * BINDING: a vigência já chegou e o aceite é obrigatório para continuar.
 * NOTICE: publicado com vigência futura. Os Termos (§6.3) prometem 30 dias corridos
 * de aviso prévio em que nada muda para o usuário, então este modo NÃO pode bloquear.
 *
 * Banco anterior à migration v6.6 não devolve o campo: nesse caso o modo é BINDING,
 * nunca NOTICE, para que o aceite obrigatório não se torne opcional por acidente.
 */
enum class LegalAcceptanceMode {
    BINDING,
    NOTICE;

    val remoteValue: String
        get() = if (this == NOTICE) "notice" else "binding"

    companion object {
        fun fromRemote(value: String?): LegalAcceptanceMode =
            if (value?.trim()?.lowercase() == "notice") NOTICE else BINDING
    }
}

data class LegalChange(
    val version: String,
    val effectiveDate: String,
    val section: String,
    val changeType: String,
    val summary: String,
    val legalBasis: String?
)

data class PendingLegalChanges(
    val needsTerms: Boolean,
    val needsPrivacy: Boolean,
    val currentTermsVersion: String,
    val currentPrivacyVersion: String,
    val termsChanges: List<LegalChange>,
    val privacyChanges: List<LegalChange>,
    val mode: LegalAcceptanceMode = LegalAcceptanceMode.BINDING,
    val termsEffectiveDate: String? = null,
    val privacyEffectiveDate: String? = null,
    val daysUntilEffective: Int? = null
) {
    val requiresAcceptance: Boolean
        get() = needsTerms || needsPrivacy

    /** Só o modo binding pode prender o usuário na tela. */
    val isBlocking: Boolean
        get() = requiresAcceptance && mode == LegalAcceptanceMode.BINDING

    /** Chave de adiamento por par de versões, igual à do web ("6.6/6.6"). */
    val versionKey: String
        get() = "$currentTermsVersion/$currentPrivacyVersion"

    val effectiveDate: String?
        get() = termsEffectiveDate?.takeIf { it.isNotBlank() }
            ?: privacyEffectiveDate?.takeIf { it.isNotBlank() }
}

data class LegalAcceptanceResult(
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Comparação de versão componente a componente. Comparar como texto faz
 * `"10.0" >= "4.2"` virar `false` em ordem lexicográfica e reabre o aceite para
 * quem já aceitou — foi um bug real no web.
 *
 * Retorna negativo se [left] < [right], zero se equivalentes, positivo se maior.
 */
fun compareLegalVersions(left: String?, right: String?): Int {
    val leftParts = legalVersionParts(left)
    val rightParts = legalVersionParts(right)
    val size = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until size) {
        val leftPart = leftParts.getOrElse(index) { 0 }
        val rightPart = rightParts.getOrElse(index) { 0 }
        if (leftPart != rightPart) return leftPart.compareTo(rightPart)
    }
    return 0
}

private fun legalVersionParts(version: String?): List<Int> {
    if (version.isNullOrBlank()) return listOf(0)
    return version.trim()
        .split('.', '-', '_')
        .map { part -> part.filter(Char::isDigit).toIntOrNull() ?: 0 }
        .ifEmpty { listOf(0) }
}
