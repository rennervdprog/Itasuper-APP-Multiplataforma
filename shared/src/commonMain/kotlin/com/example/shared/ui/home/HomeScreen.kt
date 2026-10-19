package com.example.shared.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.shared.generated.resources.Res
import com.example.shared.generated.resources.ic_cat_acai_3d
import com.example.shared.generated.resources.ic_cat_bebidas_3d
import com.example.shared.generated.resources.ic_cat_churrasco_3d
import com.example.shared.generated.resources.ic_cat_farmacia_3d
import com.example.shared.generated.resources.ic_cat_lanches_3d
import com.example.shared.generated.resources.ic_cat_marmita_3d
import com.example.shared.generated.resources.ic_cat_mercado_3d
import com.example.shared.generated.resources.ic_cat_pastel_3d
import com.example.shared.generated.resources.ic_cat_pizzaria_3d
import com.example.shared.generated.resources.ic_cat_todos_3d
import com.example.shared.generated.resources.ic_home_placeholder_smile
import com.example.shared.generated.resources.ic_home_sort
import com.example.shared.generated.resources.ic_ita_bag
import com.example.shared.generated.resources.ic_ita_chevron_right
import com.example.shared.generated.resources.ic_ita_store
import com.example.shared.generated.resources.ic_lucide_bell
import com.example.shared.generated.resources.ic_lucide_chevron_down
import com.example.shared.generated.resources.ic_lucide_search
import com.example.shared.generated.resources.ic_lucide_shopping_bag
import com.example.shared.model.Banner
import com.example.shared.model.CategoryItem
import com.example.shared.model.DiscoverProduct
import com.example.shared.model.Order
import com.example.shared.model.Store
import com.example.shared.model.StoreWithCoupon
import com.example.shared.network.SupabaseApi
import com.example.shared.platform.LocationProvider
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperHighlightText
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSecondary
import com.example.shared.theme.ItaSuperSuccess
import com.example.shared.theme.ItaSuperTextPrimary
import com.example.shared.theme.ItaSuperTextSecondary
import com.example.shared.theme.ItaSuperWarning
import com.example.shared.theme.ManropeFontFamily
import com.example.shared.theme.SoraFontFamily
import com.example.shared.ui.mock.mockUserSession
import com.example.shared.ui.permissions.LocationAndPermissionsDialog
import com.example.shared.ui.permissions.PlatformPermissions
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.round

/**
 * HomeScreen — reescrita fiel ao app Android original
 * (`Itasuper-APP-NATIVO/.../ui/home/HomeScreen.kt`).
 *
 * Estrutura, textos, ordem, espaçamentos (dp), tamanhos de fonte (sp) e cores
 * reproduzidos literalmente do original. Adaptações KMP documentadas:
 *
 * - Sem ViewModel: estado local (`remember`) + mocks
 *   (`com.example.shared.ui.mock`) com fallback `SupabaseApi.fetchActiveStores()`.
 * - Recursos visuais via composeResources (`Res.drawable.*`, `Res.font.*`):
 *   drawables do app original (ic_ita_*, ic_lucide_*, ic_preview_*, marca)
 *   usados com `painterResource`, iguais aos do Android.
 * - `String.format` não existe no commonMain: [formatPriceBR] / [formatKmBR].
 * - `java.util.Calendar` não existe no commonMain: relógio via kotlinx-datetime
 *   (America/Sao_Paulo), mesma regra de faixa de horário do original.
 * - A busca é "toque para abrir" ([onSearchClick], como a fase 3a) em vez do
 *   campo editável inline do original — visual idêntico.
 * - Scaffold/bottom bar/snackbar/pull-to-refresh pertencem ao App.kt no KMP;
 *   aqui vai só o conteúdo rolável (fundo 0xFFFAFAFA, como o Scaffold original).
 * - Diálogos de permissões dependem de APIs de plataforma e ficam fora desta
 *   tela. Diálogos de endereço (ITEM 3/6) e o SupportBottomSheet (ITEM 4/6)
 *   foram migrados e estão conectados aqui.
 * - Seções definidas mas não chamadas no original (banners compactos, bento
 *   hero/destaques, descubra, chips de categoria, último pedido, ticket de
 *   cupom) foram portadas por fidelidade, mas seguem sem chamada — como no
 *   original.
 */

/** Limite de lojas exibidas na lista antes do botão "Ver todas" (original: HomeViewModel). */
private const val HOME_STORE_LIST_LIMIT = 12
private const val HOME_STORE_LIST_OVERFLOW_THRESHOLD = 15

/** Ordenação da lista de lojas (original: enum HomeStoreSort no HomeViewModel). */
private enum class HomeStoreSort(val label: String) {
    RELEVANCE("Mais relevantes"),
    DISTANCE("Mais perto"),
    DELIVERY_FEE("Menor taxa"),
    NAME("Nome da loja")
}

/** Faixa de horário do dia (original: DaypartRule.kt). */
private data class HomeDaypart(
    val id: String,
    val titulo: String,
    val subtitulo: String,
    val categorias: List<String>
) {
    val categoriaPrimaria: String get() = categorias.first()
}

private val DAYPART_MADRUGADA = HomeDaypart(
    id = "madrugada", titulo = "Aberto de madrugada",
    subtitulo = "Quem ainda está atendendo agora",
    categorias = listOf("lanches", "bebidas", "farmacias")
)
private val DAYPART_MANHA = HomeDaypart(
    id = "manha", titulo = "Bom dia",
    subtitulo = "Para resolver o começo do dia",
    categorias = listOf("mercado", "pastel", "farmacias")
)
private val DAYPART_ALMOCO = HomeDaypart(
    id = "almoco", titulo = "Hora do almoço",
    subtitulo = "Pratos prontos perto de você",
    categorias = listOf("marmita", "churrasco", "pastel")
)
private val DAYPART_TARDE = HomeDaypart(
    id = "tarde", titulo = "Lanche da tarde",
    subtitulo = "Um docinho ou um salgado",
    categorias = listOf("acai", "pastel", "lanches")
)
private val DAYPART_NOITE = HomeDaypart(
    id = "noite", titulo = "Hora do jantar",
    subtitulo = "O que pedir hoje à noite",
    categorias = listOf("pizzaria", "lanches", "bebidas")
)

/** Mesma regra do original: minuto do dia (America/Sao_Paulo) -> faixa. */
private fun currentHomeDaypart(): HomeDaypart {
    val now = Clock.System.now().toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
    val minuteOfDay = now.hour * 60 + now.minute
    return when {
        minuteOfDay < 6 * 60 -> DAYPART_MADRUGADA
        minuteOfDay < 10 * 60 + 30 -> DAYPART_MANHA
        minuteOfDay < 15 * 60 -> DAYPART_ALMOCO
        minuteOfDay < 18 * 60 -> DAYPART_TARDE
        else -> DAYPART_NOITE
    }
}

