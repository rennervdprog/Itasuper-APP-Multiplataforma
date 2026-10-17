package com.example.shared.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Simplificação do `CurrentLocationProvider` do app nativo: obtém uma posição
 * atual (não cacheada) e filtra por precisão aceitável (≤ 250 m).
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

    private fun isUsable(location: android.location.Location): Boolean {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()) return false
        return !location.hasAccuracy() || location.accuracy <= MAX_ACCEPTED_ACCURACY_METERS
    }

    private const val MAX_ACCEPTED_ACCURACY_METERS = 250f
}
