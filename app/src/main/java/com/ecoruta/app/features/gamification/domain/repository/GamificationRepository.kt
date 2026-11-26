package com.ecoruta.app.features.gamification.domain.repository

// Contrato de dominio para catálogo de beneficios, gamificación y saldo de EcoPuntos
// Operaciones abstractas de consulta de cupones, canje atómico, saldo e historial de movimientos

import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.EcoPointsLedger
import com.ecoruta.app.features.gamification.domain.model.RedeemedBenefitCoupon

interface GamificationRepository {

    // Consulta el saldo disponible de EcoPuntos del ciudadano autenticado
    suspend fun getBalance(): Result<Int>

    // Consulta el historial de movimientos de EcoPuntos (abonos y débitos)
    suspend fun getPointsLedger(): Result<EcoPointsLedger>

    // Consulta el catálogo de cupones de beneficios disponibles con stock
    suspend fun getAvailableCoupons(): Result<List<BenefitCoupon>>

    // Retorna un cupón individual del catálogo por su ID para el flujo de redención
    suspend fun getCoupon(couponId: String): Result<BenefitCoupon>

    // Canjea un cupón de forma atómica debitando los EcoPuntos del ciudadano
    suspend fun redeemCoupon(couponId: String): Result<RedeemedBenefitCoupon>

    // Consulta el historial de cupones canjeados del ciudadano autenticado
    suspend fun getMyCoupons(): Result<List<RedeemedBenefitCoupon>>

    // Consulta el detalle de un cupón canjeado por su identificador
    suspend fun getMyCouponById(redeemedCouponId: String): Result<RedeemedBenefitCoupon>

    // Registra un nuevo cupón de descuento asociado al comercio aliado autenticado
    suspend fun createCoupon(
        title: String,
        pointsCost: Int,
        quantityAvailable: Int,
        validUntilMillis: Long? = null,
        category: String = "general",
        description: String = "",
    ): Result<BenefitCoupon>

    // Actualiza los datos de un cupón existente verificando pertenencia al comercio
    suspend fun updateCoupon(
        couponId: String,
        title: String,
        pointsCost: Int,
        quantityAvailable: Int,
        validUntilMillis: Long? = null,
        category: String = "general",
        description: String = "",
        isActive: Boolean = true,
    ): Result<BenefitCoupon>

    // Desactiva un cupón del comercio aliado mediante baja lógica sin eliminar redenciones históricas
    suspend fun deactivateCoupon(couponId: String): Result<Unit>

    // Consulta todos los cupones creados por el comercio autenticado (activos e inactivos)
    suspend fun getMerchantCoupons(): Result<List<BenefitCoupon>>

    // Retorna un cupón del comercio por su ID para pre-poblar el formulario de edición
    suspend fun getMerchantCoupon(couponId: String): Result<BenefitCoupon>
}