/** "R$ 12,90" — substitui String.format("R$ %.2f", price).replace('.', ','). */
private fun formatPriceBR(price: Double): String {
    val cents = round(price * 100).toLong()
    return "R$ ${cents / 100}," + (cents % 100).toString().padStart(2, '0')
}

/** "1,5 km" — substitui String.format(Locale("pt","BR"), "%.1f km", d). */
private fun formatKmBR(distance: Double): String {
    val rounded = round(distance * 10) / 10.0
    val intPart = rounded.toInt()
    val text = if (rounded == intPart.toDouble()) "$intPart,0"
    else rounded.toString().replace('.', ',')
    return "$text km"
}

private fun String.normalizedAccents(): String = lowercase()
    .replace('á', 'a').replace('à', 'a').replace('ã', 'a').replace('â', 'a')
    .replace('é', 'e').replace('ê', 'e').replace('í', 'i')
    .replace('ó', 'o').replace('ô', 'o').replace('õ', 'o')
    .replace('ú', 'u').replace('ü', 'u').replace('ç', 'c')

private fun deliveryFeeSortValue(store: Store): Double {
    val fee = store.deliveryFee.trim()
    if (fee.isBlank()) return Double.MAX_VALUE
    if (fee.equals("Grátis", ignoreCase = true)) return 0.0
    return fee.replace(Regex("[^0-9,]"), "").replace(',', '.')
        .toDoubleOrNull() ?: Double.MAX_VALUE
}

/** Filtros + ordenação da lista (no original vivem no HomeViewModel). */
private fun applyHomeStoreFilters(
    stores: List<Store>,
    selectedCategory: String,
    freeFee: Boolean,
    directDelivery: Boolean,
    sort: HomeStoreSort
): List<Store> {
    var result = stores
    if (!selectedCategory.equals("todas", ignoreCase = true)) {
        val wanted = selectedCategory.normalizedAccents()
        result = result.filter { it.category.normalizedAccents().startsWith(wanted) }
    }
    if (freeFee) result = result.filter { it.isFreeDelivery }
    if (directDelivery) result = result.filter { it.deliveryMode.equals("own", ignoreCase = true) }
    return when (sort) {
        HomeStoreSort.RELEVANCE -> result
        HomeStoreSort.DISTANCE -> result.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
        HomeStoreSort.DELIVERY_FEE -> result.sortedBy { deliveryFeeSortValue(it) }
        HomeStoreSort.NAME -> result.sortedBy { it.name.normalizedAccents() }
    }
}

private fun mockHomeCategories(): List<CategoryItem> = listOf(
    CategoryItem(id = "lanches", name = "Lanches", iconName = "fastfood"),
    CategoryItem(id = "pizza", name = "Pizza", iconName = "local_pizza"),
    CategoryItem(id = "marmita", name = "Marmita", iconName = "restaurant"),
    CategoryItem(id = "pastel", name = "Pastel", iconName = "bakery_dining"),
    CategoryItem(id = "acai", name = "Açaí", iconName = "icecream"),
    CategoryItem(id = "bebidas", name = "Bebidas", iconName = "local_bar"),
    CategoryItem(id = "mercado", name = "Mercado", iconName = "shopping_cart"),
    CategoryItem(id = "farmacia", name = "Farmácia", iconName = "pharmacy"),
    CategoryItem(id = "churrasco", name = "Churrasco", iconName = "churrasco")
)

private fun mockHomeBanners(): List<Banner> = listOf(
    Banner(
        id = "b1",
        title = "50% OFF na primeira compra",
        imageUrl = "",
        description = "Válido para novos clientes"
    ),
    Banner(id = "b2", title = "Frete grátis acima de R$ 35", imageUrl = "")
)

