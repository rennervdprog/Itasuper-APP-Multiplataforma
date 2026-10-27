package com.example.shared.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.shared.model.Store
import com.example.shared.model.StoreOpeningHour
import com.example.shared.network.SupabaseApi
import com.example.shared.session.FavoriteStoresRepository
import com.example.shared.theme.ItaSuperPrimary
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Tela "Sobre a loja" — layout IDÊNTICO ao StoreInfoScreen do app Android original
 * (mesma estrutura, textos, ordem, espaçamentos em dp, fontes em sp e cores do tema ItaSuper).
 *
 * Dados reais (Fase E): avaliações vêm de store_rating_aggregates + reviews
 * públicas; favorito persiste localmente (FavoriteStoresRepository).
 * Diferença documentada: abrir o mapa (geo:) e o WhatsApp usavam Intents
 * Android no original; aqui o toque no mapa/rota chama [onOpenMap].
 */

// ---------------------------------------------------------------------------
// Dados da aba "Avaliações" (carregados da API real no corpo da tela)
// ---------------------------------------------------------------------------

/** Resumo de avaliações da loja. [dist] = contagens de 5→1 estrelas. */
data class StoreRatingSummary(
    val avg: Double = 0.0,
    val count: Int = 0,
    val dist: List<Int> = emptyList()
)

data class StoreReview(
    val authorName: String = "Cliente",
    val rating: Int = 5,
    val comment: String = "",
    /** ISO-8601 da criação (ex.: "2026-10-17T10:00:00+00:00"); exibido como "há 2 h". */
    val createdAtIso: String = "",
    val storeResponse: String = ""
)

// ---------------------------------------------------------------------------
// Tela
// ---------------------------------------------------------------------------

