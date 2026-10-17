package com.example.shared.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.Banner
import com.example.shared.model.Store
import com.example.shared.theme.ItaSuperBackground
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperTextSecondary

/**
 * Tela inicial — migração fase 3a do HomeScreen do app Android.
 * Versão com dados mockados (a rede real vem na próxima fase).
 * Visual segue o original: cabeçalho com localização, busca,
 * categorias, banners e lista de lojas.
 */

// TODO(fase-rede): trocar mocks por dados do SupabaseClient
private val MOCK_CATEGORIES = listOf(
    "Lanches", "Pizza", "Japonesa", "Marmitas", "Doces", "Bebidas", "Saudável", "Padaria"
)

private val MOCK_BANNERS = listOf(
    Banner(id = "b1", title = "50% OFF na primeira compra", imageUrl = ""),
    Banner(id = "b2", title = "Frete grátis acima de R$ 35", imageUrl = "")
)

private val MOCK_STORES = listOf(
    Store(id = "s1", name = "Pastelão Carioca", category = "Lanches", rating = 4.8,
          deliveryTime = "25-35 min", deliveryFee = "R$ 4,90", isOpen = true),
    Store(id = "s2", name = "Doceria Da Cleide", category = "Doces", rating = 4.9,
          deliveryTime = "30-40 min", deliveryFee = "R$ 3,90", isOpen = true),
    Store(id = "s3", name = "Sushi Araruama", category = "Japonesa", rating = 4.7,
          deliveryTime = "40-55 min", deliveryFee = "R$ 6,90", isOpen = false)
)

@Composable
fun HomeScreen(
    onSearchClick: () -> Unit = {},
    onStoreClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ItaSuperBackground),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item { HomeHeader() }
        item { HomeSearchBar(onClick = onSearchClick) }
        item { HomeCategories() }
        item { HomeBanners(MOCK_BANNERS) }
        item {
            Text(
                text = "Lojas em destaque",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        items(MOCK_STORES) { store ->
            StoreCard(store = store, onClick = { onStoreClick(store.id) })
        }
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Localização",
                tint = ItaSuperPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(text = "Entregar em", fontSize = 12.sp, color = ItaSuperTextSecondary)
                Text(text = "Araruama, RJ", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = "Avisos",
            tint = ItaSuperTextSecondary,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun HomeSearchBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Buscar",
                tint = ItaSuperTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Buscar lojas e pratos...", fontSize = 14.sp, color = ItaSuperTextSecondary)
        }
    }
}

@Composable
private fun HomeCategories() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MOCK_CATEGORIES) { category ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(text = category, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeBanners(banners: List<Banner>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(banners) { banner ->
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .height(140.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ItaSuperPrimary)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = banner.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreCard(store: Store, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo placeholder (imagem real vem com coil3 numa fase futura)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ItaSuperPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = store.name.take(1),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = ItaSuperPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = store.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = store.category, fontSize = 13.sp, color = ItaSuperTextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "%.1f".format(store.rating),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = store.deliveryTime,
                        fontSize = 12.sp,
                        color = ItaSuperTextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = store.deliveryFee,
                        fontSize = 12.sp,
                        color = ItaSuperTextSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (store.isOpen) Color(0xFF1EAE53).copy(alpha = 0.12f)
                        else Color.Gray.copy(alpha = 0.12f)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (store.isOpen) "Aberta" else "Fechada",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (store.isOpen) Color(0xFF1EAE53) else Color.Gray
                )
            }
        }
    }
}
