package com.example.shared.ui.notifications

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import shared.generated.resources.Res
import shared.generated.resources.ic_ita_bell
import shared.generated.resources.ic_ita_check
import shared.generated.resources.ic_ita_delivery
import shared.generated.resources.ic_ita_refresh
import org.jetbrains.compose.resources.painterResource
import com.example.shared.model.ClientNotification
import com.example.shared.theme.ItaSuperBackground
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperTextPrimary
import com.example.shared.theme.ItaSuperTextSecondary
import com.example.shared.ui.mock.mockNotifications
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// TODO(fase-rede): trocar mockNotifications pela busca real via Ktor/SupabaseClient.

private enum class NotificationsFilter(val label: String) {
    ALL("Todos"),
    UNREAD("Não lidos"),
    ORDER("Pedidos"),
    DELIVERY("Entrega"),
    PROMO("Promoções")
}

private fun ClientNotification.matchesFilter(filter: NotificationsFilter): Boolean = when (filter) {
    NotificationsFilter.ALL -> true
    NotificationsFilter.UNREAD -> !isRead
    NotificationsFilter.ORDER -> type.lowercase() in setOf("preparando", "pronto_para_entrega", "cancelado", "finalizado", "order_update")
    NotificationsFilter.DELIVERY -> type.lowercase() in setOf("saiu_entrega", "em_transito", "entregue")
    NotificationsFilter.PROMO -> type.lowercase() in setOf("promocao", "promotion", "promo", "cupom", "coupon", "oferta", "offer")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen() {
    var notifications by remember { mutableStateOf(mockNotifications()) }
    var selectedFilter by remember { mutableStateOf(NotificationsFilter.ALL) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage: String? by remember { mutableStateOf(null) }
    val everHadNotifications = remember { mutableStateOf(mockNotifications().isNotEmpty()) }

    val filteredNotifications = notifications.filter { it.matchesFilter(selectedFilter) }
    val unreadCount = notifications.count { !it.isRead }

    fun markRead(id: String) {
        notifications = notifications.map {
            if (it.id == id) it.copy(readAt = "manual") else it
        }
    }

    fun markAllAsRead() {
        notifications = notifications.map { it.copy(readAt = "manual") }
    }

    fun dismissNotification(id: String) {
        notifications = notifications.filterNot { it.id == id }
    }

    fun doRefresh() {
        isLoading = true
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(700)
            isLoading = false
        }
    }

    Scaffold(
        containerColor = ItaSuperBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notificações",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = ItaSuperTextPrimary
                            )
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount nova" + if (unreadCount == 1) "" else "s",
                                style = MaterialTheme.typography.labelMedium.copy(color = ItaSuperPrimary)
                            )
                        }
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = ItaSuperPrimary
                        )
                    } else {
                        IconButton(
                            onClick = { doRefresh() },
                            modifier = Modifier.testTag("refresh_notifications")
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_ita_refresh),
                                contentDescription = "Atualizar notificações",
                                tint = ItaSuperTextPrimary
                            )
                        }
                    }
                },
                // O Scaffold raiz do App() ja aplica o inset da status bar;
                // deixar o default (systemBars) aqui duplicaria o espaco no topo.
                windowInsets = WindowInsets(0, 0, 0, 0)
)
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { doRefresh() },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when {
                isLoading && notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = ItaSuperPrimary) }
                }
                notifications.isEmpty() -> EmptyNotificationsContent(
                    errorMessage = errorMessage,
                    everHadNotifications = everHadNotifications.value,
                    onExploreClick = { /* mock: sem navegação nesta fase */ }
                )
                else -> {
                    val grouped = remember(filteredNotifications) {
                        groupNotificationsByDate(filteredNotifications)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag("notifications_list"),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            NotificationsFilterBar(
                                selected = selectedFilter,
                                onSelect = { selectedFilter = it }
                            )
                        }
                        if (unreadCount > 0) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { markAllAsRead() },
                                        modifier = Modifier.testTag("mark_all_read")
                                    ) {
                                        Text(
                                            text = "Marcar todas como lidas",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = ItaSuperPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = ItaSuperPrimary
                                    )
                                    Text(
                                        "Sincronizando avisos",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ItaSuperTextSecondary
                                    )
                                }
                            }
                        }
                        if (!errorMessage.isNullOrBlank()) {
                            item {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = ItaSuperTextSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                )
                            }
                        }
                        if (filteredNotifications.isEmpty()) {
                            item {
                                Text(
                                    text = "Nenhum aviso nesta categoria.",
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 36.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ItaSuperTextSecondary
                                )
                            }
                        } else {
                            grouped.forEach { (group, groupItems) ->
                                item {
                                    DateGroupHeader(label = group.label)
                                }
                                items(groupItems, key = { it.id }) { notification ->
                                    NotificationRow(
                                        notification = notification,
                                        onMarkRead = { markRead(notification.id) },
                                        onDismiss = { dismissNotification(notification.id) },
                                        onClick = { markRead(notification.id) }
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

@Composable
private fun NotificationsFilterBar(selected: NotificationsFilter, onSelect: (NotificationsFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(NotificationsFilter.entries, key = { it.name }) { filter ->
            val isSelected = filter == selected
            TextButton(
                onClick = { onSelect(filter) },
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (isSelected) ItaSuperHighlightBg else Color.White,
                    contentColor = if (isSelected) ItaSuperPrimary else ItaSuperTextSecondary
                ),
                modifier = Modifier.testTag("notifications_filter_${filter.name.lowercase()}")
            ) {
                Text(
                    filter.label,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsContent(
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    everHadNotifications: Boolean = false,
    onExploreClick: () -> Unit = {}
) {
    val title: String
    val body: String
    val showExploreCta: Boolean
    when {
        !errorMessage.isNullOrBlank() -> {
            title = "Não foi possível carregar os avisos"
            body = errorMessage
            showExploreCta = false
        }
        everHadNotifications -> {
            title = "Você está em dia! 🎉"
            body = "Nenhum aviso novo por aqui."
            showExploreCta = false
        }
        else -> {
            title = "Nenhuma notificação ainda"
            body = "As atualizações dos seus pedidos aparecerão aqui."
            showExploreCta = true
        }
    }
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(ItaSuperHighlightBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_ita_bell),
                contentDescription = null,
                tint = ItaSuperPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ItaSuperTextPrimary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium.copy(color = ItaSuperTextSecondary),
            textAlign = TextAlign.Center
        )
        if (showExploreCta) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ItaSuperPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Explorar ofertas", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationRow(
    notification: ClientNotification,
    onMarkRead: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { target ->
            when (target) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onMarkRead()
                    false // executa a ação e volta (não remove a linha)
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDismiss()
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !notification.isRead,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction != SwipeToDismissBoxValue.Settled) {
                val isMarkRead = direction == SwipeToDismissBoxValue.StartToEnd
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isMarkRead) Color(0xFF43A047) else Color(0xFFE53935))
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (isMarkRead) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isMarkRead) Icons.Filled.Check else Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (isMarkRead) "Marcar como lida" else "Excluir",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        },
        content = { NotificationRowContent(notification = notification, onClick = onClick) }
    )
    HorizontalDivider(color = Color(0xFFECECEC), thickness = 1.dp)
}

@Composable
private fun NotificationRowContent(notification: ClientNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) Color.White else ItaSuperHighlightBg.copy(alpha = 0.52f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("notification_${notification.id}"),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(
                if (notification.isRead) Color(0xFFF2F2F2) else ItaSuperPrimary.copy(alpha = 0.14f)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(when (notification.type.lowercase()) {
                    "saiu_entrega", "em_transito", "entregue" -> Res.drawable.ic_ita_delivery
                    "pronto_para_entrega", "finalizado" -> Res.drawable.ic_ita_check
                    else -> Res.drawable.ic_ita_bell
                }),
                contentDescription = null,
                tint = if (notification.isRead) ItaSuperTextSecondary else ItaSuperPrimary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.ExtraBold,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!notification.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ItaSuperPrimary))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ItaSuperTextSecondary,
                    lineHeight = 19.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = formatNotificationDate(notification.createdAt),
                style = MaterialTheme.typography.labelSmall.copy(color = ItaSuperTextSecondary)
            )
        }
    }
}

