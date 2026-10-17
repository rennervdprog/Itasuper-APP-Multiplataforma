package com.example.shared.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.DiscoverProduct
import com.example.shared.model.Store
import com.example.shared.theme.ItaSuperBackground
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperTextPrimary
import com.example.shared.theme.ItaSuperTextSecondary
import com.example.shared.ui.mock.mockCategories
import com.example.shared.ui.mock.mockProducts
import com.example.shared.ui.mock.mockStores

/**
 * Tela Descobrir/Busca — migração fase 3b do SearchScreen do app Android.
 * Versão com dados mockados (a rede real vem na próxima fase).
 * Mantém: campo de busca, filtros rápidos, grade de categorias, lista de resultados.
 */

// TODO(fase-rede): trocar mocks por dados do SupabaseClient
private val recentSearches = listOf("pastel", "pizza", "marmita")

enum class QuickFilter(val label: String) {
    FREE_DELIVERY("Entrega grátis"),
    OPEN_NOW("Aberto agora"),
    TOP_RATED("Melhor avaliados"),
    FAST("Entrega rápida")
}

fun formatSearchRating(rating: Double): String {
    val rounded = kotlin.math.round(rating * 10) / 10.0
    val int = rounded.toInt()
    return if (rounded == int.toDouble()) "$int.0" else rounded.toString()
}

fun formatMoney(value: Double): String {
    val total = kotlin.math.round(value * 100).toLong()
    val reais = total / 100
    val cents = (total % 100).toString().padStart(2, '0')
    return "R$ $reais,$cents"
}

@Composable
fun SearchScreen(onStoreClick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<QuickFilter?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val searching = query.length >= 2 || selectedCategory != null || selectedFilter != null

    val filteredStores = remember(query, selectedCategory, selectedFilter) {
        mockStores.filter { store ->
            val q = query.trim().lowercase()
            val matchQuery = q.length < 2 ||
                store.name.lowercase().contains(q) ||
                store.category.lowercase().contains(q)
            val matchCategory = selectedCategory == null || store.category == selectedCategory
            val matchFilter = when (selectedFilter) {
                null -> true
                QuickFilter.FREE_DELIVERY -> store.isFreeDelivery
                QuickFilter.OPEN_NOW -> store.isOpen
                QuickFilter.TOP_RATED -> store.rating >= 4.5
                QuickFilter.FAST -> store.deliveryTime.startsWith("20") || store.deliveryTime.startsWith("25")
            }
            matchQuery && matchCategory && matchFilter
        }
    }
    val filteredProducts = remember(query, selectedFilter) {
        mockProducts.filter { product ->
            val q = query.trim().lowercase()
            (q.length < 2 ||
                product.name.lowercase().contains(q) ||
                product.storeName.lowercase().contains(q))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ItaSuperBackground)
    ) {
        SearchAppBar(
            query = query,
            onQueryChange = { query = it },
            onClearQuery = { query = "" }
        )
        if (searching) {
            SearchResults(
                stores = filteredStores,
                products = filteredProducts,
                onClearAll = {
                    query = ""
                    selectedCategory = null
                    selectedFilter = null
                },
                onStoreClick = onStoreClick
            )
        } else {
            QuickFilterChips(
                selected = selectedFilter,
                onClick = { selectedFilter = if (selectedFilter == it) null else it }
            )
            CategoryGrid(
                selected = selectedCategory,
                onSelect = { selectedCategory = if (selectedCategory == it) null else it }
            )
            RecentSearches(
                onRecentSearch = { query = it }
            )
        }
    }
}

@Composable
private fun SearchAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Entregar em",
                style = TextStyle(fontSize = 12.sp, color = ItaSuperTextSecondary)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Araruama, RJ",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperTextPrimary
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Buscar lojas e pratos", fontSize = 14.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Buscar", tint = ItaSuperTextSecondary)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Limpar", tint = ItaSuperTextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuickFilterChips(
    selected: QuickFilter?,
    onClick: (QuickFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(QuickFilter.entries.toList()) { filter ->
            val isSelected = selected == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(
                        width = 1.5.dp,
                        color = if (isSelected) ItaSuperPrimary else Color(0xFFEEE3D6),
                        shape = RoundedCornerShape(50)
                    )
                    .background(if (isSelected) ItaSuperPrimary else Color.Transparent)
                    .clickable { onClick(filter) }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
            ) {
                Text(
                    text = filter.label,
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else ItaSuperTextPrimary
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    selected: String?,
    onSelect: (String) -> Unit
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth().height(168.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(mockCategories) { category ->
            val isSelected = selected == category
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(62.dp).clickable { onSelect(category) }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF4EDE5))
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = if (isSelected) ItaSuperPrimary else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.take(1),
                        style = TextStyle(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = ItaSuperPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category,
                    style = TextStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = if (isSelected) ItaSuperPrimary else ItaSuperTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentSearches(onRecentSearch: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = "Buscas recentes",
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ItaSuperTextPrimary),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
        recentSearches.forEach { term ->
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
                    tint = ItaSuperTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = term,
                    style = TextStyle(fontSize = 14.sp, color = ItaSuperTextPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    stores: List<Store>,
    products: List<DiscoverProduct>,
    onClearAll: () -> Unit,
    onStoreClick: (String) -> Unit
) {
    if (stores.isEmpty() && products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Nenhuma loja bate com esse filtro. Toca de novo no filtro pra limpar.",
                    style = TextStyle(fontSize = 14.sp, color = ItaSuperTextSecondary)
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = "Voltar para Descobrir",
                    modifier = Modifier.clickable(onClick = onClearAll),
                    style = TextStyle(
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
                modifier = Modifier.clickable(onClick = onClearAll).padding(horizontal = 18.dp, vertical = 12.dp),
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ItaSuperPrimary)
            )
        }
        if (stores.isNotEmpty()) {
            item {
                Text(
                    text = "Lojas",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ItaSuperTextPrimary),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
            items(stores, key = { it.id }) { store ->
                StoreResultCard(store = store, onClick = { onStoreClick(store.id) })
            }
        }
        if (products.isNotEmpty()) {
            item {
                Text(
                    text = "Pratos",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ItaSuperTextPrimary),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
            items(products, key = { it.id }) { product ->
                ProductResultRow(product = product)
            }
        }
    }
}

@Composable
private fun StoreResultCard(store: Store, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFFE9AD)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = store.name.trim().firstOrNull()?.uppercase() ?: "I",
                style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = ItaSuperPrimary)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = store.name,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ItaSuperTextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFE8930C),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${formatSearchRating(store.rating)} \u00b7 ${store.category} \u00b7 ${store.deliveryTime}",
                    style = TextStyle(fontSize = 12.sp, color = ItaSuperTextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (!store.isOpen) "Fechada no momento" else store.deliveryFee,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (!store.isOpen) Color(0xFFC53D43) else ItaSuperTextSecondary
                )
            )
        }
    }
}

@Composable
private fun ProductResultRow(product: DiscoverProduct) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF4EDE5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = product.name.trim().firstOrNull()?.uppercase() ?: "P",
                style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = ItaSuperPrimary)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ItaSuperTextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.storeName,
                style = TextStyle(fontSize = 12.sp, color = ItaSuperTextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = formatMoney(product.price),
            style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = ItaSuperPrimary)
        )
    }
}
