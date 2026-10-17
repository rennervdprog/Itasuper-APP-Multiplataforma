package com.example.shared.platform

/**
 * Armazenamento chave-valor persistente — expect/actual (Fase 5).
 *
 * Implementado sobre `multiplatform-settings` (SharedPreferences no Android,
 * NSUserDefaults no iOS).
 *
 * TODO(fase 5): para dados sensíveis (tokens de sessão/push), trocar o backend
 * por EncryptedSharedPreferences (Android) / Keychain (iOS). Hoje o conteúdo
 * é persistido sem criptografia — adequado apenas para preferências.
 */
expect class SecureStorage() {

    /** Salva (ou sobrescreve) um valor de texto. */
    fun save(key: String, value: String)

    /** Lê um valor salvo, ou null se a chave não existir. */
    fun get(key: String): String?

    /** Remove a chave. */
    fun remove(key: String)
}
