package com.example.shared.platform

actual object PushNotifications {

    actual fun init(appContext: Any?) {
        // Push nativo no iOS (APNs) fica para quando houver conta Apple Developer.
    }

    actual fun register() {
        // Push nativo no iOS (APNs) ainda não implementado.
        println("ItaSuper: push não implementado no iOS ainda")
    }

    actual suspend fun getToken(): String? {
        // Push nativo no iOS (APNs) ainda não implementado.
        return null
    }
}
