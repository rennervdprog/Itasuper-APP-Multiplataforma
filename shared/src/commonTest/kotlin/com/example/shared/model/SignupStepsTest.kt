package com.example.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fase 3 (3A) — testes da lógica pura do cadastro em 3 passos
 * (espelha `src/lib/signupSteps.ts` da web).
 */
class SignupStepsTest {

    private fun fullValues() = SignupValues(
        email = "maria@email.com",
        whatsapp = "(22) 99999-8888",
        fullName = "Maria Silva",
        password = "Senha1!",
        cep = "28979-129",
        number = "123",
        street = "Rua das Flores",
        neighborhood = "Centro",
        complement = "Apto 1",
        referencePoint = "Próximo à praça",
        deliveryPin = "1234",
        confirmPin = "1234",
        pinAcknowledged = true,
        acceptedTerms = true,
        acceptedPrivacy = true
    )

    @Test
    fun passwordRules_requireAllFive() {
        assertEquals(listOf(false, false, false, false, false), passwordRules(""))
        assertEquals(listOf(true, false, true, false, false), passwordRules("abcdefgh"))
        assertEquals(listOf(true, true, false, false, false), passwordRules("ABCDEFGH"))
        assertEquals(listOf(true, true, true, false, false), passwordRules("Abcdefgh"))
        assertEquals(listOf(true, true, true, true, false), passwordRules("Abcdefg1"))
        assertEquals(listOf(true, true, true, true, true), passwordRules("Abcdef1!"))
        assertEquals(5, passwordRulesPassedCount("Senha123!"))
        assertEquals(0, passwordRulesPassedCount(""))
    }

