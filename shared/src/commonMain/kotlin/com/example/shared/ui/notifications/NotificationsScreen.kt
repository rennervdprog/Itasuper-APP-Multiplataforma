package com.example.shared.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.ClientNotification
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.ui.mock.mockNotifications

// TODO(fase-rede): trocar mockNotifications pela busca real via Ktor/SupabaseClient.

private enum class NotificationsFilter(val label: String) {
    ALL("Todos"),
    UNREAD("Não lidos"),
    ORDER("Pedidos"),
    PROMO("Promoções")
}

private fun ClientNotification.matchesFilter(filter: NotificationsFilter): Boolean = when (filter) {
    NotificationsFilter.ALL -> true
    NotificationsFilter.UNREAD -> !isRead
    NotificationsFilter.ORDER -> type.lowercase() in setOf("preparando", "pronto_para_entrega", "cancelado", "finalizado", "order_update", "saiu_entrega", "em_transito", "entregue")
    NotificationsFilter.PROMO -> type.lowercase() in setOf("promocao", "promotion", "promo", "cupom", "coupon", "oferta", "offer")
}

private fun notificationIcon(type: String): ImageVector = when (type.lowercase()) {
    "promocao", "promotion", "promo", "cupom", "coupon", "oferta", "offer" -> Icons.Default.ShoppingCart
    "saiu_entrega", "em_transito", "entregue" -> Icons.Default.LocalShipping
    else -> Icons.Default.Notifications
}

private fun ClientNotification.timeLabel(): String {
    return try {
        val date = createdAt.take(10).split("-")
        val time = createdAt.substring(11, 16)
        if (date.size == 3) "${date[2]}/${date[1]} $time" else createdAt
    } catch (_: Throwable) { createdAt }
}

@Composable
fun NotificationsScreen() {
    val notifications = remember { mutableStateOf(mockNotifications()) }
    val selectedFilter = remember { mutableStateOf(NotificationsFilter.ALL) }
    val filtered = notifications.value.filter { it.matchesFilter(selectedFilter.value) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Text(
            "Notificações",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            items(NotificationsFilter.entries.toList()) { filter ->
                FilterChip(
                    selected = filter == selectedFilter.value,
                    onClick = { selectedFilter.value = filter },
                    label = { Text(filter.label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(26.dp))
                            .background(ItaSuperHighlightBg),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Notifications, null, tint = ItaSuperPrimary, modifier = Modifier.size(42.dp)) }
                    Spacer(Modifier.height(16.dp))
                    Text("Nenhuma notificação", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Avisos sobre seus pedidos aparecem aqui.",
                        color = Color(0xFF737373),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filtered, key = { it.id }) { n ->
                    NotificationRow(
                        notification = n,
                        onClick = {
                            // Marca como lida (local; TODO(fase-rede): sincronizar no banco)
                            notifications.value = notifications.value.map {
                                if (it.id == n.id) it.copy(readAt = "now") else it
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: ClientNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (notification.isRead) Color.White else ItaSuperHighlightBg)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                notificationIcon(notification.type),
                null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    notification.title,
                    fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(notification.timeLabel(), color = Color(0xFF737373), fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                notification.body,
                color = Color(0xFF737373),
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!notification.isRead) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(ItaSuperPrimary)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}
