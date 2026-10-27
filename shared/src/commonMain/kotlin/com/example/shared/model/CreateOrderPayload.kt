package com.example.shared.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.Uuid

/**
 * Fase 2 (G4) — montador do payload da RPC `create_order_with_items`.
 *
 * Espelha `src/lib/createOrder.ts` da web (função pura, testável): recebe os
 * dados do checkout e devolve os parâmetros da RPC, sem tocar no Supabase.
 * A RPC cria pedido + itens + descontos numa transação só, com idempotência
 * pela `idempotency_key` — o INSERT direto em `orders`/`order_items` não
 * existe mais no app.
 *
 * Regras de mapeamento (iguais às da web):
 * - Item normal: `product_id` = id do produto; `unit_price` = preço unitário
 *   do carrinho (a RPC revalida contra o catálogo e rejeita divergência).
 * - Item montado (`custom_pizza_`/`custom_pastel_`): `product_id` é o 1º
 *   sabor (a RPC não aceita id sintético — precisa de UUID de produto real
 *   da loja); `unit_price` é o preço do produto montado (o total do
 *   montador, já com sabores + borda — Fase 1); os adicionais vão como estão
 *   (sabores "½ X" com id `flavor-<uuid>`, "Tamanho: X", "Borda: X").
 * - A RPC só aceita `pix_direto` / `pix_machine` / `cartao` / `dinheiro`
 *   (whitelist endurecida na Fase 6A). "PIX Online" não existe mais no
 *   checkout do app — nenhum caminho pode produzir `payment_method='pix'`.
 */

private val rpcJson = Json { ignoreUnknownKeys = true }

/** Resultado da RPC `create_order_with_items` (mesmo formato da web). */
data class CreateOrderResult(
    val orderId: String,
    val orderNumber: Int? = null,
    val status: String = "",
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalPrice: Double = 0.0,
    val walletDiscount: Double = 0.0,
    val deliveryPin: String = "",
    val isNew: Boolean = true
)

/** Parser puro da resposta da RPC. null = resposta inesperada. */
fun parseCreateOrderResult(text: String): CreateOrderResult? {
    if (text.isBlank()) return null
    return try {
        val obj = when (val el = rpcJson.parseToJsonElement(text)) {
            is JsonObject -> el
            is JsonArray -> el.firstOrNull() as? JsonObject
            else -> null
        } ?: return null
        val orderId = obj.str("order_id")?.trim().orEmpty()
        if (orderId.isBlank()) return null
        CreateOrderResult(
            orderId = orderId,
            orderNumber = obj.str("order_number")?.toIntOrNull(),
            status = obj.str("status").orEmpty(),
            subtotal = obj.str("subtotal")?.toDoubleOrNull() ?: 0.0,
            deliveryFee = obj.str("delivery_fee")?.toDoubleOrNull() ?: 0.0,
            totalPrice = obj.str("total_price")?.toDoubleOrNull() ?: 0.0,
            walletDiscount = obj.str("wallet_discount")?.toDoubleOrNull() ?: 0.0,
            deliveryPin = obj.str("delivery_pin").orEmpty(),
            isNew = obj.str("is_new")?.toBooleanStrictOrNull() ?: true
        )
    } catch (_: Exception) {
        null
    }
}

/** Conteúdo textual de um campo (string, número ou booleano); null se ausente ou null JSON. */
private fun JsonObject.str(key: String): String? {
    val el = this[key] ?: return null
    if (el is JsonNull) return null
    return (el as? JsonPrimitive)?.content
}

/** Entrada do montador (mesmos campos do `BuildCreateOrderInput` da web). */
data class BuildCreateOrderInput(
    val clientId: String,
    val storeId: String,
    val idempotencyKey: String,
    val items: List<CartItem>,
    val deliveryFee: Double,
    val deliveryFeeAbsorbed: Double,
    val paymentMethod: String,
    val neighborhood: String? = null,
    val addressDetails: String? = null,
    val deliveryCep: String? = null,
    val deliveryCity: String? = null,
    val deliveryState: String? = null,
    val clientLat: Double? = null,
    val clientLng: Double? = null,
    val needsChange: Boolean = false,
    val changeFor: Double? = null,
    val isPickup: Boolean = false,
    val isPreorder: Boolean = false,
    val scheduledFor: String? = null,
    val couponId: String? = null,
    val couponDiscount: Double = 0.0,
    val loyaltyPoints: Int = 0,
    val loyaltyDiscount: Double = 0.0,
    val walletDiscount: Double = 0.0,
    val deliveryQuote: DeliveryQuoteSnapshot? = null
)

