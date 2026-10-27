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
    /**
     * Regra de preço da loja (settings.pizza_price_mode / pastel_price_mode).
     * NULL quando a loja não configurou — e NULL NÃO é "maior": o servidor
     * (RPC create_order_expected_item_price, 20261026070000) trata modo
     * ausente como média no montador e soma no modal. O painel mostra
     * "Maior valor" como rótulo, mas o preço que vale é o do servidor (G12).
     */
    val pizzaPriceMode: String? = null,
    val pizzaSizesCatalog: List<PizzaSizeCatalogItem> = emptyList(),
    val pizzaPriceMatrix: Map<String, Map<String, Double>> = emptyMap(),
    val pastelPriceMode: String? = null,
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
    /** A loja tem chave Pix Direto configurada (coluna pix_direto_key_configured da view — a chave em si NÃO vem mais na view desde a Fase 5, 26/10). */
    val pixDirectKeyConfigured: Boolean = false,
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
    /** Texto do badge, ex: "-10%" ou "-R$ 10,50". */
    val badgeText: String
        get() = when (discountType) {
            "percent", "percentage" -> "-${discountValue.toInt()}%"
            else -> "-${com.example.shared.util.formatBRL(discountValue)}"
        }
}

/**
 * Calcula a distância em km entre dois pontos usando a fórmula de Haversine.
 */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Raio da Terra em km
    val dLat = (lat2 - lat1) * kotlin.math.PI / 180.0
    val dLon = (lon2 - lon1) * kotlin.math.PI / 180.0
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(lat1 * kotlin.math.PI / 180.0) * kotlin.math.cos(lat2 * kotlin.math.PI / 180.0) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}

/**
 * Ordena lojas por relevância: abertas primeiro, depois por distância, depois por nome.
 */
fun List<Store>.sortedByRelevance(userLat: Double? = null, userLon: Double? = null): List<Store> {
    return this.sortedWith(compareBy<Store> { 
        // Fechadas por último (0 = aberta, 1 = fechada)
        if (it.isOpen) 0 else 1 
    }.thenBy { store ->
        // Por distância (se disponível)
        if (userLat != null && userLon != null && 
            store.latitude != null && store.longitude != null) {
            haversineKm(userLat, userLon, store.latitude, store.longitude)
        } else {
            store.distanceKm ?: Double.MAX_VALUE
        }
    }.thenBy { 
        it.name.lowercase() 
    })
}

/** Disponibilidade de entrega da loja (RPC store_delivery_availability). */
data class StoreDeliveryAvailability(
    val canAcceptDeliveryOrders: Boolean = false,
    val availableDriversCount: Int = 0,
    val reasonCode: String = "",
    val reasonMessage: String = ""
)
