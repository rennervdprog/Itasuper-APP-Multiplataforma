package com.example.shared.ui.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.shared.generated.resources.ic_cat_acai_3d
import com.example.shared.generated.resources.ic_cat_bebidas_3d
import com.example.shared.generated.resources.ic_cat_churrasco_3d
import com.example.shared.generated.resources.ic_cat_farmacia_3d
import com.example.shared.generated.resources.ic_cat_lanches_3d
import com.example.shared.generated.resources.ic_cat_marmita_3d
import com.example.shared.generated.resources.ic_cat_mercado_3d
import com.example.shared.generated.resources.ic_cat_pastel_3d
import com.example.shared.generated.resources.ic_cat_pizzaria_3d
import com.example.shared.generated.resources.Res
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.example.shared.model.Coupon
import com.example.shared.model.DiscoverProduct
import com.example.shared.model.Product
import com.example.shared.model.Store
import com.example.shared.model.haversineKm
import com.example.shared.model.StoreWithCoupon
import com.example.shared.network.SupabaseApi
import com.example.shared.platform.LocationProvider
import com.example.shared.session.UserSessionRepository
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ManropeFontFamily
import com.example.shared.ui.cart.CartHolder
import com.example.shared.ui.common.StoreSwitchBottomSheet
import com.example.shared.ui.permissions.LocationAndPermissionsDialog
import com.example.shared.ui.permissions.PlatformPermissions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Tela Descobrir/Busca — migração do SearchScreen do app Android (Itasuper-APP-NATIVO).
 * Layout idêntico ao original (tokens, textos, ordem, espaçamentos, tamanhos).
 * Dados 100% reais (SupabaseApi + sessão), com a mesma lógica do
 * SearchViewModel do original: região pela cidade efetiva, lojas de teste
 * fora, produtos/cupons/pedidos reais — nada de dados mockados.
 */

// ===== Tokens visuais do protótipo (idênticos ao app Android) =====
private val DscBg = Color(0xFFFFFFFF)      // branco puro
private val DscInk = Color(0xFF221812)     // --phone-ink
private val DscInk2 = Color(0xFF6F6154)    // --phone-ink-2
private val DscLine = Color(0xFFEEE3D6)     // --phone-line
private val DscChipBg = Color(0xFFF4EDE5)   // --chip-bg
private val DscSkel = Color(0xFFEADFD2)     // --skel
private val DscAmber = Color(0xFFE8930C)    // --amber (estrelas)
private val DscGreen = Color(0xFF177A3D)    // --green
private val DscBrown = Color(0xFF8A4B0F)    // banner-3

// ===== Modelos locais da Descoberta (TODO fase-rede: virão do Supabase) =====

private data class SearchCategory(
    val id: String,
    val name: String,
    val matchingTerms: List<String>
)

private enum class DiscoverQuickFilter(val label: String) {
    FREE("Entrega grátis"),
    TOP("4.5+ ★"),
    FAST("< 30 min"),
    COUPON("Com cupom"),
    CHEAP("Até R$ 30")
}

private data class PromoBanner(
    val id: String,
    val tag: String = "Cupom",
    val title: String,
    val subtitle: String,
    val storeId: String,
    val imageUrl: String = ""
)

private data class DiscoverCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val stores: List<Store>,
    val backgroundColor: Long = 0xFFFFF4EC,
    val saved: Boolean = false
)

private data class DailyDeal(
    val id: String,
    val title: String,
    val description: String,
    val discountBadge: String,
    val endsAtMillis: Long,
    val tall: Boolean = false,
    val storeId: String? = null
)

private data class OrderAgainItem(
    val productId: String,
    val productName: String,
    val storeId: String,
    val storeName: String,
    val price: Double,
    val imageUrl: String,
    val product: Product
)

// ===== Categorias da busca (mesma lista fixa do StoreRepository do original) =====

private val discoverCategories = listOf(
    SearchCategory("lanches", "Lanches", listOf("lanchonete", "hamburgueria", "lanches", "lanche", "burger")),
    SearchCategory("pizzaria", "Pizzaria", listOf("pizzaria", "pizza")),
    SearchCategory("marmita", "Marmita", listOf("marmitaria", "restaurante", "comida caseira", "marmita")),
    SearchCategory("acai", "Açaí & Sobremesas", listOf("acai", "açaí", "sorveteria", "doceria", "confeitaria", "sobremesa")),
    SearchCategory("bebidas", "Bebidas", listOf("adega", "bebidas", "bebida", "cerveja")),
    SearchCategory("mercado", "Mercado", listOf("mercado", "supermercado", "hortifruti", "conveniencia")),
    SearchCategory("farmacias", "Farmácias", listOf("farmacias", "farmácia", "farmacia", "drogaria", "drogarias")),
    SearchCategory("pastel", "Pastel & Salgados", listOf("pastel", "pasteis", "salgados", "salgado")),
    SearchCategory("churrasco", "Churrasco", listOf("churrascaria", "carnes", "churrasco", "carne"))
)

// ===== Lógica da Descoberta (porte do SearchViewModel do app original) =====

/** Normalização de texto da busca: minúsculas, sem acentos, sem espaços nas pontas. */
private fun normalizeSearchText(value: String): String {
    var result = value.lowercase().trim()
    val replacements = mapOf(
        'á' to 'a', 'à' to 'a', 'ã' to 'a', 'â' to 'a', 'ä' to 'a',
        'é' to 'e', 'ê' to 'e', 'ë' to 'e',
        'í' to 'i', 'î' to 'i', 'ï' to 'i',
        'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
        'ú' to 'u', 'û' to 'u', 'ü' to 'u',
        'ç' to 'c', 'ñ' to 'n'
    )
    result = result.map { replacements[it] ?: it }.joinToString("")
    return result
}

/** Lojas de teste/QA nunca aparecem na Descobrir (ex.: "Playwright Audit Loja"). */
private fun isTestStore(store: Store): Boolean {
    val n = store.name.lowercase()
    return n.contains("playwright") || n.contains("audit") ||
        n.contains("teste") || n.contains("test")
}

/** Badge do cupom real, ex.: "-20%" ou "-R$ 10,50". */
private fun couponBadgeText(discountType: String, discountValue: Double): String =
    if (discountType.equals("percent", ignoreCase = true) || discountType.equals("percentage", ignoreCase = true))
        "-${discountValue.toInt()}%"
    else
        "-${com.example.shared.util.formatBRL(discountValue)}"

