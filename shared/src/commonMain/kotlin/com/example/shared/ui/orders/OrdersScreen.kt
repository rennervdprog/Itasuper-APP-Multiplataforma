package com.example.shared.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.Order
import com.example.shared.model.RefundEligibility
import com.example.shared.network.SupabaseApi
import com.example.shared.model.PixDirectPaymentUiState
import com.example.shared.model.PixPaymentUiState
import com.example.shared.platform.rememberFilePicker
import com.example.shared.ui.cart.CartHolder
import com.example.shared.ui.common.StoreSwitchBottomSheet
import com.example.shared.platform.SecureStorage
import com.example.shared.session.UserSessionRepository
import com.example.shared.theme.ItaSuperBorder
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSuccess
import coil3.compose.AsyncImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

// =============================================================================
// ESTADO DESTA TELA (pós Fases A–E do mapa do app original):
// - Pedidos, cancelar (RPC), confirmar entrega (RPC), avaliação (reviews),
//   reembolso PIX Direto (RPC), gerar PIX (payment-router) e chave PIX Direto
//   da loja: TUDO real, igual ao OrdersViewModel do app atual.
// - Clipboard: LocalClipboardManager do Compose (código de entrega e PIX).
// - QR Code PIX: imagem Base64 real decodificada e exibida via Coil.
// - Upload de comprovante do PIX Direto: seletor nativo via expect/actual
//   (rememberFilePicker) + upload real; "repetir pedido" repõe a sacola
//   com confirmação ao trocar de loja, igual ao original.
// - "Acompanhar entrega": abre DeliveryTrackingScreen via AppNavigator.
// =============================================================================

private data class OrderStatusPresentation(
    val label: String,
    val color: Color,
    val stage: Int
)

private fun Order.presentation(): OrderStatusPresentation = when (status.lowercase()) {
    "aguardando_pagamento" -> OrderStatusPresentation("Aguardando pagamento", Color(0xFFE58A00), 0)
    "aguardando_comprovante" -> OrderStatusPresentation("Aguardando comprovante", Color(0xFFE58A00), 0)
    "comprovante_enviado" -> OrderStatusPresentation("Comprovante enviado", Color(0xFFE58A00), 0)
    "scheduled" -> OrderStatusPresentation("Agendado", Color(0xFF5B5CE2), 0)
    "pendente", "recebido" -> OrderStatusPresentation("Pedido recebido", ItaSuperPrimary, 0)
    "preparando", "em_preparo" -> OrderStatusPresentation("Em preparo", ItaSuperPrimary, 1)
    "pronto_para_entrega", "pronto" -> OrderStatusPresentation("Pronto", ItaSuperPrimary, 2)
    "saiu_entrega", "em_transito" -> OrderStatusPresentation("A caminho", ItaSuperPrimary, 3)
    "entregue", "finalizado" -> OrderStatusPresentation("Entregue", ItaSuperSuccess, 4)
    "cancelado" -> OrderStatusPresentation("Cancelado", Color(0xFFD32F2F), 0)
    else -> OrderStatusPresentation(status.replaceFirstChar { it.uppercase() }, ItaSuperPrimary, 0)
}

private fun Order.isFinished(): Boolean = status.lowercase() in setOf("entregue", "finalizado", "cancelado")

private enum class OrdersFilter(val label: String) {
    ACTIVE("Em andamento"),
    FINISHED("Finalizados")
}

private fun Order.matchesFilter(filter: OrdersFilter): Boolean = when (filter) {
    OrdersFilter.ACTIVE -> !isFinished()
    OrdersFilter.FINISHED -> isFinished()
}

private fun Order.estimatedWindow(): String = when (status.lowercase()) {
    "scheduled" -> "Agendado"
    "pendente", "recebido" -> "≈ 25–45 min"
    "preparando", "em_preparo" -> "≈ 15–30 min"
    "pronto_para_entrega", "pronto" -> "≈ 10–20 min"
    "saiu_entrega", "em_transito" -> "A caminho"
    "entregue", "finalizado" -> "Concluído"
    else -> "Em atualização"
}

private fun money(value: Double): String = com.example.shared.util.formatBRL(value)

private fun oneDecimal(value: Double): String {
    val tenths = (value * 10).roundToLong()
    val inteiro = tenths / 10
    val dec = (tenths % 10).let { if (it < 0) -it else it }
    return "$inteiro,$dec"
}

private fun Order.shortCode(): String = "#${id.take(6).uppercase()}"

private fun Order.createdLabel(): String {
    // createdAt vem do banco em UTC (ex: "2026-10-12T17:06:00Z").
    // Converte para o fuso horário local do aparelho antes de exibir.
    // KMP: kotlinx.datetime no lugar do SimpleDateFormat do original.
    try {
        val normalized = createdAt
            .replace(Regex("\\.\\d+"), "")
            .replace("Z", "")
            .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
            .take(19)
        if (normalized.length >= 16) {
            val instant = LocalDateTime.parse(normalized).toInstant(UtcOffset.ZERO)
            val zoned = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val dd = zoned.dayOfMonth.toString().padStart(2, '0')
            val mm = zoned.monthNumber.toString().padStart(2, '0')
            val hh = zoned.hour.toString().padStart(2, '0')
            val mi = zoned.minute.toString().padStart(2, '0')
            return "$dd/$mm, $hh:$mi"
        }
    } catch (_: Throwable) {
        // fallback para o formato antigo abaixo
    }
    if (createdAt.length >= 16) {
        val date = createdAt.take(10).split("-")
        val time = createdAt.substring(11, 16)
        if (date.size == 3) return "${date[2]}/${date[1]}, $time"
    }
    return createdAt
}

