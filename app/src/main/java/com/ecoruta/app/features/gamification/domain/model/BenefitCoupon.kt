package com.ecoruta.app.features.gamification.domain.model

// Modelos de dominio para el catálogo de beneficios y la gestión de cupones del comercio

// Cupón del catálogo de beneficios visto por el ciudadano
data class BenefitCoupon(
    val id: String,
    val title: String,
    val description: String,
    val storeName: String,
    val pointsCost: Int,
    val availableQuantity: Int,
    val validUntilMillis: Long,
    val discountPercent: Int,
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
