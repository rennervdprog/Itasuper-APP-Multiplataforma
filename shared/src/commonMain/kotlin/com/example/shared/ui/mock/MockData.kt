package com.example.shared.ui.mock

import com.example.shared.model.DiscoverProduct
import com.example.shared.model.Store
import com.example.shared.model.StoreOpeningHour

// TODO(fase-rede): remover mocks quando o SupabaseClient via Ktor estiver pronto.
// Dados de exemplo baseados nas lojas reais de Araruama/RJ.

val mockStores: List<Store> = listOf(
    Store(
        id = "s1", name = "Pastelão Carioca", category = "Lanches",
        rating = 4.8, deliveryTime = "30-45 min", deliveryFee = "R$ 4,90",
        isOpen = true, addressStreet = "Rua das Palmeiras", addressNumber = "123",
        addressNeighborhood = "Centro", addressCity = "Araruama", addressState = "RJ",
        openingHours = listOf(
            StoreOpeningHour(storeId = "s1", dayOfWeek = 1, dayOfWeekStr = "segunda-feira", openTime = "11:00", closeTime = "23:00"),
            StoreOpeningHour(storeId = "s1", dayOfWeek = 2, dayOfWeekStr = "terça-feira", openTime = "11:00", closeTime = "23:00"),
            StoreOpeningHour(storeId = "s1", dayOfWeek = 3, dayOfWeekStr = "quarta-feira", openTime = "11:00", closeTime = "23:00"),
            StoreOpeningHour(storeId = "s1", dayOfWeek = 4, dayOfWeekStr = "quinta-feira", openTime = "11:00", closeTime = "23:00"),
            StoreOpeningHour(storeId = "s1", dayOfWeek = 5, dayOfWeekStr = "sexta-feira", openTime = "11:00", closeTime = "23:59"),
            StoreOpeningHour(storeId = "s1", dayOfWeek = 6, dayOfWeekStr = "sábado", openTime = "11:00", closeTime = "23:59"),
            StoreOpeningHour(storeId = "s1", dayOfWeek = 0, dayOfWeekStr = "domingo", openTime = "", closeTime = "", isClosedAllDay = true)
        )
    ),
    Store(
        id = "s2", name = "Doceria da Cleide", category = "Doces",
        rating = 4.9, deliveryTime = "20-35 min", deliveryFee = "Grátis",
        isFreeDelivery = true, isOpen = true,
        addressStreet = "Av. Brasil", addressNumber = "456",
        addressNeighborhood = "Centro", addressCity = "Araruama", addressState = "RJ"
    ),
    Store(
        id = "s3", name = "Gelobol Açaí", category = "Açaí",
        rating = 4.7, deliveryTime = "25-40 min", deliveryFee = "R$ 3,90",
        isOpen = true,
        addressStreet = "Rua do Comércio", addressNumber = "78",
        addressNeighborhood = "Praia do Hospício", addressCity = "Araruama", addressState = "RJ"
    ),
    Store(
        id = "s4", name = "Marmitaria Sabor Caseiro", category = "Marmitas",
        rating = 4.6, deliveryTime = "40-55 min", deliveryFee = "R$ 5,90",
        isOpen = false,
        addressStreet = "Rua Projetada", addressNumber = "200",
        addressNeighborhood = "Parati", addressCity = "Araruama", addressState = "RJ"
    ),
    Store(
        id = "s5", name = "Pizza do Zé", category = "Pizza",
        rating = 4.5, deliveryTime = "35-50 min", deliveryFee = "R$ 6,90",
        isOpen = true,
        addressStreet = "Rua Getúlio Vargas", addressNumber = "900",
        addressNeighborhood = "Centro", addressCity = "Araruama", addressState = "RJ"
    )
)

val mockProducts: List<DiscoverProduct> = listOf(
    DiscoverProduct(id = "p1", storeId = "s1", storeName = "Pastelão Carioca", name = "Pastel de Carne", price = 12.90, imageUrl = ""),
    DiscoverProduct(id = "p2", storeId = "s1", storeName = "Pastelão Carioca", name = "Pastel de Queijo", price = 11.90, imageUrl = ""),
    DiscoverProduct(id = "p3", storeId = "s2", storeName = "Doceria da Cleide", name = "Bolo de Chocolate", price = 18.00, imageUrl = ""),
    DiscoverProduct(id = "p4", storeId = "s3", storeName = "Gelobol Açaí", name = "Açaí 500ml", price = 16.90, imageUrl = ""),
    DiscoverProduct(id = "p5", storeId = "s4", storeName = "Marmitaria Sabor Caseiro", name = "Marmita Executiva", price = 22.90, imageUrl = ""),
    DiscoverProduct(id = "p6", storeId = "s5", storeName = "Pizza do Zé", name = "Pizza Calabresa Grande", price = 49.90, imageUrl = "")
)

val mockCategories = listOf("Lanches", "Pizza", "Marmitas", "Doces", "Açaí", "Bebidas")

fun findMockStore(id: String): Store = mockStores.firstOrNull { it.id == id } ?: mockStores.first()