/** Snapshot da cotação no formato que a RPC grava em `metadata.delivery_quote`. */
fun DeliveryQuoteSnapshot.toRpcJson(): JsonObject = buildJsonObject {
    put("policy_version", JsonPrimitive(policyVersion))
    put("distance_km", JsonPrimitive(distanceKm))
    put("distance_source", JsonPrimitive(distanceSource))
    put("max_delivery_km", if (maxDeliveryKm == null) JsonNull else JsonPrimitive(maxDeliveryKm))
    put("destination_precision", JsonPrimitive(destinationPrecision))
    put("store_delivery_base", JsonPrimitive(storeDeliveryBase))
    put("platform_fee_customer", JsonPrimitive(platformFeeCustomer))
    put("platform_fee_store_absorbed", JsonPrimitive(platformFeeStoreAbsorbed))
    put("delivery_fee", JsonPrimitive(deliveryFee))
    put("vip_override_applied", if (vipOverrideApplied == null) JsonNull else JsonPrimitive(vipOverrideApplied))
    put("split_mode", if (splitMode == null) JsonNull else JsonPrimitive(splitMode))
    put("plan_type", if (planType == null) JsonNull else JsonPrimitive(planType))
    put("free_delivery_applied", JsonPrimitive(freeDeliveryApplied))
}

/** Monta o JSON da RPC. Itens montados sem sabor identificado devolvem null. */
fun buildCreateOrderPayload(input: BuildCreateOrderInput): JsonObject? {
    // Item montado sem sabor: a RPC exige UUID de produto real — sem ele o
    // pedido não pode ser gravado (mesma guarda do INSERT direto antigo).
    for (item in input.items) {
        if (PizzaPricing.isBuiltProduct(item.product) &&
            item.product.customFlavorIds.firstOrNull().isNullOrBlank()
        ) {
            return null
        }
    }
    return buildJsonObject {
        put("p_client_id", JsonPrimitive(input.clientId))
        put("p_store_id", JsonPrimitive(input.storeId))
        put("p_idempotency_key", JsonPrimitive(input.idempotencyKey))
        put(
            "p_items",
            buildJsonArray {
                input.items.forEach { cartItem ->
                    val addonsJson = buildJsonArray {
                        cartItem.selectedAddons.forEach { addon ->
                            add(
                                buildJsonObject {
                                    put("id", JsonPrimitive(addon.itemId))
                                    put("groupId", JsonPrimitive(addon.groupId))
                                    put("groupName", JsonPrimitive(addon.groupName))
                                    put("name", JsonPrimitive(addon.itemName))
                                    put("price", JsonPrimitive(addon.itemPrice))
                                }
                            )
                        }
                    }
                    add(
                        buildJsonObject {
                            put(
                                "product_id",
                                JsonPrimitive(
                                    if (PizzaPricing.isBuiltProduct(cartItem.product)) {
                                        cartItem.product.customFlavorIds.firstOrNull().orEmpty()
                                    } else {
                                        cartItem.product.id
                                    }
                                )
                            )
                            put("quantity", JsonPrimitive(cartItem.quantity))
                            put("unit_price", JsonPrimitive(cartItem.unitPrice))
                            put("addons", if (addonsJson.isEmpty()) JsonNull else addonsJson)
                            put(
                                "observations",
                                if (cartItem.notes.isBlank()) JsonNull
                                else JsonPrimitive(cartItem.notes)
                            )
                        }
                    )
                }
            }
        )
        put("p_delivery_fee", JsonPrimitive(input.deliveryFee))
        put("p_delivery_fee_absorbed", JsonPrimitive(input.deliveryFeeAbsorbed))
        put("p_payment_method", JsonPrimitive(input.paymentMethod))
        put("p_neighborhood", input.neighborhood?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_address_details", input.addressDetails?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_delivery_cep", input.deliveryCep?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_delivery_city", input.deliveryCity?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_delivery_state", input.deliveryState?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_client_lat", input.clientLat?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_client_lng", input.clientLng?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_needs_change", JsonPrimitive(input.needsChange))
        put("p_change_for", input.changeFor?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_is_pickup", JsonPrimitive(input.isPickup))
        put("p_is_preorder", JsonPrimitive(input.isPreorder))
        put("p_scheduled_for", input.scheduledFor?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_coupon_id", input.couponId?.let { JsonPrimitive(it) } ?: JsonNull)
        put("p_coupon_discount", JsonPrimitive(input.couponDiscount))
        put("p_loyalty_points", JsonPrimitive(input.loyaltyPoints))
        put("p_loyalty_discount", JsonPrimitive(input.loyaltyDiscount))
        put("p_wallet_discount", JsonPrimitive(input.walletDiscount))
        put("p_empties", JsonArray(emptyList()))
        put("p_empties_discount", JsonPrimitive(0.0))
        put("p_delivery_quote", input.deliveryQuote?.toRpcJson() ?: JsonNull)
        put("p_is_guest", JsonPrimitive(false))
        put("p_delivery_pin", JsonNull)
        put("p_order_source", JsonPrimitive("delivery"))
    }
}

/**
 * Chave de idempotência: um UUID por tentativa de checkout. A mesma chave
 * reutilizada no retry devolve o pedido já criado, sem duplicar (a RPC
 * retorna `is_new=false`). Gerar UMA vez por checkout e reutilizar no
 * retry — nunca gerar uma chave nova por tentativa.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun newIdempotencyKey(): String = Uuid.random().toString()
