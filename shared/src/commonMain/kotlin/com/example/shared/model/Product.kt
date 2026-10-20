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
    val legacyPizzaSizes: List<PizzaLegacySize> = emptyList()
)

data class CartItem(
    val product: Product,
    val quantity: Int,
    val notes: String = "",
    val selectedAddons: List<SelectedAddonItem> = emptyList()
) {
    val unitPrice: Double
        get() {
            var base = product.price
            val replacingAddon = selectedAddons.firstOrNull { it.priceReplacesBase }
            if (replacingAddon != null) {
                base = replacingAddon.itemPrice
                val otherAddonsSum = selectedAddons.filter { it.itemId != replacingAddon.itemId }.sumOf { it.itemPrice }
                return base + otherAddonsSum
            }
            val addonsSum = selectedAddons.sumOf { it.itemPrice }
            return base + addonsSum
        }

    val totalPrice: Double
        get() = unitPrice * quantity
}

/** Grupos e itens de adicionais vinculados a um produto. */
data class ProductAddonsPayload(
    val groups: List<AddonGroup> = emptyList(),
    val items: List<AddonItem> = emptyList()
)
