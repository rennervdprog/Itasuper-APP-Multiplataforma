package com.example.shared.ui.cart

import com.example.shared.model.DeliveryAddressInput
import com.example.shared.model.SavedAddress
import com.example.shared.model.UserSession
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Política de localização do checkout — porte de
 * ui/orders/CheckoutLocationPolicy.kt + activeLocationDiffersFromAddress do
 * app original.
 */

const val ACTIVE_LOCATION_TTL_MILLIS: Long = 5 * 60 * 1000L

fun isActiveLocationFresh(
    session: UserSession,
    nowMillis: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
): Boolean {
    val capturedAt = session.activeLocationUpdatedAt
    val latitude = session.activeLocationLatitude
    val longitude = session.activeLocationLongitude
    if (capturedAt <= 0L || latitude == null || longitude == null) return false
    if (!latitude.isFinite() || !longitude.isFinite()) return false
    return nowMillis - capturedAt in 0..ACTIVE_LOCATION_TTL_MILLIS
}

/**
 * Monta somente um endereço inteiramente originado do GPS. O CEP salvo nunca
 * é usado como fallback, porque isso criaria um endereço híbrido com
 * coordenadas diferentes.
 */
fun gpsAddressIfComplete(
    session: UserSession,
    nowMillis: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
): DeliveryAddressInput? {
    if (!isActiveLocationFresh(session, nowMillis)) return null
    val address = DeliveryAddressInput(
        street = session.activeLocationStreet,
        number = session.activeLocationNumber,
        neighborhood = session.activeLocationNeighborhood,
        city = session.activeLocationCity,
        state = session.activeLocationState,
        cep = session.activeLocationCep.filter(Char::isDigit).take(8)
    )
    return address.takeIf { it.isComplete() }
}

/**
 * A localização ativa difere do endereço aplicado? Coordenadas a mais de
 * 300m divergem; sem coordenadas do endereço, compara cidade/rua/número.
 */
fun activeLocationDiffersFromAddress(session: UserSession, address: SavedAddress?): Boolean {
    if (!isActiveLocationFresh(session)) return false
    val latitude = session.activeLocationLatitude
    val longitude = session.activeLocationLongitude
    if (address?.latitude != null && address.longitude != null && latitude != null && longitude != null) {
        return haversineMeters(latitude, longitude, address.latitude, address.longitude) > 300.0
    }
    if (session.activeLocationCity.isBlank() || session.activeLocationStreet.isBlank()) return false
    fun normalized(value: String) = value.trim().lowercase().replace(Regex("\\s+"), " ")
    val street = address?.street ?: session.addressStreet
    val number = address?.number ?: session.addressNumber
    val targetCity = address?.city?.ifBlank { session.addressCity } ?: session.addressCity
    val sameCity = normalized(session.activeLocationCity) == normalized(targetCity)
    val sameStreet = normalized(session.activeLocationStreet) == normalized(street)
    val activeNumber = normalized(session.activeLocationNumber)
    val sameNumber = activeNumber.isBlank() || number.isBlank() || activeNumber == normalized(number)
    return !(sameCity && sameStreet && sameNumber)
}

fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val earthRadius = 6_371_000.0
    val dLat = (lat2 - lat1) * (kotlin.math.PI / 180.0)
    val dLng = (lng2 - lng1) * (kotlin.math.PI / 180.0)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1 * (kotlin.math.PI / 180.0)) * cos(lat2 * (kotlin.math.PI / 180.0)) *
        sin(dLng / 2) * sin(dLng / 2)
    return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
}