private fun mockStoresWithCoupons(): List<StoreWithCoupon> = listOf(
    StoreWithCoupon(
        storeId = "s2", storeName = "Doceria da Cleide", storeLogoUrl = null,
        couponCode = "DOCE10", discountType = "value",
        discountValue = 10.0, couponDescription = null
    ),
    StoreWithCoupon(
        storeId = "s1", storeName = "Pastelão Carioca", storeLogoUrl = null,
        couponCode = "PASTEL15", discountType = "percent",
        discountValue = 15.0, couponDescription = null
    )
)

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onStoreClick: (String) -> Unit = {},
    onCartClick: () -> Unit = {},
    cartItemCount: Int = 0
) {
    // Lojas reais do Supabase. Sem fallback para mocks (regra: nada de dado fake).
    // Erro de rede -> tela de erro com "Tentar novamente" (igual ao app original).
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }
    var isLoadingStores by remember { mutableStateOf(true) }
    var storesErrorMessage by remember { mutableStateOf<String?>(null) }
    var storesReloadKey by remember { mutableStateOf(0) }
    LaunchedEffect(storesReloadKey) {
        isLoadingStores = true
        storesErrorMessage = null
        try {
            val result = SupabaseApi.fetchActiveStores()
            if (result.isSuccess) {
                stores = result.stores
            } else {
                storesErrorMessage = "Não foi possível carregar as lojas. Verifique sua conexão."
            }
        } catch (_: Throwable) {
            storesErrorMessage = "Não foi possível carregar as lojas. Verifique sua conexão."
        }
        isLoadingStores = false
    }

    // Estado que no original vive no HomeViewModel.
    var selectedCategory by remember { mutableStateOf("todas") }
    var isFreeFeeActive by remember { mutableStateOf(false) }
    var isDirectDeliveryActive by remember { mutableStateOf(false) }
    var storeSort by remember { mutableStateOf(HomeStoreSort.RELEVANCE) }

    // ITEM 3/6: estado dos diálogos de endereço/localização (no original vive
    // no HomeViewModel; fluxos reais de GPS/CEP/rede virão na fase-rede).
    val addressScope = rememberCoroutineScope()
    var showAddressChoiceDialog by remember { mutableStateOf(false) }
    var showAddressForm by remember { mutableStateOf(false) }
    var addressFormError by remember { mutableStateOf<String?>(null) }
    var isRefreshingLocation by remember { mutableStateOf(false) }
    // ITEM 6/6: diálogo contextual de permissão de localização (espelha o
    // HomeScreen do app Android: mostra o diálogo quando ainda não há permissão).
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var isLookingUpCep by remember { mutableStateOf(false) }
    var isSavingAddress by remember { mutableStateOf(false) }

    // ITEM 4/6: estado do SupportBottomSheet (no original vive no HomeViewModel;
    // no app original o gatilho openSupportSheet não tinha chamador na UI —
    // no KMP o sheet é aberto pelo menu Perfil).
    var showSupportSheet by remember { mutableStateOf(false) }

    // Banner promocional dispensável (padrão iFood): visível enquanto há cupons e não foi fechado.
    var promoBannerDismissed by remember { mutableStateOf(false) }

    val session = remember { mockUserSession() }
    var addressDraft by remember {
        mutableStateOf(
            AddressDraft(
                cep = session.addressCep,
                street = session.addressStreet,
                number = session.addressNumber,
                neighborhood = session.addressNeighborhood,
                city = session.addressCity,
                state = session.addressState
            )
        )
    }
    // Endereço "atual" vindo do GPS (mock até a fase-rede ligar a
    // geocodificação reversa de verdade).
    var currentStreet by remember { mutableStateOf(session.activeLocationStreet) }
    var currentNumber by remember { mutableStateOf(session.activeLocationNumber) }
    var currentNeighborhood by remember { mutableStateOf(session.activeLocationNeighborhood) }
    var currentCity by remember { mutableStateOf(session.activeLocationCity) }

    // ITEM 6/6: leitura do GPS (extraída para ser chamada tanto do
    // onAllowLocation — quando a permissão já existe — quanto do resultado do
    // LocationAndPermissionsDialog; espelha viewModel.fetchGpsLocation do original).
    fun fetchGpsLocation() {
        isRefreshingLocation = true
        addressScope.launch {
            val coords = LocationProvider.getCurrentLocation()
            if (coords != null) {
                // TODO(fase-rede): geocodificação reversa real da coordenada
                // (rua/número/bairro/cidade de verdade).
                currentStreet = "Av. Getúlio Vargas"
                currentNumber = "500"
                currentNeighborhood = "Centro"
                currentCity = "Araruama"
            }
            isRefreshingLocation = false
        }
    }

    val activeCity = session.addressCity.ifBlank { "Araruama" }
    val categories = remember { mockHomeCategories() } // filtros estáticos da UI (não são dados)
    // Seções promocionais com dados mock removidas (regra: nada de dado fake).
    // banners, cupons, ofertas e "peça de novo" só voltam com dados reais.
    val daypart = remember { currentHomeDaypart() }
    val spotlightStores = remember(stores) { stores.filter { it.isOpen }.take(4) }
    val recentStores = remember(stores) { stores.take(2) }
    val storesWithCoupons = remember { emptyList<StoreWithCoupon>() }
    val affordableProducts = remember { emptyList<DiscoverProduct>() }
    val repeatProducts = remember { emptyList<DiscoverProduct>() }

    val visibleStores = remember(stores, selectedCategory, isFreeFeeActive, isDirectDeliveryActive, storeSort) {
        applyHomeStoreFilters(stores, selectedCategory, isFreeFeeActive, isDirectDeliveryActive, storeSort)
    }
    val hasActiveFilters = selectedCategory != "todas" || isFreeFeeActive || isDirectDeliveryActive

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeaderSection(
            userName = session.name,
            streetName = session.addressStreet,
            streetNumber = session.addressNumber,
            cartItemCount = cartItemCount,
            onCouponClick = { /* TODO: tela de cupons */ },
            onCartClick = onCartClick,
            onAddressClick = { showAddressChoiceDialog = true }
        )

        // Banner promocional dispensável (só aparece quando há cupons).
        if (storesWithCoupons.isNotEmpty() && !promoBannerDismissed) {
            Spacer(modifier = Modifier.height(8.dp))
            HomePromoBanner(onDismiss = { promoBannerDismissed = true })
        }

        HomeSearchSection(
            activeCity = activeCity,
            onClick = onSearchClick
        )

        Spacer(modifier = Modifier.height(22.dp))

        HomeExplorationSection(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelect = { selectedCategory = it }
        )

        // Destaque da faixa de horário. Só aparece quando há loja aberta
        // suficiente na faixa; a lista regional completa continua abaixo.
        if (spotlightStores.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            HomeDaypartSection(
                daypart = daypart,
                stores = spotlightStores,
                onStoreClick = onStoreClick,
                onViewAll = onSearchClick
            )
        }

        // Atalhos exclusivamente para lojas em que o cliente já comprou.
        if (recentStores.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            HomeFavoriteStoresSection(
                favoriteStores = recentStores,
                onStoreClick = onStoreClick
            )
        }

        // Lojas com cupons ativos.
        if (storesWithCoupons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            HomeStoresWithCouponsSection(
                storesWithCoupons = storesWithCoupons,
                onStoreClick = { storeId, _ -> onStoreClick(storeId) }
            )
        }

        if (affordableProducts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            HomeAffordableProductsSection(
                products = affordableProducts,
                onProductClick = { onStoreClick(it.storeId) },
                onViewMore = onSearchClick
            )
        }

        if (repeatProducts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            HomeRepeatProductsSection(
                products = repeatProducts,
                onProductClick = { onStoreClick(it.storeId) },
                onViewMore = { /* TODO(fase-rede): tela de pedidos */ }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        HomeStoreListSection(
            stores = visibleStores,
            regionalStoreCount = stores.size,
            hasActiveFilters = hasActiveFilters,
            isLoading = isLoadingStores,
            errorMessage = storesErrorMessage,
            requiresAddress = false,
            activeCity = activeCity,
            selectedCategory = selectedCategory,
            isFreeFeeActive = isFreeFeeActive,
            isDirectDeliveryActive = isDirectDeliveryActive,
            storeSort = storeSort,
            onCategorySelect = { selectedCategory = it },
            onToggleFreeFee = { isFreeFeeActive = !isFreeFeeActive },
            onToggleDirectDelivery = { isDirectDeliveryActive = !isDirectDeliveryActive },
            onStoreSortSelect = { storeSort = it },
            onRetry = { storesReloadKey++ },
            onInformAddress = { showAddressChoiceDialog = true },
            onStoreClick = onStoreClick,
            // Sem categoria: a Busca abre o catálogo completo da cidade.
            onViewAllStores = onSearchClick
        )

        // A primeira dobra termina na lista, como no layout aprovado. A lista é
        // cortada e o catálogo completo continua acessível pela Busca.
        Spacer(modifier = Modifier.height(18.dp))
    }

    // ITEM 4/6: bottom sheet de suporte (migração 1:1 do original — lá o sheet
    // é hospedado pela Home via uiState.showSupportSheet).
    if (showSupportSheet) {
        SupportBottomSheet(onDismiss = { showSupportSheet = false })
    }

    // ITEM 3/6: diálogos de endereço/localização (migração 1:1 do original).
    LocationOrAddressDialog(
        visible = showAddressChoiceDialog,
        registeredStreet = session.addressStreet,
        registeredNumber = session.addressNumber,
        registeredNeighborhood = session.addressNeighborhood,
        registeredCity = session.addressCity,
        currentStreet = currentStreet,
        currentNumber = currentNumber,
        currentNeighborhood = currentNeighborhood,
        currentCity = currentCity,
        isRefreshingLocation = isRefreshingLocation,
        onDismiss = { showAddressChoiceDialog = false },
        // TODO(fase-rede): aplicar o endereço cadastrado como endereço ativo.
        onUseRegisteredAddress = { showAddressChoiceDialog = false },
        onAllowLocation = {
            showAddressChoiceDialog = false
            // ITEM 6/6: fluxo de permissão de localização (expect/actual por
            // plataforma; espelha o HomeScreen do app Android).
            if (PlatformPermissions.hasLocationPermission()) {
                fetchGpsLocation()
            } else {
                showLocationPermissionDialog = true
            }
        },
        onRegisterAddress = {
            showAddressChoiceDialog = false
            addressFormError = null
            showAddressForm = true
        }
    )

    // ITEM 6/6: diálogo contextual de permissão de localização (migração 1:1 do
    // original; no Android usa a ActivityResult API, no iOS é stub).
    LocationAndPermissionsDialog(
        showDialog = showLocationPermissionDialog,
        onDismiss = { showLocationPermissionDialog = false },
        onLocationPermissionResult = { granted ->
            if (granted) fetchGpsLocation()
        }
    )

    AddressRegistrationSheet(
        visible = showAddressForm,
        draft = addressDraft,
        isLookingUpCep = isLookingUpCep,
        isSaving = isSavingAddress,
        errorMessage = addressFormError,
        onDismiss = { showAddressForm = false },
        onDraftChange = { transform -> addressDraft = transform(addressDraft) },
        onLookupCep = {
            isLookingUpCep = true
            addressFormError = null
            addressScope.launch {
                delay(900)
                // TODO(fase-rede): consulta real ao ViaCEP via Ktor.
                addressDraft = addressDraft.copy(
                    street = addressDraft.street.ifBlank { "Rua das Palmeiras" },
                    neighborhood = addressDraft.neighborhood.ifBlank { "Centro" },
                    city = addressDraft.city.ifBlank { "Araruama" },
                    state = addressDraft.state.ifBlank { "RJ" }
                )
                isLookingUpCep = false
            }
        },
        onSave = {
            if (addressDraft.street.isBlank() || addressDraft.city.isBlank()) {
                addressFormError = "Preencha a rua e a cidade para continuar."
            } else {
                isSavingAddress = true
                addressFormError = null
                addressScope.launch {
                    delay(900)
                    // TODO(fase-rede): persistir em saved_addresses (Supabase).
                    isSavingAddress = false
                    showAddressForm = false
                    showAddressChoiceDialog = false
                }
            }
        }
    )
}

