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
import androidx.compose.foundation.layout.WindowInsets
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
import com.example.shared.network.SupabaseApi
import com.example.shared.platform.TrackingMapView
import com.example.shared.session.UserSessionRepository
import com.example.shared.theme.ItaSuperBorder
import com.example.shared.theme.ItaSuperPrimary
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant

// =============================================================================
// Mapa REAL (portado em 2026-10-21): WebView + Leaflet igual ao original —
// HTML gerado uma vez (trackingMapHtml) e posição do entregador atualizada
// via window.updateDriver, através do expect/actual TrackingMapView
// (Android: WebView; iOS: WKWebView). Antes era um desenho placeholder.
// Pedido e localização do entregador são REAIS (API): pedidos do cliente via
// fetchOrdersForClient e driver_locations com polling de 20s, como no original.
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryTrackingScreen(
    orderId: String,
    onBack: () -> Unit
) {
    val session by UserSessionRepository.session.collectAsState()
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    LaunchedEffect(session.userId) {
        if (session.userId.isNotBlank() && session.accessToken.isNotBlank()) {
            try {
                orders = SupabaseApi.fetchOrdersForClient(session.userId, session.accessToken)
            } catch (_: Throwable) {
                // Sem rede: o pedido não resolve e a tela mostra o carregando.
            }
        }
    }
    val order = remember(orderId, orders) { orders.find { it.id == orderId } }
    var driverLocation by remember { mutableStateOf<DriverLocation?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Localização do entregador em driver_locations (JWT do cliente), com
    // polling de 20s igual ao original. Falha mantém a última posição boa.
    LaunchedEffect(order?.id, order?.driverId) {
        val o = order ?: return@LaunchedEffect
        isLoading = false
        if (o.driverId.isBlank()) return@LaunchedEffect
        suspend fun fetchLocation() {
            try {
                SupabaseApi.fetchDriverLocation(o.driverId, session.accessToken)
                    ?.let { driverLocation = it }
            } catch (_: Throwable) {
                // Mantém a última posição conhecida.
            }
        }
        // Primeira busca imediata
        fetchLocation()
        // Atualiza a cada 20 segundos
        while (isActive) {
            delay(20_000)
            fetchLocation()
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                // O Scaffold raiz do App() ja aplica o inset da status bar;
                // deixar o default (systemBars) aqui duplicaria o espaco no topo.
                windowInsets = WindowInsets(0, 0, 0, 0)
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
                        // Mapa real (WebView + Leaflet, igual ao original):
                        // HTML gerado uma vez com o pino do cliente; o pino do
                        // entregador é criado/atualizado via updateDriver.
                        val mapHtml = remember(order.id) {
                            trackingMapHtml(
                                clientLat = order.clientLatitude,
                                clientLng = order.clientLongitude
                            )
                        }
                        TrackingMapView(
                            html = mapHtml,
                            driverLatitude = driverLocation?.latitude,
                            driverLongitude = driverLocation?.longitude,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * HTML do mapa de rastreio — porte fiel do trackingMapHtml do app original
 * (WebView + Leaflet/OpenStreetMap). Só o pino do cliente ("Você") nasce no
 * HTML; o pino do entregador é criado e movido por window.updateDriver a
 * cada atualização de posição (evita pino duplicado/congelado).
 * Regra do produto: NUNCA mostrar pino da loja — só entregador e cliente.
 */
private fun trackingMapHtml(
    clientLat: Double?,
    clientLng: Double?
): String {
    val centerLat = clientLat ?: -22.5
    val centerLng = clientLng ?: -48.5
    val markers = buildString {
        if (clientLat != null && clientLng != null) {
            append("L.marker([$clientLat, $clientLng], {icon: homeIcon}).addTo(map).bindPopup('Você');")
        }
    }
    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
<script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
<style>
html, body { margin: 0; padding: 0; height: 100%; }
#map { height: 420px; width: 100%; }
.driver-pin { width: 40px; height: 40px; background: #2563eb; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 20px;
  border: 3px solid white; box-shadow: 0 4px 12px rgba(37,99,235,0.5); }
.home-pin { width: 36px; height: 36px; background: #16a34a; border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 18px;
  border: 3px solid white; box-shadow: 0 3px 10px rgba(22,163,74,0.4); }
</style>
</head>
<body>
<div id="map"></div>
<script>
var map = L.map('map').setView([$centerLat, $centerLng], 14);
L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
var driverIcon = L.divIcon({ html: '<div class="driver-pin">\uD83C\uDFCD\uFE0F</div>', className: '', iconSize: [40, 40], iconAnchor: [20, 20] });
var homeIcon = L.divIcon({ html: '<div class="home-pin">\uD83C\uDFE0</div>', className: '', iconSize: [36, 36], iconAnchor: [18, 18] });
$markers
// Recalcula o tamanho do mapa apos o layout assentar (fix para WebView)
var _tries = 0;
var _timer = setInterval(function() {
  map.invalidateSize();
  if (++_tries >= 20) clearInterval(_timer);
}, 150);
// Atualiza a posicao do entregador sem recarregar o mapa
var driverMarker = null;
window.updateDriver = function(lat, lng) {
  if (driverMarker == null) {
    driverMarker = L.marker([lat, lng], {icon: driverIcon}).addTo(map).bindPopup('Entregador');
  } else {
    driverMarker.setLatLng([lat, lng]);
  }
  map.panTo([lat, lng]);
};
</script>
</body>
</html>
""".trimIndent()
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
