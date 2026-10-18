package com.example.shared.ui.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.shared.generated.resources.Res
import com.example.shared.generated.resources.ic_home_placeholder_smile
import org.jetbrains.compose.resources.painterResource
import com.example.shared.model.AddonItem
import com.example.shared.model.MenuSection
import com.example.shared.model.PizzaAddonGroup
import com.example.shared.model.PizzaBorder
import com.example.shared.model.Product
import com.example.shared.model.SelectedAddonItem
import com.example.shared.model.Store
import com.example.shared.network.SupabaseApi
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSecondary
import com.example.shared.theme.ItaSuperTextPrimary
import com.example.shared.theme.ItaSuperWarning
import com.example.shared.ui.cart.CartHolder
import com.example.shared.ui.cart.formatBRL
import com.example.shared.ui.common.StoreSwitchBottomSheet
import com.example.shared.ui.mock.findMockStore
import com.example.shared.ui.mock.mockMenuSections
import com.example.shared.ui.mock.mockStoreProducts
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// TODO(fase-rede): trocar mocks por SupabaseClient via Ktor (loja, produtos,
// seções do cardápio, avaliações). Layout espelha StoreDetailScreen do app Android.

private fun formatOneDecimal(value: Double): String {
    val tenths = ((value * 10) + 0.5).toInt()
    return "${tenths / 10},${tenths % 10}"
}

/** 0(Dom)..6(Sáb), igual ao Calendar.DAY_OF_WEEK - 1 do original. */
private fun todayDowSun0(): Int {
    val dow = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek
    return (dow.ordinal + 1) % 7
}

/**
 * Item aguardando confirmação do StoreSwitchBottomSheet (espelha
 * uiState.pendingStoreSwitch do StoreDetailViewModel do app Android).
 */
