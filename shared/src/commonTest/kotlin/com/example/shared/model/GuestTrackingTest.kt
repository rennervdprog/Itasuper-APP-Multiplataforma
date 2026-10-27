package com.example.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuestTrackingTest {

    private val okBody = """
        {
          "order": {
            "id": "46cbe74d-1111-2222-3333-444455556666",
            "client_id": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "status": "saiu_para_entrega",
            "total_price": 25.69,
            "subtotal": 24.7,
            "delivery_fee": 0.99,
            "payment_method": "pix_direto",
            "address_details": "Rua A, 10",
            "neighborhood": "Centro",
            "created_at": "2026-10-26T22:09:00Z",
            "store_id": "8197bdd8-f041-410d-acc0-af9ecb448f55",
            "is_guest": true,
            "delivery_pin": "4814",
            "pix_expires_at": "2026-10-26T22:29:00Z"
          },
          "store": {
            "name": "Loja Demonstração",
            "slug": "pdv-teste-fase-1-2",
            "pix_direto_enabled": true,
            "pix_direto_key": "teste-fix-rastreio",
            "pix_direto_key_type": "CNPJ",
            "pix_direto_beneficiary": "Loja Demo",
            "pix_direto_instructions": "Pague aqui",
            "whatsapp_number": "5522992796291"
          },
          "items": [
            { "quantity": 2, "unit_price": 12.35, "products": { "name": "Batata Frita" } },
            { "quantity": 1, "unit_price": 0.99, "products": { "name": "Taxa" } }
          ],
          "customer_name": "Teste 5555",
          "delivery_pin": "4814"
        }
    """.trimIndent()

    @Test
    fun parseGuestOrderStatusOk() {
        val result = parseGuestOrderStatusResponse(okBody)
        assertNotNull(result)
        assertNull(result.error)
        val status = result.status
        assertNotNull(status)
        assertEquals("46cbe74d-1111-2222-3333-444455556666", status.order.id)
        assertEquals("saiu_para_entrega", status.order.status)
        assertEquals(25.69, status.order.totalPrice)
        assertEquals(0.99, status.order.deliveryFee)
        assertTrue(status.order.isGuest)
        assertEquals("4814", status.deliveryPin)
        assertEquals("Loja Demonstração", status.store?.name)
        assertEquals("5522992796291", status.store?.whatsappNumber)
        assertEquals(2, status.items.size)
        assertEquals("Batata Frita", status.items[0].productName)
        assertEquals(2, status.items[0].quantity)
        assertEquals("Teste 5555", status.customerName)
    }

    @Test
    fun parseGuestOrderStatusStoreNull() {
        val body = okBody.replace(
            Regex("\"store\": \\{[^}]*\\},", RegexOption.DOT_MATCHES_ALL),
            "\"store\": null,"
        )
        val result = parseGuestOrderStatusResponse(body)
        assertNotNull(result)
        assertNull(result?.status?.store)
        assertEquals("saiu_para_entrega", result?.status?.order?.status)
    }

    @Test
    fun parseGuestOrderStatusErrors() {
        assertEquals("not_found", parseGuestOrderStatusResponse("""{"error":"not_found"}""")?.error)
        assertEquals("rate_limited", parseGuestOrderStatusResponse("""{"error":"rate_limited"}""")?.error)
        assertEquals("invalid_params", parseGuestOrderStatusResponse("""{"error":"invalid_params"}""")?.error)
        assertNull(parseGuestOrderStatusResponse("""{"error":"not_found"}""")?.status)
    }

    @Test
    fun parseGuestOrderStatusGarbageReturnsNull() {
        assertNull(parseGuestOrderStatusResponse(""))
        assertNull(parseGuestOrderStatusResponse("não é json"))
        assertNull(parseGuestOrderStatusResponse("""{"store":null}"""))
        assertNull(parseGuestOrderStatusResponse("""{"order":{"status":"pendente"}}"""))
    }

    @Test
    fun parseClaimGuestAccountOkAndErrors() {
        assertTrue(parseClaimGuestAccountResponse("""{"ok":true}""")?.ok == true)
        assertEquals("already_has_account", parseClaimGuestAccountResponse("""{"error":"already_has_account"}""")?.error)
        assertEquals("email_in_use", parseClaimGuestAccountResponse("""{"error":"email_in_use"}""")?.error)
        assertEquals("invalid_email", parseClaimGuestAccountResponse("""{"error":"invalid_email"}""")?.error)
        assertEquals("weak_password", parseClaimGuestAccountResponse("""{"error":"weak_password"}""")?.error)
        assertEquals("not_found", parseClaimGuestAccountResponse("""{"error":"not_found"}""")?.error)
        assertNull(parseClaimGuestAccountResponse("lixo"))
    }

    @Test
    fun statusLabelsMirrorWeb() {
        assertEquals("Recebido pela loja", guestOrderStatusLabel("pendente"))
        assertEquals("Recebido pela loja", guestOrderStatusLabel("recebido"))
        assertEquals("Em preparo", guestOrderStatusLabel("em_preparo"))
        assertEquals("Em preparo", guestOrderStatusLabel("preparando"))
        assertEquals("Aguardando pagamento", guestOrderStatusLabel("aguardando_pagamento"))
        assertEquals("Aguardando pagamento (Pix Direto)", guestOrderStatusLabel("aguardando_comprovante"))
        assertEquals("Comprovante enviado — aguardando loja", guestOrderStatusLabel("comprovante_enviado"))
        assertEquals("Comprovante recusado", guestOrderStatusLabel("pix_direto_recusado"))
        assertEquals("Pronto para entrega", guestOrderStatusLabel("pronto_para_entrega"))
        assertEquals("A caminho", guestOrderStatusLabel("saiu_para_entrega"))
        assertEquals("A caminho", guestOrderStatusLabel("em_transito"))
        assertEquals("Agendado", guestOrderStatusLabel("scheduled"))
        assertEquals("Entregue", guestOrderStatusLabel("entregue"))
        assertEquals("Entregue", guestOrderStatusLabel("finalizado"))
        assertEquals("Cancelado", guestOrderStatusLabel("cancelado"))
        assertEquals("Outro_status", guestOrderStatusLabel("outro_status"))
    }

    @Test
    fun terminalStatuses() {
        assertTrue(isTerminalOrderStatus("entregue"))
        assertTrue(isTerminalOrderStatus("finalizado"))
        assertTrue(isTerminalOrderStatus("cancelado"))
        assertTrue(isTerminalOrderStatus("ENTREGUE"))
        assertFalse(isTerminalOrderStatus("pendente"))
        assertFalse(isTerminalOrderStatus("saiu_para_entrega"))
        assertFalse(isTerminalOrderStatus("em_preparo"))
        assertFalse(isTerminalOrderStatus(""))
    }

    @Test
    fun parseTrackingLinkOrId() {
        val uuid = "46cbe74d-1111-2222-3333-444455556666"
        assertEquals(TrackingLinkParse(uuid, null), parseTrackingLinkOrId(uuid))
        assertEquals(
            TrackingLinkParse(uuid, "5555"),
            parseTrackingLinkOrId("https://www.itasuper.com.br/p/$uuid?t=5555")
        )
        assertEquals(
            TrackingLinkParse(uuid, "5555"),
            parseTrackingLinkOrId("itasuper.com.br/p/$uuid?x=1&t=5555")
        )
        assertEquals(
            TrackingLinkParse(uuid, null),
            parseTrackingLinkOrId("  $uuid  ")
        )
        assertNull(parseTrackingLinkOrId(""))
        assertNull(parseTrackingLinkOrId("EF4A2232"))
        assertNull(parseTrackingLinkOrId("pedido 123"))
    }

    @Test
    fun validateClaimForm() {
        assertNull(validateClaimGuestAccountForm("cliente@teste.com", "senha123", "senha123"))
        assertEquals("invalid_email", validateClaimGuestAccountForm("nao-email", "senha123", "senha123"))
        assertEquals("invalid_email", validateClaimGuestAccountForm("a@b", "senha123", "senha123"))
        assertEquals("weak_password", validateClaimGuestAccountForm("a@b.com", "12345", "12345"))
        assertEquals("password_mismatch", validateClaimGuestAccountForm("a@b.com", "senha123", "senha456"))
        // E-mail com espaços é aparado antes de validar.
        assertNull(validateClaimGuestAccountForm("  a@b.com  ", "senha123", "senha123"))
    }

    @Test
    fun claimErrorMessagesMirrorWeb() {
        assertEquals("Digite um e-mail válido.", claimGuestAccountErrorMessage("invalid_email"))
        assertEquals("A senha deve ter pelo menos 6 caracteres.", claimGuestAccountErrorMessage("weak_password"))
        assertEquals("As senhas não conferem.", claimGuestAccountErrorMessage("password_mismatch"))
        assertEquals(
            "Não encontramos esse pedido com esses dados — confira o link e tente de novo.",
            claimGuestAccountErrorMessage("not_found")
        )
        assertEquals(
            "Esse número já tem conta. Entre com seu e-mail e senha ou recupere a senha.",
            claimGuestAccountErrorMessage("already_has_account")
        )
        assertEquals(
            "Esse e-mail já está em uso por outra conta. Use outro e-mail.",
            claimGuestAccountErrorMessage("email_in_use")
        )
        assertEquals("Não foi possível criar sua conta agora. Tente de novo.", claimGuestAccountErrorMessage("xyz"))
        assertEquals("Não foi possível criar sua conta agora. Tente de novo.", claimGuestAccountErrorMessage(null))
    }

    @Test
    fun guestTrackingErrorMessages() {
        assertEquals(
            "Muitas tentativas — aguarde alguns minutos e tente de novo.",
            guestTrackingErrorMessage("rate_limited")
        )
        assertEquals("Pedido não encontrado ou link inválido.", guestTrackingErrorMessage("not_found"))
        assertEquals("Pedido não encontrado ou link inválido.", guestTrackingErrorMessage(null))
    }
}