@Composable
private fun HomeHeaderSection(
    userName: String,
    streetName: String,
    streetNumber: String,
    cartItemCount: Int,
    onCouponClick: () -> Unit,
    onCartClick: () -> Unit,
    onAddressClick: () -> Unit
) {
    // Saudação dinâmica por horário (America/Sao_Paulo) — padrão iFood.
    val greeting = remember {
        val hour = Clock.System.now().toLocalDateTime(TimeZone.of("America/Sao_Paulo")).hour
        when (hour) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
    }
    val displayAddress = listOf(streetName.trim(), streetNumber.trim())
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val hasAddress = displayAddress.isNotBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (userName.isNotBlank()) "$greeting, $userName" else greeting,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFontFamily(),
                    fontSize = 13.sp,
                    color = ItaSuperTextSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onAddressClick() }
            ) {
                Text(
                    text = displayAddress.ifBlank { "Informe seu endereço" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (hasAddress) ItaSuperTextPrimary else ItaSuperPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    painter = painterResource(Res.drawable.ic_lucide_chevron_down),
                    contentDescription = "Alterar endereço",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cupons (padrão iFood: ícone de fidelidade à direita; aqui o de oferta ItaSuper).
            IconButton(
                onClick = onCouponClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE8E8E8), CircleShape)
                    .testTag("header_coupons_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalOffer,
                    contentDescription = "Meus cupons",
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            // Sacola com badge de quantidade.
            Box {
                HomeTopActionButton(
                    icon = Res.drawable.ic_lucide_shopping_bag,
                    contentDescription = "Abrir sacola",
                    onClick = onCartClick,
                    modifier = Modifier.testTag("header_cart_button")
                )
                if (cartItemCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(ItaSuperPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (cartItemCount > 99) "99+" else cartItemCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

/** Banner promocional dispensável abaixo do header (padrão iFood, cores ItaSuper). */
@Composable
private fun HomePromoBanner(
    onDismiss: () -> Unit,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF1E3))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocalOffer,
            contentDescription = null,
            tint = ItaSuperPrimary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Você tem cupons disponíveis!",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ItaSuperTextPrimary
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Fechar",
                tint = ItaSuperTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


@Composable
private fun HomeTopActionButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE8E8E8), CircleShape)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun HomeSearchSection(
    activeCity: String,
    onClick: () -> Unit
) {
    val placeholder = if (activeCity.isNotBlank()) "Buscar em $activeCity"
    else "Buscar lojas e categorias"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("home_search_input"),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E5E5))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_lucide_search),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color(0xFF5F5F5F)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeExplorationSection(
    categories: List<CategoryItem>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    val visibleCategories = buildList {
        // Item local de limpeza de filtro: não altera nem depende do catálogo do Supabase.
        add(CategoryItem(id = "todas", name = "Todos", iconName = "layout_grid"))
        addAll(categories.filterNot { it.id.equals("todas", ignoreCase = true) }.take(5))
    }
    if (visibleCategories.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Explorar",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = ItaSuperTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ItaSuperPrimary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(visibleCategories, key = { index, category -> "home_explore_${index}_${category.id}" }) { _, category ->
                HomeExplorationCategoryItem(
                    category = category,
                    selected = category.id.equals(selectedCategory, ignoreCase = true),
                    onClick = { onCategorySelect(category.id) }
                )
            }
        }
    }
}