/** Campos de pedido são imutáveis no insert; snapshots novos trazem CEP/cidade/UF e cotação financeira. */
private fun Order.isPickupOrder(): Boolean = neighborhood.equals("RETIRADA", ignoreCase = true) ||
    deliveryAddress.equals("Retirada na loja", ignoreCase = true)

private fun Order.historyDeliveryAddress(): String {
    if (isPickupOrder()) return "Retirada na loja"
    val address = deliveryAddress.trim()
    val location = listOf(deliveryCity.trim(), deliveryState.trim())
        .filter { it.isNotBlank() }
        .joinToString(" - ")
    val cep = deliveryCep.filter(Char::isDigit).takeIf { it.length == 8 }
        ?.let { "CEP ${it.take(5)}-${it.drop(5)}" }
    return listOf(address, neighborhood.trim(), location, cep.orEmpty())
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(" · ")
        .ifBlank { "Endereço registrado no pedido" }
}

private fun Order.historyDeliveryMeta(): String? {
    if (isPickupOrder()) return null
    val snapshot = deliveryQuoteSnapshot ?: return null
    val source = when (snapshot.distanceSource.lowercase()) {
        "fixed" -> "Taxa fixa confirmada"
        else -> "≈ ${oneDecimal(snapshot.distanceKm)} km da loja"
    }
    return "$source · ${money(snapshot.deliveryFee)}"
}

private fun Order.confirmedDeliveryFee(): Double = deliveryQuoteSnapshot?.deliveryFee ?: deliveryFee

private fun paymentLabel(method: String): String = when (method.lowercase()) {
    "pix" -> "Pagamento via PIX"
    "pix_direto" -> "PIX direto"
    "cartao", "card", "credito", "debito" -> "Cartão"
    "dinheiro" -> "Dinheiro"
    else -> method.ifBlank { "Pagamento em atualização" }.replaceFirstChar { it.uppercase() }
}