/** Descrição legível do cupom real em pt-BR. */
private fun couponDescriptionText(coupon: Coupon): String {
    val parts = mutableListOf<String>()
    val discount = if (coupon.discountType.equals("percent", ignoreCase = true) ||
        coupon.discountType.equals("percentage", ignoreCase = true)
    ) "${coupon.discountValue.toInt()}% de desconto"
    else "${dscMoney(coupon.discountValue)} de desconto"
    parts += discount
    if (coupon.minOrderValue > 0) parts += "em pedidos acima de ${dscMoney(coupon.minOrderValue)}"
    if (coupon.firstOrderOnly) parts += "só na 1ª compra"
    return parts.joinToString(" • ")
}

/** Converte ISO-8601 em millis. 0 = sem expiração/ilegível. */
private fun parseIsoToMillis(iso: String?): Long {
    if (iso.isNullOrBlank()) return 0L
    val clean = iso.trim()
    return try {
        kotlinx.datetime.Instant.parse(clean).toEpochMilliseconds()
    } catch (_: Throwable) {
        try {
            val noFraction = clean.substringBefore(".")
            val withZone = if (noFraction.endsWith("Z") || Regex("[+-]\\d{2}:?\\d{2}$").containsMatchIn(noFraction)) noFraction else "${noFraction}Z"
            kotlinx.datetime.Instant.parse(withZone).toEpochMilliseconds()
        } catch (_: Throwable) {
            0L
        }
    }
}

/** Política única dos chips da Descoberta (igual ao original). */
private fun matchesQuickFilter(
    store: Store,
    filter: DiscoverQuickFilter?,
    couponStoreIds: Set<String>,
    cheapStoreIds: Set<String>
): Boolean = when (filter) {
    null -> true
    DiscoverQuickFilter.FREE -> store.isFreeDelivery
    DiscoverQuickFilter.TOP -> store.rating >= 4.5
    DiscoverQuickFilter.FAST -> (maxDeliveryMinutes(store.deliveryTime) ?: Int.MAX_VALUE) <= 30
    DiscoverQuickFilter.COUPON -> store.id in couponStoreIds
    DiscoverQuickFilter.CHEAP -> store.id in cheapStoreIds
}

/** Aliases de categoria (mesma tabela do StoreRepository do original). */
private val SEARCH_CATEGORY_ALIASES: Map<String, List<String>> = mapOf(
    "lanches" to listOf("lanche", "lanches", "hamburguer", "hamburgueria", "burger", "sanduiche", "hot dog", "cachorro quente", "fast food", "batata"),
    "pizzaria" to listOf("pizza", "pizzas", "pizzaria"),
    "marmita" to listOf("marmita", "marmitas", "refeicao", "almoco", "comida caseira", "restaurante"),
    "pastel" to listOf("pastel", "pasteis", "pastelaria", "feira"),
    "acai" to listOf("acai", "sorvete", "sorveteria", "gelato", "sobremesa"),
    "bebidas" to listOf("bebida", "bebidas", "conveniencia", "distribuidora", "bar", "adega"),
    "mercado" to listOf("mercado", "mercados", "supermercado", "mercearia", "minimercado", "hortifruti", "padaria"),
    "farmacias" to listOf("farmacia", "farmacias", "drogaria", "perfumaria"),
    "churrasco" to listOf("churrasco", "espetinho", "espetos", "grill", "carnes", "churrascaria")
)

private fun normalizeCategoryKey(value: String): String =
    normalizeSearchText(value).replace(Regex("[^a-z0-9]+"), " ").trim().replace(Regex("\\s+"), " ")

private fun matchesSearchCategory(categoryId: String?, store: Store): Boolean {
    val selectedKey = normalizeCategoryKey(categoryId.orEmpty())
    if (selectedKey.isBlank()) return true
    val storeCategory = normalizeCategoryKey(store.category)
    if (storeCategory == selectedKey) return true
    val aliases = SEARCH_CATEGORY_ALIASES[selectedKey].orEmpty().map { normalizeCategoryKey(it) }
    return aliases.filter { it.isNotBlank() }.any { alias ->
        storeCategory == alias ||
            (storeCategory.isNotBlank() && (storeCategory.contains(alias) || alias.contains(storeCategory))) ||
            normalizeCategoryKey(store.name).let { name ->
                name.isNotBlank() && (name.contains(alias) || alias.contains(name))
            }
    }
}

/** "Ofertas do dia": cupons reais ativos (countdown só com expiração real). */
private fun buildDailyDeals(coupons: List<Coupon>): List<DailyDeal> {
    val now = Clock.System.now().toEpochMilliseconds()
    return coupons.mapNotNull { coupon ->
        if (coupon.code.isBlank()) return@mapNotNull null
        val endsAt = parseIsoToMillis(coupon.expiresAt)
        if (!coupon.expiresAt.isNullOrBlank() && endsAt in 1 until now) return@mapNotNull null
        DailyDeal(
            id = coupon.id.ifBlank { coupon.code },
            title = coupon.code.uppercase(),
            description = couponDescriptionText(coupon),
            discountBadge = couponBadgeText(coupon.discountType, coupon.discountValue),
            endsAtMillis = endsAt,
            storeId = coupon.storeId?.takeIf { it.isNotBlank() }
        )
    }.take(6).mapIndexed { index, deal -> deal.copy(tall = index % 2 == 0) }
}

/** Coleções dinâmicas de dados reais da cidade. Só aparece quem tem loja. */
private fun buildDiscoverCollections(
    stores: List<Store>,
    couponStoreIds: Set<String>,
    savedIds: Set<String>
): List<DiscoverCollection> {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val thirtyDaysAgoMs = nowMs - 30L * 24 * 3600 * 1000
    val thirtyDaysAgo = kotlinx.datetime.Instant.fromEpochMilliseconds(thirtyDaysAgoMs)
        .toString().substringBefore(".")
    return buildList {
        val onSale = stores.filter { it.id in couponStoreIds }
        if (onSale.isNotEmpty()) add(
            DiscoverCollection(
                id = "em_oferta",
                title = "Em oferta",
                subtitle = "${onSale.size} ${if (onSale.size == 1) "loja" else "lojas"} com cupom",
                stores = onSale.sortedByDescending { it.rating }.take(3),
                backgroundColor = 0xFFFFF4EC,
                saved = "em_oferta" in savedIds
            )
        )
        val topRated = stores.filter { it.rating >= 4.5 }
        if (topRated.isNotEmpty()) add(
            DiscoverCollection(
                id = "bem_avaliados",
                title = "Bem avaliados",
                subtitle = "${topRated.size} ${if (topRated.size == 1) "loja" else "lojas"} nota 4.5+",
                stores = topRated.sortedByDescending { it.rating }.take(3),
                backgroundColor = 0xFFE8F5E9,
                saved = "bem_avaliados" in savedIds
            )
        )
        val freeDelivery = stores.filter { it.isFreeDelivery }
        if (freeDelivery.isNotEmpty()) add(
            DiscoverCollection(
                id = "entrega_gratis",
                title = "Entrega grátis",
                subtitle = "${freeDelivery.size} ${if (freeDelivery.size == 1) "loja" else "lojas"} sem taxa",
                stores = freeDelivery.sortedByDescending { it.rating }.take(3),
                backgroundColor = 0xFFE3F2FD,
                saved = "entrega_gratis" in savedIds
            )
        )
        val newOnes = stores.filter { store ->
            store.createdAt.isNotBlank() && store.createdAt >= thirtyDaysAgo
        }
        if (newOnes.isNotEmpty()) add(
            DiscoverCollection(
                id = "novidades",
                title = "Novidades",
                subtitle = "${newOnes.size} ${if (newOnes.size == 1) "loja nova" else "lojas novas"}",
                stores = newOnes.sortedByDescending { it.createdAt }.take(3),
                backgroundColor = 0xFFF3E5F5,
                saved = "novidades" in savedIds
            )
        )
    }
}

