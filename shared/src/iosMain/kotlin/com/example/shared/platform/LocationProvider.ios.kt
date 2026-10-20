package com.example.shared.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.CLPlacemark
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Porte do `CurrentLocationProvider` do app original para o iOS:
 * posição atual via CoreLocation (precisão aceitável ≤ 250 m) e
 * geocodificação reversa via CLGeocoder, com os mesmos campos do Android.
 */
@OptIn(ExperimentalForeignApi::class)
actual object LocationProvider {

    actual fun init(appContext: Any?) {
        // CoreLocation não precisa de contexto de aplicação.
    }

    actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val location = requestLocation() ?: return null
        return location.coordinate.useContents { latitude to longitude }
    }

    actual suspend fun getCurrentAddress(): CurrentLocationAddress? {
        val location = requestLocation() ?: return null
        val (latitude, longitude) = location.coordinate.useContents { latitude to longitude }
        val placemark = reverseGeocode(location)
        return CurrentLocationAddress(
            latitude = latitude,
            longitude = longitude,
            street = placemark?.thoroughfare?.trim().orEmpty(),
            number = placemark?.subThoroughfare?.trim().orEmpty(),
            neighborhood = placemark?.subLocality?.trim().orEmpty(),
            city = placemark?.locality?.trim().orEmpty(),
            state = placemark?.administrativeArea?.trim().orEmpty(),
            cep = placemark?.postalCode.orEmpty().filter(Char::isDigit).take(8)
        )
    }

    private suspend fun requestLocation(): CLLocation? =
        suspendCancellableCoroutine { continuation ->
            val manager = CLLocationManager()
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didUpdateLocations: List<*>
                ) {
                    val location = didUpdateLocations.lastOrNull() as? CLLocation
                    if (continuation.isActive) {
                        continuation.resume(location?.takeIf(::isUsable))
                    }
                    manager.delegate = null
                }

                override fun locationManager(
                    manager: CLLocationManager,
                    didFailWithError: NSError
                ) {
                    if (continuation.isActive) continuation.resume(null)
                    manager.delegate = null
                }
            }
            manager.delegate = delegate
            manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
            manager.requestWhenInUseAuthorization()
            manager.requestLocation()
            continuation.invokeOnCancellation { manager.delegate = null }
        }

    private suspend fun reverseGeocode(location: CLLocation): CLPlacemark? =
        suspendCancellableCoroutine { continuation ->
            CLGeocoder().reverseGeocodeLocation(location) { placemarks, _ ->
                if (continuation.isActive) {
                    continuation.resume(placemarks?.firstOrNull() as? CLPlacemark)
                }
            }
        }

    private fun isUsable(location: CLLocation): Boolean {
        val (latitude, longitude) = location.coordinate.useContents { latitude to longitude }
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        val accuracy = location.horizontalAccuracy
        return accuracy >= 0.0 && accuracy <= MAX_ACCEPTED_ACCURACY_METERS
    }

    private const val MAX_ACCEPTED_ACCURACY_METERS = 250.0
}
