package com.example.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory multiplataforma do HttpClient Ktor.
 * Cada plataforma fornece o engine nativo (OkHttp no Android, Darwin no iOS).
 */
expect fun createHttpClient(): HttpClient

/**
 * Configuração compartilhada: JSON com tolerância a campos desconhecidos,
 * igual ao comportamento do OkHttp manual do app Android atual.
 */
fun createDefaultHttpClient(): HttpClient = createHttpClient().config {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        })
    }
}
