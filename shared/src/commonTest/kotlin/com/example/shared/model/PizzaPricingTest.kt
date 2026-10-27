package com.example.shared.model

import com.example.shared.network.SupabaseApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Fase 1 do alinhamento web × KMP — números da auditoria (fase0.md):
 * 3 sabores 64,99 + 74,99 + 79,99, modo "maior", borda 9,99.
 * Web: montador 89,98. App antes do fix: carrinho 99,97 (G1) e modal 219,97 (G2).
 */
class PizzaPricingTest {

    private fun flavor(groupId: String, itemId: String, price: Double) =
        SelectedAddonItem(
            itemId = itemId,
            itemName = "Sabor $itemId",
            itemPrice = price,
            groupId = groupId,
            groupName = "Sabores",
            priceReplacesBase = true
        )

    private fun normal(groupId: String, itemId: String, price: Double) =
        SelectedAddonItem(
            itemId = itemId,
            itemName = "Item $itemId",
            itemPrice = price,
            groupId = groupId,
            groupName = "Extras"
        )

    private fun pizza(price: Double, id: String = "pizza-45cm") =
        Product(id = id, storeId = "store-1", name = "Pizza 45cm", description = "", price = price, category = "Pizzas")

    private val tresSabores = listOf(
        flavor("sabores", "f1", 64.99),
        flavor("sabores", "f2", 74.99),
        flavor("sabores", "f3", 79.99)
    )

    // --- combinePricesByMode (montador) ---

    @Test
    fun combineMaiorValeOMaior() {
        assertEquals(79.99, PizzaPricing.combinePricesByMode(listOf(64.99, 74.99, 79.99), "maior"), 0.001)
    }

    @Test
    fun combineMediaTiraAMedia() {
        assertEquals(73.323, PizzaPricing.combinePricesByMode(listOf(64.99, 74.99, 79.99), "media"), 0.001)
    }

    @Test
    fun combineSomaTiraAMediaComoAWeb() {
        // Quirk da web (combinePricesByMode): "soma" NUNCA soma no montador —
        // cai no mesmo ramo da média. O modal é que soma (1D).
        assertEquals(73.323, PizzaPricing.combinePricesByMode(listOf(64.99, 74.99, 79.99), "soma"), 0.001)
    }

    @Test
    fun combineVazioValeZero() {
        assertEquals(0.0, PizzaPricing.combinePricesByMode(emptyList(), "maior"), 0.001)
    }

    // --- G12 (Fase 5): montador com modo ausente vale a média, como o servidor ---

    @Test
    fun builderModoAusenteValeAMedia() {
        // RPC create_order_expected_item_price (20261026070000): no montador,
        // só "maior" vale o maior — outro/ausente vale a média. Antes da Fase 5
        // o app tratava ausente como "maior" e o pedido era recusado pela RPC
        // ("Preço do item divergente do catálogo").
        assertEquals(73.323, PizzaPricing.combineBuilderPrices(listOf(64.99, 74.99, 79.99), null), 0.001)
    }

    @Test
    fun builderMaiorValeOMaior() {
        assertEquals(79.99, PizzaPricing.combineBuilderPrices(listOf(64.99, 74.99, 79.99), "maior"), 0.001)
    }

    @Test
    fun builderSomaEMediaValemAMedia() {
        assertEquals(73.323, PizzaPricing.combineBuilderPrices(listOf(64.99, 74.99, 79.99), "soma"), 0.001)
        assertEquals(73.323, PizzaPricing.combineBuilderPrices(listOf(64.99, 74.99, 79.99), "media"), 0.001)
    }

    @Test
    fun builderVazioValeZero() {
        assertEquals(0.0, PizzaPricing.combineBuilderPrices(emptyList(), null), 0.001)
    }

    @Test
    fun g12ModalModoAusenteSomaOsSabores() {
        // No modal, modo ausente NÃO é "maior": o servidor soma os lineTotals
        // do grupo (RPC 20261026070000, caminho v_has_repl). 3 sabores +
        // borda = 219,97 + 9,99.
        val item = CartItem(
            product = pizza(0.0),
            quantity = 1,
            selectedAddons = tresSabores + normal("borda", "b1", 9.99),
            priceMode = null
        )
        assertEquals(229.96, item.unitPrice, 0.001)
    }

