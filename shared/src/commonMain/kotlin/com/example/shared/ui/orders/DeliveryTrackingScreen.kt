package com.example.shared.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Motorcycle
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shared.model.DriverLocation
import com.example.shared.model.Order
import com.example.shared.theme.ItaSuperBorder
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.ui.mock.mockDriverLocation
import com.example.shared.ui.mock.mockOrders
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant

// =============================================================================
// LIMITAÇÕES KMP DOCUMENTADAS (prévia com mocks; sem backend ainda):
// - Mapa: o original usa WebView + Leaflet (HTML gerado uma vez + updates via
//   evaluateJavascript). WebView não é portável ao commonMain: aqui há um
//   placeholder visual (pinos do entregador/cliente) com TODO(mapa-nativo) para
//   um expect/actual futuro (Android: WebView/Leaflet; iOS: MapKit/Google Maps).
// - Localização do entregador: o original busca em driver_locations via
//   SupabaseClient a cada 20s. Aqui mockDriverLocation() com TODO(fase-rede);
//   a estrutura do polling (LaunchedEffect + delay 20s) foi mantida 1:1.
// - Sem OrdersViewModel no KMP ainda: o pedido é resolvido da lista passada
//   (default: mockOrders()). Na fase de rede, virá do estado real de pedidos.
// =============================================================================

// TODO(fase-rede): trocar mockOrders pela lista real de pedidos (estado do app / Ktor).

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryTrackingScreen(
    orderId: String,
    orders: List<Order> = mockOrders(),
    onBack: () -> Unit
) {
    val order = remember(orderId, orders) { orders.find { it.id == orderId } }
    var driverLocation by remember { mutableStateOf<DriverLocation?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // TODO(fase-rede): trocar mockDriverLocation por SupabaseClient.fetchDriverLocation
    // (tabela driver_locations) via Ktor. Polling de 20s mantido como no original.
    LaunchedEffect(order?.id) {
        val o = order ?: return@LaunchedEffect
        // Primeira busca imediata
        driverLocation = mockDriverLocation(o.id)
        isLoading = false
        // Atualiza a cada 20 segundos
        while (isActive) {
            delay(20_000)
            mockDriverLocation(o.id)?.let { driverLocation = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acompanhar entrega", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (order == null) {
                // Pedido ainda não carregado na lista - mostra loading em vez de tela em branco
                // (KMP: sem OrdersViewModel para recarregar; a lista real chega na fase de rede).
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ItaSuperPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Carregando pedido...", color = Color(0xFF737373))
                    }
                }
            } else {
                // Card com dados do pedido
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Store, null, tint = ItaSuperPrimary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                order.storeName.ifBlank { "Loja" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = Color(0xFF737373), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                order.deliveryAddress.ifBlank { "Endereço de entrega" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5D5D5D)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Motorcycle, null, tint = ItaSuperPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(
                                    if (driverLocation != null) "Entregador a caminho" else "Buscando localização do entregador...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ItaSuperPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                driverLocation?.updatedAt?.takeIf { it.isNotBlank() }?.let { updatedAt ->
                                    Text(
                                        text = "Atualizado ${formatRelativeTime(updatedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Mapa
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ItaSuperPrimary)
                        }
                    } else {
                        TrackingMapPlaceholder(
                            driverLocation = driverLocation,
                            hasClientPosition = order.clientLatitude != null && order.clientLongitude != null
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Placeholder visual do mapa de rastreio (mesmas dimensões do original: 420.dp).
 * Pino azul do entregador e pino verde do cliente, nas cores do HTML Leaflet original.
 */
// TODO(mapa-nativo): substituir por expect/actual nativo — Android: WebView + Leaflet como no
// original (HTML gerado uma vez, updates via evaluateJavascript); iOS: MapKit/Google Maps.
@Composable
private fun TrackingMapPlaceholder(
    driverLocation: DriverLocation?,
    hasClientPosition: Boolean
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE9F1E5))
            .border(1.dp, ItaSuperBorder, RoundedCornerShape(16.dp))
    ) {
        val w = maxWidth
        val h = maxHeight
        // "Ruas" estilizadas para sugerir o mapa
        Box(
            modifier = Modifier
                .offset(x = w * 0.08f, y = h * 0.42f)
                .width(w * 0.84f)
                .height(12.dp)
                .background(Color.White.copy(alpha = 0.85f))
        )
        Box(
            modifier = Modifier
                .offset(x = w * 0.55f, y = h * 0.06f)
                .width(12.dp)
                .height(h * 0.88f)
                .background(Color.White.copy(alpha = 0.85f))
        )
        if (driverLocation != null) {
            MapPin(
                modifier = Modifier.offset(x = w * 0.20f, y = h * 0.24f),
                backgroundColor = Color(0xFF2563EB),
                label = "Entregador"
            ) {
                Icon(Icons.Filled.Motorcycle, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        if (hasClientPosition) {
            MapPin(
                modifier = Modifier.offset(x = w * 0.62f, y = h * 0.58f),
                backgroundColor = Color(0xFF16A34A),
                label = "Você"
            ) {
                Icon(Icons.Filled.LocationOn, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF262626).copy(alpha = 0.78f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                "Mapa interativo em breve",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MapPin(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    label: String,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) { content() }
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF414141),
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Formata timestamp ISO em tempo relativo (ex: "há 2 min").
 * KMP: kotlinx-datetime no lugar do SimpleDateFormat do original (que usava
 * java.text, indisponível no commonMain).
 */
private fun formatRelativeTime(isoTimestamp: String): String {
    return try {
        val normalized = isoTimestamp
            .replace(Regex("\\.\\d+"), "")
            .replace("Z", "")
            .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
            .take(19)
        if (normalized.length < 16) return ""
        val instant = LocalDateTime.parse(normalized).toInstant(UtcOffset.ZERO)
        val minutes = (Clock.System.now().toEpochMilliseconds() - instant.toEpochMilliseconds()) / 60000
        when {
            minutes < 1 -> "agora mesmo"
            minutes < 60 -> "há $minutes min"
            else -> "há ${minutes / 60}h"
        }
    } catch (_: Throwable) {
        ""
    }
}