private data class PendingAdd(
    val product: Product,
    val quantity: Int,
    val notes: String,
    val addons: List<SelectedAddonItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    storeId: String,
    cart: CartHolder,
    onBackClick: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToInfo: () -> Unit
) {
    val store = remember(storeId) { findMockStore(storeId) }
    val products = remember(storeId) { mockStoreProducts(storeId) }
    val menuSections = remember(storeId) { mockMenuSections(storeId) }

    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var modalQuantity by remember { mutableStateOf(1) }
    var modalNotes by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }
    var showBuilder by remember { mutableStateOf<String?>(null) }

    // Confirmação de troca de loja (espelha pendingStoreSwitch do original):
    // se a sacola tem itens de outra loja, o StoreSwitchBottomSheet pede
    // confirmação antes de esvaziar, em vez do auto-clear silencioso.
    var pendingAdd by remember(storeId) { mutableStateOf<PendingAdd?>(null) }
    var showStoreSwitch by remember(storeId) { mutableStateOf(false) }

    fun requestAdd(
        product: Product,
        quantity: Int = 1,
        notes: String = "",
        addons: List<SelectedAddonItem> = emptyList()
    ) {
        if (cart.isSwitchingStore(product)) {
            pendingAdd = PendingAdd(
                product = product,
                quantity = quantity,
                notes = notes,
                addons = addons
            )
            showStoreSwitch = true
        } else {
            cart.addProduct(
                product,
                quantity = quantity,
                notes = notes,
                addons = addons,
                storeName = store.name
            )
        }
    }

    val menuListState = rememberLazyListState()
    val menuScrollScope = rememberCoroutineScope()

    val isPharmacyStore = store.category.equals("farmacias", ignoreCase = true) ||
        store.secondaryCategories.any { it.equals("farmacias", ignoreCase = true) }
    val hasPizzaCategory = store.category.lowercase().contains("pizza") ||
        store.secondaryCategories.any { it.lowercase().contains("pizza") }
    val hasPastelCategory = store.category.lowercase().contains("pastel") ||
        store.category.lowercase().contains("pasteis") ||
        store.secondaryCategories.any {
            it.lowercase().contains("pastel") || it.lowercase().contains("pasteis")
        }
    val builderType = when {
        hasPastelCategory && store.settings.pastelHalfEnabled && products.isNotEmpty() -> "pastel"
        hasPizzaCategory && store.settings.pizzaHalfEnabled && products.isNotEmpty() -> "pizza"
        else -> null
    }
    val isDeliveryUnavailable = store.deliveryMode.equals("own", ignoreCase = true) &&
        store.hasAvailableDriver == false && store.isOpen

    // Agrupa produtos por seção do cardápio (espelha a lógica do original).
    val groupedProducts = remember(products, menuSections) {
        val map = LinkedHashMap<String, MutableList<Product>>()
        fun resolveSectionName(prod: Product): String {
            if (!prod.sectionId.isNullOrBlank()) {
                val found = menuSections.find { it.id == prod.sectionId }
                if (found != null) return found.name
            }
            return if (prod.category.isNotBlank()) prod.category else "Geral"
        }
        menuSections.forEach { sec -> map[sec.name] = mutableListOf() }
        products.forEach { prod ->
            map.getOrPut(resolveSectionName(prod)) { mutableListOf() }.add(prod)
        }
        map.filterValues { it.isNotEmpty() }
    }
    val productSections = remember(groupedProducts) {
        groupedProducts.entries
            .filterNot { (sectionName, _) -> sectionName.equals("Destaques", ignoreCase = true) }
            .map { it.key to it.value }
    }
    val allSectionNames = listOf("Todos") + productSections.map { it.first }
    val featuredProducts = products.take(6)
    val sectionStartIndexByName = remember(productSections, builderType, featuredProducts, isDeliveryUnavailable) {
        val starts = LinkedHashMap<String, Int>()
        var itemIndex = 1 // Hero
        if (isDeliveryUnavailable) itemIndex += 1
        if (builderType != null) itemIndex += 1
        if (featuredProducts.isNotEmpty()) {
            itemIndex += 1 + featuredProducts.chunked(3).size // título + linhas da grade
        }
        productSections.forEach { (sectionName, sectionProducts) ->
            starts[sectionName] = itemIndex
            itemIndex += 1 + sectionProducts.size // título da seção + linhas de produtos
        }
        starts
    }
    val activeSectionName by remember(menuListState, sectionStartIndexByName) {
        derivedStateOf {
            sectionStartIndexByName
                .filterValues { it <= menuListState.firstVisibleItemIndex }
                .maxByOrNull { it.value }
                ?.key ?: "Todos"
        }
    }
    val showContextualTopBar by remember(menuListState, sectionStartIndexByName) {
        derivedStateOf {
            val firstMenuSectionIndex = sectionStartIndexByName.values.minOrNull() ?: Int.MAX_VALUE
            menuListState.firstVisibleItemIndex >= firstMenuSectionIndex
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = cart.totalItemCount > 0) {
                Surface(
                    shadowElevation = 7.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFAFAFA))
                            .clickable(onClick = onNavigateToCart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = ItaSuperPrimary.copy(alpha = 0.11f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingBag,
                                    contentDescription = null,
                                    tint = ItaSuperPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ver sacola",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ItaSuperTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Surface(
                            shape = CircleShape,
                            color = ItaSuperPrimary
                        ) {
                            Text(
                                text = "${cart.totalItemCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = formatBRL(cart.subtotal),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = ItaSuperTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(13.dp))
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = menuListState,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    StoreShowcaseHeader(
                        store = store,
                        onBackClick = onBackClick,
                        onContactClick = { /* TODO(fase-rede): abrir WhatsApp */ },
                        onOpenMaps = { /* TODO(fase-rede): abrir mapa */ },
                        onInfoClick = onNavigateToInfo,
                        isFavorite = isFavorite,
                        onFavoriteClick = { isFavorite = !isFavorite }
                    )
                }

                if (isDeliveryUnavailable) {
                    item(key = "delivery_unavailable_notice") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = ItaSuperWarning.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, ItaSuperWarning.copy(alpha = 0.32f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = ItaSuperWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Entrega indisponível no momento",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ItaSuperTextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = store.deliveryAvailabilityMessage.ifBlank {
                                            "Esta loja está sem entregador disponível. Você ainda pode fazer seu pedido para retirada."
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = ItaSuperSecondary,
                                            lineHeight = 17.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (builderType != null) {
                    item {
                        StoreBuilderHighlight(
                            title = if (builderType == "pastel") "Monte seu pastel" else "Monte sua pizza",
                            subtitle = if (builderType == "pastel") {
                                "Escolha recheios e adicionais do seu jeito"
                            } else {
                                "Escolha tamanho, sabores e adicionais do seu jeito"
                            },
                            imageUrl = products.firstOrNull {
                                if (builderType == "pastel") it.isPastelFlavor else it.pizzaCategoryId.isNotBlank()
                            }?.imageUrl.orEmpty(),
                            icon = if (builderType == "pastel") Icons.Filled.Restaurant else Icons.Filled.LocalPizza,
                            onClick = { showBuilder = builderType }
                        )
                    }
                }

                if (featuredProducts.isNotEmpty()) {
                    item(key = "highlights_title") {
                        Text(
                            text = "Destaques",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = ItaSuperTextPrimary
                            ),
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 10.dp)
                        )
                    }
                    featuredProducts.chunked(3).forEachIndexed { rowIndex, productRow ->
                        item(key = "highlights_grid_$rowIndex") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                productRow.forEach { product ->
                                    ProductShowcaseTile(
                                        product = product,
                                        onCardClick = {
                                            selectedProduct = product
                                            modalQuantity = 1
                                            modalNotes = ""
                                        },
                                        onAddClick = {
                                            requestAdd(product)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - productRow.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (products.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Esta loja ainda não cadastrou produtos",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    productSections.forEachIndexed { secIndex, (secName, secProducts) ->
                        item(key = "section_header_${secIndex}_$secName") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = secName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = ItaSuperTextPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${secProducts.size} itens",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = Color(0xFF777777)
                                    )
                                )
                            }
                        }
                        itemsIndexed(secProducts, key = { productIndex, product ->
                            "product_row_${secIndex}_${productIndex}_${product.id}"
                        }) { _, product ->
                            ProductMenuRow(
                                product = product,
                                onCardClick = {
                                    selectedProduct = product
                                    modalQuantity = 1
                                    modalNotes = ""
                                },
                                onAddClick = {
                                    requestAdd(product)
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            AnimatedVisibility(
                visible = showContextualTopBar,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                StoreStickyMenuHeader(
                    storeName = store.name,
                    sectionNames = allSectionNames,
                    activeSectionName = activeSectionName,
                    onBackClick = onBackClick,
                    onSectionClick = { sectionName ->
                        val targetIndex = if (sectionName == "Todos") {
                            sectionStartIndexByName.values.minOrNull() ?: 0
                        } else {
                            sectionStartIndexByName[sectionName] ?: 0
                        }
                        menuScrollScope.launch {
                            menuListState.animateScrollToItem(targetIndex)
                        }
                    }
                )
            }
        }
    }

    // Sheet de detalhes do produto
    selectedProduct?.let { product ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState
        ) {
            ProductDetailSheetContent(
                product = product,
                quantity = modalQuantity,
                notes = modalNotes,
                onQuantityIncrement = { modalQuantity++ },
                onQuantityDecrement = { if (modalQuantity > 1) modalQuantity-- },
                onNotesChange = { modalNotes = it },
                onAddToCartClick = {
                    requestAdd(product, quantity = modalQuantity, notes = modalNotes)
                    selectedProduct = null
                }
            )
        }
    }

    // Montador de pizza/pastel — fluxo completo igual ao da web.
    showBuilder?.let { type ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBuilder = null },
            sheetState = sheetState
        ) {
            PizzaBuilderSheetContent(
                type = type,
                store = store,
                products = products,
                menuSections = menuSections,
                onRequestAdd = { product, quantity, notes, addons ->
                    requestAdd(product, quantity = quantity, notes = notes, addons = addons)
                },
                onClose = { showBuilder = null }
            )
        }
    }

    // Bottom sheet estilo iFood: confirma esvaziar a sacola ao trocar de loja
    // (espelha uiState.pendingStoreSwitch do original).
    if (showStoreSwitch) {
        pendingAdd?.let { pending ->
            StoreSwitchBottomSheet(
                currentStoreName = cart.storeName,
                onConfirm = {
                    cart.clearCart()
                    cart.addProduct(
                        pending.product,
                        quantity = pending.quantity,
                        notes = pending.notes,
                        addons = pending.addons,
                        storeName = store.name
                    )
                    pendingAdd = null
                    showStoreSwitch = false
                },
                onDismiss = {
                    pendingAdd = null
                    showStoreSwitch = false
                }
            )
        }
    }
}

@Composable
private fun StoreStickyMenuHeader(
    storeName: String,
    sectionNames: List<String>,
    activeSectionName: String,
    onBackClick: () -> Unit,
    onSectionClick: (String) -> Unit
) {
    val categoryRowState = rememberLazyListState()
    LaunchedEffect(activeSectionName, sectionNames) {
        val activeIndex = sectionNames.indexOfFirst { it.equals(activeSectionName, ignoreCase = true) }
        if (activeIndex >= 0) categoryRowState.animateScrollToItem(activeIndex)
    }

    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = onBackClick),
                    shape = CircleShape,
                    color = Color(0xFFF5F5F5)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = ItaSuperTextPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CARDÁPIO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF8A8A8A)
                        ),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = storeName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFF1F1F1))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFAFAFA))
            ) {
                LazyRow(
                    state = categoryRowState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sectionNames, key = { index, name -> "sticky_section_${index}_$name" }) { _, sectionName ->
                        StoreMenuFilterPill(
                            label = sectionName,
                            selected = sectionName.equals(activeSectionName, ignoreCase = true),
                            onClick = { onSectionClick(sectionName) }
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFECECEC))
        }
    }
}