    @Test
    fun g12ModalModoAusenteSomaDuplicadoDuasVezes() {
        // Sem "maior" não há dedupe: o mesmo sabor 2x soma 2x (lineTotals).
        val addons = listOf(
            flavor("sabores", "f3", 79.99),
            flavor("sabores", "f3", 79.99)
        )
        val item = CartItem(product = pizza(0.0), quantity = 1, selectedAddons = addons, priceMode = null)
        assertEquals(159.98, item.unitPrice, 0.001)
    }

    // --- G1: item montado não soma os adicionais de novo ---

    @Test
    fun g1Montador8998IgualCarrinho8998() {
        // O builtProduct.price JÁ é o total do montador (89,98); a borda e os
        // sabores vão junto no CartItem, mas não podem somar de novo.
        val built = pizza(89.98, id = "custom_pizza_1729999999999")
        val item = CartItem(
            product = built,
            quantity = 1,
            selectedAddons = tresSabores + normal("border", "b1", 9.99),
            priceMode = "maior"
        )
        assertEquals(89.98, item.unitPrice, 0.001)
        assertEquals(179.96, CartItem(product = built, quantity = 2, selectedAddons = tresSabores + normal("border", "b1", 9.99)).totalPrice, 0.001)
    }

    // --- G2: modal por grupo price_replaces_base ---

    @Test
    fun g2ModalMaiorTresSabores7999() {
        val item = CartItem(
            product = pizza(0.0),
            quantity = 1,
            selectedAddons = tresSabores + normal("borda", "b1", 9.99),
            priceMode = "maior"
        )
        assertEquals(89.98, item.unitPrice, 0.001)
    }

    @Test
    fun g2ModalSomaSomaOsLineTotals() {
        // No modal, modo "soma" soma os lineTotals do grupo (219,97) + borda.
        val item = CartItem(
            product = pizza(0.0),
            quantity = 1,
            selectedAddons = tresSabores + normal("borda", "b1", 9.99),
            priceMode = "soma"
        )
        assertEquals(229.96, item.unitPrice, 0.001)
    }

    @Test
    fun g2SaborDuplicadoContaUmaVezSo() {
        val addons = listOf(
            flavor("sabores", "f3", 79.99),
            flavor("sabores", "f3", 79.99),
            flavor("sabores", "f1", 64.99)
        )
        val item = CartItem(product = pizza(0.0), quantity = 1, selectedAddons = addons, priceMode = "maior")
        assertEquals(79.99, item.unitPrice, 0.001)
    }

    @Test
    fun maiorPorGrupoSeparado() {
        val addons = listOf(
            flavor("sabores", "f1", 10.0),
            flavor("sabores", "f2", 20.0),
            flavor("queijos", "q1", 5.0),
            flavor("queijos", "q2", 7.0),
            normal("extras", "e1", 3.0)
        )
        val item = CartItem(product = pizza(0.0), quantity = 1, selectedAddons = addons, priceMode = "maior")
        assertEquals(30.0, item.unitPrice, 0.001)
    }

    // --- Regressões: comportamento anterior preservado ---

    @Test
    fun produtoNormalSomaAdicionais() {
        val item = CartItem(
            product = pizza(25.0),
            quantity = 1,
            selectedAddons = listOf(normal("extras", "e1", 3.0))
        )
        assertEquals(28.0, item.unitPrice, 0.001)
    }

    @Test
    fun umSaborSubstituindoABase() {
        val item = CartItem(
            product = pizza(50.0),
            quantity = 1,
            selectedAddons = listOf(flavor("sabores", "f3", 64.99)),
            priceMode = "maior"
        )
        assertEquals(64.99, item.unitPrice, 0.001)
    }

    // --- 1C: parser da RPC get_store_pix_direto ---

    @Test
    fun parsePixDiretoArrayComChave() {
        val json = """[{"name":"Loja Demonstração","pix_direto_key":"5514999999999","pix_direto_key_type":"phone","pix_direto_beneficiary":"Loja","pix_direto_instructions":"Pague até 20 min"}]"""
        val info = SupabaseApi.parsePixDiretoResponse(json)
        assertEquals("Loja Demonstração", info?.storeName)
        assertEquals("5514999999999", info?.pixKey)
        assertEquals("phone", info?.pixKeyType)
        assertEquals("Loja", info?.beneficiary)
    }

    @Test
    fun parsePixDiretoVazioENulo() {
        assertNull(SupabaseApi.parsePixDiretoResponse("[]"))
        assertNull(SupabaseApi.parsePixDiretoResponse(""))
        assertNull(SupabaseApi.parsePixDiretoResponse("""[{"name":"Loja","pix_direto_key":""}]"""))
    }
}
