package com.example.shared.model

data class PizzaSizeCatalogItem(
    val id: String,
    val name: String,
    val description: String = "",
    val maxFlavors: Int = 4,
    val active: Boolean = true
)

data class StoreSettings(
    val pizzaHalfEnabled: Boolean = true,
    val pastelHalfEnabled: Boolean = true,
    val pizzaMaxFlavors: Int = 4,
    val pastelMaxFlavors: Int = 4,
    val pastelMaxComplements: Int = 3,
    val pizzaPriceMode: String = "maior",
    val pizzaSizesCatalog: List<PizzaSizeCatalogItem> = emptyList(),
    val pizzaPriceMatrix: Map<String, Map<String, Double>> = emptyMap(),
    val pastelPriceMode: String = "maior",
    val pizzaSingleSize: Boolean = false,
    val pastelSingleSize: Boolean = false,
    val acceptPixOnline: Boolean = false,
    val acceptPixMachine: Boolean = false,
    val acceptCard: Boolean = true,
    val acceptCash: Boolean = true
)

data class Store(
    val id: String,
    val name: String,
    val category: String,
    val secondaryCategories: List<String> = emptyList(),
    val rating: Double,
    val deliveryTime: String = "",
    val deliveryFee: String = "",
    /** Taxa-base ao cliente já calculada pela regra VIP da loja, antes da validação por endereço. */
    val officialCustomerDeliveryFee: Double? = null,
    val isFreeDelivery: Boolean = false,
    val isOpen: Boolean = true,
    val forceClosed: Boolean = false,
    val preorderEnabled: Boolean = false,
    val preorderMinutesBefore: Int = 0,
    val distanceKm: Double? = null,
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val minOrder: Double = 0.0,
    val createdAt: String = "",
    val slug: String = "",
    val status: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val settings: StoreSettings = StoreSettings(),
    val deliveryMode: String = "",
    /** null enquanto a disponibilidade ainda não foi confirmada; false bloqueia somente entrega. */
    val hasAvailableDriver: Boolean? = null,
    val deliveryAvailabilityMessage: String = "",
    val ownDeliveryFee: Double = 0.0,
    val deliveryFeeType: String = "fixed",
    val deliveryBaseKm: Double = 0.0,
    val deliveryFeeBase: Double = 0.0,
    val deliveryFeePerKm: Double = 0.0,
    val platformDeliveryFee: Double = 0.0,
    val platformFeeSplit: String = "cliente",
    val planType: String = "",
    val platformDeliverySplitOverride: Double? = null,
    val autonomyLifetimeFree: Boolean = false,
    val pixDirectEnabled: Boolean = false,
    val pixDirectKey: String = "",
    val pixDirectKeyType: String = "",
    val pixDirectBeneficiary: String = "",
    val pixDirectInstructions: String = "",
    val addressStreet: String = "",
    val addressNumber: String = "",
    val addressNeighborhood: String = "",
    val addressCity: String = "",
    val addressState: String = "",
    val addressCep: String = "",
    val addressReference: String = "",
    val address: String = "",
    val whatsapp: String = "",
    val openingHours: List<StoreOpeningHour> = emptyList()
)

data class StoreOpeningHour(
    val storeId: String,
    val dayOfWeek: Int,
    val dayOfWeekStr: String = "",
    val openTime: String,
    val closeTime: String,
    val isClosedAllDay: Boolean = false
)

data class CategoryItem(
    val id: String,
    val name: String,
    val iconName: String
)

data class LastOrder(
    val id: String,
    val storeId: String,
    val storeName: String,
    val storeLogoUrl: String,
    val dateText: String,
    val itemsSummary: String,
    val totalPrice: Double
)

data class StoreWithCoupon(
    val storeId: String,
    val storeName: String,
    val storeLogoUrl: String?,
    val couponCode: String,
    val discountType: String,
    val discountValue: Double,
    val couponDescription: String?
) {
    /** Texto do badge, ex: "-10%" ou "-R$10". */
    val badgeText: String
        get() = when (discountType) {
            "percent" -> "-${discountValue.toInt()}%"
            else -> "-R$${discountValue.toInt()}"
        }
}