@Composable
private fun StoreBuilderHighlight(
    title: String,
    subtitle: String,
    imageUrl: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE9E9E9)),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 92.dp, height = 74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFF4EA)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF707070)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Montar agora",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ItaSuperPrimary
                            )
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreMenuFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFFFF0E6) else Color.White,
        border = BorderStroke(1.dp, if (selected) ItaSuperPrimary.copy(alpha = 0.55f) else Color(0xFFE5E5E5))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(ItaSuperPrimary, CircleShape)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) ItaSuperPrimary else Color(0xFF343434),
                    fontSize = 13.sp
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StoreShowcaseHeader(
    store: Store,
    onBackClick: () -> Unit,
    onContactClick: () -> Unit,
    onOpenMaps: () -> Unit,
    onInfoClick: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    val isOpen = store.isOpen
    val feeText = store.deliveryFee.takeUnless { it.isBlank() || it.equals("null", true) } ?: "—"
    val timeText = store.deliveryTime.takeUnless { it.isBlank() || it.equals("null", true) } ?: "—"

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp)
                .background(Color(0xFF4B1A11))
        ) {
            if (store.bannerUrl.isNotBlank()) {
                AsyncImage(
                    model = store.bannerUrl,
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ItaSuperPrimary.copy(alpha = 0.82f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    // statusBarsPadding removido: o Scaffold raiz do App() ja aplica
                    // o inset da status bar; manter aqui duplicava o espaco no topo.
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoreHeroActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    description = "Voltar",
                    onClick = onBackClick
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StoreHeroActionButton(
                        icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        description = if (isFavorite) "Remover dos favoritos" else "Favoritar loja",
                        onClick = onFavoriteClick,
                        iconTint = if (isFavorite) Color(0xFFFF5252) else Color.White
                    )
                    StoreHeroActionButton(
                        icon = Icons.Filled.Share,
                        description = "Contato da loja",
                        onClick = onContactClick,
                        enabled = store.whatsapp.isNotBlank()
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 188.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 66.dp, end = 16.dp, bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInfoClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = ItaSuperTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Mais informações",
                        tint = Color(0xFF4A4A4A),
                        modifier = Modifier.size(26.dp)
                    )
                }
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInfoClick() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = store.category.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: "Alimentação",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = Color(0xFF686868),
                        modifier = Modifier.weight(1f)
                    )
                    val todayCloseTime = remember(store.openingHours) {
                        val hours = store.openingHours
                        if (hours.isEmpty()) null
                        else {
                            val todayDow = todayDowSun0()
                            hours.firstOrNull { it.dayOfWeek == todayDow && !it.isClosedAllDay }
                                ?.closeTime?.takeIf { it.isNotBlank() }
                        }
                    }
                    Surface(
                        color = if (isOpen) Color(0xFFF1FAF3) else Color(0xFFFFF3F3),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isOpen) Color(0xFF16A34A) else Color(0xFFE5484D), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isOpen) "Aberta" + (todayCloseTime?.let { " · até ${it.take(5)}" } ?: "") else "Fechada",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (isOpen) Color(0xFF16803A) else Color(0xFFC9343C)
                                )
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(22.dp)
                    )
                }
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInfoClick() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val ratingText = if (store.rating > 0) {
                        "★ ${formatOneDecimal(store.rating)}"
                    } else {
                        "★ Novo"
                    }
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF1F1F1F),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFBDBDBD),
                        modifier = Modifier.size(22.dp)
                    )
                }
                HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = buildAnnotatedString {
                                append("Padrão • $timeText • Taxa - ")
                                withStyle(style = SpanStyle(color = ItaSuperPrimary, fontWeight = FontWeight.Bold)) {
                                    append(feeText)
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            ),
                            color = Color(0xFF1F1F1F)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append("Entrega pela loja")
                            if (store.minOrder > 0) {
                                append(" • Pedido mín. ${formatBRL(store.minOrder)}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = Color(0xFF888888)
                    )
                }
            }
        }

        StoreShowcaseLogo(
            store = store,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 150.dp)
        )
    }
}

