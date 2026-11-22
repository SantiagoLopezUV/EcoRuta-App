package com.ecoruta.app.features.gamification.domain.model

// Modelos de dominio para el catálogo de beneficios y la gestión de cupones del comercio

// Cupón del catálogo de beneficios visto por el ciudadano con su categoría y condiciones
data class BenefitCoupon(
    val id: String,
    val title: String,
    val description: String,
    val storeName: String,
    val category: String,
    val pointsCost: Int,
    val availableQuantity: Int,
    val validUntilMillis: Long,
    val discountPercent: Int,
    val conditions: List<String> = emptyList(),
)

// Cupón ya redimido por el ciudadano con su código de caja y fecha de canje
data class RedeemedCouponModel(
    val id: String,
    val couponId: String,
    val couponTitle: String,
    val storeName: String,
    val category: String,
    val pointsCost: Int,
    val code: String,
    val redeemedAtMillis: Long,
    val balanceAfter: Int,
)

// Cupón gestionado por el comercio aliado con sus métricas de disponibilidad
data class MerchantCouponModel(
    val id: String,
    val title: String,
    val description: String,
    val pointsCost: Int,
    val availableQuantity: Int,
    val validUntilMillis: Long,
    val isActive: Boolean,
)
