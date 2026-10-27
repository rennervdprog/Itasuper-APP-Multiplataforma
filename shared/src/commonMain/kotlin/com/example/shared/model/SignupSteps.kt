package com.example.shared.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Fase 3 (G6) — cadastro do cliente em 3 passos, espelhando
 * `src/lib/signupSteps.ts` da web (PR #366).
 *
 * Passos: 1 Conta → 2 Endereço → 3 PIN. CPF/CNPJ foi REMOVIDO do cadastro
 * (decisão do Renner, 25/10/2026): o campo era gravado em profiles.document
 * mas nada no fluxo o usava. O e-mail passa a ser campo obrigatório do
 * passo 1 — antes o app criava contas com e-mail placeholder
 * (`cliente_<ts>@itasuper.com.br`) porque o formulário de cadastro nem
 * tinha campo de e-mail (o `loginEmail` da aba de login era reaproveitado).
 *
 * Funções puras, sem Compose e sem rede — testáveis sem montar a tela.
 *
 * Desvio intencional vs. a web: o passo 3 da web valida um único
 * `acceptedTerms`; o app já separa Termos e Privacidade em dois checkboxes
 * (com `needsTerms`/`needsPrivacy` vindos do servidor via
 * `get_pending_legal_changes`), então a validação checa cada um só quando
 * o servidor exige. As mensagens são as mesmas da web.
 */

enum class SignupStep(val number: Int) {
    ACCOUNT(1),
    ADDRESS(2),
    PIN(3);

    companion object {
        fun fromNumber(n: Int): SignupStep = when (n) {
            2 -> ADDRESS
            3 -> PIN
            else -> ACCOUNT
        }
    }
}

data class SignupValues(
    val email: String = "",
    val whatsapp: String = "",
    val fullName: String = "",
    val password: String = "",
    val cep: String = "",
    val number: String = "",
    val street: String = "",
    val neighborhood: String = "",
    val complement: String = "",
    val referencePoint: String = "",
    val deliveryPin: String = "",
    val confirmPin: String = "",
    val pinAcknowledged: Boolean = false,
    val acceptedTerms: Boolean = false,
    val acceptedPrivacy: Boolean = false
) {
    companion object {
        val EMPTY = SignupValues()
    }
}

data class SignupStepInfo(
    val step: SignupStep,
    val title: String,
    val label: String,
    val subtitle: String
)

val SIGNUP_STEPS: List<SignupStepInfo> = listOf(
    SignupStepInfo(
        SignupStep.ACCOUNT,
        "Sua conta",
        "Conta",
        "Seus dados de acesso. Usaremos o e-mail para entrar e recuperar a senha."
    ),
    SignupStepInfo(
        SignupStep.ADDRESS,
        "Seu endereço de entrega",
        "Endereço",
        "Usaremos para calcular o frete e mostrar lojas perto de você."
    ),
    SignupStepInfo(
        SignupStep.PIN,
        "Seu PIN de entrega",
        "PIN",
        "Escolha 4 dígitos que só você sabe. O entregador vai pedir esse código em todas as suas entregas."
    )
)

/** Campos de cada passo, na ordem em que aparecem (para achar o primeiro inválido). */
val SIGNUP_STEP_FIELDS: Map<SignupStep, List<String>> = mapOf(
    SignupStep.ACCOUNT to listOf("email", "whatsapp", "fullName", "password"),
    SignupStep.ADDRESS to listOf("cep", "number", "street", "neighborhood"),
    SignupStep.PIN to listOf("deliveryPin", "confirmPin", "pinAcknowledged", "acceptedTerms", "acceptedPrivacy")
)

/** Erros por campo (mapa campo → mensagem); vazio = passo válido. */
typealias SignupErrors = Map<String, String>

private val EMAIL_REGEX = Regex("^\\S+@\\S+\\.\\S+$")

fun isValidSignupEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

/**
 * As 5 regras de senha da web (`PASSWORD_RULES` em AuthPage.tsx), na ordem:
 * mínimo 8 caracteres, uma maiúscula, uma minúscula, um número, um especial.
 */
fun passwordRules(password: String): List<Boolean> = listOf(
    password.length >= 8,
    password.any { it.isUpperCase() },
    password.any { it.isLowerCase() },
    password.any { it.isDigit() },
    password.any { !it.isLetterOrDigit() }
)

fun passwordRulesPassedCount(password: String): Int = passwordRules(password).count { it }

/**
 * Valida um passo do cadastro e devolve o mapa campo → mensagem de erro.
 * `passwordRulesPassed` é se a senha atende as 5 regras. `needsTerms` /
 * `needsPrivacy` vêm do servidor (`get_pending_legal_changes`) — só cobra
 * o aceite que o servidor exige.
 */
fun validateSignupStep(
    step: SignupStep,
    values: SignupValues,
    passwordRulesPassed: Boolean,
    needsTerms: Boolean = true,
    needsPrivacy: Boolean = true
): SignupErrors {
    val errors = mutableMapOf<String, String>()
    val digits = { v: String -> v.filter { it.isDigit() } }

    when (step) {
        SignupStep.ACCOUNT -> {
            if (!isValidSignupEmail(values.email)) {
                errors["email"] = "Informe um e-mail válido."
            }
            val whatsDigits = digits(values.whatsapp)
            if (whatsDigits.length < 10 || whatsDigits.length > 11) {
                errors["whatsapp"] = "Informe um WhatsApp válido com DDD."
            }
            if (values.fullName.trim().length < 3) {
                errors["fullName"] = "Informe seu nome completo."
            }
            if (values.password.isBlank()) {
                errors["password"] = "Crie uma senha."
            } else if (!passwordRulesPassed) {
                errors["password"] = "Sua senha precisa atender as 5 regras acima."
            }
        }
        SignupStep.ADDRESS -> {
            if (digits(values.cep).length != 8) {
                errors["cep"] = "Informe um CEP válido com 8 dígitos."
            }
            if (values.number.isBlank()) {
                errors["number"] = "Informe o número."
            }
            if (values.street.isBlank()) {
                errors["street"] = "Informe a rua."
            }
            if (values.neighborhood.isBlank()) {
                errors["neighborhood"] = "Informe o bairro."
            }
        }
        SignupStep.PIN -> {
            if (!Regex("^\\d{4}$").matches(values.deliveryPin)) {
                errors["deliveryPin"] = "Defina um PIN de entrega com 4 dígitos numéricos."
            }
            if (values.confirmPin.isBlank() || values.confirmPin != values.deliveryPin) {
                errors["confirmPin"] = "Os PINs informados não coincidem."
            }
            if (!values.pinAcknowledged) {
                errors["pinAcknowledged"] = "Confirme que este PIN será usado em todas as suas entregas."
            }
            if (needsTerms && !values.acceptedTerms) {
                errors["acceptedTerms"] = "Você precisa aceitar os Termos de Uso e a Política de Privacidade."
            }
            if (needsPrivacy && !values.acceptedPrivacy) {
                errors["acceptedPrivacy"] = "Você precisa aceitar os Termos de Uso e a Política de Privacidade."
            }
        }
    }
    return errors
}

/** Primeiro campo com erro de um passo, na ordem dos campos. */
fun firstErrorField(step: SignupStep, errors: SignupErrors): String? {
    for (field in SIGNUP_STEP_FIELDS[step].orEmpty()) {
        if (errors[field] != null) return field
    }
    return null
}

/**
 * Normaliza o WhatsApp digitado para o formato das edges de claim
 * (só dígitos, com 55) — igual a `normalizeClaimPhone` da web e a
 * `normalizePhoneBR` das edges. null = número inválido.
 */
fun normalizeClaimPhone(whatsapp: String): String? {
    val d = whatsapp.filter { it.isDigit() }
    return when (d.length) {
        10, 11 -> "55$d"
        12, 13 -> d
        else -> null
    }
}

/** WhatsApp com DDD no formato `55 + DDD + número` (para gravar em `phone` e `whatsapp_number`). */
fun signupPhoneDigits(whatsapp: String): String = "55" + whatsapp.filter { it.isDigit() }

// ---------------------------------------------------------------------------
// Rascunho local — salvo no aparelho (Settings), apagado ao concluir.
// Não sai do aparelho, não toca no servidor, não cria conta parcial.
// Inclui a senha digitada (é o mesmo dado que o usuário já digitou no
// formulário; some ao concluir) — igual ao rascunho da web (localStorage).
// ---------------------------------------------------------------------------

const val SIGNUP_DRAFT_KEY = "itasuper_signup_draft"

private val draftJson = Json { ignoreUnknownKeys = true }

private fun JsonObject.str(key: String): String? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return (el as? JsonPrimitive)?.content
}