@Composable
private fun StoreHeroActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconTint: Color = Color.White
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.56f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = iconTint,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun StoreShowcaseLogo(store: Store, modifier: Modifier = Modifier) {
    val imageUrl = store.logoUrl.trim()
    var imageFailed by remember(store.id, imageUrl) {
        mutableStateOf(imageUrl.isBlank() || imageUrl.equals("null", ignoreCase = true))
    }
    val initial = store.name.trim().take(1).uppercase().ifBlank { "I" }
    val useDarkPlaceholder = initial == "C"
    val placeholderBackground = if (useDarkPlaceholder) Color(0xFF741914) else Color(0xFFFFE8A8)
    val placeholderForeground = if (useDarkPlaceholder) Color.White else ItaSuperPrimary

    Surface(
        modifier = modifier.size(82.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (imageFailed) placeholderBackground else Color(0xFFFFE9AD),
        border = BorderStroke(2.dp, Color.White),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!imageFailed) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    onError = { imageFailed = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )
            }
            if (imageFailed) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            lineHeight = 32.sp,
                            color = placeholderForeground
                        )
                    )
                    Icon(
                        painter = painterResource(Res.drawable.ic_home_placeholder_smile),
                        contentDescription = null,
                        tint = placeholderForeground,
                        modifier = Modifier
                            .offset(y = (-7).dp)
                            .size(31.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductShowcaseTile(
    product: Product,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onCardClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF4F4F4))
        ) {
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    tint = Color(0xFFB9B9B9),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(25.dp)
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clickable { onAddClick() },
                shape = CircleShape,
                color = ItaSuperPrimary,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Adicionar ${product.name}",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                color = ItaSuperTextPrimary
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        val hasPromo = product.originalPrice != null && product.originalPrice > product.price
        if (hasPromo) {
            val discountPct = ((1 - product.price / product.originalPrice!!) * 100).toInt()
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFFF6B00)
            ) {
                Text(
                    text = "Promoção! -$discountPct%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatBRL(product.originalPrice),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = Color(0xFF9E9E9E),
                    textDecoration = TextDecoration.LineThrough
                ),
                maxLines = 1
            )
        }
        Text(
            text = formatBRL(product.price),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = ItaSuperPrimary
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun ProductMenuRow(
    product: Product,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.description.isNotBlank() && !product.description.equals("null", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Color(0xFF737373)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(9.dp))
                val hasPromoRow = product.originalPrice != null && product.originalPrice > product.price
                if (hasPromoRow) {
                    val discountPctRow = ((1 - product.price / product.originalPrice!!) * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFF6B00)
                    ) {
                        Text(
                            text = "Promoção! -$discountPctRow%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color.White
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatBRL(product.originalPrice),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color(0xFF9E9E9E),
                            textDecoration = TextDecoration.LineThrough
                        ),
                        maxLines = 1
                    )
                }
                Text(
                    text = formatBRL(product.price),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ItaSuperPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF4F4F4))
            ) {
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        tint = Color(0xFFB9B9B9),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clickable { onAddClick() },
                    shape = CircleShape,
                    color = ItaSuperPrimary,
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Adicionar ${product.name}",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color(0xFFEEEEEE),
            thickness = 1.dp
        )
    }
}

