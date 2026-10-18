package com.example.shared.ui.permissions

import com.example.shared.platform.SecureStorage

/**
 * Migração de `LocationOnboardingPreferences` e `NotificationOnboardingPreferences`
 * do app Android (`ui/permissions/*Onboarding.kt`).
 *
 * Adaptação KMP: o original persistia em `SharedPreferences` via `Context`;
 * aqui a persistência é o [SecureStorage] (multiplatform-settings: SharedPreferences
 * no Android, NSUserDefaults no iOS). A semântica é a mesma — conclusão por par
 * (usuário, dispositivo); o onboarding só aparece quando o usuário ainda não
 * tomou uma decisão.
 */
object LocationOnboardingPreferences {

    private const val KEY_PREFIX = "location_onboarding_completed_"

    fun shouldShow(storage: SecureStorage, userId: String): Boolean {
        if (userId.isBlank()) return false
        return storage.get(KEY_PREFIX + userId) != "true"
    }

    fun markCompleted(storage: SecureStorage, userId: String) {
        if (userId.isBlank()) return
        storage.save(KEY_PREFIX + userId, "true")
    }

    fun reset(storage: SecureStorage, userId: String) {
        if (userId.isBlank()) return
        storage.remove(KEY_PREFIX + userId)
    }
}

object NotificationOnboardingPreferences {

    private const val KEY_PREFIX = "notification_onboarding_completed_"

    fun shouldShow(storage: SecureStorage, userId: String): Boolean {
        if (userId.isBlank()) return false
        return storage.get(KEY_PREFIX + userId) != "true"
    }

    fun markCompleted(storage: SecureStorage, userId: String) {
        if (userId.isBlank()) return
        storage.save(KEY_PREFIX + userId, "true")
    }
}