    @Test
    fun validateAccountStep_acceptsValidValues() {
        val errors = validateSignupStep(SignupStep.ACCOUNT, fullValues(), passwordRulesPassed = true)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun validateAccountStep_flagsInvalidEmail() {
        val errors = validateSignupStep(
            SignupStep.ACCOUNT,
            fullValues().copy(email = "sem-arroba"),
            passwordRulesPassed = true
        )
        assertEquals("Informe um e-mail válido.", errors["email"])
    }

    @Test
    fun validateAccountStep_flagsShortWhatsapp() {
        val errors = validateSignupStep(
            SignupStep.ACCOUNT,
            fullValues().copy(whatsapp = "(22) 9999"),
            passwordRulesPassed = true
        )
        assertEquals("Informe um WhatsApp válido com DDD.", errors["whatsapp"])
    }

    @Test
    fun validateAccountStep_flagsShortName() {
        val errors = validateSignupStep(
            SignupStep.ACCOUNT,
            fullValues().copy(fullName = "Ma"),
            passwordRulesPassed = true
        )
        assertEquals("Informe seu nome completo.", errors["fullName"])
    }

    @Test
    fun validateAccountStep_flagsWeakPassword() {
        val errors = validateSignupStep(
            SignupStep.ACCOUNT,
            fullValues().copy(password = "fraca"),
            passwordRulesPassed = false
        )
        assertEquals("Sua senha precisa atender as 5 regras acima.", errors["password"])
        val blank = validateSignupStep(
            SignupStep.ACCOUNT,
            fullValues().copy(password = ""),
            passwordRulesPassed = false
        )
        assertEquals("Crie uma senha.", blank["password"])
    }

    @Test
    fun validateAddressStep_flagsInvalidCepAndMissingFields() {
        val errors = validateSignupStep(
            SignupStep.ADDRESS,
            fullValues().copy(cep = "123", number = "", street = "", neighborhood = ""),
            passwordRulesPassed = true
        )
        assertEquals("Informe um CEP válido com 8 dígitos.", errors["cep"])
        assertEquals("Informe o número.", errors["number"])
        assertEquals("Informe a rua.", errors["street"])
        assertEquals("Informe o bairro.", errors["neighborhood"])
    }

    @Test
    fun validateAddressStep_acceptsFormattedCep() {
        val errors = validateSignupStep(SignupStep.ADDRESS, fullValues(), passwordRulesPassed = true)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun validatePinStep_flagsAllProblems() {
        val errors = validateSignupStep(
            SignupStep.PIN,
            fullValues().copy(deliveryPin = "12", confirmPin = "9999", pinAcknowledged = false, acceptedTerms = false, acceptedPrivacy = false),
            passwordRulesPassed = true
        )
        assertEquals("Defina um PIN de entrega com 4 dígitos numéricos.", errors["deliveryPin"])
        assertEquals("Os PINs informados não coincidem.", errors["confirmPin"])
        assertEquals("Confirme que este PIN será usado em todas as suas entregas.", errors["pinAcknowledged"])
        assertEquals("Você precisa aceitar os Termos de Uso e a Política de Privacidade.", errors["acceptedTerms"])
        assertEquals("Você precisa aceitar os Termos de Uso e a Política de Privacidade.", errors["acceptedPrivacy"])
    }

    @Test
    fun validatePinStep_skipsTermsWhenServerDoesNotRequire() {
        val errors = validateSignupStep(
            SignupStep.PIN,
            fullValues().copy(acceptedTerms = false, acceptedPrivacy = false),
            passwordRulesPassed = true,
            needsTerms = false,
            needsPrivacy = false
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun firstErrorField_returnsFirstInStepOrder() {
        val errors = validateSignupStep(
            SignupStep.ACCOUNT,
            fullValues().copy(email = "x", whatsapp = "1", fullName = "Ma"),
            passwordRulesPassed = true
        )
        assertEquals("email", firstErrorField(SignupStep.ACCOUNT, errors))
        assertNull(firstErrorField(SignupStep.ACCOUNT, emptyMap()))
    }

    @Test
    fun normalizeClaimPhone_addsCountryCode() {
        assertEquals("5522999998888", normalizeClaimPhone("(22) 99999-8888"))
        assertEquals("5522999998888", normalizeClaimPhone("22999998888"))
        assertEquals("5522999998888", normalizeClaimPhone("5522999998888"))
        assertNull(normalizeClaimPhone("123"))
        assertNull(normalizeClaimPhone(""))
    }

    @Test
    fun signupPhoneDigits_prepends55() {
        assertEquals("5522999998888", signupPhoneDigits("(22) 99999-8888"))
    }

    @Test
    fun draft_roundTripsAllFields() {
        val values = fullValues()
        val encoded = encodeSignupDraft(values, SignupStep.ADDRESS)
        val (decoded, step) = decodeSignupDraft(encoded) ?: error("decode falhou")
        assertEquals(values, decoded)
        assertEquals(SignupStep.ADDRESS, step)
    }

    @Test
    fun draft_decodeReturnsNullForGarbage() {
        assertNull(decodeSignupDraft(""))
        assertNull(decodeSignupDraft("não é json"))
    }

    @Test
    fun draft_decodeEmptyObjectYieldsEmptyValues() {
        val (values, step) = decodeSignupDraft("{}") ?: error("decode falhou")
        assertEquals(SignupValues.EMPTY, values)
        assertEquals(SignupStep.ACCOUNT, step)
    }

    @Test
    fun draft_decodeDefaultsToStepOne() {
        val encoded = encodeSignupDraft(SignupValues.EMPTY, SignupStep.ACCOUNT)
        val (_, step) = decodeSignupDraft(encoded) ?: error("decode falhou")
        assertEquals(SignupStep.ACCOUNT, step)
        assertEquals(SignupStep.PIN, SignupStep.fromNumber(3))
        assertEquals(SignupStep.ACCOUNT, SignupStep.fromNumber(99))
    }

    @Test
    fun signupSteps_haveWebCopy() {
        assertEquals(3, SIGNUP_STEPS.size)
        assertEquals("Sua conta", SIGNUP_STEPS[0].title)
        assertEquals("Seu endereço de entrega", SIGNUP_STEPS[1].title)
        assertEquals("Seu PIN de entrega", SIGNUP_STEPS[2].title)
        assertFalse(SIGNUP_STEPS[0].subtitle.isBlank())
    }
}
