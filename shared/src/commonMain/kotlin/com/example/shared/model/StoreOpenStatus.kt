package com.example.shared.model

import com.example.shared.util.formatBRL
import kotlinx.datetime.Clock

/**
 * Fase 2 (2C) — status de abertura da loja no app, espelhando
 * `getStoreOpenStatus` da web (`src/lib/storeStatus.ts`).
 *
 * Mesma regra do servidor (a RPC `create_order_with_items` espelha o mesmo
 * cálculo): força de fechamento, horário de ontem (overnight), horário de
 * hoje, janela de pré-pedido e próximo dia de abertura.
 *
 * O cálculo usa América/São Paulo (UTC-3 fixo, sem horário de verão desde
 * 2019) com matemática pura de millis, igual a `Preorder.kt` — não depende
 * do fuso configurado no aparelho.
 */
data class StoreOpenStatus(
    val isOpen: Boolean,
    /** Ex.: "Aberto até 22:00", "Abre hoje às 18:00", "Fechado temporariamente". */
    val reason: String,
    /** Loja fechada, mas aceitando pré-pedidos na janela antes da abertura. */
    val acceptingPreorder: Boolean = false,
    val nextOpenTime: String? = null,
    val nextOpenDay: String? = null
)

private val dayNamesPt = listOf(
    "Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"
)

private fun parseMinutes(timeStr: String): Int {
    val parts = timeStr.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return h * 60 + m
}

private fun shortTime(timeStr: String): String {
    val parts = timeStr.split(":")
    val h = (parts.getOrNull(0)?.toIntOrNull() ?: 0).toString().padStart(2, '0')
    val m = (parts.getOrNull(1)?.toIntOrNull() ?: 0).toString().padStart(2, '0')
    return "$h:$m"
}

private fun List<StoreOpeningHour>.forDay(day: Int): StoreOpeningHour? {
    firstOrNull { it.dayOfWeek == day }?.let { return it }
    return firstOrNull { hour ->
        when {
            hour.dayOfWeekStr.equals("domingo", true) && day == 0 -> true
            hour.dayOfWeekStr.equals("segunda", true) && day == 1 -> true
            (hour.dayOfWeekStr.equals("terca", true) || hour.dayOfWeekStr.equals("terça", true)) && day == 2 -> true
            hour.dayOfWeekStr.equals("quarta", true) && day == 3 -> true
            hour.dayOfWeekStr.equals("quinta", true) && day == 4 -> true
            hour.dayOfWeekStr.equals("sexta", true) && day == 5 -> true
            (hour.dayOfWeekStr.equals("sabado", true) || hour.dayOfWeekStr.equals("sábado", true)) && day == 6 -> true
            else -> false
        }
    }
}

fun Store.storeOpenStatus(
    nowMillis: Long = Clock.System.now().toEpochMilliseconds()
): StoreOpenStatus {
    if (forceClosed) {
        return StoreOpenStatus(isOpen = false, reason = "Fechado temporariamente")
    }
    if (openingHours.isEmpty()) {
        return if (isOpen) {
            StoreOpenStatus(isOpen = true, reason = "Aberto")
        } else {
            StoreOpenStatus(isOpen = false, reason = "Fechado")
        }
    }

    // Horário de SP (UTC-3); 1970-01-01 foi quinta-feira (4); domingo = 0.
    val spMillis = nowMillis - 3 * 3600_000L
    val dayStartSp = (spMillis / 86_400_000L) * 86_400_000L
    val currentDay = ((dayStartSp / 86_400_000L + 4) % 7).toInt()
    val nowMinutes = ((spMillis - dayStartSp) / 60_000L).toInt()

    // Ontem (overnight): ex. terça 18h–02h, à 00:30 de quarta está aberta.
    val yesterday = (currentDay + 6) % 7
    val yHours = openingHours.forDay(yesterday)
    if (yHours != null && !yHours.isClosedAllDay) {
        val yOpenMin = parseMinutes(yHours.openTime)
        val yCloseMin = parseMinutes(yHours.closeTime)
        if (yCloseMin <= yOpenMin && nowMinutes < yCloseMin) {
            return StoreOpenStatus(
                isOpen = true,
                reason = "Aberto até ${shortTime(yHours.closeTime)}"
            )
        }
    }

    val today = openingHours.forDay(currentDay)
    if (today != null && !today.isClosedAllDay) {
        val openMin = parseMinutes(today.openTime)
        val closeMin = parseMinutes(today.closeTime)
        val openNow = if (closeMin <= openMin) {
            nowMinutes >= openMin || nowMinutes < closeMin
        } else {
            nowMinutes >= openMin && nowMinutes < closeMin
        }
        if (openNow) {
            return StoreOpenStatus(
                isOpen = true,
                reason = "Aberto até ${shortTime(today.closeTime)}"
            )
        }
        if (nowMinutes < openMin) {
            val acceptingPreorder = preorderEnabled && preorderMinutesBefore > 0 &&
                nowMinutes >= openMin - preorderMinutesBefore
            return StoreOpenStatus(
                isOpen = false,
                reason = "Abre hoje às ${shortTime(today.openTime)}",
                acceptingPreorder = acceptingPreorder,
                nextOpenTime = shortTime(today.openTime),
                nextOpenDay = "Hoje"
            )
        }
    }

    // Próximo dia de abertura (igual à web).
    for (offset in 1..7) {
        val nextDay = (currentDay + offset) % 7
        val nextHours = openingHours.forDay(nextDay)
        if (nextHours != null && !nextHours.isClosedAllDay) {
            val dayLabel = if (offset == 1) "Amanhã" else dayNamesPt[nextDay]
            return StoreOpenStatus(
                isOpen = false,
                reason = "Abre $dayLabel às ${shortTime(nextHours.openTime)}",
                nextOpenTime = shortTime(nextHours.openTime),
                nextOpenDay = dayLabel
            )
        }
    }

    return StoreOpenStatus(isOpen = false, reason = "Fechado")
}

/**
 * Recusa do checkout ANTES do submit (Fase 2, 2C) — mesmas mensagens do
 * checkout logado da web (`CheckoutPage.handleConfirm`): loja fechada, com
 * o motivo do status, ou pedido abaixo do mínimo, com quanto falta.
 * null = pode prosseguir (a RPC continua sendo o backstop).
 */
fun checkoutRefusalReason(
    store: Store,
    subtotal: Double,
    nowMillis: Long = Clock.System.now().toEpochMilliseconds()
): String? {
    val status = store.storeOpenStatus(nowMillis)
    if (!status.isOpen && !status.acceptingPreorder) {
        return "Loja fechada. ${status.reason}"
    }
    if (store.minOrder > 0 && subtotal < store.minOrder) {
        val missing = store.minOrder - subtotal
        return "Pedido mínimo desta loja: ${formatBRL(store.minOrder)}. " +
            "Adicione mais ${formatBRL(missing)}."
    }
    return null
}
