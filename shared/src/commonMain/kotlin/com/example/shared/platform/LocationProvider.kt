package com.example.shared.platform

/**
 * Endereço obtido da posição atual do GPS (porte do `CurrentLocationAddress`
 * do app original). Os campos de endereço vêm da geocodificação reversa;
 * quando ela falha, voltam vazios — a coordenada continua utilizável, mas
 * nenhum endereço é inventado.
 */
data class CurrentLocationAddress(
    val latitude: Double,
    val longitude: Double,
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val cep: String = ""
)

/**
 * Localização/GPS — expect/actual.
 *
 * Porte do `CurrentLocationProvider` do app nativo: posição atual (não
 * cacheada), filtrada por precisão aceitável (≤ 250 m), com geocodificação
 * reversa para endereço.
 *
 * Android: FusedLocationProviderClient + Geocoder (requer permissão de
 * localização e [init] com o contexto da aplicação).
 * iOS: CoreLocation (CLLocationManager) + CLGeocoder.
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

    /**
     * Posição atual já geocodificada em endereço, ou null se a posição estiver
     * indisponível. Endereço parcial (cidade vazia, por exemplo) NÃO é
     * completado com chute: quem chama decide o que fazer com ele.
     */
    suspend fun getCurrentAddress(): CurrentLocationAddress?
}