@Composable
private fun DateGroupHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            color = ItaSuperTextSecondary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(ItaSuperBackground)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

private enum class NotificationDateGroup(val label: String) {
    TODAY("Hoje"),
    YESTERDAY("Ontem"),
    THIS_WEEK("Esta semana"),
    OLDER("Anteriores")
}

/** createdAt vem do banco em UTC (ex: "2026-10-12T17:06:00Z"). Retorna null se inválido. */
private fun parseNotificationEpochUtc(value: String): Long? {
    return try {
        val normalized = value
            .replace(Regex("\\.\\d+"), "")
            .replace("Z", "")
            .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
            .take(19)
        if (normalized.length < 16) return null
        val datePart = normalized.substring(0, 10)
        val timePart = normalized.substring(11)
        val dateParts = datePart.split("-")
        val timeParts = timePart.split(":")
        if (dateParts.size != 3 || timeParts.size < 2) return null
        val year = dateParts[0].toInt()
        val month = dateParts[1].toInt()
        val day = dateParts[2].toInt()
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val second = if (timeParts.size > 2) timeParts[2].toInt() else 0
        daysFromCivil(year, month, day) * 86_400_000L +
            ((hour * 60L + minute) * 60 + second) * 1_000L
    } catch (_: Throwable) {
        null
    }
}

