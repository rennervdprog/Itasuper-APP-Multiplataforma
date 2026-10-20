package com.example.shared.session

import com.example.shared.platform.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lojas favoritas do usuário — porte do FavoriteStoresStorage do app
 * original: conjunto LOCAL de IDs (sem backend), persistido no aparelho.
 * No original é SharedPreferences "itasuper_favorites"; aqui usa o
 * SecureStorage multiplataforma com a mesma chave lógica.
 */
object FavoriteStoresRepository {

    private const val KEY_FAVORITES = "favorite_store_ids"

    private val storage = SecureStorage()

    private val _favorites = MutableStateFlow(readFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    fun isFavorite(storeId: String): Boolean = _favorites.value.contains(storeId)

    /** Alterna o favorito e devolve o novo estado (true = favoritada). */
    fun toggleFavorite(storeId: String): Boolean {
        val current = _favorites.value.toMutableSet()
        val isNowFavorite = if (current.contains(storeId)) {
            current.remove(storeId)
            false
        } else {
            current.add(storeId)
            true
        }
        persist(current)
        _favorites.value = current
        return isNowFavorite
    }

    private fun readFavorites(): Set<String> {
        val raw = try {
            storage.get(KEY_FAVORITES)
        } catch (_: Throwable) {
            null
        }
        return raw?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()
    }

    private fun persist(favorites: Set<String>) {
        try {
            if (favorites.isEmpty()) {
                storage.remove(KEY_FAVORITES)
            } else {
                storage.save(KEY_FAVORITES, favorites.joinToString(","))
            }
        } catch (_: Throwable) {
            // Persistência local é best-effort; o estado em memória segue valendo.
        }
    }
}