@Composable
private fun HomeExplorationCategoryItem(
    category: CategoryItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Mesma regra do app original: iconName -> drawable (composeResources).
    val iconRes = when (category.iconName.lowercase()) {
        "layout_grid", "todas", "todos", "all" -> Res.drawable.ic_cat_todos_3d
        "fastfood", "lanche", "lanches", "hamburger", "hamburguer" -> Res.drawable.ic_cat_lanches_3d
        "local_pizza", "pizza", "pizzaria" -> Res.drawable.ic_cat_pizzaria_3d
        "restaurant", "marmita" -> Res.drawable.ic_cat_marmita_3d
        "bakery_dining", "pastel", "pasteis" -> Res.drawable.ic_cat_pastel_3d
        "icecream", "acai", "açaí" -> Res.drawable.ic_cat_acai_3d
        "local_bar", "bebidas", "bebida" -> Res.drawable.ic_cat_bebidas_3d
        "shopping_cart", "mercado", "mercados", "market" -> Res.drawable.ic_cat_mercado_3d
        "pharmacy", "farmacia", "farmácia", "farmacias" -> Res.drawable.ic_cat_farmacia_3d
        "churrasco" -> Res.drawable.ic_cat_churrasco_3d
        else -> Res.drawable.ic_cat_lanches_3d
    }
    val label = category.name.ifBlank { category.id.replaceFirstChar { it.uppercase() } }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable(onClick = onClick)
            .testTag("explore_category_${category.id}")
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) Color(0xFFFFF8F3) else Color.White,
            border = BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) ItaSuperPrimary else Color(0xFFE8E8E8)
            ),
            modifier = Modifier.size(50.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = ManropeFontFamily(),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) ItaSuperPrimary else Color(0xFF262626),
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeQuickFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFFFF8F3) else Color.White,
        border = BorderStroke(1.dp, if (selected) ItaSuperPrimary else Color(0xFFE3E3E3))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) ItaSuperPrimary else Color(0xFF262626),
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Filtro selecionado",
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Definida no original mas sem chamada no corpo atual da Home — mantida por
 * fidelidade (o original também a define sem usar).
 */
