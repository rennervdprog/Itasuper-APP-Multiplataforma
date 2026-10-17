package com.example.shared.model

data class Banner(
    val id: String,
    val title: String,
    val imageUrl: String,
    val targetStoreId: String? = null,
    val description: String = ""
)
