package com.example.shared.model

/**
 * Preço da pizza — espelha a web (src/lib/pizzaPricing.ts,
 * ProductDetailModal.tsx e PizzaHalfHalfModal.tsx, produção).
 *
 * Regras da web:
 * - Montador (PizzaHalfHalfModal): combinePricesByMode — "media"/"soma" tira a
 *   média dos sabores; qualquer outro modo vale o maior.
 * - Modal de produto (ProductDetailModal): por grupo price_replaces_base, o
 *   modo "maior" vale o MAIOR item distinto do grupo (o mesmo sabor 2x conta
 *   uma vez só); demais modos somam os lineTotals do grupo. Adicionais
 *   normais sempre somam.
 * - Item montado (pizza/pastel meio a meio): o preço do produto JÁ é o total
 *   do montador — somar os adicionais de novo no carrinho contaria 2x (G1).
 */
object PizzaPricing {

    /** Igual a combinePricesByMode da web: "media"/"soma" → média; resto → maior. */
    fun combinePricesByMode(prices: List<Double>, mode: String): Double {
        if (prices.isEmpty()) return 0.0
        return if (mode == "media" || mode == "soma") prices.sum() / prices.size
        else prices.maxOrNull() ?: 0.0
    }

    /**
     * Preço do MONTADOR quando o modo pode estar ausente (G12).
     * Regra do SERVIDOR (RPC create_order_expected_item_price, migration
     * 20261026070000, comentário "outro/ausente = soma"): no caminho do
     * montador, só "maior" vale o maior — qualquer outro valor E a ausência
     * valem a MÉDIA dos sabores. O painel rotula a ausência como "Maior
     * valor", mas o pedido seria recusado pela RPC se o app cobrasse o maior
     * ("Preço do item divergente do catálogo"). Não usar combinePricesByMode
     * aqui: ela trata ausente como maior.
     */
    fun combineBuilderPrices(prices: List<Double>, mode: String?): Double {
        if (prices.isEmpty()) return 0.0
        return if (mode == "maior") prices.maxOrNull() ?: 0.0
        else prices.sum() / prices.size
    }

    /** Id sintético do item montado (custom_pizza_/custom_pastel_ + millis). */
    fun isBuiltProduct(product: Product): Boolean =
        product.id.startsWith("custom_pizza_") || product.id.startsWith("custom_pastel_")
}
