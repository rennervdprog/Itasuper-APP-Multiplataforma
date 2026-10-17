package com.example.shared.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shared.model.Store
import com.example.shared.model.StoreOpeningHour
import com.example.shared.theme.ItaSuperPrimary
import com.example.shared.theme.ItaSuperTextPrimary

/**
 * Tela "Sobre a loja" — migração fase 3b do StoreInfoScreen do app Android.
 * Versão com dados mockados (a rede real vem na próxima fase).
 * Cabeçalho segue a preferência do Renner: nome 19sp, logo 52dp.
 */

// TODO(fase-rede): carregar loja real via SupabaseClient
@Composable
fun StoreInfoScreen(
    store: Store,
    onBackClick: () -> Unit,
    onOpenMap: () -> Unit = {}
) {
    val address = store.displayAddress()
    val payments = store.paymentMethods()
    val hours = store.openingHours.sortedWith(
        compareBy<StoreOpeningHour> { it.dayOrder() }.thenBy { it.openTime }
    )

    Scaffold(
        containerColor = Color(0xFFFAFAFA),
        topBar = {
            Surface(color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp).clickable(onClick = onBackClick),
                        shape = CircleShape,
                        color = Color(0xFFF4F4F4)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "\u2190",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ItaSuperTextPrimary
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sobre a loja",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = ItaSuperTextPrimary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            item { StoreInfoIdentity(store) }
            item { SectionHeading(title = "Horários", subtitle = "Horário de Brasília") }
            if (hours.isEmpty()) {
                item { NeutralInfoRow("Horários não informados pela loja") }
            } else {
                items(hours, key = { "hour_${it.dayOfWeekStr}_${it.dayOfWeek}_${it.openTime}" }) { hour ->
                    OpeningHourRow(hour)
                }
            }
            item { SectionHeading(title = "Formas de pagamento") }
            if (payments.isEmpty()) {
                item { NeutralInfoRow("Formas de pagamento não informadas pela loja") }
            } else {
                items(payments, key = { it }) { method -> PaymentMethodRow(method) }
            }
            item { SectionHeading(title = "Endereço") }
            item { AddressMapRow(address = address, onOpenMap = onOpenMap) }
            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun StoreInfoIdentity(store: Store) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(15.dp),
            color = Color(0xFFFFE9AD)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = store.name.trim().firstOrNull()?.uppercase() ?: "I",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = ItaSuperPrimary
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = store.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 19.sp,
                    color = ItaSuperTextPrimary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = store.category.takeIf { it.isNotBlank() } ?: "Categoria não informada",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color(0xFF727272)
                )
            )
        }
    }
    HorizontalDivider(color = Color(0xFFEDEDED))
}

@Composable
private fun SectionHeading(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 25.dp, end = 16.dp, bottom = 9.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = ItaSuperTextPrimary
            )
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF777777)
                )
            )
        }
    }
}

@Composable
private fun OpeningHourRow(hour: StoreOpeningHour) {
    val schedule = if (hour.isClosedAllDay) {
        "Fechado"
    } else {
        "${hour.openTime.toClock()} \u2013 ${hour.closeTime.toClock()}"
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = hour.dayLabel(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = ItaSuperTextPrimary
            ),
            modifier = Modifier.width(46.dp)
        )
        Text(
            text = schedule,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (hour.isClosedAllDay) Color(0xFFC53D43) else Color(0xFF555555)
            ),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = Color(0xFF9A9A9A),
            modifier = Modifier.size(18.dp)
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFEDEDED))
}

@Composable
private fun PaymentMethodRow(method: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFFF2E8)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (method.contains("Cart", ignoreCase = true)) {
                        Icons.Filled.CreditCard
                    } else {
                        Icons.Filled.AccountBalanceWallet
                    },
                    contentDescription = null,
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = method,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ItaSuperTextPrimary
            )
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFEDEDED))
}

@Composable
private fun AddressMapRow(address: String, onOpenMap: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clickable(enabled = address.isNotBlank(), onClick = onOpenMap),
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6E6E6))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF2E8)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = ItaSuperPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (address.isBlank()) "Endereço não informado pela loja" else address,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ItaSuperTextPrimary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ver no mapa",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ItaSuperPrimary
                        )
                    )
                }
            }
            if (address.isNotBlank()) {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = "Abrir no mapa",
                    tint = ItaSuperPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun NeutralInfoRow(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color(0xFF777777)
        ),
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 18.dp)
    )
}

private fun Store.paymentMethods(): List<String> = buildList {
    if (settings.acceptPixOnline) add("PIX online")
    if (settings.acceptPixMachine) add("PIX na maquininha")
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

private fun StoreOpeningHour.dayLabel(): String {
    val raw = dayOfWeekStr.trim().lowercase()
        .replace("ç", "c")
        .replace("á", "a")
        .replace("ã", "a")
        .replace("é", "e")
        .replace("-feira", "")
    return when {
        raw.startsWith("dom") || raw == "0" -> "Dom"
        raw.startsWith("seg") || raw == "1" -> "Seg"
        raw.startsWith("ter") || raw == "2" -> "Ter"
        raw.startsWith("qua") || raw == "3" -> "Qua"
        raw.startsWith("qui") || raw == "4" -> "Qui"
        raw.startsWith("sex") || raw == "5" -> "Sex"
        raw.startsWith("sab") || raw == "6" -> "Sáb"
        else -> when (dayOfWeek) {
            0 -> "Dom"
            1 -> "Seg"
            2 -> "Ter"
            3 -> "Qua"
            4 -> "Qui"
            5 -> "Sex"
            6 -> "Sáb"
            else -> "Dia"
        }
    }
}

private fun StoreOpeningHour.dayOrder(): Int = when (dayLabel()) {
    "Dom" -> 0
    "Seg" -> 1
    "Ter" -> 2
    "Qua" -> 3
    "Qui" -> 4
    "Sex" -> 5
    "Sáb" -> 6
    else -> 7
}

private fun String.toClock(): String {
    val t = trim()
    Regex("^\\d{1,2}:\\d{2}").find(t)?.let { return it.value.padStart(5, '0') }
    val ti = t.indexOf('T')
    if (ti >= 0 && t.length >= ti + 6) return t.substring(ti + 1, ti + 6)
    return t
}