@Composable
private fun HomeCompactBannerSection(banners: List<Banner>, onStoreClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(banners, key = { index, banner -> "home_banner_${index}_${banner.id}" }) { _, banner ->
            Surface(
                modifier = Modifier
                    .width(284.dp)
                    .height(116.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !banner.targetStoreId.isNullOrBlank()) {
                        banner.targetStoreId?.let(onStoreClick)
                    },
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                AsyncImage(
                    model = banner.imageUrl,
                    contentDescription = banner.title.ifBlank { "Campanha ItaSuper" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Definida no original mas sem chamada no corpo atual da Home — mantida por
 * fidelidade (o original também a define sem usar).
 */
@Composable
private fun HomeHeroBentoSection(
    banners: List<Banner>,
    isFreeFeeActive: Boolean,
    isDirectDeliveryActive: Boolean,
    onToggleFreeFee: () -> Unit,
    onToggleDirectDelivery: () -> Unit,
    onStoreClick: (String) -> Unit
) {
    val activeBanner = banners.firstOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Grande (Left)
        Card(
            modifier = Modifier
                .weight(1.8f)
                .height(160.dp)
                .clickable {
                    activeBanner?.targetStoreId?.let { storeId ->
                        onStoreClick(storeId)
                    }
                },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (activeBanner != null && activeBanner.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = activeBanner.imageUrl,
                        contentDescription = activeBanner.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = activeBanner.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeBanner.description.isNotBlank()) {
                            Text(
                                text = activeBanner.description,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.85f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(ItaSuperPrimary, ItaSuperPrimary.copy(alpha = 0.85f))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "OFERTAS ITASUPER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "Os melhores estabelecimentos de Itaboraí",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    lineHeight = 20.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Ver ofertas",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = ItaSuperPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mini Cards Fixos (Right Column)
        Column(
            modifier = Modifier
                .weight(1f)
                .height(160.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mini Card 1: Sem taxa
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleFreeFee() }
                    .testTag("filter_card_sem_taxa"),
                shape = RoundedCornerShape(16.dp),
                color = if (isFreeFeeActive) ItaSuperPrimary.copy(alpha = 0.15f) else ItaSuperHighlightBg,
                border = BorderStroke(
                    width = if (isFreeFeeActive) 2.dp else 1.dp,
                    color = if (isFreeFeeActive) ItaSuperPrimary else Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (isFreeFeeActive) ItaSuperPrimary else ItaSuperHighlightText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sem taxa",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isFreeFeeActive) ItaSuperPrimary else ItaSuperTextPrimary,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "de entrega",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ItaSuperTextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Mini Card 2: Entrega direta
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleDirectDelivery() }
                    .testTag("filter_card_entrega_direta"),
                shape = RoundedCornerShape(16.dp),
                color = if (isDirectDeliveryActive) ItaSuperPrimary.copy(alpha = 0.15f) else ItaSuperSecondary,
                border = BorderStroke(
                    width = if (isDirectDeliveryActive) 2.dp else 1.dp,
                    color = if (isDirectDeliveryActive) ItaSuperPrimary else Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Entrega",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDirectDeliveryActive) ItaSuperPrimary else ItaSuperTextPrimary,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "direta da loja",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ItaSuperTextSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Definida no original mas sem chamada no corpo atual da Home — mantida por
 * fidelidade (o original também a define sem usar).
 */
@Composable
private fun HomeHighlightsBentoSection(
    stores: List<Store>,
    onStoreClick: (String) -> Unit
) {
    val topStores = stores.sortedByDescending { it.rating }.take(3)
    if (topStores.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Destaques",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SoraFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
        }

        val mainStore = topStores[0]
        val sideStore1 = topStores.getOrNull(1)
        val sideStore2 = topStores.getOrNull(2)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Store Card (2 cols x 2 rows effect)
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .clickable { onStoreClick(mainStore.id) }
                    .testTag("highlight_main_store_${mainStore.id}"),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = mainStore.bannerUrl.ifBlank { mainStore.logoUrl },
                        contentDescription = mainStore.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    // Badge "Destaque" top left
                    Surface(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.TopStart),
                        color = ItaSuperPrimary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Destaque",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = mainStore.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = ItaSuperWarning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = mainStore.rating.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperWarning,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${mainStore.category}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Side Column (2 stacked smaller stores)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOfNotNull(sideStore1, sideStore2).forEach { store ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clickable { onStoreClick(store.id) }
                            .testTag("highlight_side_store_${store.id}"),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = store.logoUrl.ifBlank { store.bannerUrl },
                                contentDescription = store.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = store.name,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = ItaSuperWarning,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = store.rating.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperWarning,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Definida no original mas sem chamada no corpo atual da Home — mantida por
 * fidelidade (o original também a define sem usar).
 */
@Composable
private fun HomeDiscoverProductsSection(
    products: List<DiscoverProduct>,
    onProductClick: (String) -> Unit
) {
    val productsWithImages = products.filter { product ->
        product.imageUrl.isNotBlank() && !product.imageUrl.equals("null", ignoreCase = true)
    }
    if (productsWithImages.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Fastfood,
                    contentDescription = null,
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Descubra",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SoraFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
            Text(
                text = "SELECIONADO PRA VOCÊ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperTextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            productsWithImages.chunked(2).forEach { rowProducts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowProducts.forEach { product ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onProductClick(product.storeId) }
                                .testTag("discover_product_${product.id}"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(ItaSuperHighlightBg)
                                ) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = product.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Badge "Aberta" com bolinha verde (produtos já vêm só de lojas abertas)
                                    Surface(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .align(Alignment.TopStart),
                                        color = Color.White.copy(alpha = 0.9f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color(0xFF10B981), CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Aberta",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = ItaSuperTextPrimary,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }

                                    // Badge de categoria da loja
                                    if (product.storeCategory.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .align(Alignment.TopEnd),
                                            color = Color.Black.copy(alpha = 0.75f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = product.storeCategory.replace("_", " ").uppercase(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 9.sp
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = SoraFontFamily(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            lineHeight = 16.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        minLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = product.storeName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = ItaSuperTextSecondary,
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            color = ItaSuperPrimary,
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = formatPriceBR(product.price),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = SoraFontFamily(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontSize = 11.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (rowProducts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Definida no original mas sem chamada no corpo atual da Home — mantida por
 * fidelidade (o original também a define sem usar).
 */
@Composable
private fun HomeCategoryChipsSection(
    categories: List<CategoryItem>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(categories, key = { idx, cat -> "cat_${idx}_${cat.id}" }) { _, cat ->
            val isSelected = cat.id == selectedCategory

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCategorySelect(cat.id) }
                    .testTag("chip_category_${cat.id}"),
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) ItaSuperPrimary else ItaSuperSecondary,
                border = if (!isSelected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                } else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (cat.iconName) {
                        "fastfood" -> Icons.Filled.Fastfood
                        "local_pizza" -> Icons.Filled.LocalPizza
                        "shopping_cart" -> Icons.Filled.ShoppingCart
                        "medical_services" -> Icons.Filled.MedicalServices
                        "local_bar" -> Icons.Filled.LocalBar
                        else -> Icons.Filled.Apps
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else ItaSuperPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else ItaSuperTextPrimary,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * Definida no original mas sem chamada no corpo atual da Home — mantida por
 * fidelidade (o original também a define sem usar).
 */
@Composable
private fun HomeLastOrderSection(
    order: Order,
    isReordering: Boolean,
    onViewStore: () -> Unit,
    onReorder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = ItaSuperTextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ÚLTIMO PEDIDO",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperTextSecondary,
                    letterSpacing = 0.5.sp
                )
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Store Avatar Logo
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ItaSuperHighlightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = order.storeName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = order.createdAt.take(10).ifBlank { "Pedido anterior" },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Text(
                        text = formatPriceBR(order.total),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperHighlightText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewStore,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("view_last_store_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ver loja",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Button(
                        onClick = onReorder,
                        enabled = !isReordering,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("reorder_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isReordering) "Adicionando..." else "Pedir de novo",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreLogoThumbnail(
    store: Store,
    modifier: Modifier,
    circular: Boolean
) {
    val imageUrl = store.logoUrl.ifBlank { store.bannerUrl }.trim()
    val imageFailed = imageUrl.isBlank() || imageUrl.equals("null", ignoreCase = true)
    val shape = if (circular) CircleShape else RoundedCornerShape(16.dp)
    val initial = store.name.trim().take(1).uppercase().ifBlank { "I" }
    val useDarkPlaceholder = initial == "C"
    val placeholderBackground = if (useDarkPlaceholder) Color(0xFF741914) else Color(0xFFFFE8A8)
    val placeholderForeground = if (useDarkPlaceholder) Color.White else ItaSuperPrimary

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (imageFailed) placeholderBackground else ItaSuperHighlightBg),
        contentAlignment = Alignment.Center
    ) {
        if (!imageFailed) {
            AsyncImage(
                model = imageUrl,
                contentDescription = store.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (imageFailed) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = initial,
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (circular) 18.sp else 24.sp,
                    lineHeight = if (circular) 18.sp else 23.sp,
                    color = placeholderForeground
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_home_placeholder_smile),
                    contentDescription = null,
                    tint = placeholderForeground,
                    modifier = Modifier
                        .offset(y = (-6).dp)
                        .size(if (circular) 17.dp else 22.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeFavoriteStoresSection(
    favoriteStores: List<Store>,
    onStoreClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(25.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_ita_store),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Suas lojas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFF1F1F1F)
                    ),
                    maxLines = 1
                )
            }
            Text(
                text = "compradas recentemente",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF6D6D6D)
                ),
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.height(42.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(favoriteStores.take(2), key = { idx, store -> "fav_${idx}_${store.id}" }) { _, store ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .width(155.dp)
                        .clickable { onStoreClick(store.id) }
                        .testTag("fav_store_${store.id}")
                ) {
                    StoreLogoThumbnail(
                        store = store,
                        modifier = Modifier
                            .size(40.dp)
                            .border(1.dp, Color(0xFFE8E8E8), CircleShape),
                        circular = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF242424)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStoresWithCouponsSection(
    storesWithCoupons: List<StoreWithCoupon>,
    onStoreClick: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(25.dp)
        ) {
            Text(
                text = "Lojas com Cupom",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF1F1F1F)
                ),
                maxLines = 1
            )
        }
        Text(
            text = "Descontos exclusivos pra você",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = ManropeFontFamily(),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(0xFF6D6D6D)
            ),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(storesWithCoupons, key = { it.storeId }) { item ->
                PromoBannerCard(
                    badge = "CUPOM",
                    title = "-R$${item.discountValue}",
                    subtitle = item.storeName,
                    captionTitle = "R$${item.discountValue} OFF em ${item.storeName}",
                    captionSubtitle = "Use o cupom ${item.couponCode}",
                    emoji = "🎟",
                    gradientColors = listOf(ItaSuperPrimary, Color(0xFFFFB37A)),
                    onClick = { onStoreClick(item.storeId, item.couponCode) }
                )
            }
        }
    }
}

/**
 * Banner promocional estilo iFood: gradiente, badge, valor em destaque,
 * elemento visual e título + subtítulo abaixo.
 */
@Composable
private fun PromoBannerCard(
    badge: String,
    title: String,
    subtitle: String,
    captionTitle: String,
    captionSubtitle: String,
    emoji: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.width(280.dp).clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(colors = gradientColors))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp, end = 100.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    fontSize = 26.sp,
                    lineHeight = 28.sp,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
            Text(
                text = emoji,
                fontSize = 72.sp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp)
            )
        }
        Column(modifier = Modifier.padding(start = 4.dp, top = 8.dp)) {
            Text(
                text = captionTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1F1F1F)
            )
            Text(
                text = captionSubtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                color = Color(0xFF6D6D6D)
            )
        }
    }
}

/**
 * Card estilo "ticket" de cupom. Definido no original mas sem chamada —
 * mantido por fidelidade.
 */
@Composable
private fun CouponTicketCard(
    item: StoreWithCoupon,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val logoUrl = item.storeLogoUrl?.trim().orEmpty()
            val logoFailed = logoUrl.isBlank() || logoUrl.equals("null", ignoreCase = true)
            val initial = item.storeName.trim().take(1).uppercase().ifBlank { "I" }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (logoFailed) Color(0xFFFFE8A8) else Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                if (!logoFailed) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = item.storeName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (logoFailed) {
                    Text(
                        text = initial,
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = ItaSuperPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.storeName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1F1F1F)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.badgeText + " com " + item.couponCode,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = ItaSuperPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeDaypartSection(
    daypart: HomeDaypart,
    stores: List<Store>,
    onStoreClick: (String) -> Unit,
    onViewAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_ita_store),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = daypart.titulo,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF1F1F1F)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = daypart.subtitulo,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF6D6D6D)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Ver todas",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ItaSuperPrimary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAll)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .testTag("home_daypart_view_all")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(stores, key = { _, store -> "daypart_${store.id}" }) { _, store ->
                Card(
                    modifier = Modifier
                        .width(126.dp)
                        .clickable { onStoreClick(store.id) }
                        .testTag("daypart_store_${store.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        StoreLogoThumbnail(
                            store = store,
                            modifier = Modifier
                                .size(40.dp)
                                .border(1.dp, Color(0xFFE8E8E8), CircleShape),
                            circular = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = store.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF242424)
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = daypartStoreDetail(store),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = Color(0xFF737373)
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mesma leitura do card da lista: tempo quando existe, senão distância.
 * O card do destaque é estreito e não comporta a linha completa.
 */
private fun daypartStoreDetail(store: Store): String {
    val deliveryTime = store.deliveryTime.takeUnless { it.isBlank() || it.equals("null", true) }
    if (deliveryTime != null) return deliveryTime
    val distance = store.distanceKm?.takeIf { it.isFinite() && it >= 0.0 } ?: return "Aberta agora"
    return if (distance < 1.0) "${(distance * 1000).toInt()} m"
    else formatKmBR(distance)
}

@Composable
private fun HomeAffordableProductsSection(
    products: List<DiscoverProduct>,
    onProductClick: (DiscoverProduct) -> Unit,
    onViewMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preço acessível",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1F1F1F)
                )
            )
            Text(
                text = "Ver mais",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ItaSuperPrimary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewMore)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .testTag("affordable_products_view_more")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(products.take(8), key = { _, product -> "affordable_product_${product.id}" }) { _, product ->
                Card(
                    modifier = Modifier
                        .width(126.dp)
                        .clickable { onProductClick(product) }
                        .testTag("affordable_product_${product.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFFF7F7F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl.isNotBlank() && !product.imageUrl.equals("null", ignoreCase = true)) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_ita_bag),
                                    contentDescription = null,
                                    tint = Color(0xFF8A8A8A),
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF242424)
                            ),
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = product.storeName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = Color(0xFF737373)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatPriceBR(product.price),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeRepeatProductsSection(
    products: List<DiscoverProduct>,
    onProductClick: (DiscoverProduct) -> Unit,
    onViewMore: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Peça de novo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1F1F1F)
                    )
                )
                Text(
                    text = "produtos que você já pediu",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = Color(0xFF6D6D6D)
                    )
                )
            }
            Text(
                text = "Ver mais",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = ItaSuperPrimary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewMore)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .testTag("repeat_products_view_more")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(products.take(8), key = { _, product -> "repeat_product_${product.id}" }) { _, product ->
                Card(
                    modifier = Modifier
                        .width(126.dp)
                        .clickable { onProductClick(product) }
                        .testTag("repeat_product_${product.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE8E8E8)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(76.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFFF7F7F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (product.imageUrl.isNotBlank() && !product.imageUrl.equals("null", ignoreCase = true)) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_ita_bag),
                                    contentDescription = null,
                                    tint = Color(0xFF8A8A8A),
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFF242424)
                            ),
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = product.storeName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp,
                                color = Color(0xFF737373)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatPriceBR(product.price),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ManropeFontFamily(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = ItaSuperPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStoreListSection(
    stores: List<Store>,
    regionalStoreCount: Int,
    hasActiveFilters: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    requiresAddress: Boolean,
    activeCity: String,
    selectedCategory: String,
    isFreeFeeActive: Boolean,
    isDirectDeliveryActive: Boolean,
    storeSort: HomeStoreSort,
    onCategorySelect: (String) -> Unit,
    onToggleFreeFee: () -> Unit,
    onToggleDirectDelivery: () -> Unit,
    onStoreSortSelect: (HomeStoreSort) -> Unit,
    onRetry: () -> Unit,
    onInformAddress: () -> Unit,
    onStoreClick: (String) -> Unit,
    onViewAllStores: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Text(
                text = if (activeCity.isNotBlank()) "Lojas em $activeCity" else "Lojas",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = ItaSuperTextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (activeCity.isNotBlank()) {
                Text(
                    text = if (regionalStoreCount == 1) "1 loja" else "$regionalStoreCount lojas",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFontFamily(),
                        color = ItaSuperTextSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            HomeStoreSortControl(
                selectedSort = storeSort,
                onSelect = onStoreSortSelect
            )
        }

        HomeStoreFilterBar(
            isAllSelected = selectedCategory.equals("todas", ignoreCase = true),
            isFreeFeeActive = isFreeFeeActive,
            isDirectDeliveryActive = isDirectDeliveryActive,
            onSelectAll = { onCategorySelect("todas") },
            onToggleFreeFee = onToggleFreeFee,
            onToggleDirectDelivery = onToggleDirectDelivery
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEDEDED))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.68f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color(0xFFEDEDED))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(11.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF1F1F1))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.58f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFFF1F1F1))
                            )
                        }
                    }
                }
            }
        } else if (requiresAddress) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSuperHighlightBg)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Informe sua localização ou endereço",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Precisamos da sua cidade para mostrar lojas que atendem sua região.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = ItaSuperTextSecondary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onInformAddress,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Informar localização/endereço", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (errorMessage != null && stores.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tentar novamente", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (stores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        regionalStoreCount > 0 && hasActiveFilters -> "Nenhuma loja encontrada com os filtros selecionados"
                        activeCity.isNotBlank() -> "Ainda não há lojas cadastradas em $activeCity"
                        else -> "Nenhuma loja encontrada para esse filtro"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        } else {
            // Em cidade grande a lista inteira vira parede de loja sem sentido.
            // A Home mostra o começo e o catálogo completo fica na Busca.
            val visibleStores = stores.take(HOME_STORE_LIST_LIMIT)
            Column(modifier = Modifier.fillMaxWidth()) {
                visibleStores.forEachIndexed { index, store ->
                    StoreCardItem(
                        store = store,
                        showDivider = index < visibleStores.lastIndex,
                        onClick = { onStoreClick(store.id) }
                    )
                }
            }
            // O botão só aparece quando sobra loja de fato: cortar 13 para mostrar
            // 12 não justifica mandar o cliente para outra tela.
            if (stores.size > HOME_STORE_LIST_OVERFLOW_THRESHOLD) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onViewAllStores,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("home_view_all_stores"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ItaSuperPrimary)
                ) {
                    Text(
                        text = "Ver todas as ${stores.size} lojas",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ItaSuperPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeStoreFilterBar(
    isAllSelected: Boolean,
    isFreeFeeActive: Boolean,
    isDirectDeliveryActive: Boolean,
    onSelectAll: () -> Unit,
    onToggleFreeFee: () -> Unit,
    onToggleDirectDelivery: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "filter_all") {
            HomeQuickFilterPill(label = "Todos", selected = isAllSelected, onClick = onSelectAll)
        }
        item(key = "filter_free_fee") {
            HomeQuickFilterPill(label = "Taxa grátis", selected = isFreeFeeActive, onClick = onToggleFreeFee)
        }
        item(key = "filter_direct_delivery") {
            HomeQuickFilterPill(label = "Entrega direta", selected = isDirectDeliveryActive, onClick = onToggleDirectDelivery)
        }
    }
}

@Composable
private fun HomeStoreSortControl(
    selectedSort: HomeStoreSort,
    onSelect: (HomeStoreSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
            modifier = Modifier
                .width(100.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .testTag("home_sort_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_home_sort),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Ordenar",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HomeStoreSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = sort.label,
                            fontWeight = if (sort == selectedSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (sort == selectedSort) ItaSuperPrimary else ItaSuperTextPrimary
                        )
                    },
                    trailingIcon = if (sort == selectedSort) {
                        { Icon(Icons.Filled.Check, contentDescription = "Selecionado", tint = ItaSuperPrimary) }
                    } else null,
                    onClick = {
                        expanded = false
                        onSelect(sort)
                    }
                )
            }
        }
    }
}

/**
 * Rótulo "Abre hoje às 18:00" para lojas fechadas (original usava
 * java.util.Calendar; aqui kotlinx-datetime, mesma regra America/Sao_Paulo).
 * Convenção dayOfWeek: 0 = domingo … 6 = sábado.
 */
private fun nextStoreOpeningLabel(store: Store): String {
    if (store.isOpen || store.openingHours.isEmpty()) return ""
    val now = Clock.System.now().toLocalDateTime(TimeZone.of("America/Sao_Paulo"))
    val today = when (now.dayOfWeek) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 0
    }
    val currentMinutes = now.hour * 60 + now.minute

    fun startMinutes(value: String): Int? {
        val parts = value.trim().split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hours * 60 + minutes
    }

    for (offset in 0..7) {
        val day = (today + offset) % 7
        val nextHour = store.openingHours
            .filter { it.dayOfWeek == day && !it.isClosedAllDay }
            .sortedBy { startMinutes(it.openTime) ?: Int.MAX_VALUE }
            .firstOrNull { offset > 0 || (startMinutes(it.openTime) ?: Int.MAX_VALUE) > currentMinutes }
            ?: continue
        val prefix = when (offset) {
            0 -> "Abre hoje às"
            1 -> "Abre amanhã às"
            else -> "Abre"
        }
        // Remove os segundos (08:00:00 -> 08:00)
        val timeShort = nextHour.openTime.take(5)
        return "$prefix $timeShort"
    }
    return ""
}

@Composable
private fun StoreCardItem(
    store: Store,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val categoryLabel = store.category
        .replace("_", " ")
        .replaceFirstChar { character -> character.uppercase() }
        .ifBlank { "Loja" }
    val deliveryTime = store.deliveryTime.takeUnless { it.isBlank() || it.equals("null", true) }
    val rawDeliveryFee = store.deliveryFee.takeUnless { it.isBlank() || it.equals("null", true) }
    val deliveryFee = rawDeliveryFee?.let { fee ->
        when {
            fee.equals("Grátis", true) || fee.equals("Retirada", true) || fee.startsWith("A partir", true) -> fee
            else -> "Taxa $fee"
        }
    }
    val distanceLabel = store.distanceKm
        ?.takeIf { it.isFinite() && it >= 0.0 }
        ?.let { distance ->
            if (distance < 1.0) "${(distance * 1000).toInt()} m" else formatKmBR(distance)
        }
    val deliveryDetails = listOfNotNull(deliveryTime, deliveryFee, distanceLabel).joinToString("  •  ")
    val openingMessage = nextStoreOpeningLabel(store)
    val deliveryUnavailable = store.deliveryMode.equals("own", ignoreCase = true) && store.hasAvailableDriver == false

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("store_card_${store.id}")
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoreLogoThumbnail(
                store = store,
                modifier = Modifier.size(56.dp),
                circular = false
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color(0xFF1F1F1F)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    StoreAvailabilityBadge(isOpen = store.isOpen)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = categoryLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = ManropeFontFamily(),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color(0xFF686868)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (deliveryDetails.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = deliveryDetails,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = ManropeFontFamily(),
                            fontSize = 13.sp,
                            color = if (store.isFreeDelivery) ItaSuperSuccess else Color(0xFF686868),
                            fontWeight = if (store.isFreeDelivery) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (deliveryUnavailable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = store.deliveryAvailabilityMessage.ifBlank {
                            "Entrega indisponível no momento"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ManropeFontFamily(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = Color(0xFFC13550)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!store.isOpen && openingMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = openingMessage,
                        style = MaterialTheme.typography.labelMedium.copy(color = ItaSuperTextSecondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                painter = painterResource(Res.drawable.ic_ita_chevron_right),
                contentDescription = "Abrir loja",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = Color(0xFFEEEEEE),
                thickness = 1.dp
            )
        }
    }
}

@Composable
private fun StoreAvailabilityBadge(isOpen: Boolean) {
    val background = if (isOpen) Color(0xFFF0FAF3) else Color(0xFFFFF0F2)
    val foreground = if (isOpen) ItaSuperSuccess else Color(0xFFC13550)
    val label = if (isOpen) "Aberta" else "Fechada"

    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(foreground)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = ManropeFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    color = foreground,
                    fontSize = 11.sp
                )
            )
        }
    }
}
