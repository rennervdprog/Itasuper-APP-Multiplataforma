package com.example.shared.model

data class DiscoverProduct(
    val id: String,
    val storeId: String,
    val storeName: String,
    val storeCategory: String = "",
    val name: String,
    val price: Double,
    val imageUrl: String
)

data class MenuSection(
    val id: String,
    val storeId: String,
    val name: String,
    val sortOrder: Int
)

data class AddonGroup(
    val id: String,
    val storeId: String? = null,
    val productId: String? = null,
    val name: String,
    val minSelect: Int = 0,
    val maxSelect: Int = 1,
    val sortOrder: Int = 0,
    val priceReplacesBase: Boolean = false
)

data class AddonItem(
    val id: String,
    val groupId: String,
    val name: String,
    val price: Double = 0.0,
    val sortOrder: Int = 0,
    val isAvailable: Boolean = true
)

data class SelectedAddonItem(
    val itemId: String,
    val itemName: String,
    val itemPrice: Double,
    val groupId: String,
    val groupName: String,
    val priceReplacesBase: Boolean = false
)

data class PastelBorder(
    val id: String,
    val storeId: String,
    val name: String,
    val price: Double = 0.0,
    val isAvailable: Boolean = true,
    val sortOrder: Int = 0
)

/** Borda da pizza (tabela pizza_borders) — mesma estrutura da web. */
data class PizzaBorder(
    val id: String,
    val storeId: String,
    val name: String,
    val price: Double = 0.0,
    val isAvailable: Boolean = true,
    val sortOrder: Int = 0
)

/** Grupo de adicionais do montador com seus itens (product_addon_groups + join). */
data class PizzaAddonGroup(
    val id: String,
    val name: String,
    val minSelect: Int = 0,
    val maxSelect: Int = 99,
    val priceReplacesBase: Boolean = false,
    val items: List<AddonItem> = emptyList()
)

data class PizzaLegacySize(
    val name: String,
    val price: Double
)

data class Product(
    val id: String,
    val storeId: String,
    val name: String,
    val description: String,
    val price: Double,
    val originalPrice: Double? = null,
    val category: String,
    val sectionId: String? = null,
    val imageUrl: String = "",
    val isAvailable: Boolean = true,
    val hasStuffedCrust: Boolean = false,
    val isCombo: Boolean = false,
    val isPastelFlavor: Boolean = false,
    val isBeverage: Boolean = false,
    val requiresPrescription: Boolean = false,
    val isControlled: Boolean = false,
    val pharmacySaleMode: String = "platform_checkout",
    val pharmacyType: String = "",
    val activeIngredient: String = "",
    val dosage: String = "",
    val pharmaForm: String = "",
    val manufacturer: String = "",
    val packQuantity: String = "",
    val isGeneric: Boolean = false,
    val pizzaCategoryId: String = "",
    val pizzaSizeOverrides: Map<String, Double> = emptyMap(),
    val pizzaUnavailableSizeIds: Set<String> = emptySet(),
    val legacyPizzaSizes: List<PizzaLegacySize> = emptyList(),
    /** Sabores de um item montado (pizza/pastel meio a meio) — igual ao app original. */
    val customFlavorIds: List<String> = emptyList()
)

data class CartItem(
    val product: Product,
    val quantity: Int,
    val notes: String = "",
    val selectedAddons: List<SelectedAddonItem> = emptyList(),
    /**
     * Regra de preço da loja (settings.pizza_price_mode). Só afeta grupos
     * price_replaces_base no modal de produto (1B/1D). O default "maior"
     * preserva o comportamento anterior nos demais caminhos.
     */
    val priceMode: String = "maior"
) {
    val unitPrice: Double
        get() {
            // Item montado (pizza/pastel meio a meio): o preço do produto JÁ é
            // o total do montador — somar os adicionais de novo contaria a
            // borda/adicionais 2x no carrinho (G1: 89,98 virava 99,97).
            if (PizzaPricing.isBuiltProduct(product)) {
                return product.price
            }
            val replacing = selectedAddons.filter { it.priceReplacesBase }
            if (replacing.isNotEmpty()) {
                // Modal da web (ProductDetailModal): por grupo que substitui a
                // base, o modo "maior" vale o MAIOR item distinto do grupo (o
                // mesmo sabor 2x conta uma vez só); demais modos somam os
                // lineTotals do grupo (G2: 64,99+74,99+79,99 = 79,99 em
                // "maior", 219,97 nos demais).
                val replacementTotal = replacing.groupBy { it.groupId }.values.sumOf { rows ->
                    if (priceMode == "maior") {
                        rows.distinctBy { it.itemId }.maxOfOrNull { it.itemPrice } ?: 0.0
                    } else {
                        rows.sumOf { it.itemPrice }
                    }
                }
                val normalTotal = selectedAddons.filter { !it.priceReplacesBase }.sumOf { it.itemPrice }
                return replacementTotal + normalTotal
            }
            val addonsSum = selectedAddons.sumOf { it.itemPrice }
            return product.price + addonsSum
        }

    val totalPrice: Double
        get() = unitPrice * quantity
}

/** Grupos e itens de adicionais vinculados a um produto. */
data class ProductAddonsPayload(
    val groups: List<AddonGroup> = emptyList(),
    val items: List<AddonItem> = emptyList()
)
