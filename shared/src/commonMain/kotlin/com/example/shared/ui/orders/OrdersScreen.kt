package com.example.shared.ui.orders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.Order
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSuccess
import com.example.shared.ui.mock.mockOrders

// TODO(fase-rede): trocar mockOrders pela busca real via Ktor/SupabaseClient.

private enum class OrdersFilter(val label: String) {
    ACTIVE("Em andamento"),
    FINISHED("Finalizados")
}

private data class OrderStatusPresentation(val label: String, val color: Color)

private fun Order.presentation(): OrderStatusPresentation = when (status.lowercase()) {
    "aguardando_pagamento" -> OrderStatusPresentation("Aguardando pagamento", Color(0xFFE58A00))
    "pendente", "recebido" -> OrderStatusPresentation("Pedido recebido", ItaSuperPrimary)
    "preparando", "em_preparo" -> OrderStatusPresentation("Em preparo", ItaSuperPrimary)
    "pronto_para_entrega", "pronto" -> OrderStatusPresentation("Pronto", ItaSuperPrimary)
    "saiu_entrega", "em_transito" -> OrderStatusPresentation("A caminho", ItaSuperPrimary)
    "entregue", "finalizado" -> OrderStatusPresentation("Entregue", ItaSuperSuccess)
    "cancelado" -> OrderStatusPresentation("Cancelado", Color(0xFFD32F2F))
    else -> OrderStatusPresentation("Em andamento", ItaSuperPrimary)
}

private fun Order.isFinished(): Boolean =
    status.lowercase() in setOf("entregue", "finalizado", "cancelado")

private fun formatMoney(value: Double): String {
    val inteiro = value.toLong()
    val centavos = ((value - inteiro) * 100).toLong().coerceIn(0, 99)
    val intStr = inteiro.toString().reversed().chunked(3).joinToString(".").reversed()
    val centStr = if (centavos < 10) "0$centavos" else "$centavos"
    return "R$ $intStr,$centStr"
}

private fun Order.shortCode(): String = "#${id.take(6).uppercase()}"

private fun Order.createdLabel(): String {
    // createdAt ISO "2026-10-12T17:06:00" -> "12/10, 17:06"
    return try {
        val date = createdAt.take(10).split("-")
        val time = createdAt.substring(11, 16)
        if (date.size == 3) "${date[2]}/${date[1]}, $time" else createdAt
    } catch (_: Throwable) { createdAt }
}

@Composable
fun OrdersScreen(onExploreClick: () -> Unit = {}) {
    val orders = remember { mockOrders() }
    val selectedFilter = remember { mutableStateOf(OrdersFilter.ACTIVE) }
    val filtered = orders.filter {
        if (selectedFilter.value == OrdersFilter.ACTIVE) !it.isFinished() else it.isFinished()
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFA))) {
        // Topo
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Receipt, null, tint = ItaSuperPrimary, modifier = Modifier.padding(end = 8.dp))
            Text("Meus Pedidos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
        // Abas de filtro
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            OrdersFilter.entries.forEach { filter ->
                val selected = filter == selectedFilter.value
                TextButton(
                    onClick = { selectedFilter.value = filter },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (selected) ItaSuperPrimary else Color(0xFF737373)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            filter.label,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth().height(3.dp)
                                .background(if (selected) ItaSuperPrimary else Color.Transparent)
                        )
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            // Estado vazio
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(26.dp))
                            .background(ItaSuperHighlightBg),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Receipt, null, tint = ItaSuperPrimary, modifier = Modifier.size(42.dp)) }
                    Spacer(Modifier.height(20.dp))
                    Text("Nenhum pedido ainda", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Quando fizer um pedido, você acompanhará cada etapa por aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF737373)
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(filtered, key = { it.id }) { order ->
                    OrderCard(order = order)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    val pres = order.presentation()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.storeName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    pres.label,
                    color = pres.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${order.shortCode()} · ${order.createdLabel()}",
                color = Color(0xFF737373),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${order.items.sumOf { it.quantity }} ${if (order.items.sumOf { it.quantity } == 1) "item" else "itens"}",
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.deliveryAddress, color = Color(0xFF737373), fontSize = 13.sp, maxLines = 1)
                Text(formatMoney(order.total), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = ItaSuperPrimary)
            }
        }
    }
}
