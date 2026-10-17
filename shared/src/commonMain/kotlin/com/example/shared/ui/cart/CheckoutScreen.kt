package com.example.shared.ui.cart

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.theme.ItaSuperBorder
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSuccess
import com.example.shared.ui.mock.mockSavedAddresses

// TODO(fase-rede): endereço (edição/busca CEP), pagamento real (Woovi),
// agendamento real e envio do pedido precisam do SupabaseClient via Ktor.

private val PAYMENT_METHODS = listOf(
    "PIX Online",
    "PIX na Maquininha",
    "PIX Direto",
    "Cartão",
    "Dinheiro"
)

private fun paymentDescription(method: String): String = when (method) {
    "PIX Online" -> "Pagamento confirmado no aplicativo"
    "PIX Direto" -> "Envie o comprovante depois do pedido"
    "PIX na Maquininha" -> "Pague na entrega"
    "Cartão" -> "Pague na entrega"
    else -> "Informe o valor para troco"
}

private val SCHEDULE_SLOTS = listOf("Hoje 19:30", "Hoje 20:00", "Amanhã 12:00", "Amanhã 18:30")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cart: CartHolder,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Finalizar Pedido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFAFAFA)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. Endereço de entrega / local de retirada
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (cart.deliveryType == DeliveryType.PICKUP) Icons.Filled.Storefront else Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (cart.deliveryType == DeliveryType.PICKUP) "Local de Retirada" else "Endereço de Entrega",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (cart.deliveryType == DeliveryType.PICKUP) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFFFF0E6))
                                    .border(1.dp, Color(0xFFF7CBAE), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = cart.storeName.ifBlank { "Estabelecimento Parceiro" },
                                        fontWeight = FontWeight.Bold,
                                        color = ItaSuperPrimary
                                    )
                                    Text(
                                        text = "Você deverá retirar o pedido diretamente na loja após confirmação de preparo.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Endereços salvos",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            mockSavedAddresses().forEach { address ->
                                val selected = address.id == cart.selectedAddressId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) Color(0xFFFFF0E6) else Color.White)
                                        .border(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) ItaSuperPrimary else Color(0xFFE2E2E2),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .selectable(
                                            selected = selected,
                                            onClick = { cart.selectAddress(address.id) }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(selectedColor = ItaSuperPrimary)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = address.label,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = listOf(address.displayLine, address.neighborhood)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (address.pinConfirmed) {
                                            Text(
                                                text = "Localização confirmada",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ItaSuperSuccess
                                            )
                                        }
                                    }
                                    if (address.isDefault) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(ItaSuperHighlightBg)
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Padrão",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ItaSuperPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            // TODO(fase-rede): edição de endereço, busca por CEP e confirmação via GPS.
                            Text(
                                text = "Taxa de entrega confirmada pela regra da loja",
                                style = MaterialTheme.typography.bodySmall,
                                color = ItaSuperSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Forma de pagamento
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text(
                            text = "Forma de pagamento",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PAYMENT_METHODS.forEach { method ->
                            PaymentSelectionCard(
                                method = method,
                                selected = cart.paymentMethod == method,
                                onClick = { cart.selectPaymentMethod(method) }
                            )
                        }

                        if (cart.paymentMethod.startsWith("PIX", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            PixQrPlaceholder(
                                method = cart.paymentMethod,
                                total = cart.total
                            )
                        }

                        if (cart.paymentMethod == "Dinheiro") {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (cart.needsChange) Color(0xFFFFF0E6) else Color(0xFFF7F7F7))
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = cart.needsChange,
                                    onCheckedChange = { cart.toggleNeedsChange(it) },
                                    colors = CheckboxDefaults.colors(checkedColor = ItaSuperPrimary)
                                )
                                Text(
                                    text = "Preciso de troco",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (cart.needsChange) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = cart.changeForAmount,
                                    onValueChange = { cart.changeForAmount = it },
                                    label = { Text("Troco para quanto?") },
                                    placeholder = { Text("Ex: 50,00 ou 100,00") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ItaSuperPrimary,
                                        unfocusedBorderColor = ItaSuperBorder,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Informe o valor em cédulas para que o entregador leve o troco correto.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Agendamento
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = ItaSuperPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (cart.deliveryType == DeliveryType.PICKUP) "Agendar retirada" else "Agendar entrega",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    cart.scheduleNow = true
                                    cart.scheduledSlot = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cart.scheduleNow) ItaSuperPrimary else Color.White,
                                    contentColor = if (cart.scheduleNow) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Agora")
                            }
                            Button(
                                onClick = { cart.scheduleNow = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!cart.scheduleNow) ItaSuperPrimary else Color.White,
                                    contentColor = if (!cart.scheduleNow) Color.White else MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Escolher horário")
                            }
                        }
                        if (!cart.scheduleNow) {
                            Spacer(modifier = Modifier.height(10.dp))
                            // TODO(fase-rede): horários reais de agendamento vindos da loja.
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SCHEDULE_SLOTS.forEach { slot ->
                                    val selected = cart.scheduledSlot == slot
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) Color(0xFFFFF0E6) else Color.White)
                                            .border(
                                                if (selected) 2.dp else 1.dp,
                                                if (selected) ItaSuperPrimary else Color(0xFFE2E2E2),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { cart.scheduledSlot = slot }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = slot,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = "Selecionado",
                                                    tint = ItaSuperPrimary,
                                                    modifier = Modifier.size(20.dp)
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

            // 4. Resumo final
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                        Text(
                            text = "Resumo final do pedido",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal (${cart.totalItemCount} itens)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatBRL(cart.subtotal))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Taxa de entrega", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            when {
                                cart.deliveryType == DeliveryType.PICKUP -> Text("Grátis (Retirada)", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                cart.effectiveDeliveryFee == 0.0 -> Text("Grátis", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                                else -> Text(formatBRL(cart.effectiveDeliveryFee))
                            }
                        }
                        if (cart.effectiveCouponDiscount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cupom", color = ItaSuperSuccess)
                                Text("- ${formatBRL(cart.effectiveCouponDiscount)}", color = ItaSuperSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = ItaSuperBorder, modifier = Modifier.padding(vertical = 14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFEEDF))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total a pagar", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                Text("Revise os dados antes de confirmar", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                text = formatBRL(cart.total),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = ItaSuperPrimary
                            )
                        }
                    }
                }
            }

            if (cart.orderError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cart.orderError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { cart.placeOrder() },
                    enabled = !cart.isPlacingOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    if (cart.isPlacingOrder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Enviando pedido para ${cart.storeName.ifBlank { "a loja" }}...",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Confirmar e Enviar Pedido • ${formatBRL(cart.total)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun PaymentSelectionCard(
    method: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when {
        method.contains("PIX", ignoreCase = true) -> Icons.Filled.Pix
        method.contains("Cartão", ignoreCase = true) -> Icons.Filled.CreditCard
        else -> Icons.Filled.AttachMoney
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) Color(0xFFFFF3EC) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(38.dp)
                    .background(if (selected) ItaSuperPrimary else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) ItaSuperPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = paymentDescription(method),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        HorizontalDivider(color = Color(0xFFEAEAEA))
    }
}

@Composable
private fun PixQrPlaceholder(
    method: String,
    total: Double
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, ItaSuperBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TODO(fase-rede): gerar o QR Code real a partir do payload PIX
        // retornado pelo backend (Woovi) ao criar a cobrança.
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ItaSuperHighlightBg)
                .border(2.dp, ItaSuperPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.QrCode2,
                contentDescription = "QR Code do PIX (placeholder)",
                tint = ItaSuperPrimary,
                modifier = Modifier.size(110.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "QR Code do $method",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "O QR Code de ${formatBRL(total)} será gerado na confirmação do pedido.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (method == "PIX Direto") {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Após o pedido, envie o comprovante para a loja.",
                style = MaterialTheme.typography.bodySmall,
                color = ItaSuperPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
