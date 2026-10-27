package com.example.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fase 2 (2C) — status de abertura e recusas do checkout no app.
 * Espelha `getStoreOpenStatus` da web e as guardas do checkout logado
 * (`CheckoutPage.handleConfirm`): loja fechada e pedido mínimo, com as
 * mesmas mensagens.
 *
 * Base de tempo: segunda-feira 2026-10-26, 00:00 em SP (UTC-3) =
 * 1792983600000 ms. dayOffset 0 = segunda (dayOfWeek 1).
 */
class StoreOpenStatusTest {

    private fun spMillis(dayOffset: Int, hour: Int, minute: Int = 0): Long =
        1792983600000L + dayOffset * 86_400_000L + hour * 3_600_000L + minute * 60_000L

    private fun hours(day: Int, open: String, close: String) =
        StoreOpeningHour(storeId = "s1", dayOfWeek = day, openTime = open, closeTime = close)

    private fun store(
        openingHours: List<StoreOpeningHour> = emptyList(),
        minOrder: Double = 0.0,
        forceClosed: Boolean = false,
        isOpen: Boolean = true,
        preorderEnabled: Boolean = false,
        preorderMinutesBefore: Int = 0
    ) = Store(
        id = "s1",
        name = "Loja",
        category = "Lanches",
        rating = 4.5,
        minOrder = minOrder,
        forceClosed = forceClosed,
        isOpen = isOpen,
        preorderEnabled = preorderEnabled,
        preorderMinutesBefore = preorderMinutesBefore,
        openingHours = openingHours
    )

    // --- Status de abertura ---

    @Test
    fun forceClosedRecusaComMotivo() {
        val status = store(forceClosed = true).storeOpenStatus(spMillis(0, 20))
        assertFalse(status.isOpen)
        assertEquals("Fechado temporariamente", status.reason)
    }

    @Test
    fun semHorariosUsaFlagManual() {
        assertEquals("Aberto", store(isOpen = true).storeOpenStatus(spMillis(0, 20)).reason)
        assertFalse(store(isOpen = false).storeOpenStatus(spMillis(0, 20)).isOpen)
    }

    @Test
    fun abertaAgoraMostraAteQuando() {
        // Segunda 18:00–02:00, agora segunda 20:00.
        val status = store(listOf(hours(1, "18:00:00", "02:00:00"))).storeOpenStatus(spMillis(0, 20))
        assertTrue(status.isOpen)
        assertEquals("Aberto até 02:00", status.reason)
    }

    @Test
    fun fechadaAbreHoje() {
        // Segunda 18:00–02:00, agora segunda 10:00.
        val status = store(listOf(hours(1, "18:00:00", "02:00:00"))).storeOpenStatus(spMillis(0, 10))
        assertFalse(status.isOpen)
        assertEquals("Abre hoje às 18:00", status.reason)
    }

    @Test
    fun overnightDeOntemContinuaAberta() {
        // Terça 18:00–02:00, agora quarta 00:30.
        val status = store(listOf(hours(2, "18:00:00", "02:00:00"))).storeOpenStatus(spMillis(2, 0, 30))
        assertTrue(status.isOpen)
        assertEquals("Aberto até 02:00", status.reason)
    }

    @Test
    fun proximoDiaAmanha() {
        // Só terça 18:00–02:00, agora segunda 10:00.
        val status = store(listOf(hours(2, "18:00:00", "02:00:00"))).storeOpenStatus(spMillis(0, 10))
        assertFalse(status.isOpen)
        assertEquals("Abre Amanhã às 18:00", status.reason)
    }

    @Test
    fun proximoDiaComNomeDoDia() {
        // Só segunda 18:00–02:00, agora terça 10:00.
        val status = store(listOf(hours(1, "18:00:00", "02:00:00"))).storeOpenStatus(spMillis(1, 10))
        assertFalse(status.isOpen)
        assertEquals("Abre Segunda às 18:00", status.reason)
    }