// ===== Formatação (sem String.format / sem java.util.Locale) =====

private fun dscMoney(value: Double): String = com.example.shared.util.formatBRL(value)

private fun dscRating(rating: Double?): String {
    if (rating == null) return "—"
    val rounded = kotlin.math.round(rating * 10) / 10.0
    val whole = rounded.toLong()
    return if (rounded == whole.toDouble()) "$whole,0"
    else rounded.toString().replace('.', ',')
}

/** Formata millis restantes como HH:MM:SS ou MM:SS (sem java.time). */
private fun formatCountdown(remainingMillis: Long): String {
    val totalSec = (remainingMillis / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    fun two(n: Int) = n.toString().padStart(2, '0')
    return if (h > 0) "${two(h)}:${two(m)}:${two(s)}" else "${two(m)}:${two(s)}"
}

private fun formatDistanceKm(km: Double): String {
    val rounded = kotlin.math.round(km * 10) / 10.0
    val whole = rounded.toLong()
    val text = if (rounded == whole.toDouble()) "$whole,0"
    else rounded.toString().replace('.', ',')
    return "$text km"
}

/** Extrai o maior número de minutos de um texto tipo "20-30 min". Null se não der pra parsear. */
private fun maxDeliveryMinutes(deliveryTime: String): Int? {
    val numbers = Regex("\\d+").findAll(deliveryTime).map { it.value.toInt() }.toList()
    return numbers.maxOrNull()
}

@Composable
fun SearchScreen(
    onStoreClick: (String) -> Unit,
    onProductClick: (DiscoverProduct) -> Unit = { onStoreClick(it.storeId) },
    initialCategory: String? = null,
    cart: CartHolder? = null
) {
    val session by UserSessionRepository.session.collectAsState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var quickFilter by remember { mutableStateOf<DiscoverQuickFilter?>(null) }
    var savedCollectionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var recentTerms by remember { mutableStateOf(SearchHistory.load()) }
    var isLoadingDiscovery by remember { mutableStateOf(true) }
    var isPullRefreshingDiscover by remember { mutableStateOf(false) }
    var discoverReloadKey by remember { mutableStateOf(0) }
    // ITEM 6/6: diálogo contextual de permissão de localização (espelha o
    // SearchScreen do app Android).
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    // Troca de loja pendente ao pedir de novo um item de outra loja.
    var pendingReorder by remember { mutableStateOf<OrderAgainItem?>(null) }

    // Categoria inicial (vinda da rota/Home), aplicada uma única vez.
    // Id desconhecido é ignorado — a Busca abre completa (regra do original).
    var initialCategoryApplied by remember { mutableStateOf(false) }
    LaunchedEffect(initialCategory) {
        if (!initialCategoryApplied) {
            initialCategoryApplied = true
            val wanted = initialCategory?.trim().orEmpty()
            if (wanted.isNotBlank()) {
                selectedCategoryId = discoverCategories
                    .firstOrNull { it.id.equals(wanted, ignoreCase = true) }?.id
            }
        }
    }

    // Debounce de 250 ms da digitação (igual ao SearchViewModel do original).
    LaunchedEffect(query) {
        delay(250)
        debouncedQuery = query
    }

    // Catálogo público de lojas (mesma fonte da Home).
    var allStores by remember { mutableStateOf<List<Store>>(emptyList()) }
    LaunchedEffect(discoverReloadKey) {
        try {
            val result = SupabaseApi.fetchActiveStores()
            if (result.isSuccess) allStores = result.stores
        } catch (_: Throwable) {
            // Sem rede: região fica vazia (nada de dado mockado).
        }
        isPullRefreshingDiscover = false
    }

    // Região = cidade efetiva (GPS ativo, senão cadastro); sem cidade = vazio;
    // lojas de teste fora (só na Descobrir, como no original).
    val activeCity = session.activeLocationCity.ifBlank { session.addressCity }
    // Distância real de cada loja a partir da localização ativa da sessão:
    // haversine × 1,3 (fator urbano da referência web, igual ao app original).
    val allStoresWithDistance = remember(
        allStores, session.activeLocationLatitude, session.activeLocationLongitude
    ) {
        val userLat = session.activeLocationLatitude
        val userLon = session.activeLocationLongitude
        if (userLat == null || userLon == null) {
            allStores
        } else {
            allStores.map { store ->
                val sLat = store.latitude
                val sLon = store.longitude
                if (sLat == null || sLon == null) store
                else store.copy(distanceKm = haversineKm(userLat, userLon, sLat, sLon) * 1.3)
            }
        }
    }
    val regionalStores = remember(allStoresWithDistance, activeCity) {
        val cityKey = normalizeSearchText(activeCity)
        if (cityKey.isBlank()) emptyList()
        else allStoresWithDistance.filter {
            normalizeSearchText(it.addressCity) == cityKey && !isTestStore(it)
        }
    }

    // Produtos da descoberta + lojas com cupom da região.
    var searchableProducts by remember { mutableStateOf<List<DiscoverProduct>>(emptyList()) }
    var storesWithCoupons by remember { mutableStateOf<List<StoreWithCoupon>>(emptyList()) }
    val regionalKey = regionalStores.joinToString(",") { it.id }
    LaunchedEffect(regionalKey) {
        isLoadingDiscovery = true
        try {
            val openStores = regionalStores.filter { it.isOpen }
            val eligibleStores = openStores.ifEmpty { regionalStores }
            searchableProducts = SupabaseApi.fetchDiscoverProducts(eligibleStores, limit = 60)
        } catch (_: Throwable) {
            searchableProducts = emptyList()
        }
        try {
            val ids = regionalStores.map { it.id }.toSet()
            storesWithCoupons = SupabaseApi.fetchStoresWithCoupons(limit = 20)
                .filter { it.storeId in ids }
        } catch (_: Throwable) {
            storesWithCoupons = emptyList()
        }
        isLoadingDiscovery = false
    }

    // Ofertas do dia: cupons reais ativos.
    var dailyDeals by remember { mutableStateOf<List<DailyDeal>>(emptyList()) }
    LaunchedEffect(Unit) {
        dailyDeals = try {
            buildDailyDeals(SupabaseApi.fetchActiveCoupons(limit = 10))
        } catch (_: Throwable) {
            emptyList()
        }
    }

    // "Peça de novo": pratos distintos do histórico real de pedidos.
    var orderAgainItems by remember { mutableStateOf<List<OrderAgainItem>>(emptyList()) }
    LaunchedEffect(session.userId) {
        if (session.userId.isNotBlank() && session.accessToken.isNotBlank()) {
            try {
                val orders = SupabaseApi.fetchOrdersForClient(session.userId, session.accessToken)
                orderAgainItems = orders
                    .flatMap { order -> order.items.map { it to order } }
                    .distinctBy { (item, _) -> item.product.id }
                    .take(8)
                    .map { (item, order) ->
                        OrderAgainItem(
                            productId = item.product.id,
                            productName = item.product.name,
                            storeId = item.product.storeId,
                            storeName = order.storeName,
                            price = item.unitPrice,
                            imageUrl = item.product.imageUrl,
                            product = item.product
                        )
                    }
            } catch (_: Throwable) {
                // Histórico indisponível: a seção some (sem dados mockados).
            }
        }
    }

    val couponStoreIds = remember(storesWithCoupons) {
        storesWithCoupons.map { it.storeId }.toSet()
    }
    val cheapStoreIds = remember(searchableProducts) {
        searchableProducts.filter { it.price <= 30.0 }.map { it.storeId }.toSet()
    }
    val featuredProducts = remember(searchableProducts) { searchableProducts.take(6) }
    val trendingStores = remember(regionalStores) {
        regionalStores.filter { it.isOpen && (it.logoUrl.isNotBlank() || it.bannerUrl.isNotBlank()) }
            .sortedByDescending { it.rating }
            .take(8)
    }
    val recommendedStores = remember(regionalStores) {
        regionalStores.filter { it.isOpen }
            .sortedWith(compareByDescending<Store> { it.rating }.thenBy { it.distanceKm ?: Double.MAX_VALUE })
            .take(8)
    }
    val utilityStores = remember(regionalStores) {
        val utilityTerms = setOf("farmacia", "pet", "mercado", "supermercado", "conveniencia", "padaria")
        regionalStores.filter { store ->
            val cats = (listOf(store.category) + store.secondaryCategories).map { normalizeSearchText(it) }
            utilityTerms.any { term -> cats.any { it.contains(term) } }
        }.sortedWith(compareByDescending<Store> { it.isOpen }.thenByDescending { it.rating }).take(6)
    }
    val promoBanners = remember(storesWithCoupons) {
        storesWithCoupons.take(5).map { swc ->
            PromoBanner(
                id = "coupon_${swc.storeId}_${swc.couponCode}",
                title = couponBadgeText(swc.discountType, swc.discountValue),
                subtitle = "${swc.storeName} • Cupom ${swc.couponCode}",
                storeId = swc.storeId,
                imageUrl = swc.storeLogoUrl.orEmpty()
            )
        }
    }
    val collections = remember(regionalStores, couponStoreIds, savedCollectionIds) {
        buildDiscoverCollections(regionalStores, couponStoreIds, savedCollectionIds)
    }

    val selectedCategory = discoverCategories.firstOrNull { it.id == selectedCategoryId }
    val isSearchActive = normalizeSearchText(debouncedQuery).length >= 2 ||
        selectedCategory != null || quickFilter != null

    val filteredStores = remember(
        regionalStores, debouncedQuery, selectedCategoryId, quickFilter, couponStoreIds, cheapStoreIds
    ) {
        val normQuery = normalizeSearchText(debouncedQuery)
        regionalStores.filter { store ->
            val matchesCategory = selectedCategory == null || matchesSearchCategory(selectedCategoryId, store)
            val matchesQuery = normQuery.length < 2 ||
                normalizeSearchText(store.name).contains(normQuery) ||
                (listOf(store.category) + store.secondaryCategories).any { normalizeSearchText(it).contains(normQuery) } ||
                normalizeSearchText(store.addressNeighborhood).contains(normQuery)
            val matchesFilter = matchesQuickFilter(store, quickFilter, couponStoreIds, cheapStoreIds)
            matchesCategory && matchesQuery && matchesFilter
        }.sortedWith(compareByDescending<Store> { it.isOpen }.thenByDescending { it.rating })
    }
    val filteredProducts = remember(
        searchableProducts, debouncedQuery, selectedCategoryId, quickFilter,
        regionalStores, couponStoreIds, cheapStoreIds
    ) {
        val q = normalizeSearchText(debouncedQuery)
        val storesById = regionalStores.associateBy { it.id }
        searchableProducts.filter { product ->
            val store = storesById[product.storeId]
            val matchesQuery = q.length < 2 ||
                normalizeSearchText(product.name).contains(q) ||
                normalizeSearchText(product.storeName).contains(q) ||
                normalizeSearchText(product.storeCategory).contains(q)
            val matchesCategory = selectedCategory == null ||
                selectedCategory.matchingTerms.any { term ->
                    normalizeSearchText(product.storeCategory).contains(normalizeSearchText(term))
                } ||
                normalizeSearchText(product.storeCategory) == normalizeSearchText(selectedCategory.name)
            val matchesFilter = store != null &&
                matchesQuickFilter(store, quickFilter, couponStoreIds, cheapStoreIds)
            matchesQuery && matchesCategory && matchesFilter
        }
    }

    // GPS: grava o endereço REAL geocodificado na sessão (localização ativa);
    // sem cidade identificada, limpa a localização ativa — nunca inventado.
    fun fetchGpsLocation() {
        scope.launch {
            val current = try {
                LocationProvider.getCurrentAddress()
            } catch (_: Throwable) {
                null
            }
            if (current == null || current.city.isBlank()) {
                // Igual ao original: sem cidade identificada pelo GPS, limpa a
                // localização ativa e volta ao endereço cadastrado.
                UserSessionRepository.clearActiveLocation()
            } else {
                // Endereço REAL geocodificado da posição — nunca inventado.
                UserSessionRepository.updateActiveLocation(
                    street = current.street,
                    number = current.number,
                    neighborhood = current.neighborhood,
                    cep = current.cep,
                    city = current.city,
                    state = current.state,
                    latitude = current.latitude,
                    longitude = current.longitude
                )
            }
        }
    }

    // Aviso visível ao pedir de novo: antes a adição era silenciosa e parecia
    // que o botão não fazia nada (apontado pelo Renner em 2026-10-21).
    val reorderSnackbar = remember { SnackbarHostState() }
    var reorderSnackJob by remember { mutableStateOf<Job?>(null) }
    fun notifyReordered(item: OrderAgainItem) {
        reorderSnackJob?.cancel()
        reorderSnackJob = scope.launch {
            reorderSnackbar.showSnackbar("${item.product.name} adicionado à sacola")
        }
    }

    fun reorder(item: OrderAgainItem) {
        val activeCart = cart ?: return
        if (activeCart.isSwitchingStore(item.product)) {
            pendingReorder = item
        } else {
            activeCart.addProduct(item.product, quantity = 1, storeName = item.storeName)
            notifyReordered(item)
        }
    }

    val displayCity = when {
        activeCity.isBlank() -> ""
        else -> {
            val state = session.activeLocationState.ifBlank { session.addressState }
            if (state.isNotBlank()) "$activeCity, $state" else activeCity
        }
    }

    // A bottom bar é renderizada pelo App (BottomNavBar); aqui só o conteúdo.
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DscBg)
            .imePadding()
    ) {
        DiscoverAppBar(
            city = displayCity,
            query = query,
            onQueryChange = { query = it },
            onClearQuery = { query = ""; debouncedQuery = "" },
            onLocationClick = {
                // ITEM 6/6: espelha o SearchScreen do app Android — sem permissão,
                // mostra o diálogo contextual de localização.
                if (PlatformPermissions.hasLocationPermission()) {
                    fetchGpsLocation()
                } else {
                    showLocationPermissionDialog = true
                }
            },
            onSubmit = {
                recentTerms = SearchHistory.add(query, recentTerms)
            }
        )

        if (isSearchActive) {
            DiscoverResults(
                query = debouncedQuery,
                selectedCategory = selectedCategory,
                selectedQuickFilter = quickFilter,
                products = filteredProducts,
                stores = filteredStores,
                recentSearches = recentTerms,
                onClearAll = {
                    query = ""
                    debouncedQuery = ""
                    selectedCategoryId = null
                    quickFilter = null
                },
                onRecentSearch = { term ->
                    query = term
                    debouncedQuery = term
                    recentTerms = SearchHistory.add(term, recentTerms)
                },
                onNavigateToStore = onStoreClick,
                onProductClick = onProductClick
            )
        } else {
            DiscoverLanding(
                isLoadingDiscovery = isLoadingDiscovery,
                isRefreshing = isPullRefreshingDiscover,
                onRefresh = {
                    isPullRefreshingDiscover = true
                    discoverReloadKey++
                },
                quickFilter = quickFilter,
                categories = discoverCategories,
                selectedCategoryId = selectedCategoryId,
                featuredProducts = featuredProducts,
                trendingStores = trendingStores,
                promoBanners = promoBanners,
                collections = collections,
                orderAgainItems = orderAgainItems,
                dailyDeals = dailyDeals,
                utilityStores = utilityStores,
                recommendedStores = recommendedStores,
                couponStoreIds = couponStoreIds,
                onQuickFilter = { quickFilter = if (quickFilter == it) null else it },
                onCategory = { selectedCategoryId = if (selectedCategoryId == it) null else it },
                onToggleCollectionSaved = { id ->
                    savedCollectionIds = if (id in savedCollectionIds) savedCollectionIds - id
                    else savedCollectionIds + id
                },
                onReorder = { reorder(it) },
                onNavigateToStore = onStoreClick
            )
        }
    }

        SnackbarHost(
            hostState = reorderSnackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

    // Confirmação de troca de loja ao pedir de novo (estilo iFood, igual ao original).
    val reorderPending = pendingReorder
    if (reorderPending != null && cart != null) {
        StoreSwitchBottomSheet(
            currentStoreName = cart.storeName,
            onConfirm = {
                cart.clearCart()
                cart.addProduct(reorderPending.product, quantity = 1, storeName = reorderPending.storeName)
                pendingReorder = null
                notifyReordered(reorderPending)
            },
            onDismiss = { pendingReorder = null }
        )
    }

    // ITEM 6/6: diálogo contextual de permissão de localização (migração 1:1 do
    // original; no Android usa a ActivityResult API, no iOS é stub).
    LocationAndPermissionsDialog(
        showDialog = showLocationPermissionDialog,
        onDismiss = { showLocationPermissionDialog = false },
        onLocationPermissionResult = { granted ->
            if (granted) fetchGpsLocation()
        }
    )
}

