package com.example.shared.platform

actual object PushNotifications {

    actual fun register() {
        // TODO(fase 5): push nativo no iOS (APNs) ainda não implementado.
        println("ItaSuper: push não implementado no iOS ainda")
    }

    actual suspend fun getToken(): String? {
        // TODO(fase 5): push nativo no iOS (APNs) ainda não implementado.
        println("ItaSuper: push não implementado no iOS ainda")
        return null
    }
}