/** Dias desde 1970-01-01 (algoritmo days-from-civil, sem dependência de fuso). */
private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    var y = year
    val m = if (month <= 2) {
        y -= 1
        month + 12
    } else {
        month
    }
    val era = if (y >= 0) y else y - 399
    val yoe = y - era * 400
    val mp = m - 3
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468L
}

private fun twoDigits(n: Int): String = if (n < 10) "0$n" else "$n"

private fun localDateOf(epochMillis: Long): Long {
    val ldt = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return daysFromCivil(ldt.year, ldt.monthNumber, ldt.dayOfMonth)
}

private fun dateGroupOf(notification: ClientNotification, nowMillis: Long): NotificationDateGroup {
    val epoch = parseNotificationEpochUtc(notification.createdAt) ?: return NotificationDateGroup.OLDER
    val diffDays = localDateOf(nowMillis) - localDateOf(epoch)
    return when {
        diffDays <= 0 -> NotificationDateGroup.TODAY
        diffDays == 1L -> NotificationDateGroup.YESTERDAY
        diffDays < 7 -> NotificationDateGroup.THIS_WEEK
        else -> NotificationDateGroup.OLDER
    }
}

private fun groupNotificationsByDate(
    notifications: List<ClientNotification>
): List<Pair<NotificationDateGroup, List<ClientNotification>>> {
    val now = Clock.System.now().toEpochMilliseconds()
    val grouped = notifications.groupBy { dateGroupOf(it, now) }
    // Ordem fixa do enum (TODAY, YESTERDAY, THIS_WEEK, OLDER) — sem toSortedMap,
    // que o compilador iOS (Kotlin/Native) não resolve neste contexto.
    return NotificationDateGroup.values()
        .filter { grouped.containsKey(it) }
        .map { it to grouped.getValue(it) }
}

private fun formatNotificationDate(value: String): String {
    if (value.isBlank()) return "Agora"
    val fallback = value.take(16).replace("T", " às ")
    val epoch = parseNotificationEpochUtc(value) ?: return fallback
    // Converte UTC -> fuso local do aparelho (mesmo comportamento do original).
    val ldt = Instant.fromEpochMilliseconds(epoch)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${ldt.year}-${twoDigits(ldt.monthNumber)}-${twoDigits(ldt.dayOfMonth)} " +
        "às ${twoDigits(ldt.hour)}:${twoDigits(ldt.minute)}"
}
