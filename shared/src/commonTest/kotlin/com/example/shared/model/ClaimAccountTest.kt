package com.example.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fase 3 (3B / 6D-B) — testes da lógica pura da reivindicação de conta:
 * parse das respostas das edges claim-* e montagem do corpo do verify.
 * Os corpos JSON abaixo são os formatos reais que as edges devolvem
 * (contrato conferido no repo web, `supabase/functions/claim-*`).
 */
class ClaimAccountTest {

    @Test
    fun parseClaimCheck_guest() {
        val result = parseClaimCheckResponse(
            """{"status":"guest","guest_user_id":"34994e8f-ec55-4f63-b82e-0d4fdea47eac","last_store_id":"8197bdd8-f041-410d-acc0-af9ecb448f55","last_store_name":"Loja Demonstração","last_order_id":"46cbe74d"}"""
        ) ?: error("parse falhou")
        assertEquals(ClaimStatus.GUEST, result.status)
        assertEquals("34994e8f-ec55-4f63-b82e-0d4fdea47eac", result.guestUserId)
        assertEquals("8197bdd8-f041-410d-acc0-af9ecb448f55", result.lastStoreId)
        assertEquals("Loja Demonstração", result.lastStoreName)
        assertEquals("46cbe74d", result.lastOrderId)
        assertNull(result.error)
    }

    @Test
    fun parseClaimCheck_realAndNew() {
        val real = parseClaimCheckResponse("""{"status":"real"}""") ?: error("parse falhou")
        assertEquals(ClaimStatus.REAL, real.status)
        val new = parseClaimCheckResponse("""{"status":"new"}""") ?: error("parse falhou")
        assertEquals(ClaimStatus.NEW, new.status)
    }

    @Test
    fun parseClaimCheck_errorAndGarbage() {
        val error = parseClaimCheckResponse("""{"error":"missing_phone"}""") ?: error("parse falhou")
        assertNull(error.status)
        assertEquals("missing_phone", error.error)
        assertNull(parseClaimCheckResponse(""))
        assertNull(parseClaimCheckResponse("não é json"))
        assertNull(parseClaimCheckResponse("""{"status":"desconhecido"}"""))
    }

    @Test
    fun parseClaimSendOtp_okAndFallback() {
        val ok = parseClaimSendOtpResponse(
            """{"ok":true,"expires_at":"2026-10-27T10:00:00Z","resend_after_seconds":60}"""
        ) ?: error("parse falhou")
        assertTrue(ok.ok)
        assertEquals("2026-10-27T10:00:00Z", ok.expiresAt)
        assertEquals(60, ok.resendAfterSeconds)
        assertNull(ok.error)

        val fallback = parseClaimSendOtpResponse(
            """{"ok":false,"error":"store_whatsapp_unavailable","fallback":"tracking"}"""
        ) ?: error("parse falhou")
        assertFalse(fallback.ok)
        assertEquals("store_whatsapp_unavailable", fallback.error)
        assertEquals("tracking", fallback.fallback)

        val tooSoon = parseClaimSendOtpResponse(
            """{"ok":false,"error":"resend_too_soon","retry_after_seconds":42}"""
        ) ?: error("parse falhou")
        assertEquals("resend_too_soon", tooSoon.error)
        assertEquals(42, tooSoon.retryAfterSeconds)
        assertNull(parseClaimSendOtpResponse(""))
    }

    @Test
    fun parseClaimVerifyOtp_okAndErrors() {
        val ok = parseClaimVerifyOtpResponse(
            """{"ok":true,"user_id":"34994e8f-ec55-4f63-b82e-0d4fdea47eac"}"""
        ) ?: error("parse falhou")
        assertTrue(ok.ok)
        assertEquals("34994e8f-ec55-4f63-b82e-0d4fdea47eac", ok.userId)

        val invalid = parseClaimVerifyOtpResponse(
            """{"ok":false,"error":"invalid_code","attempts_remaining":3}"""
        ) ?: error("parse falhou")
        assertFalse(invalid.ok)
        assertEquals("invalid_code", invalid.error)
        assertEquals(3, invalid.attemptsRemaining)

        val expired = parseClaimVerifyOtpResponse(
            """{"ok":false,"error":"code_expired_or_not_found"}"""
        ) ?: error("parse falhou")
        assertEquals("code_expired_or_not_found", expired.error)
        assertNull(parseClaimVerifyOtpResponse("não é json"))
    }

    @Test
    fun buildClaimVerifyOtpBody_matchesWebContract() {
        val body = buildClaimVerifyOtpBody(
            phone = "5514999998888",
            guestUserId = "34994e8f-ec55-4f63-b82e-0d4fdea47eac",
            code = "123 456",
            email = "  Maria@Email.com ",
            password = "Senha1!",
            fullName = "  Maria Silva ",
            address = ClaimAddressInput(
                cep = "28979-129",
                street = "Rua das Flores",
                number = "123",
                complement = "",
                neighborhood = "Centro",
                referencePoint = "Próximo à praça"
            ),
            deliveryPin = "1234",
            termsVersion = "6.8",
            privacyVersion = "6.7",
            userAgent = "ItaSuper KMP"
        )
        // Código só com dígitos; e-mail normalizado; nomes sem espaços.
        assertTrue(body.contains("\"code\":\"123456\""))
        assertTrue(body.contains("\"phone\":\"5514999998888\""))
        assertTrue(body.contains("\"email\":\"maria@email.com\""))
        assertTrue(body.contains("\"full_name\":\"Maria Silva\""))
        assertTrue(body.contains("\"guest_user_id\":\"34994e8f-ec55-4f63-b82e-0d4fdea47eac\""))
        assertTrue(body.contains("\"delivery_pin\":\"1234\""))
        assertTrue(body.contains("\"terms_version\":\"6.8\""))
        assertTrue(body.contains("\"privacy_version\":\"6.7\""))
        assertTrue(body.contains("\"user_agent\":\"ItaSuper KMP\""))
        assertTrue(body.contains("\"cep\":\"28979-129\""))
        assertTrue(body.contains("\"street\":\"Rua das Flores\""))
        assertTrue(body.contains("\"number\":\"123\""))
        assertTrue(body.contains("\"neighborhood\":\"Centro\""))
        assertTrue(body.contains("\"reference_point\":\"Próximo à praça\""))
    }

    @Test
    fun claimStatus_fromRaw() {
        assertEquals(ClaimStatus.GUEST, ClaimStatus.fromRaw("guest"))
        assertEquals(ClaimStatus.REAL, ClaimStatus.fromRaw("REAL"))
        assertEquals(ClaimStatus.NEW, ClaimStatus.fromRaw(" new "))
        assertNull(ClaimStatus.fromRaw("outro"))
        assertNull(ClaimStatus.fromRaw(null))
    }
}
