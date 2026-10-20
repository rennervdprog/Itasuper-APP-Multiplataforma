package com.example.shared.model

/**
 * Avaliação pública de uma loja (tabela `reviews` + tags/respostas).
 * Porte tipado do que o app original lê como JSON em getStoreReviews.
 */
data class StoreReview(
    val id: String,
    val authorId: String,
    val authorName: String = "Cliente",
    val rating: Int = 0,
    val comment: String = "",
    val createdAt: String = "",
    val tags: List<Pair<String, String>> = emptyList(),
    val responses: List<StoreReviewResponse> = emptyList()
)

/** Resposta da loja a uma avaliação (`review_responses`). */
data class StoreReviewResponse(
    val message: String = "",
    val createdAt: String = ""
)

/**
 * Agregado de avaliações da loja em 90 dias (`store_rating_aggregates`):
 * nota média, total e distribuição por estrela (dist_5..dist_1).
 */
data class StoreRatingAggregate(
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    val distribution: Map<Int, Int> = emptyMap()
)
