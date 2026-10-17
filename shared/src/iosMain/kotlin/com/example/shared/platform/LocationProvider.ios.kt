package com.example.shared.platform

actual object LocationProvider {

    actual fun init(appContext: Any?) {
        // TODO(fase 5): localização no iOS (CoreLocation) ainda não implementada.
    }

    actual suspend fun getCurrentLocation(): Pair<Double, Double>? {
        // TODO(fase 5): localização no iOS (CoreLocation) ainda não implementada.
        println("ItaSuper: localização não implementada no iOS ainda")
        return null
    }
}
