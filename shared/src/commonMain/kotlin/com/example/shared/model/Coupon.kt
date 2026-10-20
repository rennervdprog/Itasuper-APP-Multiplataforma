package com.example.shared.model

data class Coupon(
    val id: String = "",
    val code: String,
    val description: String = "",
    val discountType: String = "fixed", // "fixed" or "percentage"
    val discountValue: Double,
    val minOrderValue: Double = 0.0,
    val expiresAt: String? = null,
    val firstOrderOnly: Boolean = false,
    val maxUses: Int? = null,
    val usedCount: Int = 0,
    val isActive: Boolean = true,
    val storeId: String? = null
)