    @Test
    fun prePedidoNaJanelaAceita() {
        // Segunda 18:00–02:00, agora segunda 17:30, janela de 60 min.
        val s = store(
            openingHours = listOf(hours(1, "18:00:00", "02:00:00")),
            preorderEnabled = true,
            preorderMinutesBefore = 60
        )
        val status = s.storeOpenStatus(spMillis(0, 17, 30))
        assertFalse(status.isOpen)
        assertTrue(status.acceptingPreorder)
    }

    @Test
    fun prePedidoForaDaJanelaNaoAceita() {
        // Mesma loja, agora segunda 16:00 (antes da janela de 60 min).
        val s = store(
            openingHours = listOf(hours(1, "18:00:00", "02:00:00")),
            preorderEnabled = true,
            preorderMinutesBefore = 60
        )
        assertFalse(s.storeOpenStatus(spMillis(0, 16)).acceptingPreorder)
    }

    // --- Recusas do checkout (2C) ---

    @Test
    fun recusaLojaFechadaComMotivo() {
        val s = store(listOf(hours(1, "18:00:00", "02:00:00")))
        assertEquals(
            "Loja fechada. Abre hoje às 18:00",
            checkoutRefusalReason(s, 25.0, spMillis(0, 10))
        )
    }

    @Test
    fun recusaAbaixoDoMinimoComQuantoFalta() {
        val s = store(
            openingHours = listOf(hours(1, "18:00:00", "02:00:00")),
            minOrder = 20.0
        )
        assertEquals(
            "Pedido mínimo desta loja: R$ 20,00. Adicione mais R$ 1,00.",
            checkoutRefusalReason(s, 19.0, spMillis(0, 20))
        )
    }

    @Test
    fun acimaDoMinimoNaoRecusa() {
        val s = store(
            openingHours = listOf(hours(1, "18:00:00", "02:00:00")),
            minOrder = 20.0
        )
        assertNull(checkoutRefusalReason(s, 20.0, spMillis(0, 20)))
        assertNull(checkoutRefusalReason(s, 25.0, spMillis(0, 20)))
    }

    @Test
    fun fechadaVenceSobreMinimo() {
        val s = store(
            openingHours = listOf(hours(1, "18:00:00", "02:00:00")),
            minOrder = 20.0
        )
        assertEquals(
            "Loja fechada. Abre hoje às 18:00",
            checkoutRefusalReason(s, 5.0, spMillis(0, 10))
        )
    }

    @Test
    fun prePedidoNaoRecusaPorFechada() {
        val s = store(
            openingHours = listOf(hours(1, "18:00:00", "02:00:00")),
            minOrder = 20.0,
            preorderEnabled = true,
            preorderMinutesBefore = 60
        )
        // 17:30, dentro da janela: loja fechada mas aceitando pré-pedido.
        assertNull(checkoutRefusalReason(s, 25.0, spMillis(0, 17, 30)))
    }

    // --- normalizePaymentMethod (remoção do PIX Online) ---

    @Test
    fun normalizeNaoProduzPix() {
        assertEquals("pix_direto", com.example.shared.ui.cart.normalizePaymentMethod("PIX Direto"))
        assertEquals("cartao", com.example.shared.ui.cart.normalizePaymentMethod("Cartão"))
        assertEquals("dinheiro", com.example.shared.ui.cart.normalizePaymentMethod("Dinheiro"))
        // "PIX Online" NÃO mapeia mais para "pix" — nenhum caminho do app
        // pode produzir payment_method='pix' (a RPC rejeitaria de qualquer
        // forma; o servidor é o backstop).
        assertFalse(com.example.shared.ui.cart.normalizePaymentMethod("PIX Online") == "pix")
        assertEquals("pix_machine", com.example.shared.ui.cart.normalizePaymentMethod("PIX na Maquininha"))
    }
}
