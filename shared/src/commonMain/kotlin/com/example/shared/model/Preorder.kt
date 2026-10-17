package com.example.shared.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Um pré-pedido só é aceito na janela configurada antes da próxima abertura do mesmo dia.
 * A decisão usa America/Sao_Paulo para não depender do fuso configurado no aparelho.
 */
@OptIn(ExperimentalTime::class)
fun Store.preorderReleaseAtMillis(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): Long? {
    if (isOpen || forceClosed || !preorderEnabled || preorderMinutesBefore <= 0) return null

    val zone = TimeZone.of("America/Sao_Paulo")
    val now: LocalDateTime = Clock.System.now().toLocalDateTime(zone)
    // kotlinx-datetime DayOfWeek: MONDAY=1..SUNDAY=7; banco/web: domingo=0..sábado=6
    val currentDay = (now.dayOfWeek.ordinal + 1) % 7
    val nowMinutes = now.hour * 60 + now.minute
    val today = openingHours.firstOrNull { it.dayOfWeek == currentDay && !it.isClosedAllDay } ?: return null
    val timeParts = today.openTime.split(":")
    val openingHour = timeParts.getOrNull(0)?.toIntOrNull() ?: return null
    val openingMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: return null
    val openingMinutes = openingHour * 60 + openingMinute

    if (nowMinutes >= openingMinutes || nowMinutes < openingMinutes - preorderMinutesBefore) return null

    // Retorna o epoch millis da abertura de hoje em America/Sao_Paulo
    // (usa o nowMillis como base para respeitar o parâmetro em testes)
    val base = kotlin.time.Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(zone)
    val openingDateTime = LocalDateTime(
        base.year, base.month, base.dayOfMonth,
        openingHour, openingMinute, 0, 0
    )
    return openingDateTime.toInstant(zone).toEpochMilliseconds()
}
