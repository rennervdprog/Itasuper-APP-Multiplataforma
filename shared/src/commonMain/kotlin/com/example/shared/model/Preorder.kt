package com.example.shared.model

/**
 * Um pré-pedido só é aceito na janela configurada antes da próxima abertura do mesmo dia.
 * A decisão usa America/Sao_Paulo (UTC-3 fixo, sem horário de verão desde 2019)
 * para não depender do fuso configurado no aparelho.
 * Implementação em Kotlin puro (multiplataforma, sem dependências).
 */
fun Store.preorderReleaseAtMillis(nowMillis: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()): Long? {
    if (isOpen || forceClosed || !preorderEnabled || preorderMinutesBefore <= 0) return null

    // Converte para horário de SP (UTC-3)
    val spMillis = nowMillis - 3 * 3600_000L
    val dayStartSp = (spMillis / 86_400_000L) * 86_400_000L
    // 1970-01-01 foi quinta-feira (4); banco/web: domingo=0..sábado=6
    val currentDay = ((dayStartSp / 86_400_000L + 4) % 7).toInt()
    val nowMinutes = ((spMillis - dayStartSp) / 60_000L).toInt()

    val today = openingHours.firstOrNull { it.dayOfWeek == currentDay && !it.isClosedAllDay } ?: return null
    val timeParts = today.openTime.split(":")
    val openingHour = timeParts.getOrNull(0)?.toIntOrNull() ?: return null
    val openingMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: return null
    val openingMinutes = openingHour * 60 + openingMinute

    if (nowMinutes >= openingMinutes || nowMinutes < openingMinutes - preorderMinutesBefore) return null

    // Epoch millis da abertura de hoje (converte SP de volta para UTC)
    val openingSpMillis = dayStartSp + (openingHour * 3600L + openingMinute * 60L) * 1000L
    return openingSpMillis + 3 * 3600_000L
}

