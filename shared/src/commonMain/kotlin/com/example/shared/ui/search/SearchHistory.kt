package com.example.shared.ui.search

import com.example.shared.platform.SecureStorage

/**
 * Porte do SearchHistoryRepository do app original: termos recentes da busca
 * (máximo 5, mínimo 2 caracteres, sem duplicados), persistidos localmente.
 */
object SearchHistory {

    private const val KEY = "search_history_terms_v1"
    private const val MAX_TERMS = 5

    private val storage by lazy { SecureStorage() }

    fun load(): List<String> = try {
        storage.get(KEY)
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.take(MAX_TERMS)
            ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }

    /** Adiciona o termo no topo e devolve a lista atualizada (já persistida). */
    fun add(term: String, current: List<String>): List<String> {
        val clean = term.trim()
        if (clean.length < 2) return current
        val updated = (listOf(clean) + current.filterNot { it == clean }).take(MAX_TERMS)
        try {
            storage.save(KEY, updated.joinToString("\n"))
        } catch (_: Throwable) {
            // Persistência é melhor esforço; a lista em memória continua válida.
        }
        return updated
    }

    fun clear() {
        try {
            storage.remove(KEY)
        } catch (_: Throwable) {
        }
    }
}
