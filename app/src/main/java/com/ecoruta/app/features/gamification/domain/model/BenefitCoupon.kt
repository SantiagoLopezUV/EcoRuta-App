package com.ecoruta.app.features.gamification.domain.model

// Modelo de dominio limpio para cupones de beneficios comerciales
// Porcentaje de descuento, comercio aliado, costo en EcoPuntos y vigencia

data class BenefitCoupon(
    val id: String,
    val businessName: String,
    val title: String,
    val subtitle: String,
    val pointsCost: Int,
    val availableQuantity: Int,
    val validUntilMillis: Long,
    val conditions: List<String>,
    val category: String,
)

data class RedeemedBenefitCoupon(
    val id: String,
    val couponId: String,
    val businessName: String,
    val title: String,
    // Código alfanumérico único para presentar en caja (ej: ECO-CAFE-8492)
    val code: String,
    val redeemedAtMillis: Long,
    val pointsSpent: Int,
    val validUntilMillis: Long,
)
