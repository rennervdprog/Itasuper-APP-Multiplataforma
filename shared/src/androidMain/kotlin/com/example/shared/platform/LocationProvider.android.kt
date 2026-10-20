package com.example.shared.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Porte do `CurrentLocationProvider` do app nativo: obtém uma posição atual
 * (não cacheada), filtra por precisão aceitável (≤ 250 m) e geocodifica
 * para endereço com o Geocoder do Android.
 */
actual object LocationProvider {

    private var appContext: Context? = null

    actual fun init(appContext: Any?) {
        this.appContext = appContext as? Context
    }

    actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
        val context = appContext?.applicationContext ?: return null
        val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null

        return suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            try {
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(
                        if (hasFine) Priority.PRIORITY_HIGH_ACCURACY
                        else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellation.token
                    )
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location?.takeIf(::isUsable)?.let { it.latitude to it.longitude })
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (_: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
            continuation.invokeOnCancellation { cancellation.cancel() }
        }
    }

    actual suspend fun getCurrentAddress(): CurrentLocationAddress? {
        val context = appContext?.applicationContext ?: return null
        val coords = getCurrentLocation() ?: return null
        var street = ""
        var number = ""
        var neighborhood = ""
        var city = ""
        var state = ""
        var cep = ""
        try {
            val address = withContext(Dispatchers.IO) {
                val geocoder = Geocoder(context, Locale("pt", "BR"))
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(coords.first, coords.second, 1)?.firstOrNull()
            }
            if (address != null) {
                street = address.thoroughfare.orEmpty().trim()
                number = address.subThoroughfare.orEmpty().trim()
                neighborhood = (address.subLocality ?: address.subAdminArea).orEmpty().trim()
                city = (address.locality ?: address.subAdminArea ?: address.adminArea).orEmpty().trim()
                state = address.adminArea.orEmpty().trim()
                cep = address.postalCode.orEmpty().filter(Char::isDigit).take(8)
            }
        } catch (_: Exception) {
            // A coordenada ainda pode ser usada para o catálogo, mas não vira
            // um endereço inventado (igual ao app original).
        }
        return CurrentLocationAddress(
            latitude = coords.first,
            longitude = coords.second,
            street = street,
            number = number,
            neighborhood = neighborhood,
            city = city,
            state = state,
            cep = cep
        )
    }

    private fun isUsable(location: android.location.Location): Boolean {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()) return false
        return !location.hasAccuracy() || location.accuracy <= MAX_ACCEPTED_ACCURACY_METERS
    }

    private const val MAX_ACCEPTED_ACCURACY_METERS = 250f
}
