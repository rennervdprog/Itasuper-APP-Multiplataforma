package com.example.shared.platform

import com.russhwolf.settings.Settings

// TODO(fase 5): migrar para EncryptedSharedPreferences (androidx.security)
// quando houver dados sensíveis (tokens). Settings() usa SharedPreferences
// comum — adequado apenas para preferências não-sensíveis.
actual class SecureStorage actual constructor() {

    private val settings: Settings = Settings()

    actual fun save(key: String, value: String) {
        settings.putString(key, value)
    }

    actual fun get(key: String): String? = settings.getStringOrNull(key)

    actual fun remove(key: String) {
        settings.remove(key)
    }
}