@Composable
fun StoreInfoScreen(
    store: Store,
    onBackClick: () -> Unit,
    onOpenMap: () -> Unit = {},
    isLoading: Boolean = false
) {
    var selectedTab by remember { mutableStateOf(1) } // 0 = Avaliações, 1 = Informações
    // Favorito REAL (persistência local, igual ao FavoriteStoresStorage).
    val favorites by FavoriteStoresRepository.favorites.collectAsState()
    val isFavorite = favorites.contains(store.id)
    // Avaliações REAIS (store_rating_aggregates + reviews públicas).
    var rating by remember(store.id) { mutableStateOf(StoreRatingSummary()) }
    var reviews by remember(store.id) { mutableStateOf<List<StoreReview>>(emptyList()) }
    var reviewsLoading by remember(store.id) { mutableStateOf(true) }
    LaunchedEffect(store.id) {
        reviewsLoading = true
        try {
            val aggregate = SupabaseApi.getStoreRatingAggregate(store.id)
            if (aggregate != null) {
                rating = StoreRatingSummary(
                    avg = aggregate.avgRating,
                    count = aggregate.reviewCount,
                    dist = listOf(5, 4, 3, 2, 1).map { aggregate.distribution[it] ?: 0 }
                )
            }
            reviews = SupabaseApi.getStoreReviews(store.id).map { review ->
                StoreReview(
                    authorName = review.authorName,
                    rating = review.rating,
                    comment = review.comment,
                    createdAtIso = review.createdAt,
                    storeResponse = review.responses.firstOrNull()?.message ?: ""
                )
            }
        } catch (_: Throwable) {
            // Sem avaliações carregadas: a aba mostra o estado vazio.
        }
        reviewsLoading = false
    }
    val address = store.displayAddress()
    val payments = store.paymentMethods()
    val hours = store.openingHours.sortedWith(
        compareBy<StoreOpeningHour> { it.brasiliaDayOrder() }.thenBy { it.openTime }
    )
    val aggAvgTop = rating.avg
    val aggCountTop = rating.count

    val listState = rememberLazyListState()
    // Padrao iFood (correcao do bug visual confirmada pelo Renner em 18/10/2026;
    // o app original tambem tinha a redundancia): a top bar e fixa e mostra
    // apenas voltar + favoritar. O nome da loja em maiusculas aparece na top bar
    // somente depois de rolar o cabecalho grande para fora da tela.
    val showTitle by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    Scaffold(
        containerColor = Color(0xFFF7F7F8),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F8))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = ItaSuperPrimary,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBackClick)
                        .padding(8.dp)
                )
                AnimatedVisibility(
                    visible = showTitle,
                    modifier = Modifier.weight(1f),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = store.name.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favoritar",
                    tint = ItaSuperPrimary,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable {
                            FavoriteStoresRepository.toggleFavorite(store.id)
                        }
                        .padding(8.dp)
                )
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = ItaSuperPrimary) }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                // Nome + logo + categoria • distância
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = store.name,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1A1A1A),
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    val cat = store.category.takeIf { it.isNotBlank() }
                                        ?.replaceFirstChar { it.uppercase() } ?: ""
                                    append(cat)
                                    val d = store.distanceKm ?: 0.0
                                    if (d > 0) {
                                        if (isNotEmpty()) append(" • ")
                                        append(formatDistance(d))
                                    }
                                },
                                fontSize = 13.sp,
                                color = Color(0xFF777777)
                            )
                        }
                        val logoUrl = store.logoUrl
                        if (logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = null,
                                modifier = Modifier.size(52.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = store.name.firstOrNull()?.uppercase() ?: "",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperPrimary
                                )
                            }
                        }
                    }
                }

                // Avaliação: nota + estrelas | nº avaliações
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (aggCountTop > 0) formatOneDecimal(aggAvgTop) else "–",
                                fontSize = 15.sp,
                                color = Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(5) { i ->
                                    Text(
                                        text = "★",
                                        fontSize = 15.sp,
                                        color = if (aggAvgTop > i) Color(0xFF1A1A1A) else Color(0xFFE0E0E0)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (aggCountTop == 1) "1 avaliação" else "$aggCountTop avaliações",
                            fontSize = 14.sp,
                            color = Color(0xFF777777)
                        )
                    }
                }

                // Abas: Avaliações | Informações
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.weight(1f).clickable { selectedTab = 0 }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Avaliações",
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == 0) ItaSuperPrimary else Color(0xFF999999)
                                )
                            }
                            if (selectedTab == 0) {
                                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ItaSuperPrimary))
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).clickable { selectedTab = 1 }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Informações",
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedTab == 1) ItaSuperPrimary else Color(0xFF999999)
                                )
                            }
                            if (selectedTab == 1) {
                                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(ItaSuperPrimary))
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                }

                // Conteúdo da aba Avaliações
                if (selectedTab == 0) {
                    item {
                        StoreReviewsContent(
                            rating = rating,
                            reviews = reviews,
                            isLoading = reviewsLoading
                        )
                    }
                }

                // Conteúdo da aba Informações — Opção A2 (Mapa Destaque)
                if (selectedTab == 1) {
                    val isOpen = store.isOpen

                    // Mapa hero com chip da loja
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp)
                                .height(190.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFDCEFE0))
                                // Toque abre o endereço no app de mapas
                                // (onOpenMap ligado no App via rememberUrlOpener).
                                .clickable { if (address.isNotBlank()) onOpenMap() }
                        ) {
                            // Pin central
                            Text(
                                text = "📍",
                                fontSize = 40.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                            // Chip da loja
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val logoUrl = store.logoUrl
                                if (logoUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = logoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1A1A1A)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = store.name.firstOrNull()?.uppercase() ?: "",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ItaSuperPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = store.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Text(
                                        text = buildString {
                                            val cat = store.category.takeIf { it.isNotBlank() }
                                                ?.replaceFirstChar { it.uppercase() } ?: ""
                                            append(cat)
                                            val d = store.distanceKm ?: 0.0
                                            if (d > 0) {
                                                if (isNotEmpty()) append(" · ")
                                                append(formatDistance(d))
                                            }
                                        },
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF777777)
                                    )
                                }
                            }
                            // Botão traçar rota
                            if (address.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(14.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFF1A1A1A))
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Traçar rota →",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Card Funcionamento
                    item {
                        InfoCard(title = "🕐", titleText = "Funcionamento") {
                            // Status pill
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(
                                        if (isOpen) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isOpen) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )
                                )
                                Spacer(modifier = Modifier.width(9.dp))
                                Text(
                                    text = if (isOpen) "Aberto agora" else "Fechado agora",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOpen) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                val nextInfo = nextOpeningInfo(hours, isOpen)
                                if (nextInfo.isNotBlank()) {
                                    Text(
                                        text = nextInfo,
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF777777)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Horários agrupados
                            val grouped = groupHoursByRange(hours)
                            if (grouped.isEmpty()) {
                                Text(
                                    text = "Horários não informados pela loja.",
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF999999)
                                )
                            } else {
                                grouped.forEach { (label, range) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF777777)
                                        )
                                        Text(
                                            text = range,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1A1A1A)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card Pagamento
                    item {
                        InfoCard(title = "💳", titleText = "Pagamento") {
                            if (payments.isEmpty()) {
                                Text(
                                    text = "Formas de pagamento não informadas pela loja.",
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF999999)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    payments.chunked(2).forEach { rowMethods ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowMethods.forEach { m ->
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(13.dp))
                                                        .background(Color(0xFFF9FAFB))
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    PaymentBadge(method = m)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = paymentDisplayName(m),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF374151)
                                                    )
                                                }
                                            }
                                            // Preenche a grade se ímpar
                                            if (rowMethods.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card Endereço
                    item {
                        InfoCard(title = "📍", titleText = "Endereço") {
                            Text(
                                text = address.ifBlank { "Endereço não informado pela loja." },
                                fontSize = 14.sp,
                                color = Color(0xFF374151),
                                lineHeight = 22.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } // fim if (selectedTab == 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Componentes privados
// ---------------------------------------------------------------------------

@Composable
private fun InfoCard(
    title: String,
    titleText: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, fontSize = 17.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = titleText,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A),
                letterSpacing = (-0.2).sp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun PaymentBadge(method: String) {
    val m = method.lowercase()
    val (bg, label) = when {
        m.contains("visa") -> Color(0xFF1A1F71) to "VISA"
        m.contains("master") -> Color(0xFFEB001B) to "MC"
        m.contains("elo") -> Color(0xFF0066B3) to "ELO"
        m.contains("hiper") -> Color(0xFFED1C24) to "HP"
        m.contains("amex") || m.contains("american") -> Color(0xFF2E77BC) to "AMEX"
        m.contains("pix") -> Color(0xFF32BCAD) to "PIX"
        m.contains("dinheiro") -> Color(0xFF4CAF50) to "R$"
        m.contains("debito") || m.contains("débito") -> Color(0xFF5C6BC0) to "DÉB"
        m.contains("credito") || m.contains("crédito") -> Color(0xFF7B1FA2) to "CRÉD"
        else -> Color(0xFF9E9E9E) to method.take(2).uppercase()
    }
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
private fun StoreReviewsContent(
    rating: StoreRatingSummary,
    reviews: List<StoreReview>,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ItaSuperPrimary)
            }
            return
        }

        val avgRating = rating.avg
        val reviewCount = rating.count

        if (reviewCount == 0) {
            // Estado vazio
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "★", fontSize = 40.sp, color = Color(0xFFDDDDDD))
                Text(
                    text = "Nenhuma avaliação ainda",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "Seja o primeiro a avaliar esta loja!",
                    fontSize = 13.sp,
                    color = Color(0xFF999999)
                )
            }
            return
        }

        // Resumo: nota + histograma
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatOneDecimal(avgRating),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..5).forEach { star ->
                        Text(
                            text = if (star <= avgRating.toInt()) "★" else "☆",
                            fontSize = 14.sp,
                            color = Color(0xFFFFA000)
                        )
                    }
                }
                Text(
                    text = "$reviewCount avaliações",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val dists = listOf(
                    5 to rating.dist.getOrElse(0) { 0 },
                    4 to rating.dist.getOrElse(1) { 0 },
                    3 to rating.dist.getOrElse(2) { 0 },
                    2 to rating.dist.getOrElse(3) { 0 },
                    1 to rating.dist.getOrElse(4) { 0 }
                )
                val maxDist = dists.maxOf { it.second }.coerceAtLeast(1)
                dists.forEach { (star, count) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "$star", fontSize = 11.sp, color = Color(0xFF999999))
                        Box(
                            modifier = Modifier.weight(1f).height(6.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxHeight()
                                    .fillMaxWidth(count.toFloat() / maxDist)
                                    .background(
                                        if (star >= 4) Color(0xFFFFA000) else Color(0xFFCCCCCC),
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                        Text(text = "$count", fontSize = 11.sp, color = Color(0xFF999999))
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

        // Lista de avaliações
        reviews.forEachIndexed { i, review ->
            val authorName = review.authorName.ifBlank { "Cliente" }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = authorName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = formatRelativeTime(review.createdAtIso),
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..5).forEach { star ->
                        Text(
                            text = if (star <= review.rating) "★" else "☆",
                            fontSize = 13.sp,
                            color = Color(0xFFFFA000)
                        )
                    }
                }
                if (review.comment.isNotBlank() && review.comment != "null") {
                    Text(text = review.comment, fontSize = 14.sp, color = Color(0xFF1A1A1A))
                }
                if (review.storeResponse.isNotBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Resposta da loja",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ItaSuperPrimary
                        )
                        Text(text = review.storeResponse, fontSize = 13.sp, color = Color(0xFF444444))
                    }
                }
            }
            if (i < reviews.size - 1) {
                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ---------------------------------------------------------------------------
// Helpers (ports fiéis do original; sem java.*/android.*)
// ---------------------------------------------------------------------------

private fun formatDistance(km: Double): String {
    if (km < 1) return "${(km * 1000).toInt()} m"
    val tenths = ((km * 10) + 0.5).toInt() // km >= 0: arredonda 1 casa, como "%.1f"
    return "${tenths / 10},${tenths % 10} km"
}

/** "4.7" -> "4,7" (substitui String.format("%.1f", v).replace(".", ",")). */
private fun formatOneDecimal(value: Double): String {
    val tenths = ((value * 10) + 0.5).toInt()
    return "${tenths / 10},${tenths % 10}"
}

private fun formatTimeShort(time: String): String {
    // "08:00:00" -> "08:00"
    return time.trim().split(":").take(2).joinToString(":")
}

private fun isToday(h: StoreOpeningHour): Boolean {
    // Calendar.DAY_OF_WEEK - 1 do original == 0(Dom)..6(Sáb); DayOfWeek.ordinal: Seg=0..Dom=6.
    val dow = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek
    val todayDow = (dow.ordinal + 1) % 7
    return h.dayOfWeek == todayDow
}

private fun formatRelativeTime(iso: String): String {
    return try {
        // Instant.parse entende "Z" e offsets ("+00:00") do timestamptz do PostgREST.
        val instant = kotlinx.datetime.Instant.parse(iso.trim())
        val diffMs = (Clock.System.now() - instant).inWholeMilliseconds
        if (diffMs < 0) return ""
        val minutes = diffMs / 60000
        val hours = minutes / 60
        val days = hours / 24
        when {
            minutes < 1 -> "agora mesmo"
            minutes < 60 -> "há $minutes min"
            hours < 24 -> "há $hours h"
            days < 7 -> if (days == 1L) "há 1 dia" else "há $days dias"
            days < 30 -> { val w = days / 7; if (w == 1L) "há 1 semana" else "há $w semanas" }
            days < 365 -> { val m = days / 30; if (m == 1L) "há 1 mês" else "há $m meses" }
            else -> { val y = days / 365; if (y == 1L) "há 1 ano" else "há $y anos" }
        }
    } catch (e: Exception) {
        ""
    }
}

private fun nextOpeningInfo(hours: List<StoreOpeningHour>, isOpen: Boolean): String {
    if (isOpen || hours.isEmpty()) return ""
    // Simplificado: informa o próximo horário de hoje ou amanhã
    val today = hours.firstOrNull { isToday(it) }
    return if (today != null && !today.isClosedAllDay) {
        "Abre hoje às ${formatTimeShort(today.openTime)}"
    } else {
        "Abre amanhã às 08:00"
    }
}

private fun groupHoursByRange(hours: List<StoreOpeningHour>): List<Pair<String, String>> {
    if (hours.isEmpty()) return emptyList()
    // Agrupa dias consecutivos com o mesmo horário
    val sorted = hours.sortedBy { it.brasiliaDayOrder() }
    val result = mutableListOf<Pair<String, String>>()
    var groupStart = sorted[0]
    var groupEnd = sorted[0]
    fun rangeLabel(start: StoreOpeningHour, end: StoreOpeningHour): String {
        val s = start.brasiliaDayLabel()
        val e = end.brasiliaDayLabel()
        return if (s == e) s else "$s – $e"
    }
    fun timeRange(h: StoreOpeningHour): String {
        return if (h.isClosedAllDay) "Fechado"
        else "${formatTimeShort(h.openTime)} – ${formatTimeShort(h.closeTime)}"
    }
    for (i in 1 until sorted.size) {
        val h = sorted[i]
        if (h.brasiliaDayOrder() == groupEnd.brasiliaDayOrder() + 1 && timeRange(h) == timeRange(groupEnd)) {
            groupEnd = h
        } else {
            result.add(rangeLabel(groupStart, groupEnd) to timeRange(groupEnd))
            groupStart = h
            groupEnd = h
        }
    }
    result.add(rangeLabel(groupStart, groupEnd) to timeRange(groupEnd))
    // Expande abreviações para nomes completos curtos
    return result.map { (label, range) ->
        val full = label
            .replace("Seg", "Segunda").replace("Ter", "Terça")
            .replace("Qua", "Quarta").replace("Qui", "Quinta")
            .replace("Sex", "Sexta").replace("Sáb", "Sábado")
            .replace("Dom", "Domingo")
        full to range
    }
}

private fun paymentDisplayName(method: String): String {
    val m = method.lowercase()
    return when {
        m.contains("pix") && m.contains("online") -> "Pix online"
        m.contains("pix") && (m.contains("maquininha") || m.contains("machine")) -> "Pix na maquininha"
        m.contains("pix") -> "Pix"
        m.contains("visa") -> "Visa"
        m.contains("master") -> "Mastercard"
        m.contains("elo") -> "Elo"
        m.contains("hiper") -> "Hipercard"
        m.contains("amex") || m.contains("american") -> "Amex"
        m.contains("dinheiro") -> "Dinheiro"
        m.contains("debito") || m.contains("débito") -> "Débito"
        m.contains("credito") || m.contains("crédito") -> "Crédito"
        else -> method.replaceFirstChar { it.uppercase() }
    }
}

private fun Store.paymentMethods(): List<String> = buildList {
    // Fase 2 — mesmos 3 métodos do checkout: "PIX Online" e "PIX na
    // Maquininha" não são mais oferecidos no app (decisão do Renner).
    if (pixDirectEnabled && pixDirectKeyConfigured) add("PIX Direto")
    if (settings.acceptCard) add("Cartão")
    if (settings.acceptCash) add("Dinheiro")
}

private fun Store.displayAddress(): String {
    if (address.isNotBlank()) return address
    return listOfNotNull(
        addressStreet.takeIf { it.isNotBlank() },
        addressNumber.takeIf { it.isNotBlank() },
        addressNeighborhood.takeIf { it.isNotBlank() },
        addressCity.takeIf { it.isNotBlank() },
        addressState.takeIf { it.isNotBlank() },
        addressCep.takeIf { it.isNotBlank() }
    ).joinToString(", ")
}

private fun StoreOpeningHour.brasiliaDayOrder(): Int = when (brasiliaDayLabel()) {
    "Dom" -> 0
    "Seg" -> 1
    "Ter" -> 2
    "Qua" -> 3
    "Qui" -> 4
    "Sex" -> 5
    "Sáb" -> 6
    else -> 7
}

private fun StoreOpeningHour.brasiliaDayLabel(): String {
    val raw = dayOfWeekStr.trim().lowercase()
        .replace("ç", "c")
        .replace("á", "a")
        .replace("ã", "a")
        .replace("é", "e")
        .replace("-feira", "")
    return when {
        raw.startsWith("dom") || raw == "0" || dayOfWeek == 0 -> "Dom"
        raw.startsWith("seg") || raw == "1" || dayOfWeek == 1 -> "Seg"
        raw.startsWith("ter") || raw == "2" || dayOfWeek == 2 -> "Ter"
        raw.startsWith("qua") || raw == "3" || dayOfWeek == 3 -> "Qua"
        raw.startsWith("qui") || raw == "4" || dayOfWeek == 4 -> "Qui"
        raw.startsWith("sex") || raw == "5" || dayOfWeek == 5 -> "Sex"
        raw.startsWith("sab") || raw == "6" || dayOfWeek == 6 -> "Sáb"
        else -> dayOfWeekStr.takeIf { it.isNotBlank() } ?: ""
    }
}
