package com.example.shared.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fase 2 (2A/2B) — montador do payload da RPC `create_order_with_items`.
 * Espelha `src/lib/createOrder.ts` da web: o item normal usa o id do
 * produto; o item montado usa o 1º sabor como `product_id` e o preço do
 * montador como `unit_price` (a RPC não soma os sabores de novo).
 */
class CreateOrderPayloadTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun normalProduct(price: Double = 25.0, id: String = "prod-nuggets") =
        Product(id = id, storeId = "store-1", name = "Porção de Nuggets", description = "", price = price, category = "Porções")

    private fun builtPizza(price: Double = 97.99, flavorIds: List<String> = listOf("flavor-uuid-1", "flavor-uuid-2")) =
        Product(
            id = "custom_pizza_1729999999999",
            storeId = "store-bittencas",
            name = "Pizza Salgada 35 cm",
            description = "",
            price = price,
            category = "Pizzas",
            customFlavorIds = flavorIds
        )

    private fun flavorAddon(itemId: String, name: String, price: Double = 0.0) =
        SelectedAddonItem(
            itemId = itemId,
            itemName = name,
            itemPrice = price,
            groupId = "sabor-35cm",
            groupName = "Sabor 35 cm",
            priceReplacesBase = true
        )

    private fun baseInput(items: List<CartItem>, paymentMethod: String = "cartao") =
        BuildCreateOrderInput(
            clientId = "client-1",
            storeId = "store-1",
            idempotencyKey = "key-123",
            items = items,
            deliveryFee = 0.0,
            deliveryFeeAbsorbed = 0.0,
            paymentMethod = paymentMethod,
            isPickup = true
        )

    private fun payloadJson(input: BuildCreateOrderInput) =
        json.parseToJsonElement(buildCreateOrderPayload(input)!!.toString()).jsonObject

    // --- 2A: item normal ---

    @Test
    fun itemNormalUsaIdDoProdutoEPrecoDoCarrinho() {
        val item = CartItem(
            product = normalProduct(25.0),
            quantity = 2,
            selectedAddons = listOf(
                SelectedAddonItem("e1", "Extra", 3.0, "extras", "Extras")
            )
        )
        val obj = payloadJson(baseInput(listOf(item)))
        val items = obj["p_items"]!!.jsonArray
        assertEquals(1, items.size)
        val first = items[0].jsonObject
        assertEquals("prod-nuggets", first["product_id"]!!.jsonPrimitive.content)
        assertEquals(2, first["quantity"]!!.jsonPrimitive.content.toInt())
        assertEquals(28.0, first["unit_price"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertEquals("e1", first["addons"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals("Extras", first["addons"]!!.jsonArray[0].jsonObject["groupName"]!!.jsonPrimitive.content)
        assertEquals("3.0", first["addons"]!!.jsonArray[0].jsonObject["price"]!!.jsonPrimitive.content)
        // Sem observações: null JSON (a RPC aceita null).
        assertEquals("null", first["observations"].toString())
    }

    // --- 2B: item montado ---

    @Test
    fun itemMontadoUsaPrimeiroSaborEPrecoDoMontador() {
        // Pizza da Bittenca's: 3 sabores + borda = 97,99 no modal e no carrinho.
        val item = CartItem(
            product = builtPizza(97.99),
            quantity = 1,
            selectedAddons = listOf(
                flavorAddon("flavor-flavor-uuid-1", "½ Muçarela"),
                flavorAddon("flavor-flavor-uuid-2", "½ Carne seca com Catupiry"),
                SelectedAddonItem("borda-1", "Borda: Catupiry", 18.0, "borda-35cm", "Borda 35 cm")
            ),
            priceMode = "maior"
        )
        val obj = payloadJson(baseInput(listOf(item)))
        val first = obj["p_items"]!!.jsonArray[0].jsonObject
        assertEquals("flavor-uuid-1", first["product_id"]!!.jsonPrimitive.content)
        assertEquals(97.99, first["unit_price"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertEquals(3, first["addons"]!!.jsonArray.size)
    }

    @Test
    fun itemMontadoSemSaborDevolveNull() {
        val item = CartItem(
            product = builtPizza(97.99, flavorIds = emptyList()),
            quantity = 1
        )
        assertNull(buildCreateOrderPayload(baseInput(listOf(item))))
    }

    // --- Parâmetros da RPC ---

    @Test
    fun payloadTemParametrosDaRpc() {
        val obj = payloadJson(baseInput(listOf(CartItem(product = normalProduct(), quantity = 1))))
        assertEquals("client-1", obj["p_client_id"]!!.jsonPrimitive.content)
        assertEquals("store-1", obj["p_store_id"]!!.jsonPrimitive.content)
        assertEquals("key-123", obj["p_idempotency_key"]!!.jsonPrimitive.content)
        assertEquals("cartao", obj["p_payment_method"]!!.jsonPrimitive.content)
        assertEquals("false", obj["p_is_guest"]!!.jsonPrimitive.content)
        assertEquals("delivery", obj["p_order_source"]!!.jsonPrimitive.content)
        assertEquals("true", obj["p_is_pickup"]!!.jsonPrimitive.content)
        assertEquals("0.0", obj["p_delivery_fee"]!!.jsonPrimitive.content)
        assertEquals("[]", obj["p_empties"].toString())
    }

    @Test
    fun payloadComSnapshotDaCotacao() {
        val snapshot = DeliveryQuoteSnapshot(
            policyVersion = 1,
            distanceKm = 2.5,
            distanceSource = "route",
            maxDeliveryKm = 15.0,
            destinationPrecision = "exact",
            storeDeliveryBase = 6.0,
            platformFeeCustomer = 0.99,
            platformFeeStoreAbsorbed = 0.0,
            deliveryFee = 6.99,
            vipOverrideApplied = null,
            splitMode = "cliente",
            planType = "essencial",
            freeDeliveryApplied = false
        )
        val obj = payloadJson(baseInput(listOf(CartItem(product = normalProduct(), quantity = 1))).copy(deliveryQuote = snapshot, isPickup = false))
        val quote = obj["p_delivery_quote"]!!.jsonObject
        assertEquals("1", quote["policy_version"]!!.jsonPrimitive.content)
        assertEquals("6.99", quote["delivery_fee"]!!.jsonPrimitive.content)
        assertEquals("0.99", quote["platform_fee_customer"]!!.jsonPrimitive.content)
    }

    // --- Parser da resposta ---

    @Test
    fun parseCreateOrderResultCompleto() {
        val result = parseCreateOrderResult(
            """{"order_id":"abc-123","order_number":42,"status":"pendente","subtotal":25.0,"delivery_fee":0.0,"total_price":25.0,"wallet_discount":0.0,"delivery_pin":"1234","is_new":true}"""
        )
        assertEquals("abc-123", result?.orderId)
        assertEquals(42, result?.orderNumber)
        assertEquals("pendente", result?.status)
        assertEquals(25.0, result?.totalPrice)
        assertEquals("1234", result?.deliveryPin)
        assertEquals(true, result?.isNew)
    }

    @Test
    fun parseCreateOrderResultRetryNaoENovo() {
        val result = parseCreateOrderResult(
            """{"order_id":"abc-123","order_number":42,"status":"pendente","subtotal":25.0,"delivery_fee":0.0,"total_price":25.0,"wallet_discount":0.0,"delivery_pin":"1234","is_new":false}"""
        )
        assertEquals(false, result?.isNew)
    }

    @Test
    fun parseCreateOrderResultInvalido() {
        assertNull(parseCreateOrderResult(""))
        assertNull(parseCreateOrderResult("{}"))
        assertNull(parseCreateOrderResult("[]"))
        assertNull(parseCreateOrderResult("não-json"))
    }

    // --- Chave de idempotência ---

    @Test
    fun newIdempotencyKeyGeraUuidsDiferentes() {
        val a = newIdempotencyKey()
        val b = newIdempotencyKey()
        assertTrue(a.isNotBlank())
        assertNotEquals(a, b)
        assertTrue(a.contains("-"))
    }
}
