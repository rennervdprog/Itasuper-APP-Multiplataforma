package com.example.shared.util

import kotlin.math.abs

/**
 * Formatação canônica de moeda (pt-BR) do app — fonte única.
 * Ex: 12.9 -> "R$ 12,90" | 1234.5 -> "R$ 1.234,50" | -5.0 -> "-R$ 5,00"
 * Manual porque o commonMain não tem String.format/Locale.
 * (Antes cada tela tinha o seu formatador — alguns sem separador de milhar
 * e até valor cru com ponto decimal na tela. Varredura de 2026-10-20.)
 */
fun formatBRL(valor: Double): String {
    val sinal = if (valor < 0) "-" else ""
    val totalCentavos = (abs(valor) * 100 + 0.5).toLong()
    val inteiro = totalCentavos / 100
    val centavos = (totalCentavos % 100).toInt()
    val intStr = inteiro.toString().reversed().chunked(3).joinToString(".").reversed()
    val centStr = if (centavos < 10) "0$centavos" else "$centavos"
    return "${sinal}R$ $intStr,$centStr"
}