@Composable
private fun ProductDetailSheetContent(
    product: Product,
    quantity: Int,
    notes: String,
    onQuantityIncrement: () -> Unit,
    onQuantityDecrement: () -> Unit,
    onNotesChange: (String) -> Unit,
    onAddToCartClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        if (product.imageUrl.isNotBlank()) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (product.description.isNotBlank() && product.description.trim() != "null") {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = formatBRL(product.price),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ItaSuperPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Observações") },
            placeholder = { Text("Ex: Sem cebola, bem passado...") },
            maxLines = 3,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onQuantityDecrement, enabled = quantity > 1) {
                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Diminuir")
                }
                Text(
                    text = "$quantity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(onClick = onQuantityIncrement) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Aumentar")
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onAddToCartClick,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
            ) {
                Text(
                    text = "Adicionar • ${formatBRL(product.price * quantity)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Montador de pizza/pastel — fluxo completo igual ao da web (PizzaHalfHalfModal / PastelBuilderModal):
 *  0. Escolhe a quantidade de sabores (2/3/4) — pulado se a loja só permite 2
 *  1. Escolhe o tamanho (se a loja tiver tamanhos cadastrados)
 *  2..N. Escolhe cada sabor (1º, 2º, 3º, 4º), agrupado por seção do cardápio
 *  N+1. Adicionais (pizza) / Complementos (pastel) — se a loja tiver
 *  N+2. Borda (pizza) — se a loja tiver
 *  Final. Observações + quantidade + "Adicionar"
 *
 * Preço: "maior" (maior sabor) ou "media" (média dos sabores), conforme a loja.
 */
@Composable
private fun PizzaBuilderSheetContent(
    type: String,
    store: Store,
    products: List<Product>,
    menuSections: List<MenuSection>,
    onRequestAdd: (product: Product, quantity: Int, notes: String, addons: List<SelectedAddonItem>) -> Unit,
    onClose: () -> Unit
) {
    val isPastel = type == "pastel"
    val settings = store.settings
    val maxFlavorsSetting = (if (isPastel) settings.pastelMaxFlavors else settings.pizzaMaxFlavors).coerceIn(2, 4)
    val priceMode = if (isPastel) settings.pastelPriceMode else settings.pizzaPriceMode
    val singleSize = if (isPastel) settings.pastelSingleSize else settings.pizzaSingleSize
    val maxComplements = settings.pastelMaxComplements.coerceIn(1, 20)

    val title = if (isPastel) "Monte seu Pastel" else "Monte sua Pizza"
    val fractionLabels = mapOf(2 to "½", 3 to "⅓", 4 to "¼")

    var flavorCount by remember { mutableStateOf(2) }
    var step by remember { mutableStateOf(if (maxFlavorsSetting == 2) 1 else 0) }
    var productIds by remember { mutableStateOf<List<String?>>(listOf(null, null)) }
    var selectedBorderId by remember { mutableStateOf<String?>(null) }
    var selectedComplementIds by remember { mutableStateOf(setOf<String>()) }
    var selectedAddonIds by remember { mutableStateOf(setOf<String>()) }
    var observations by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    var selectedSize by remember { mutableStateOf<String?>(null) }

    // Bordas (pizza) / complementos (pastel) — busca na API, vazio com mocks.
    var borders by remember { mutableStateOf<List<PizzaBorder>>(emptyList()) }
    LaunchedEffect(store.id, isPastel) {
        borders = try {
            if (isPastel) SupabaseApi.fetchPastelBorders(store.id)
            else SupabaseApi.fetchPizzaBorders(store.id)
        } catch (_: Exception) {
            emptyList()
        }
        // Auto-seleciona "Tradicional" ou a gratuita (igual à web).
        if (!isPastel && borders.isNotEmpty() && selectedBorderId == null) {
            val tradicional = borders.firstOrNull {
                it.name.lowercase().contains("tradicional") || it.price == 0.0
            } ?: borders.firstOrNull()
            tradicional?.let { selectedBorderId = it.id }
        }
    }

    // Filtra sabores (igual à web: exclui bebidas; pastel usa isPastelFlavor quando curado).
    val beverageKeywords = listOf("bebida", "drink", "suco", "refrigerante", "água", "cerveja", "energético")
    val beverageSectionIds = remember(menuSections) {
        menuSections.filter { s -> beverageKeywords.any { kw -> s.name.lowercase().contains(kw) } }
            .map { it.id }.toSet()
    }
    val flavorProducts = remember(products, isPastel, beverageSectionIds) {
        val hasCuratedPastel = products.any { it.isPastelFlavor }
        products.filter { p ->
            if (p.isBeverage) return@filter false
            if (p.sectionId != null && beverageSectionIds.contains(p.sectionId)) return@filter false
            if (isPastel && hasCuratedPastel) return@filter p.isPastelFlavor
            if (!isPastel && p.pizzaCategoryId.isBlank()) {
                // Legado: sem pizzaCategoryId, aceita se não for bebida (compatibilidade).
                // Mas evita produtos que claramente não são pizza quando há categorizados.
                val anyCategorized = products.any { it.pizzaCategoryId.isNotBlank() }
                if (anyCategorized) return@filter false
            }
            true
        }
    }

    // Tamanhos: catálogo profissional ou união dos sizes legados.
    val catalogSizes = remember(settings) {
        settings.pizzaSizesCatalog.filter { it.active && it.maxFlavors >= 2 }
    }
    val catalogActive = !singleSize && !isPastel && catalogSizes.isNotEmpty()
    val legacySizes: List<String> = remember(flavorProducts) {
        val order = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (p in flavorProducts) {
            for (s in p.legacyPizzaSizes) {
                if (s.name.isNotBlank() && s.price > 0 && seen.add(s.name)) order.add(s.name)
            }
        }
        order
    }
    val availableSizes: List<String> = if (catalogActive) catalogSizes.map { it.name } else legacySizes
    val hasSizes = !singleSize && availableSizes.isNotEmpty()

    // Auto-seleciona o primeiro tamanho.
    LaunchedEffect(hasSizes, availableSizes) {
        if (hasSizes && selectedSize == null && availableSizes.isNotEmpty()) {
            selectedSize = availableSizes[0]
        }
    }

    // Máximo de sabores efetivo (respeita o limite do tamanho no catálogo).
    val sizeMaxFlavors: Int = remember(catalogActive, selectedSize, maxFlavorsSetting) {
        if (!catalogActive || selectedSize == null) maxFlavorsSetting
        else {
            val sizeItem = catalogSizes.firstOrNull { it.name == selectedSize }
            minOf(sizeItem?.maxFlavors ?: 4, maxFlavorsSetting).coerceIn(2, 4)
        }
    }
    // Clampa flavorCount se o tamanho não suportar.
    LaunchedEffect(sizeMaxFlavors) {
        if (flavorCount > sizeMaxFlavors) {
            flavorCount = sizeMaxFlavors
            productIds = List(sizeMaxFlavors) { null }
        }
    }

    // Adicionais ligados aos sabores escolhidos (só pizza).
    val chosenFlavorIdsKey = productIds.filterNotNull().joinToString(",")
    var addonGroups by remember { mutableStateOf<List<PizzaAddonGroup>>(emptyList()) }
    LaunchedEffect(chosenFlavorIdsKey, isPastel) {
        addonGroups = try {
            if (!isPastel && chosenFlavorIdsKey.isNotBlank()) {
                SupabaseApi.fetchFlavorAddonGroups(productIds.filterNotNull())
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    val hasBorders = !isPastel && borders.isNotEmpty()
    val hasAddons = !isPastel && addonGroups.isNotEmpty()
    val hasComplements = isPastel && borders.isNotEmpty()

    val totalSteps = flavorCount + (if (hasAddons) 1 else 0) + (if (hasBorders) 1 else 0) +
        (if (hasComplements) 1 else 0)
    val addonStep = if (hasAddons) flavorCount + 1 else -1
    val borderStep = if (hasBorders) flavorCount + (if (hasAddons) 1 else 0) + 1 else -1
    val complementStep = if (hasComplements) flavorCount + 1 else -1
    val lastStep = totalSteps

    // Preço de um sabor no tamanho selecionado (override → matriz → legado → base).
    fun priceForFlavor(p: Product): Double {
        if (catalogActive && selectedSize != null) {
            val sizeItem = catalogSizes.firstOrNull { it.name == selectedSize }
            if (sizeItem != null) {
                p.pizzaSizeOverrides[sizeItem.id]?.takeIf { it > 0 }?.let { return it }
                // Matriz: procura categoria do sabor (simplificado — usa pizzaCategoryId como chave).
                // A matriz completa usa categoryId; aqui usamos o override ou legado.
                val legacy = p.legacyPizzaSizes.firstOrNull { it.name == selectedSize && it.price > 0 }
                if (legacy != null) return legacy.price
            }
        } else if (selectedSize != null) {
            val match = p.legacyPizzaSizes.firstOrNull { it.name == selectedSize && it.price > 0 }
            if (match != null) return match.price
        }
        return p.price
    }

    val selectedFlavors = productIds.map { id -> id?.let { fid -> flavorProducts.firstOrNull { it.id == fid } } }
    val allChosen = selectedFlavors.all { it != null } && selectedFlavors.size == flavorCount
    val selectedBorder = borders.firstOrNull { it.id == selectedBorderId }

    fun calcBasePrice(): Double {
        if (!allChosen) return 0.0
        val prices = selectedFlavors.map { priceForFlavor(it!!) }
        return if (priceMode == "media") prices.sum() / prices.size else (prices.maxOrNull() ?: 0.0)
    }

    val basePrice = calcBasePrice()
    val borderPrice = selectedBorder?.price ?: 0.0
    val addonItemsById = remember(addonGroups) {
        addonGroups.flatMap { it.items }.associateBy { it.id }
    }
    val selectedAddonsList = selectedAddonIds.mapNotNull { addonItemsById[it] }
    val addonsTotal = selectedAddonsList.sumOf { it.price }
    val selectedComplements = borders.filter { selectedComplementIds.contains(it.id) }
    val complementsTotal = selectedComplements.sumOf { it.price }
    val unitPrice = basePrice + borderPrice + addonsTotal + complementsTotal
    val lineTotal = unitPrice * quantity

    val isFlavorStep = step in 1..flavorCount
    val currentFlavorIdx = if (isFlavorStep) step - 1 else -1

    fun ordinal(n: Int) = listOf("1º", "2º", "3º", "4º").getOrElse(n - 1) { "${n}º" }
    fun stepLabel(s: Int): String = when {
        s == 0 -> "Quantos sabores?"
        s == addonStep -> "Adicionais (opcional)"
        s == complementStep -> "Complementos (até $maxComplements, opcional)"
        s == borderStep -> "Escolha a borda"
        else -> "Escolha o ${ordinal(s)} sabor"
    }

    fun addonGroupsValid(): Boolean {
        for (g in addonGroups) {
            val count = g.items.count { selectedAddonIds.contains(it.id) }
            if (count < g.minSelect) return false
        }
        return true
    }

    fun canAdvance(): Boolean = when {
        step == 0 -> true
        isFlavorStep -> productIds[currentFlavorIdx] != null
        step == addonStep -> addonGroupsValid()
        step == complementStep -> true
        step == borderStep -> !hasBorders || selectedBorderId != null
        else -> false
    }

    fun handleNext() {
        when {
            step == 0 -> step = 1
            isFlavorStep && productIds[currentFlavorIdx] != null -> step = step + 1
            step == addonStep && addonGroupsValid() -> step = step + 1
            step == complementStep -> step = step + 1
        }
    }

    fun handleBack() {
        when {
            step == 0 -> onClose()
            step == 1 -> if (maxFlavorsSetting == 2) onClose() else step = 0
            isFlavorStep -> {
                productIds = productIds.toMutableList().also { it[currentFlavorIdx] = null }
                step = step - 1
            }
            step == addonStep -> {
                selectedAddonIds = emptySet()
                step = flavorCount
            }
            step == complementStep -> {
                selectedComplementIds = emptySet()
                step = flavorCount
            }
            step == borderStep -> {
                selectedBorderId = null
                step = if (hasAddons) addonStep else flavorCount
            }
        }
    }

    fun setCurrentFlavor(id: String) {
        productIds = productIds.toMutableList().also { it[currentFlavorIdx] = id }
    }

    fun handleFlavorCountChange(n: Int) {
        flavorCount = n
        productIds = List(n) { null }
        selectedBorderId = null
        selectedAddonIds = emptySet()
        selectedComplementIds = emptySet()
    }

    fun toggleAddon(group: PizzaAddonGroup, itemId: String) {
        val current = selectedAddonIds.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            val inGroup = group.items.count { current.contains(it.id) }
            if (inGroup < group.maxSelect) current.add(itemId)
        }
        selectedAddonIds = current
    }

    fun toggleComplement(itemId: String) {
        val current = selectedComplementIds.toMutableSet()
        if (current.contains(itemId)) current.remove(itemId)
        else if (current.size < maxComplements) current.add(itemId)
        selectedComplementIds = current
    }

    val isFinalStep = step >= 1 && step == lastStep

    fun handleAdd() {
        if (!allChosen) return
        val flavors = selectedFlavors.filterNotNull()
        val frac = fractionLabels[flavorCount] ?: ""
        val titleName = if (isPastel) {
            "Pastel $flavorCount Sabores"
        } else if (flavorCount == 2) {
            "Pizza Meio a Meio"
        } else {
            "Pizza $flavorCount Sabores"
        }
        val name = "$titleName: ${flavors.joinToString(" / ") { it.name }}"
        val displayName = if (selectedSize != null) "$name ($selectedSize)" else name

        val addons = mutableListOf<SelectedAddonItem>()
        // Sabores como addons informativos (fração + nome), igual à web.
        flavors.forEach { f ->
            addons.add(
                SelectedAddonItem(
                    itemId = "flavor-${f.id}",
                    itemName = "$frac ${f.name}",
                    itemPrice = 0.0,
                    groupId = "flavors",
                    groupName = "Sabores"
                )
            )
        }
        if (selectedSize != null) {
            addons.add(
                0,
                SelectedAddonItem(
                    itemId = "size",
                    itemName = "Tamanho: $selectedSize",
                    itemPrice = 0.0,
                    groupId = "size",
                    groupName = "Tamanho"
                )
            )
        }
        if (selectedBorder != null) {
            addons.add(
                SelectedAddonItem(
                    itemId = selectedBorder.id,
                    itemName = "Borda: ${selectedBorder.name}",
                    itemPrice = borderPrice,
                    groupId = "border",
                    groupName = "Borda"
                )
            )
        }
        for (a in selectedAddonsList) {
            addons.add(
                SelectedAddonItem(
                    itemId = a.id,
                    itemName = a.name,
                    itemPrice = a.price,
                    groupId = a.groupId,
                    groupName = "Adicionais"
                )
            )
        }
        for (c in selectedComplements) {
            addons.add(
                SelectedAddonItem(
                    itemId = c.id,
                    itemName = "Complemento: ${c.name}",
                    itemPrice = c.price,
                    groupId = "complements",
                    groupName = "Complementos"
                )
            )
        }

        val builtProduct = flavors[0].copy(
            price = unitPrice,
            name = displayName
        )
        onRequestAdd(builtProduct, quantity, observations, addons)
        onClose()
    }

    // Agrupa sabores por seção.
    val groupedFlavors = remember(flavorProducts, menuSections) {
        if (menuSections.isEmpty()) return@remember listOf(null to flavorProducts)
        val bySection = flavorProducts.groupBy { it.sectionId }
        val result = mutableListOf<Pair<MenuSection?, List<Product>>>()
        for (sec in menuSections) {
            val items = bySection[sec.id]
            if (!items.isNullOrEmpty()) result.add(sec to items)
        }
        val unsectioned = bySection[null].orEmpty()
        if (unsectioned.isNotEmpty()) result.add(null to unsectioned)
        result
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { handleBack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = ItaSuperTextPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = ItaSuperTextPrimary
                )
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Fechar",
                    tint = ItaSuperTextPrimary
                )
            }
        }
        HorizontalDivider(color = Color(0xFFEEEEEE))

        // Progresso + rótulo da etapa
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (step > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (s in 1..totalSteps) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (s <= step) ItaSuperPrimary else Color(0xFFE0E0E0))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF0E6),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPastel) Icons.Filled.Restaurant else Icons.Filled.LocalPizza,
                            contentDescription = null,
                            tint = ItaSuperPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (step == 0) "Comece por aqui" else "Etapa $step de $totalSteps",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ItaSuperTextPrimary
                    )
                    Text(
                        text = stepLabel(step),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Resumo da seleção
            if ((selectedFlavors.any { it != null } || selectedBorder != null) && step > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = buildString {
                                append(selectedFlavors.filterNotNull().joinToString(" + ") {
                                    "${fractionLabels[flavorCount] ?: ""} ${it.name}"
                                })
                                if (selectedBorder != null) append(" • ${selectedBorder.name}")
                                if (selectedComplements.isNotEmpty()) {
                                    append(" • " + selectedComplements.joinToString(", ") { it.name })
                                }
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = ItaSuperTextPrimary
                        )
                        if (allChosen) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = buildString {
                                    append(if (isPastel) "Pastel " else "Pizza ")
                                    append(formatBRL(basePrice))
                                    if (borderPrice > 0) append(" + Borda ${formatBRL(borderPrice)}")
                                    if (addonsTotal > 0) append(" + Adic. ${formatBRL(addonsTotal)}")
                                    if (complementsTotal > 0) append(" + Compl. ${formatBRL(complementsTotal)}")
                                    append(" = ${formatBRL(unitPrice)}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Conteúdo rolável
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Etapa 0: quantidade de sabores
            if (step == 0) {
                if (catalogActive && availableSizes.size > 1) {
                    Text(
                        text = "Escolha o tamanho",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ItaSuperTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (sz in catalogSizes) {
                            val isSel = selectedSize == sz.name
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSize = sz.name },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFFFFF0E6) else Color(0xFFFAFAFA),
                                border = BorderStroke(1.dp, if (isSel) ItaSuperPrimary else Color(0xFFE5E5E5))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = sz.name,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSel) ItaSuperPrimary else ItaSuperTextPrimary
                                    )
                                    Text(
                                        text = buildString {
                                            if (sz.description.isNotBlank()) append(sz.description + " · ")
                                            append("até ${sz.maxFlavors} sabores")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = "Escolha quantos sabores diferentes você quer:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (n in listOf(2, 3, 4).filter { it <= sizeMaxFlavors }) {
                        val isSel = flavorCount == n
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { handleFlavorCountChange(n) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSel) Color(0xFFFFF0E6) else Color.White,
                            border = BorderStroke(
                                2.dp,
                                if (isSel) ItaSuperPrimary else Color(0xFFEEEEEE)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$n",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = ItaSuperTextPrimary
                                )
                                Text(
                                    text = if (n == 2) "Meio a meio" else "$n sabores",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${fractionLabels[n]} cada",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Etapas de sabor
            if (isFlavorStep) {
                if (!catalogActive && hasSizes && step == 1) {
                    Text(
                        text = "Escolha o tamanho",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ItaSuperTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (sizeName in availableSizes) {
                            val isSel = selectedSize == sizeName
                            Surface(
                                modifier = Modifier.clickable { selectedSize = sizeName },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFFFFF0E6) else Color(0xFFFAFAFA),
                                border = BorderStroke(2.dp, if (isSel) ItaSuperPrimary else Color.Transparent)
                            ) {
                                Text(
                                    text = sizeName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSel) ItaSuperPrimary else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "O preço de cada sabor pode variar conforme o tamanho.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                }

                val chosenElsewhere = productIds
                    .mapIndexedNotNull { i, id -> if (i != currentFlavorIdx) id else null }
                    .toSet()
                for ((section, items) in groupedFlavors) {
                    val visible = items.filter { it.id !in chosenElsewhere }
                    if (visible.isEmpty()) continue
                    if (section != null) {
                        Text(
                            text = section.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    for (product in visible) {
                        val isSelected = product.id == productIds[currentFlavorIdx]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { setCurrentFlavor(product.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color(0xFFFFF0E6) else Color.White,
                            border = BorderStroke(
                                2.dp,
                                if (isSelected) ItaSuperPrimary else Color(0xFFEEEEEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (product.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = product.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) ItaSuperPrimary else Color(0xFFE0E0E0),
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        if (isSelected) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = ItaSuperTextPrimary
                                    )
                                    if (product.description.isNotBlank()) {
                                        Text(
                                            text = product.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = formatBRL(priceForFlavor(product)),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = ItaSuperPrimary
                                )
                            }
                        }
                    }
                }
                if (flavorProducts.isEmpty()) {
                    Text(
                        text = "Nenhum sabor cadastrado ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
                // Observações na última etapa de sabor (quando não há adicionais/borda).
                if (step == flavorCount && !hasAddons && !hasBorders && !hasComplements &&
                    productIds[currentFlavorIdx] != null
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ObservationsField(observations, { observations = it })
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Etapa de adicionais (pizza)
            if (step == addonStep && hasAddons) {
                for (group in addonGroups) {
                    val countInGroup = group.items.count { selectedAddonIds.contains(it.id) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildString {
                                append(group.name)
                                if (group.minSelect > 0) append(" (mín. ${group.minSelect})")
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = ItaSuperTextPrimary
                        )
                        if (group.maxSelect < 99) {
                            Text(
                                text = "$countInGroup/${group.maxSelect}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                    for (item in group.items) {
                        val isSel = selectedAddonIds.contains(item.id)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { toggleAddon(group, item.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) Color(0xFFFFF0E6) else Color.White,
                            border = BorderStroke(
                                2.dp,
                                if (isSel) ItaSuperPrimary else Color(0xFFEEEEEE)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSel) ItaSuperPrimary else Color.Transparent,
                                    border = BorderStroke(
                                        2.dp,
                                        if (isSel) ItaSuperPrimary else Color(0xFFBDBDBD)
                                    ),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    if (isSel) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ItaSuperTextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = if (item.price > 0) "+ ${formatBRL(item.price)}" else "Grátis",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = ItaSuperPrimary
                                )
                            }
                        }
                    }
                }
                if (!hasBorders) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ObservationsField(observations, { observations = it })
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Etapa de complementos (pastel)
            if (step == complementStep && hasComplements) {
                Text(
                    text = "Selecione até $maxComplements complementos. Toque novamente para remover.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                for (c in borders) {
                    val isSel = selectedComplementIds.contains(c.id)
                    val atLimit = !isSel && selectedComplementIds.size >= maxComplements
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !atLimit) { toggleComplement(c.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFFFFF0E6) else Color.White,
                        border = BorderStroke(
                            2.dp,
                            if (isSel) ItaSuperPrimary else Color(0xFFEEEEEE)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) ItaSuperPrimary else Color.Transparent,
                                border = BorderStroke(
                                    2.dp,
                                    if (isSel) ItaSuperPrimary else Color(0xFFBDBDBD)
                                ),
                                modifier = Modifier.size(22.dp)
                            ) {
                                if (isSel) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = c.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (atLimit) Color.Gray else ItaSuperTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (c.price > 0) "+ ${formatBRL(c.price)}" else "Grátis",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = ItaSuperPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                ObservationsField(observations, { observations = it })
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Etapa de borda (pizza)
            if (step == borderStep && hasBorders) {
                for (border in borders) {
                    val isSel = border.id == selectedBorderId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedBorderId = border.id },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFFFFF0E6) else Color.White,
                        border = BorderStroke(
                            2.dp,
                            if (isSel) ItaSuperPrimary else Color(0xFFEEEEEE)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSel) ItaSuperPrimary else Color.Transparent,
                                border = BorderStroke(
                                    2.dp,
                                    if (isSel) ItaSuperPrimary else Color(0xFFBDBDBD)
                                ),
                                modifier = Modifier.size(22.dp)
                            ) {
                                if (isSel) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = border.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ItaSuperTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (border.price > 0) "+ ${formatBRL(border.price)}" else "Grátis",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Black
                                ),
                                color = ItaSuperPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                ObservationsField(observations, { observations = it })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Barra inferior fixa
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!isFinalStep) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (step > 0) {
                            Button(
                                onClick = { handleBack() },
                                modifier = Modifier.height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF0F0F0),
                                    contentColor = ItaSuperTextPrimary
                                )
                            ) {
                                Text("← Anterior", fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = { handleNext() },
                            enabled = canAdvance(),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ItaSuperPrimary,
                                disabledContainerColor = Color(0xFFE0E0E0)
                            )
                        ) {
                            Text(
                                "Próximo →",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { handleBack() },
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF0F0F0),
                                contentColor = ItaSuperTextPrimary
                            )
                        ) {
                            Text("← Anterior", fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0F0F0),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    enabled = quantity > 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Remove,
                                        contentDescription = "Diminuir"
                                    )
                                }
                                Text(
                                    text = "$quantity",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { quantity++ }) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Aumentar"
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { handleAdd() },
                        enabled = hasBorders.not() || selectedBorderId != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ItaSuperPrimary,
                            disabledContainerColor = Color(0xFFE0E0E0)
                        )
                    ) {
                        Text(
                            text = "Adicionar • ${formatBRL(lineTotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Campo de observações reutilizado nas etapas finais. */
@Composable
private fun ObservationsField(value: String, onChange: (String) -> Unit) {
    Text(
        text = "Observações",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = ItaSuperTextPrimary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 200) onChange(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Ex: Sem cebola...") },
        maxLines = 3,
        shape = RoundedCornerShape(12.dp)
    )
}

