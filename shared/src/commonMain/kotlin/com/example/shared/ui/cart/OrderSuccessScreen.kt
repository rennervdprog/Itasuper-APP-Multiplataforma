package com.example.shared.ui.cart

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.shared.model.Order
import com.example.shared.platform.rememberFilePicker
import com.example.shared.theme.ItaSuperHighlightBg
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperSuccess

// TODO(fase-rede): dados reais do pedido confirmado pelo servidor
// (número do pedido, prazo estimado, status via realtime/polling).

/**
 * Tela de confirmação de pedido — exibida após o checkout com sucesso.
 */
@Composable
fun OrderSuccessScreen(
    order: Order,
    cart: CartHolder,
    onTrackOrders: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(ItaSuperSuccess.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = ItaSuperSuccess,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Pedido realizado com sucesso!",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(ItaSuperHighlightBg)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Pedido #${order.id.take(8).uppercase()}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = ItaSuperPrimary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Seu pedido foi enviado para ${order.storeName.ifBlank { "a loja" }}. Acompanhe o andamento em Meus Pedidos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ShoppingBag,
                    contentDescription = null,
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.items.sumOf { it.quantity }.let { qtd ->
                        "$qtd ${if (qtd == 1) "item" else "itens"}"
                    },
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pagamento", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(order.paymentMethod, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.deliveryAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = Color(0xFFEFEFEF))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total pago", fontWeight = FontWeight.Bold)
                Text(
                    text = formatBRL(order.total),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = ItaSuperPrimary
                )
            }
        }

        // Seções de pagamento PIX — porte do fluxo do app original.
        if (order.paymentMethod == "pix") {
            Spacer(modifier = Modifier.height(20.dp))
            PixPaymentSection(order = order, cart = cart)
        }
        if (order.paymentMethod == "pix_direto") {
            Spacer(modifier = Modifier.height(20.dp))
            PixDirectSection(order = order, cart = cart)
        }

        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onTrackOrders,
            colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Acompanhar em Meus Pedidos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBackHome) {
            Text("Voltar ao início", color = ItaSuperPrimary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}


/**
 * Seção "Pagar com PIX" — gera o QR/código via payment-router e exibe para pagamento.
 * (Porte do fluxo PixPaymentUiState do app original.)
 */
@Composable
private fun PixPaymentSection(order: Order, cart: CartHolder) {
    val pixState = cart.pixPayment
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pagamento via PIX", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)

        when {
            pixState == null -> {
                Text(
                    "Clique abaixo para gerar o QR Code do PIX e concluir o pagamento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { cart.generatePixPayment(order) },
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pagar com PIX", fontWeight = FontWeight.Bold)
                }
            }
            pixState.isLoading -> {
                CircularProgressIndicator(color = ItaSuperPrimary)
                Text("Gerando PIX...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            pixState.errorMessage != null -> {
                Text(
                    pixState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(onClick = { cart.generatePixPayment(order) }) {
                    Text("Tentar novamente")
                }
            }
            else -> {
                pixState.qrCodeBase64?.takeIf { it.isNotBlank() }?.let { qr ->
                    AsyncImage(
                        model = if (qr.startsWith("data:")) qr else "data:image/png;base64,$qr",
                        contentDescription = "QR Code do PIX",
                        modifier = Modifier.size(200.dp)
                    )
                }
                pixState.pixCode?.takeIf { it.isNotBlank() }?.let { code ->
                    Text(
                        "PIX copia e cola:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        copied = true
                    }) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (copied) "Copiado!" else "Copiar código")
                    }
                }
                Text(
                    "Após pagar, o pedido será confirmado automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Seção "PIX Direto" — exibe a chave da loja e permite enviar o comprovante.
 * (Porte do fluxo PixDirectPaymentUiState do app original.)
 */
@Composable
private fun PixDirectSection(order: Order, cart: CartHolder) {
    val directState = cart.pixDirectPayment
    var copied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Pagamento via PIX Direto", fontWeight = FontWeight.Bold, color = ItaSuperPrimary)

        when {
            directState == null -> {
                Text(
                    "Veja a chave PIX da loja e envie o comprovante após pagar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { cart.openPixDirectPayment(order) },
                    colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver chave e enviar comprovante", fontWeight = FontWeight.Bold)
                }
            }
            directState.errorMessage != null && directState.pixKey.isBlank() -> {
                Text(
                    directState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(onClick = { cart.openPixDirectPayment(order) }) {
                    Text("Tentar novamente")
                }
            }
            else -> {
                if (directState.pixKey.isNotBlank()) {
                    Text(
                        "Chave PIX (${directState.pixKeyType.ifBlank { "chave" }}):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(directState.pixKey, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = {
                        clipboardManager.setText(AnnotatedString(directState.pixKey))
                        copied = true
                    }) {
                        Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (copied) "Copiada!" else "Copiar chave")
                    }
                }
                if (directState.beneficiary.isNotBlank()) {
                    Text(
                        "Favorecido: ${directState.beneficiary}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (directState.instructions.isNotBlank()) {
                    Text(
                        directState.instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (directState.proofSent) {
                    Text(
                        "Comprovante enviado! A loja vai confirmar seu pagamento.",
                        color = ItaSuperSuccess,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        "Após pagar, envie o comprovante (JPG, PNG ou PDF, máx. 5 MB).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    val proofPicker = rememberFilePicker(
                        mimeTypes = listOf("image/jpeg", "image/png", "application/pdf")
                    ) { picked ->
                        if (picked != null) {
                            cart.uploadPixDirectProof(picked.bytes, picked.mimeType, picked.extension)
                        }
                    }
                    Button(
                        onClick = { proofPicker() },
                        enabled = !directState.isUploading,
                        colors = ButtonDefaults.buttonColors(containerColor = ItaSuperPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (directState.isUploading) "Enviando…" else "Enviar comprovante",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (directState.errorMessage != null) {
                    Text(
                        directState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
