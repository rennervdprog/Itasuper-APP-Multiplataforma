package com.example.shared.platform

/**
 * Localização/GPS — expect/actual (Fase 5).
 *
 * Baseado no `CurrentLocationProvider` do app nativo (simplificado: só a
 * coordenada; a geocodificação para endereço fica no app quando necessária).
 *
 * Android: FusedLocationProviderClient (requer permissão de localização e
 * [init] com o contexto da aplicação).
 * iOS: stub — retorna null com TODO (CoreLocation fica para uma fase futura).
 */
expect object LocationProvider {

    /**
     * Inicializa o provedor com o contexto da aplicação.
     * No Android passe `android.content.Context`; no iOS é ignorado.
     */
    fun init(appContext: Any?)

    /**
     * Posição atual (latitude, longitude), ou null se indisponível
     * (sem permissão, GPS desligado ou timeout).
     */
    suspend fun getCurrentLocation(): Pair<Double, Double>?
}