/** Appbar do protótipo: "Entregar em" + endereço + barra de busca. */
@Composable
private fun DiscoverAppBar(
    city: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLocationClick: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DscBg)
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Text(
            text = "Entregar em",
            style = TextStyle(
                fontFamily = ManropeFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                color = DscInk2
            )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onLocationClick)
                .padding(vertical = 1.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = city.ifBlank { "Sua cidade" },
                style = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = DscInk
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DscChipBg)
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = DscInk2,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(9.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input"),
                textStyle = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontSize = 14.sp,
                    color = DscInk
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSubmit()
                    focusManager.clearFocus()
                }),
                cursorBrush = SolidColor(ItaSuperPrimary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar restaurantes e pratos",
                            style = TextStyle(
                                fontFamily = ManropeFontFamily(),
                                fontSize = 14.sp,
                                color = DscInk2
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            )
            if (query.isNotBlank()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Limpar busca",
                        tint = DscInk2,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/** Feed da Descoberta na ordem exata do protótipo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverLanding(
    isLoadingDiscovery: Boolean,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    quickFilter: DiscoverQuickFilter?,
    categories: List<SearchCategory>,
    selectedCategoryId: String?,
    featuredProducts: List<DiscoverProduct>,
    trendingStores: List<Store>,
    promoBanners: List<PromoBanner>,
    collections: List<DiscoverCollection>,
    orderAgainItems: List<OrderAgainItem>,
    dailyDeals: List<DailyDeal>,
    utilityStores: List<Store>,
    recommendedStores: List<Store>,
    couponStoreIds: Set<String>,
    onQuickFilter: (DiscoverQuickFilter) -> Unit,
    onCategory: (String) -> Unit,
    onToggleCollectionSaved: (String) -> Unit,
    onReorder: (OrderAgainItem) -> Unit,
    onNavigateToStore: (String) -> Unit
) {
    // "Perto de você": usa as lojas em alta; se vazio, cai para as recomendadas.
    val nearStores = trendingStores.ifEmpty { recommendedStores }
    val allStoresById = remember(trendingStores, recommendedStores, utilityStores) {
        (trendingStores + recommendedStores + utilityStores).associateBy { it.id }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = rememberPullToRefreshState(),
        modifier = Modifier.fillMaxSize()
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 14.dp)
    ) {
        // Filtros rápidos
        item {
            DiscoverFilterChips(
                selected = quickFilter,
                onClick = onQuickFilter
            )
        }
        // Categorias (grade 2 linhas)
        item {
            DiscoverCategoryGrid(
                categories = categories,
                selectedId = selectedCategoryId,
                onSelect = onCategory
            )
        }
        if (isLoadingDiscovery && featuredProducts.isEmpty() && nearStores.isEmpty()) {
            item { DiscoverShimmerLoading() }
        } else {
            // Carrossel de promos
            if (promoBanners.isNotEmpty()) {
                item {
                    PromoCarousel(
                        banners = promoBanners,
                        onBannerClick = { banner -> onNavigateToStore(banner.storeId) }
                    )
                }
            }
            // Peça de novo
            if (orderAgainItems.isNotEmpty()) {
                item { PSecHeader(title = "Peça de novo") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orderAgainItems, key = { it.productId }) { item ->
                            DishCard(item = item, onReorder = { onReorder(item) })
                        }
                    }
                }
            }
            // Perto de você (lista vertical)
            if (nearStores.isNotEmpty()) {
                item {
                    NearYouSection(
                        stores = nearStores,
                        couponStoreIds = couponStoreIds,
                        onNavigateToStore = onNavigateToStore
                    )
                }
            }
            // Coleções
            if (collections.isNotEmpty()) {
                item { PSecHeader(title = "Coleções") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(collections, key = { it.id }) { collection ->
                            ProtoCollectionCard(
                                collection = collection,
                                onToggleSave = { onToggleCollectionSaved(collection.id) },
                                onClick = {
                                    collection.stores.firstOrNull()?.let { store ->
                                        onNavigateToStore(store.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            // Ofertas do dia
            if (dailyDeals.isNotEmpty()) {
                item {
                    DealsSection(
                        deals = dailyDeals,
                        storesById = allStoresById,
                        onDealClick = { deal ->
                            deal.storeId?.let { storeId -> onNavigateToStore(storeId) }
                        }
                    )
                }
            }
            // Mercados e mais
            if (utilityStores.isNotEmpty()) {
                item {
                    UtilitySection(
                        stores = utilityStores,
                        onNavigateToStore = onNavigateToStore
                    )
                }
            }
        }
    }
}
}

/** Filtros rápidos do protótipo (.chips / .fchip). */
@Composable
private fun DiscoverFilterChips(
    selected: DiscoverQuickFilter?,
    onClick: (DiscoverQuickFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DiscoverQuickFilter.entries.toList()) { filter ->
            val isSelected = selected == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) DscInk else DscLine,
                        shape = RoundedCornerShape(50)
                    )
                    .background(if (isSelected) DscInk else Color.Transparent)
                    .clickable { onClick(filter) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                    text = filter.label,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isSelected) DscBg else DscInk
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/** Grade de categorias do protótipo: 2 linhas com rolagem horizontal (.cats). */

/** Mesmo mapeamento do app original (categoryDrawableId): id da categoria -> drawable. */
private fun categoryDrawableResource(categoryId: String): DrawableResource? = when (categoryId.lowercase()) {
    "pizzaria", "pizza" -> Res.drawable.ic_cat_pizzaria_3d
    "acai", "açaí" -> Res.drawable.ic_cat_acai_3d
    "marmita" -> Res.drawable.ic_cat_marmita_3d
    "bebidas", "bebida" -> Res.drawable.ic_cat_bebidas_3d
    "farmacia", "farmacias", "farmácia" -> Res.drawable.ic_cat_farmacia_3d
    "lanches", "lanche", "hamburguer", "hambúrguer", "burger" -> Res.drawable.ic_cat_lanches_3d
    "churrasco" -> Res.drawable.ic_cat_churrasco_3d
    "mercado", "mercados" -> Res.drawable.ic_cat_mercado_3d
    "pastel", "pasteis", "pastéis" -> Res.drawable.ic_cat_pastel_3d
    else -> null
}

@Composable
private fun DiscoverCategoryGrid(
    categories: List<SearchCategory>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    if (categories.isEmpty()) return
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = selectedId == category.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(62.dp)
                    .clickable { onSelect(category.id) }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            scaleX = if (isSelected) 1.05f else 1f
                            scaleY = if (isSelected) 1.05f else 1f
                        }
                        .clip(CircleShape)
                        .background(DscChipBg)
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = if (isSelected) ItaSuperPrimary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Igual ao app original: drawable da categoria quando há
                    // mapeamento; inicial como fallback.
                    val categoryIcon = categoryDrawableResource(category.id)
                    if (categoryIcon != null) {
                        Image(
                            painter = painterResource(categoryIcon),
                            contentDescription = category.name,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = category.name.take(1).uppercase(),
                            style = TextStyle(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.name,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (isSelected) ItaSuperPrimary else DscInk
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

/** Cabeçalho de seção do protótipo (.psec-head). */
@Composable
private fun PSecHeader(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontFamily = ManropeFontFamily(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.5.sp,
                color = DscInk
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        } else if (action != null) {
            Text(
                text = action,
                modifier = Modifier.clickable(
                    enabled = onActionClick != null,
                    onClick = { onActionClick?.invoke() }
                ),
                style = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ItaSuperPrimary
                )
            )
        }
    }
}

/** Carrossel de promos do protótipo (.promo): tag + título + subtítulo, dots abaixo. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PromoCarousel(
    banners: List<PromoBanner>,
    onBannerClick: (PromoBanner) -> Unit
) {
    if (banners.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { banners.size })
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000)
            val next = (pagerState.currentPage + 1) % banners.size
            try {
                pagerState.animateScrollToPage(next)
            } catch (_: Exception) {
                break
            }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp),
            pageSpacing = 10.dp
        ) { page ->
            val banner = banners[page]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFF26522), Color(0xFFFFB37A))
                        )
                    )
                    .clickable { onBannerClick(banner) }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp, end = 96.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = banner.tag.uppercase(),
                            style = TextStyle(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.08.em,
                                color = Color.White
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = banner.title,
                        style = TextStyle(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.Black,
                            fontSize = 26.sp,
                            lineHeight = 28.sp,
                            color = Color.White
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = banner.subtitle,
                        style = TextStyle(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.92f)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (banner.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = banner.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(banners.size) { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(
                            width = if (selected) 20.dp else 7.dp,
                            height = 7.dp
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (selected) ItaSuperPrimary else DscLine)
                )
            }
        }
    }
}

/** Card "Peça de novo" do protótipo (.dish-card). */
@Composable
private fun DishCard(item: OrderAgainItem, onReorder: () -> Unit) {
    Column(modifier = Modifier.width(148.dp)) {
        Box(
            modifier = Modifier
                .size(width = 148.dp, height = 104.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DscChipBg),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.productName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // KMP: sem drawable ic_ita_bag — placeholder com emoji no mesmo box.
                Text(
                    text = "\uD83C\uDF54",
                    style = TextStyle(fontSize = 32.sp, color = DscInk2.copy(alpha = 0.45f))
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = item.productName,
            style = TextStyle(
                fontFamily = ManropeFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                lineHeight = 17.sp,
                color = DscInk
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(34.dp)
        )
        Text(
            text = item.storeName,
            style = TextStyle(
                fontFamily = ManropeFontFamily(),
                fontSize = 12.sp,
                color = DscInk2
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start)
        ) {
            Text(
                text = dscMoney(item.price),
                style = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = DscInk
                ),
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ItaSuperPrimary)
                    .clickable(onClick = onReorder)
                    .padding(horizontal = 9.dp, vertical = 6.dp)
                    .testTag("reorder_${item.productId}")
            ) {
                Text(
                    text = "Pedir de novo",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = Color.White
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/** "Perto de você" do protótipo: lista vertical (.rest-list) com "Ver tudo". */
@Composable
private fun NearYouSection(
    stores: List<Store>,
    couponStoreIds: Set<String>,
    onNavigateToStore: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) stores else stores.take(3)

    Column(modifier = Modifier.fillMaxWidth()) {
        PSecHeader(
            title = "Perto de você",
            action = if (stores.size > 3) "Ver tudo" else null,
            onActionClick = if (stores.size > 3) {
                { expanded = !expanded }
            } else null
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            visible.forEach { store ->
                RestCard(
                    store = store,
                    hasCoupon = store.id in couponStoreIds,
                    onClick = { onNavigateToStore(store.id) }
                )
            }
        }
    }
}

/** Card de loja compacto estilo iFood: logo pequeno + nome + meta. Sem banner gigante. */
@Composable
private fun RestCard(
    store: Store,
    hasCoupon: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("discover_rest_${store.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo pequeno (56dp). Nunca estica logo como banner.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DscChipBg),
                contentAlignment = Alignment.Center
            ) {
                if (store.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = store.logoUrl,
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = store.name.take(1).uppercase(),
                        style = TextStyle(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = ItaSuperPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = store.name,
                        style = TextStyle(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (store.isOpen) DscInk else DscInk2
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (hasCoupon && store.isOpen) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ItaSuperPrimary)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Cupom",
                                style = TextStyle(
                                    fontFamily = ManropeFontFamily(),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.5.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append("\u2605 ${dscRating(store.rating)}")
                        if (!store.isOpen) {
                            append(" \u00b7 Fechado")
                        } else {
                            if (store.deliveryTime.isNotBlank()) append(" \u00b7 ${store.deliveryTime}")
                            if (store.isFreeDelivery) append(" \u00b7 Entrega gr\u00e1tis")
                            else if (store.deliveryFee.isNotBlank()) append(" \u00b7 Entrega ${store.deliveryFee}")
                        }
                    },
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontSize = 12.sp,
                        color = DscInk2
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DscInk2.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(1.dp)
                .background(DscLine)
        )
    }
}

/** Card de coleção compacto: fundo sólido + inicial, sem foto esticada. */
@Composable
private fun ProtoCollectionCard(
    collection: DiscoverCollection,
    onToggleSave: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(148.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(collection.backgroundColor))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("discover_collection_${collection.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = collection.stores.firstOrNull()?.name?.take(1)
                    ?.uppercase() ?: "\u2605",
                style = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = ItaSuperPrimary
                )
            )
        }
        Text(
            text = collection.title,
            style = TextStyle(
                fontFamily = ManropeFontFamily(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                color = DscInk
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = collection.subtitle,
            style = TextStyle(
                fontFamily = ManropeFontFamily(),
                fontSize = 11.sp,
                color = DscInk2
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (collection.saved) ItaSuperPrimary
                    else Color.White.copy(alpha = 0.9f)
                )
                .clickable(onClick = onToggleSave)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (collection.saved) "Salva" else "Salvar",
                style = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (collection.saved) Color.White else DscInk
                )
            )
        }
    }
}

/** "Ofertas do dia" do protótipo: título + countdown + grade 2 colunas. */
@Composable
private fun DealsSection(
    deals: List<DailyDeal>,
    storesById: Map<String, Store>,
    onDealClick: (DailyDeal) -> Unit
) {
    val soonestExpiry = remember(deals) {
        deals.mapNotNull { it.endsAtMillis.takeIf { millis -> millis > 0 } }.minOrNull()
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        PSecHeader(
            title = "Ofertas do dia",
            trailing = if (soonestExpiry != null) {
                { ProtoCountdownPill(endsAtMillis = soonestExpiry) }
            } else null
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            deals.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { deal ->
                        Box(modifier = Modifier.weight(1f)) {
                            val store = deal.storeId?.let { storesById[it] }
                            val imageUrl = store?.bannerUrl.ifNullOrBlank { store?.logoUrl.orEmpty() }
                            ProtoDealCard(
                                deal = deal,
                                imageUrl = imageUrl,
                                storeName = store?.name.orEmpty(),
                                onClick = { onDealClick(deal) }
                            )
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun String?.ifNullOrBlank(default: () -> String): String =
    if (this.isNullOrBlank()) default() else this

/** Card de oferta do protótipo (.deal). */
@Composable
private fun ProtoDealCard(
    deal: DailyDeal,
    imageUrl: String,
    storeName: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, DscChipBg, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("daily_deal_${deal.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (deal.tall) 138.dp else 92.dp)
                .background(DscLine),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = deal.discountBadge,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = ItaSuperPrimary
                    )
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
        ) {
            Text(
                text = deal.discountBadge,
                style = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = ItaSuperPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = deal.title,
                style = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    color = DscInk
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val sub = storeName.ifBlank { deal.description }.ifBlank { null }
            if (sub != null) {
                Text(
                    text = sub,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontSize = 11.5.sp,
                        color = DscInk2
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Picote do cupom
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                val dashWidth = 8.dp.toPx()
                val gapWidth = 6.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = DscChipBg,
                        start = Offset(x, size.height / 2),
                        end = Offset(x + dashWidth, size.height / 2),
                        strokeWidth = 2.dp.toPx()
                    )
                    x += dashWidth + gapWidth
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ItaSuperPrimary.copy(alpha = 0.1f))
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (storeName.isNotBlank()) "Pedir agora" else "Ver lojas",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ItaSuperPrimary
                    )
                )
            }
        }
    }
}

/** Contagem regressiva do protótipo (.countdown): pílula escura "Termina em HH:MM:SS". */
@Composable
private fun ProtoCountdownPill(endsAtMillis: Long) {
    var remaining by remember(endsAtMillis) { mutableStateOf(endsAtMillis - Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(endsAtMillis) {
        while (remaining > 0) {
            delay(1000)
            remaining = endsAtMillis - Clock.System.now().toEpochMilliseconds()
        }
    }
    if (remaining <= 0) return
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(DscInk)
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "Termina em ${formatCountdown(remaining)}",
            style = TextStyle(
                fontFamily = ManropeFontFamily(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.5.sp,
                color = DscBg
            ),
            maxLines = 1
        )
    }
}

/** "Mercados e mais" do protótipo: sub-chips + linhas compactas (.store-row). */
@Composable
private fun UtilitySection(
    stores: List<Store>,
    onNavigateToStore: (String) -> Unit
) {
    val subCats = listOf("Mercado", "Farmácia", "Pet", "Bebidas")
    var selected by remember { mutableStateOf(subCats.first()) }
    val filtered = remember(stores, selected) {
        val norm = selected.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ã", "a").replace("õ", "o")
        val matched = stores.filter { store ->
            val cats = (listOf(store.category) + store.secondaryCategories)
                .joinToString(" ").lowercase()
            cats.contains(norm) || (norm == "bebidas" && cats.contains("bebida"))
        }
        matched.ifEmpty { stores }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PSecHeader(title = "Mercados e mais")
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subCats) { cat ->
                val isSelected = selected == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) DscInk else DscLine,
                            shape = RoundedCornerShape(50)
                        )
                        .background(if (isSelected) DscInk else Color.Transparent)
                        .clickable { selected = cat }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = cat,
                        style = TextStyle(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isSelected) DscBg else DscInk
                        )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            filtered.forEach { store ->
                ProtoUtilityStoreRow(
                    store = store,
                    onClick = { onNavigateToStore(store.id) }
                )
            }
        }
    }
}

/** Linha compacta de loja do protótipo (.store-row). */
@Composable
private fun ProtoUtilityStoreRow(store: Store, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("utility_store_${store.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val logoUrl = store.logoUrl.ifBlank { store.bannerUrl }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(DscChipBg),
                contentAlignment = Alignment.Center
            ) {
                if (logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = store.name.take(1).uppercase(),
                        style = TextStyle(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DscGreen)
                            .wrapContentSize(Alignment.Center)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = store.name,
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = DscInk
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append("★ ${dscRating(store.rating)}")
                        if (store.deliveryTime.isNotBlank()) append(" · ${store.deliveryTime}")
                    },
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontSize = 12.sp,
                        color = DscInk2
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            store.distanceKm?.let { km ->
                Text(
                    text = formatDistanceKm(km),
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DscInk2
                    )
                )
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(1.dp)
                .background(DscLine)
        )
    }
}

/** Shimmer animado reutilizável. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "discover_shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(DscSkel, DscSkel.copy(alpha = 0.55f), DscSkel),
        start = Offset(offset * 400f - 200f, 0f),
        end = Offset(offset * 400f + 200f, 0f)
    )
    this.background(brush)
}

/** Skeleton do protótipo (.skel-wrap): círculos, banner, linhas e fotos no formato do conteúdo. */
@Composable
private fun DiscoverShimmerLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .shimmer()
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .shimmer()
        )
        // Linhas de loja (logo + textos), iguais ao RestCard compacto
        repeat(3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .shimmer()
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmer()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .shimmer()
                    )
                }
            }
        }
    }
}