private fun JsonObject.bool(key: String): Boolean =
    (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: false

fun encodeSignupDraft(values: SignupValues, step: SignupStep): String = buildJsonObject {
    put("step", JsonPrimitive(step.number))
    put("email", JsonPrimitive(values.email))
    put("whatsapp", JsonPrimitive(values.whatsapp))
    put("fullName", JsonPrimitive(values.fullName))
    put("password", JsonPrimitive(values.password))
    put("cep", JsonPrimitive(values.cep))
    put("number", JsonPrimitive(values.number))
    put("street", JsonPrimitive(values.street))
    put("neighborhood", JsonPrimitive(values.neighborhood))
    put("complement", JsonPrimitive(values.complement))
    put("referencePoint", JsonPrimitive(values.referencePoint))
    put("deliveryPin", JsonPrimitive(values.deliveryPin))
    put("confirmPin", JsonPrimitive(values.confirmPin))
    put("pinAcknowledged", JsonPrimitive(values.pinAcknowledged))
    put("acceptedTerms", JsonPrimitive(values.acceptedTerms))
    put("acceptedPrivacy", JsonPrimitive(values.acceptedPrivacy))
}.toString()

/** Decodifica o rascunho salvo; null = ausente ou inválido. */
fun decodeSignupDraft(raw: String): Pair<SignupValues, SignupStep>? {
    if (raw.isBlank()) return null
    return try {
        val obj = draftJson.parseToJsonElement(raw).jsonObject
        val values = SignupValues(
            email = obj.str("email").orEmpty(),
            whatsapp = obj.str("whatsapp").orEmpty(),
            fullName = obj.str("fullName").orEmpty(),
            password = obj.str("password").orEmpty(),
            cep = obj.str("cep").orEmpty(),
            number = obj.str("number").orEmpty(),
            street = obj.str("street").orEmpty(),
            neighborhood = obj.str("neighborhood").orEmpty(),
            complement = obj.str("complement").orEmpty(),
            referencePoint = obj.str("referencePoint").orEmpty(),
            deliveryPin = obj.str("deliveryPin").orEmpty(),
            confirmPin = obj.str("confirmPin").orEmpty(),
            pinAcknowledged = obj.bool("pinAcknowledged"),
            acceptedTerms = obj.bool("acceptedTerms"),
            acceptedPrivacy = obj.bool("acceptedPrivacy")
        )
        val step = SignupStep.fromNumber(obj.str("step")?.toIntOrNull() ?: 1)
        values to step
    } catch (_: Exception) {
        null
    }
}