// Verifica se o pedido está na janela de 7 dias para avaliação
private fun isWithinRatingWindow(createdAt: String): Boolean {
    return try {
        val normalized = createdAt
            .replace(Regex("\\.\\d+"), "")
            .replace("Z", "")
            .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
            .take(19)
        val instant = LocalDateTime.parse(normalized).toInstant(UtcOffset.ZERO)
        val sevenDaysAgo = Clock.System.now().toEpochMilliseconds() - 7 * 24 * 60 * 60 * 1000L
        instant.toEpochMilliseconds() >= sevenDaysAgo
    } catch (_: Throwable) {
        true // em caso de erro no parse, mostra o botão (backend valida)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrdersScreen(
    onExploreClick: () -> Unit = {},
    onTrackDelivery: (orderId: String) -> Unit = {},
    cart: CartHolder? = null,
    onNavigateToCart: () -> Unit = {}
) {
    val orders = remember { mutableStateListOf<Order>() }
    val selectedFilter = remember { mutableStateOf(OrdersFilter.ACTIVE) }
    val ratedStoreIds = remember { mutableStateOf(setOf<String>()) } // Opção 1: 1 avaliação por loja
    val isRefreshing = remember { mutableStateOf(false) }
    val session by UserSessionRepository.session.collectAsState()
    val scope = rememberCoroutineScope()
    // Erro das ações (cancelar/confirmar/avaliar/reembolso/PIX), igual ao
    // errorMessage do OrdersViewModel original.
    val actionError = remember { mutableStateOf<String?>(null) }
    val pendingRepeatOrder = remember { mutableStateOf<Order?>(null) }
    val pendingRepeatStoreName = remember { mutableStateOf("") }
    val pixDialog = remember { mutableStateOf<PixPaymentUiState?>(null) }
    val pixDirectDialog = remember { mutableStateOf<PixDirectPaymentUiState?>(null) }

    // Recarrega pedidos + lojas já avaliadas (porte do refreshOrders do
    // original: falha transitória NÃO apaga o histórico já exibido).
    fun reloadOrders() {
        val s = session
        if (!s.isLoggedIn || s.userId.isBlank() || s.accessToken.isBlank()) {
            isRefreshing.value = false
            return
        }
        scope.launch {
            try {
                val remoteOrders = SupabaseApi.fetchOrdersForClient(s.userId, s.accessToken)
                if (remoteOrders.isNotEmpty() || orders.isEmpty()) {
                    orders.clear()
                    orders.addAll(remoteOrders)
                }
                val ratedIds = SupabaseApi.fetchRatedOrderIds(s.userId, s.accessToken)
                if (ratedIds.isNotEmpty()) {
                    ratedStoreIds.value = ratedStoreIds.value + ratedIds
                }
            } catch (_: Exception) {
                // Mantém o que já estava na tela (sem mocks)
            } finally {
                isRefreshing.value = false
            }
        }
    }

    // Carrega pedidos reais do backend (com o JWT da sessão, como no original)
    LaunchedEffect(session.userId) {
        if (session.userId.isNotBlank()) reloadOrders()
    }

    // Repetir pedido — porte do repeatOrder/confirmRepeatOrder do original:
    // sacola com itens de OUTRA loja pede confirmação (StoreSwitchBottomSheet)
    // antes de esvaziar; sem conflito, adiciona e vai para a sacola.
    fun repeatOrder(order: Order) {
        val activeCart = cart ?: return
        if (order.items.isEmpty()) return
        val firstStoreId = order.items.first().product.storeId
        if (activeCart.storeId != null && activeCart.storeId != firstStoreId && activeCart.items.isNotEmpty()) {
            pendingRepeatOrder.value = order
            pendingRepeatStoreName.value = activeCart.storeName
            return
        }
        order.items.forEach { item ->
            activeCart.addProduct(
                product = item.product,
                quantity = item.quantity,
                notes = item.notes,
                storeName = order.storeName
            )
        }
        onNavigateToCart()
    }

    fun confirmRepeatOrder() {
        val activeCart = cart ?: return
        val order = pendingRepeatOrder.value ?: return
        pendingRepeatOrder.value = null
        activeCart.clearCart()
        order.items.forEach { item ->
            activeCart.addProduct(
                product = item.product,
                quantity = item.quantity,
                notes = item.notes,
                storeName = order.storeName
            )
        }
        onNavigateToCart()
    }

    // Seletor nativo de comprovante do PIX Direto (OpenDocument no Android,
    // UIDocumentPicker no iOS) + upload real via storage (uploadPixDirectProof).
    val proofPicker = rememberFilePicker(
        mimeTypes = listOf("image/jpeg", "image/png", "application/pdf")
    ) { picked ->
        val state = pixDirectDialog.value
        val order = state?.order
        if (picked != null && state != null && order != null) {
            if (session.accessToken.isBlank()) {
                pixDirectDialog.value = state.copy(
                    errorMessage = "Sua sessão expirou. Entre novamente para enviar o comprovante."
                )
            } else {
                pixDirectDialog.value = state.copy(isUploading = true, errorMessage = null)
                scope.launch {
                    val result = SupabaseApi.uploadPixDirectProof(
                        orderId = order.id,
                        storeId = order.storeId,
                        bytes = picked.bytes,
                        mimeType = picked.mimeType,
                        extension = picked.extension,
                        accessToken = session.accessToken
                    )
                    val current = pixDirectDialog.value ?: state
                    pixDirectDialog.value = result.fold(
                        onSuccess = { current.copy(isUploading = false, proofSent = true) },
                        onFailure = { e ->
                            current.copy(
                                isUploading = false,
                                errorMessage = e.message ?: "Não foi possível enviar o comprovante."
                            )
                        }
                    )
                }
            }
        }
    }

    val filteredOrders = orders.filter { it.matchesFilter(selectedFilter.value) }
    val activeOrders = filteredOrders.filterNot { it.isFinished() }
    val previousOrders = filteredOrders.filter { it.isFinished() }
    val orderPendingCancellation = remember { mutableStateOf<Order?>(null) }
    val orderPendingRating = remember { mutableStateOf<Order?>(null) }
    val orderPendingUpdateConfirm = remember { mutableStateOf<Order?>(null) } // Modal "já avaliou"
    val ratingValue = remember { mutableStateOf(5) }
    val ratingComment = remember { mutableStateOf("") }
    val ratingTags = remember { mutableStateOf(setOf<String>()) }
    val ratingPublic = remember { mutableStateOf(true) }
    val orderPendingRefund = remember { mutableStateOf<Order?>(null) }
    val refundDescription = remember { mutableStateOf("") }

    // Atualização real (pull-to-refresh recarrega do backend).
    LaunchedEffect(isRefreshing.value) {
        if (isRefreshing.value) reloadOrders()
    }

    actionError.value?.let { message ->
        AlertDialog(
            onDismissRequest = { actionError.value = null },
            title = { Text("Aviso") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { actionError.value = null }) { Text("Entendi") }
            }
        )
    }

    orderPendingCancellation.value?.let { order ->
        AlertDialog(
            onDismissRequest = { orderPendingCancellation.value = null },
            title = { Text("Cancelar pedido?") },
            text = { Text("O cancelamento será analisado conforme o estágio atual do preparo. Deseja continuar?") },
            confirmButton = {
                Button(
                    onClick = {
                        orderPendingCancellation.value = null
                        // Porte do cancelOrder do original: RPC de política de
                        // cancelamento; sucesso recarrega, erro mostra a mensagem.
                        if (session.accessToken.isBlank()) {
                            actionError.value = "Sua sessão expirou. Entre novamente para cancelar o pedido."
                        } else {
                            scope.launch {
                                val result = SupabaseApi.cancelOrderByClient(order.id, session.accessToken)
                                if (result.isSuccess) {
                                    reloadOrders()
                                } else {
                                    actionError.value = result.exceptionOrNull()?.message
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirmar cancelamento") }
            },
            dismissButton = {
                TextButton(onClick = { orderPendingCancellation.value = null }) { Text("Manter pedido") }
            }
        )
    }

    // Modal: "Você já avaliou esta loja, deseja atualizar?"
    orderPendingUpdateConfirm.value?.let { order ->
        AlertDialog(
            onDismissRequest = { orderPendingUpdateConfirm.value = null },
            title = { Text("Atualizar avaliação") },
            text = { Text("Você já avaliou esta loja. Deseja atualizar sua avaliação?") },
            confirmButton = {
                Button(onClick = {
                    orderPendingUpdateConfirm.value = null
                    orderPendingRating.value = order
                }) { Text("Sim, atualizar") }
            },
            dismissButton = {
                TextButton(onClick = { orderPendingUpdateConfirm.value = null }) { Text("Cancelar") }
            }
        )
    }

    orderPendingRating.value?.let { order ->
        val rating = ratingValue.value
        AlertDialog(
            onDismissRequest = {
                orderPendingRating.value = null
                ratingTags.value = emptySet()
                ratingPublic.value = true
            },
            title = { Text("Como foi seu pedido?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            TextButton(onClick = { ratingValue.value = star }) {
                                Text(if (star <= rating) "★" else "☆", color = ItaSuperPrimary, fontSize = 24.sp)
                            }
                        }
                    }
                    // Tags: o que pode melhorar (nota <= 4) ou o que amou (nota 5)
                    if (rating <= 4) {
                        Text("O que pode melhorar?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val negativeTags = listOf(
                            "tasty" to "Sabor", "good_packaging" to "Embalagem",
                            "generous_portion" to "Porção", "on_time" to "Prazo",
                            "cold" to "Veio fria", "late" to "Atrasou"
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            negativeTags.forEach { (key, label) ->
                                val selected = key in ratingTags.value
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        ratingTags.value = if (selected) ratingTags.value - key
                                        else ratingTags.value + key
                                    },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = ratingComment.value,
                        onValueChange = { ratingComment.value = it },
                        label = { Text(if (rating <= 3) "Comentário (obrigatório, mín. 12 caracteres)" else "Comentário (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = rating <= 3 && ratingComment.value.trim().length < 12
                    )
                    // Visibilidade: pública ou privada
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = ratingPublic.value,
                            onCheckedChange = { ratingPublic.value = it }
                        )
                        Text("Tornar avaliação pública", fontSize = 13.sp)
                    }
                    Text(
                        "Avaliações privadas não aparecem na nota da loja.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Porte do submitOrderRating do original (com tags e
                    // sentimento derivado da tag, igual ao app atual).
                    val rating = ratingValue.value
                    when {
                        session.accessToken.isBlank() || rating !in 1..5 ->
                            actionError.value = "Informe uma avaliação de 1 a 5 estrelas."
                        rating <= 3 && ratingComment.value.trim().length < 12 ->
                            actionError.value = "Conte um pouco mais (mín. 12 caracteres) para notas até 3 estrelas."
                        else -> scope.launch {
                            val tags = ratingTags.value.map { tag ->
                                val positive = tag in setOf("tasty", "good_packaging", "generous_portion", "on_time")
                                tag to if (positive) "positive" else "negative"
                            }
                            val success = SupabaseApi.submitOrderRating(
                                orderId = order.id,
                                storeId = order.storeId,
                                userId = session.userId,
                                rating = rating,
                                comment = ratingComment.value,
                                accessToken = session.accessToken,
                                isPublic = ratingPublic.value,
                                tags = tags
                            )
                            if (success) {
                                orderPendingRating.value = null
                                ratingValue.value = 5
                                ratingComment.value = ""
                                ratingTags.value = emptySet()
                                ratingPublic.value = true
                                ratedStoreIds.value = ratedStoreIds.value + order.storeId // Opção 1: 1 por loja
                            } else {
                                actionError.value = "Não foi possível enviar sua avaliação."
                            }
                        }
                    }
                }) { Text("Enviar avaliação") }
            },
            dismissButton = {
                TextButton(onClick = {
                    orderPendingRating.value = null
                    ratingTags.value = emptySet()
                    ratingPublic.value = true
                }) { Text("Agora não") }
            }
        )
    }

    orderPendingRefund.value?.let { order ->
        AlertDialog(
            onDismissRequest = { orderPendingRefund.value = null },
            title = { Text("Solicitar análise de reembolso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O PIX foi transferido diretamente para a loja. O ItaSuper abrirá um caso para que a loja registre a devolução e o comprovante. Nenhum saldo será adicionado automaticamente à sua carteira.")
                    OutlinedTextField(
                        value = refundDescription.value,
                        onValueChange = { refundDescription.value = it },
                        label = { Text("Descreva o problema") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Porte do requestRefund do original: elegibilidade do
                    // PIX Direto primeiro; motivo fixo "other" (igual ao app).
                    val refundable = RefundEligibility.canOpenPixDiretoCase(
                        order.paymentMethod,
                        order.status,
                        order.refundRequestExpiresAt
                    )
                    when {
                        !refundable -> {
                            actionError.value = if (
                                order.paymentMethod == "pix_direto" &&
                                order.status.lowercase() in setOf("entregue", "finalizado")
                            ) {
                                RefundEligibility.EXPIRED_MESSAGE
                            } else {
                                RefundEligibility.INELIGIBLE_MESSAGE
                            }
                            orderPendingRefund.value = null
                            refundDescription.value = ""
                        }
                        session.accessToken.isBlank() -> {
                            actionError.value = "Sua sessão expirou. Entre novamente para solicitar a análise."
                            orderPendingRefund.value = null
                            refundDescription.value = ""
                        }
                        else -> scope.launch {
                            val description = refundDescription.value
                            val result = SupabaseApi.requestPixDiretoRefund(
                                order.id, "other", description, session.accessToken
                            )
                            orderPendingRefund.value = null
                            refundDescription.value = ""
                            if (result.isFailure) {
                                actionError.value = result.exceptionOrNull()?.message
                                    ?: "Não foi possível abrir a solicitação de PIX Direto."
                            }
                        }
                    }
                }) { Text("Enviar solicitação") }
            },
            dismissButton = { TextButton(onClick = { orderPendingRefund.value = null }) { Text("Cancelar") } }
        )
    }

    if (pendingRepeatOrder.value != null) {
        StoreSwitchBottomSheet(
            currentStoreName = pendingRepeatStoreName.value,
            onConfirm = { confirmRepeatOrder() },
            onDismiss = { pendingRepeatOrder.value = null }
        )
    }

    pixDialog.value?.let { PixPaymentDialog(state = it, onDismiss = { pixDialog.value = null }) }
    pixDirectDialog.value?.let {
        PixDirectPaymentDialog(
            state = it,
            onDismiss = { pixDirectDialog.value = null },
            onSendProof = { proofPicker() }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, null, tint = ItaSuperPrimary, modifier = Modifier.padding(end = 8.dp))
                    Text("Meus Pedidos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            },
            actions = {
                if (isRefreshing.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(end = 4.dp),
                        strokeWidth = 2.dp,
                        color = ItaSuperPrimary
                    )
                } else {
                    IconButton(onClick = { isRefreshing.value = true }, modifier = Modifier.testTag("refresh_orders_button")) {
                        Icon(Icons.Default.Refresh, "Atualizar pedidos", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // TODO(fase-rede): badge ligado ao CartState real; na prévia a sacola está vazia.
                IconButton(onClick = { /* TODO: navegar para a sacola quando o fluxo for ligado */ }, modifier = Modifier.testTag("orders_to_cart_button")) {
                    BadgedBox(badge = { }) {
                        Icon(Icons.Default.ShoppingBag, "Ver Sacola", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            // O Scaffold raiz do App() ja aplica o inset da status bar;
            // deixar o default (systemBars) aqui duplicaria o espaco no topo.
            windowInsets = WindowInsets(0, 0, 0, 0)
)
        if (orders.isEmpty()) {
            EmptyOrdersState(onExploreClick)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(2.dp)) }
                item {
                    OrdersFilterBar(
                        selected = selectedFilter.value,
                        activeCount = orders.count { !it.isFinished() },
                        onSelect = { selectedFilter.value = it }
                    )
                }
                if (isRefreshing.value) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ItaSuperPrimary)
                            Text("Atualizando pedidos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (filteredOrders.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum pedido nesta categoria.",
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (activeOrders.isNotEmpty()) {
                    items(activeOrders, key = { it.id }) { order ->
                        ActiveOrderCard(
                            order = order,
                            onPayPix = {
                                // Porte do generatePixPayment do original: gera o
                                // PIX real do pedido (edge payment-router).
                                if (!session.isLoggedIn || session.accessToken.isBlank()) {
                                    actionError.value = "Sua sessão expirou. Entre novamente para pagar com PIX."
                                } else {
                                    pixDialog.value = PixPaymentUiState(order = order, isLoading = true)
                                    scope.launch {
                                        val response = SupabaseApi.generatePixForOrder(
                                            order = order,
                                            payerFullName = session.name,
                                            payerDocument = session.cpfCnpj,
                                            accessToken = session.accessToken
                                        )
                                        pixDialog.value = if (response.isSuccess) {
                                            PixPaymentUiState(
                                                order = order,
                                                pixCode = response.pixCode,
                                                qrCodeBase64 = response.qrCodeBase64
                                            )
                                        } else {
                                            PixPaymentUiState(
                                                order = order,
                                                errorMessage = response.errorMessage ?: "Não foi possível gerar o PIX."
                                            )
                                        }
                                    }
                                }
                            },
                            onPixDirect = {
                                // Porte do openPixDirectPayment do original: a
                                // chave vem do cadastro REAL da loja.
                                scope.launch {
                                    val store = SupabaseApi.fetchStoreById(order.storeId)
                                    if (store == null || !store.pixDirectEnabled || store.pixDirectKey.isBlank()) {
                                        actionError.value = "A chave PIX direto desta loja não está disponível."
                                    } else {
                                        pixDirectDialog.value = PixDirectPaymentUiState(
                                            order = order,
                                            storeName = store.name,
                                            pixKey = store.pixDirectKey,
                                            pixKeyType = store.pixDirectKeyType,
                                            beneficiary = store.pixDirectBeneficiary,
                                            instructions = store.pixDirectInstructions
                                        )
                                    }
                                }
                            },
                            onConfirmDelivery = {
                                // Porte do confirmDelivery do original: RPC
                                // client_confirm_delivery; sucesso recarrega.
                                if (session.accessToken.isBlank()) {
                                    actionError.value = "Sua sessão expirou. Entre novamente para confirmar a entrega."
                                } else {
                                    scope.launch {
                                        val result = SupabaseApi.confirmDeliveryByClient(order.id, session.accessToken)
                                        if (result.isSuccess) {
                                            reloadOrders()
                                        } else {
                                            actionError.value = result.exceptionOrNull()?.message
                                        }
                                    }
                                }
                            },
                            onCancelOrder = { orderPendingCancellation.value = order },
                            onTrackDelivery = { onTrackDelivery(order.id) }
                        )
                    }
                }
                if (previousOrders.isNotEmpty()) {
                    items(previousOrders, key = { it.id }) { order ->
                        PreviousOrderCard(
                            order = order,
                            onRepeatOrder = { repeatOrder(order) },
                            onRateOrder = { orderPendingRating.value = order },
                            onRefundOrder = { orderPendingRefund.value = order },
                            onEditRating = { orderPendingUpdateConfirm.value = order },
                            alreadyRated = ratedStoreIds.value.contains(order.storeId),
                            withinWindow = isWithinRatingWindow(order.createdAt)
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun OrdersFilterBar(
    selected: OrdersFilter,
    activeCount: Int,
    onSelect: (OrdersFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        OrdersFilter.entries.forEach { filter ->
            val selectedFilter = filter == selected
            TextButton(
                onClick = { onSelect(filter) },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = if (selectedFilter) ItaSuperPrimary else Color(0xFF737373)
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        text = if (filter == OrdersFilter.ACTIVE) "Em andamento ($activeCount)" else filter.label,
                        fontSize = 14.sp,
                        fontWeight = if (selectedFilter) FontWeight.ExtraBold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(if (selectedFilter) ItaSuperPrimary else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(onExploreClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(ItaSuperHighlightBg),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Receipt, null, tint = ItaSuperPrimary, modifier = Modifier.size(42.dp)) }
            Spacer(Modifier.height(20.dp))
            Text("Nenhum pedido ainda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Quando fizer um pedido, você acompanhará cada etapa por aqui.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
            ) { Text("Explorar lojas", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ActiveOrderCard(
    order: Order,
    onPayPix: () -> Unit,
    onPixDirect: () -> Unit,
    onConfirmDelivery: () -> Unit,
    onCancelOrder: () -> Unit,
    onTrackDelivery: () -> Unit
) {
    val presentation = order.presentation()
    val isWaitingPayment = order.status.lowercase() in setOf("aguardando_pagamento", "aguardando_comprovante", "comprovante_enviado")
    val showDetails = remember(order.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, ItaSuperBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(presentation)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.estimatedWindow(), style = MaterialTheme.typography.labelMedium, color = Color(0xFF5D5D5D), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(9.dp))
                    Text(order.shortCode(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF737373))
                }
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(order.storeName.ifBlank { "Loja" }, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), fontWeight = FontWeight.ExtraBold)
                    Text(money(order.total), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), fontWeight = FontWeight.ExtraBold, color = ItaSuperPrimary)
                }
                if (!isWaitingPayment) OrderTimeline(presentation.stage)
                if (order.status.equals("scheduled", ignoreCase = true)) {
                    Text(
                        text = order.releaseAt.takeIf { it.isNotBlank() }
                            ?.let { "Seu pedido será liberado para a loja no horário de abertura." }
                            ?: "Seu pedido está agendado e será liberado no horário escolhido.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ItaSuperPrimary
                    )
                }

                if (order.deliveryPin.isNotBlank() && order.neighborhood.uppercase() != "RETIRADA") {
                    DeliveryPinCard(order.deliveryPin)
                }
                ActiveOrderDetailsRows(
                    order = order,
                    detailsVisible = showDetails.value,
                    onToggleDetails = { showDetails.value = !showDetails.value }
                )
                if (showDetails.value) OrderFinancialSummary(order)

                if (order.status == "aguardando_pagamento" && order.paymentMethod == "pix") {
                    Button(
                        onClick = onPayPix,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Pagar com PIX", fontWeight = FontWeight.Bold) }
                }
                if (order.status == "aguardando_comprovante" && order.paymentMethod == "pix_direto") {
                    Button(
                        onClick = onPixDirect,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Enviar comprovante PIX", fontWeight = FontWeight.Bold) }
                }

                val canConfirmDelivery = order.status.lowercase() in setOf("saiu_entrega", "em_transito") && !order.deliveryConfirmedByClient
                if (canConfirmDelivery) {
                    Button(
                        onClick = onConfirmDelivery,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperSuccess),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sim, recebi meu pedido!", fontWeight = FontWeight.Bold)
                    }
                }

                // Fase 4 (C3): botão para a tela de rastreio (antes inacessível)
                val canTrack = order.status.lowercase() in setOf("saiu_entrega", "em_transito", "pronto_para_entrega")
                if (canTrack) {
                    OutlinedButton(
                        onClick = onTrackDelivery,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Acompanhar entrega", fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(order.createdLabel(), style = MaterialTheme.typography.labelSmall, color = Color(0xFF737373))
                    val canCancel = order.status.lowercase() in setOf("pendente", "recebido", "preparando", "em_preparo", "pronto_para_entrega")
                    if (canCancel) {
                        TextButton(onClick = onCancelOrder) {
                            Text("Cancelar pedido", color = Color(0xFF4E4E4E), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(paymentLabel(order.paymentMethod), style = MaterialTheme.typography.labelSmall, color = Color(0xFF737373))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(presentation: OrderStatusPresentation) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(presentation.color.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(if (presentation.stage >= 4) Icons.Default.CheckCircle else Icons.Default.LocalShipping, null, tint = presentation.color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(presentation.label, color = presentation.color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OrderTimeline(activeStage: Int) {
    val labels = listOf("Recebido", "Preparando", "Pronto", "A caminho", "Entregue")
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            labels.forEachIndexed { index, _ ->
                val reached = index <= activeStage
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (reached) ItaSuperPrimary else Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (index == 3) Icons.Default.LocalShipping else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (reached) Color.White else Color(0xFF8D8D8D),
                        modifier = Modifier.size(17.dp)
                    )
                }
                if (index < labels.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (index < activeStage) ItaSuperPrimary else Color(0xFFE2E2E2))
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (index <= activeStage) ItaSuperPrimary else Color(0xFF777777),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DeliveryPinCard(pin: String) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFFF8F3))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = ItaSuperPrimary, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Código de entrega", fontWeight = FontWeight.ExtraBold, color = Color(0xFF262626), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(9.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(pin.chunked(1).joinToString(" "), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            OutlinedButton(
                onClick = { clipboardManager.setText(AnnotatedString(pin)) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Default.ContentCopy, null, tint = ItaSuperPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text("Copiar", color = ItaSuperPrimary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Informe ao motoboy apenas quando receber seu pedido.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF686868))
    }
}

@Composable
private fun ActiveOrderDetailsRows(
    order: Order,
    detailsVisible: Boolean,
    onToggleDetails: () -> Unit
) {
    val deliveryAddress = order.historyDeliveryAddress()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF414141),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (order.isPickupOrder()) "Retirada" else "Endereço de entrega",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    deliveryAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF686868),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF858585))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 13.dp), color = Color(0xFFEEEEEE))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggleDetails)
                .testTag("toggle_order_details"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF414141), modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(13.dp))
            Text(
                if (detailsVisible) "Ocultar detalhes do pedido" else "Ver detalhes do pedido",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF858585))
        }
        HorizontalDivider(modifier = Modifier.padding(top = 13.dp), color = Color(0xFFEEEEEE))
    }
}

@Composable
private fun OrderDeliverySnapshotCard(order: Order) {
    val deliveryAddress = order.historyDeliveryAddress()
    val deliveryMeta = order.historyDeliveryMeta()
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            if (order.isPickupOrder()) "Retirada" else "Endereço de entrega",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(deliveryAddress, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        deliveryMeta?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = ItaSuperPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun OrderFinancialSummary(order: Order) {
    val confirmedDeliveryFee = order.confirmedDeliveryFee()
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        order.items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.quantity}x ${item.product.name}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text(money(item.totalPrice), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        SummaryLine("Subtotal", money(order.subtotal))
        if (!order.isPickupOrder() && (confirmedDeliveryFee > 0 || order.deliveryQuoteSnapshot?.freeDeliveryApplied == true)) {
            SummaryLine(
                "Taxa de entrega",
                if (confirmedDeliveryFee > 0) money(confirmedDeliveryFee) else "Grátis"
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", fontWeight = FontWeight.ExtraBold)
            Text(money(order.total), fontWeight = FontWeight.ExtraBold, color = ItaSuperPrimary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        SummaryLine("Data do pedido", order.createdLabel())
        SummaryLine("Pagamento", paymentLabel(order.paymentMethod))
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviousOrderCard(
    order: Order,
    onRepeatOrder: () -> Unit,
    onRateOrder: () -> Unit,
    onRefundOrder: () -> Unit,
    onEditRating: () -> Unit = {},
    alreadyRated: Boolean = false,
    withinWindow: Boolean = true
) {
    val presentation = order.presentation()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, ItaSuperBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(order.storeName.ifBlank { "Loja" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${order.createdLabel()} · ${presentation.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(money(order.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (order.status == "cancelado") MaterialTheme.colorScheme.onSurfaceVariant else ItaSuperPrimary)
            }
            Text(
                order.items.joinToString(", ") { "${it.quantity}x ${it.product.name}" }.ifBlank { "Itens do pedido" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            OrderDeliverySnapshotCard(order)
            if (order.status.lowercase() in setOf("entregue", "finalizado")) {
                OutlinedButton(onClick = onRepeatOrder, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = ItaSuperPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Pedir novamente", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Opção 1: 1 avaliação por loja
                    if (withinWindow) {
                        if (alreadyRated) {
                            TextButton(onClick = onEditRating) { Text("Editar avaliação") }
                        } else {
                            TextButton(onClick = onRateOrder) { Text("Avaliar pedido") }
                        }
                    }
                    if (RefundEligibility.canOpenPixDiretoCase(order.paymentMethod, order.status, order.refundRequestExpiresAt)) {
                        TextButton(onClick = onRefundOrder) { Text("Analisar reembolso") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
private fun PixPaymentDialog(
    state: PixPaymentUiState,
    onDismiss: () -> Unit
) {
    val pixCode = state.pixCode
    val clipboardManager = LocalClipboardManager.current
    // QR real: a imagem vem em Base64 da edge function; decodifica para
    // bytes e o Coil renderiza (sem placeholder).
    val qrBytes = remember(state.qrCodeBase64) {
        state.qrCodeBase64?.takeIf { it.isNotBlank() }?.let { encoded ->
            runCatching {
                Base64.decode(encoded.replace("\n", "").replace(" ", ""))
            }.getOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pagamento PIX",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(color = ItaSuperPrimary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Gerando QR Code PIX…")
                    }
                    state.errorMessage != null -> {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        if (qrBytes != null) {
                            AsyncImage(
                                model = qrBytes,
                                contentDescription = "QR Code PIX",
                                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF0F0F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("QR Code", style = MaterialTheme.typography.labelLarge, color = Color(0xFF8D8D8D))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Escaneie o QR Code ou copie o código para pagar no aplicativo do seu banco.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!pixCode.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = pixCode,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!pixCode.isNullOrBlank() && !state.isLoading && state.errorMessage == null) {
                Button(
                    onClick = { clipboardManager.setText(AnnotatedString(pixCode)) },
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) {
                    Text("Copiar código PIX")
                }
            } else {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)) {
                    Text("Fechar")
                }
            }
        },
        dismissButton = {
            if (!state.isLoading && pixCode != null && state.errorMessage == null) {
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

private fun twoDigits(v: Long): String = v.toString().padStart(2, '0')

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PixDirectPaymentDialog(
    state: PixDirectPaymentUiState,
    onDismiss: () -> Unit,
    onSendProof: () -> Unit = {}
) {
    val expiresAtMillis = state.order?.pixExpiresAt?.ifBlank { null }?.let { raw ->
        runCatching { Instant.parse(raw).toEpochMilliseconds() }.getOrNull()
    }
    val nowMillis = remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(expiresAtMillis) {
        while (true) {
            nowMillis.value = Clock.System.now().toEpochMilliseconds()
            delay(1000)
        }
    }
    val remainingMillis = expiresAtMillis?.minus(nowMillis.value)?.coerceAtLeast(0L) ?: 0L
    val deadlineExpired = expiresAtMillis == null || remainingMillis == 0L
    val remainingMinutes = remainingMillis / 60_000L
    val remainingSeconds = (remainingMillis % 60_000L) / 1_000L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pagamento via PIX Direto",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Pagando para", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.storeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Valor: ${money(state.order?.total ?: 0.0)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = ItaSuperPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("Chave PIX da loja", fontWeight = FontWeight.Bold)
                if (state.beneficiary.isNotBlank()) {
                    Text("Beneficiário: ${state.beneficiary}", style = MaterialTheme.typography.bodySmall)
                }
                if (state.pixKeyType.isNotBlank()) {
                    Text(state.pixKeyType.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    state.pixKey,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
                if (state.instructions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(state.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (deadlineExpired) {
                    Text(
                        if (expiresAtMillis == null) "Prazo do PIX indisponível. Solicite um novo pedido à loja."
                        else "Prazo para envio do comprovante encerrado.",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "Envie o comprovante em ${twoDigits(remainingMinutes)}:${twoDigits(remainingSeconds)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    state.isUploading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ItaSuperPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviando comprovante…")
                        }
                    }
                    state.proofSent -> Text(
                        "Comprovante enviado. Aguarde a confirmação da loja.",
                        color = ItaSuperSuccess,
                        fontWeight = FontWeight.Bold
                    )
                    state.errorMessage != null -> Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                    else -> Text(
                        if (deadlineExpired) "O envio não está mais disponível para este pedido."
                        else "Envie um comprovante JPG, PNG ou PDF de até 5 MB.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            if (!state.proofSent) {
                Button(
                    onClick = onSendProof,
                    enabled = !state.isUploading && !deadlineExpired,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)
                ) {
                    Text(if (state.isUploading) "Enviando…" else "Enviar comprovante")
                }
            } else {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary)) {
                    Text("Fechar")
                }
            }
        },
        dismissButton = {
            if (!state.isUploading) TextButton(onClick = onDismiss) { Text("Voltar") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