/** Tela de resultados da busca, nas cores do protótipo. */
@Composable
private fun DiscoverResults(
    query: String,
    selectedCategory: SearchCategory?,
    selectedQuickFilter: DiscoverQuickFilter?,
    products: List<DiscoverProduct>,
    stores: List<Store>,
    recentSearches: List<String>,
    onClearAll: () -> Unit,
    onRecentSearch: (String) -> Unit,
    onNavigateToStore: (String) -> Unit,
    onProductClick: (DiscoverProduct) -> Unit
) {
    if (query.length < 2 && selectedCategory == null && selectedQuickFilter == null) {
        // Busca vazia: mostra buscas recentes
        if (recentSearches.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Busque por restaurantes e pratos",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontSize = 14.sp,
                        color = DscInk2
                    )
                )
            }
            return
        }
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            PSecHeader(title = "Buscas recentes")
            recentSearches.take(6).forEach { term ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRecentSearch(term) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = DscInk2,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = term,
                        style = TextStyle(
                            fontFamily = ManropeFontFamily(),
                            fontSize = 14.sp,
                            color = DscInk
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        return
    }

    if (products.isEmpty() && stores.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // KMP: sem drawable ic_empty_filter_3d — placeholder de lupa.
                Text(
                    text = "\uD83D\uDD0D",
                    style = TextStyle(fontSize = 96.sp, color = DscInk2.copy(alpha = 0.35f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nenhuma loja bate com esse filtro. Toca de novo no filtro pra limpar.",
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontSize = 13.5.sp,
                        color = DscInk2
                    )
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "Voltar para Descobrir",
                    modifier = Modifier
                        .clickable(onClick = onClearAll)
                        .testTag("clear_discovery_button"),
                    style = TextStyle(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ItaSuperPrimary
                    )
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "Voltar para Descobrir",
                modifier = Modifier
                    .clickable(onClick = onClearAll)
                    .testTag("clear_discovery_button")
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                style = TextStyle(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ItaSuperPrimary
                )
            )
        }
        if (stores.isNotEmpty()) {
            item {
                PSecHeader(title = "Lojas")
            }
            items(stores, key = { it.id }) { store ->
                RestCard(
                    store = store,
                    hasCoupon = false,
                    onClick = { onNavigateToStore(store.id) }
                )
            }
        }
        if (products.isNotEmpty()) {
            item {
                PSecHeader(title = "Pratos")
            }
            items(products, key = { it.id }) { product ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductClick(product) }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DscChipBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (product.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = TextStyle(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = DscInk
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = product.storeName,
                            style = TextStyle(
                                fontFamily = ManropeFontFamily(),
                                fontSize = 13.sp,
                                color = DscInk2
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = dscMoney(product.price),
                            style = TextStyle(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(1.dp)
                        .background(DscLine)
                )
            }
        }
    }
}

