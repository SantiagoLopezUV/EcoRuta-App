package com.ecoruta.app.features.gamification.domain.repository

// Contrato de dominio para catálogo de beneficios, gamificación y saldo de EcoPuntos
// Operaciones abstractas de consulta de cupones, canje atómico, saldo e historial de movimientos

import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.EcoPointsLedger
import com.ecoruta.app.features.gamification.domain.model.RedeemedBenefitCoupon
import com.ecoruta.app.features.gamification.presentation.benefits.RedeemedCoupon

interface GamificationRepository {

    // Consulta el saldo disponible de EcoPuntos del ciudadano autenticado
    suspend fun getBalance(): Result<Int>

    // Consulta el historial de movimientos de EcoPuntos (abonos y débitos)
    suspend fun getPointsLedger(): Result<EcoPointsLedger>

    // Consulta el catálogo de cupones de beneficios disponibles con stock
    suspend fun getAvailableCoupons(): Result<List<BenefitCoupon>>

    // Canjea un cupón de forma atómica debitando los EcoPuntos del ciudadano
    suspend fun redeemCoupon(couponId: String): Result<RedeemedCoupon>

    // Consulta el historial de cupones canjeados del ciudadano autenticado
    suspend fun getMyCoupons(): Result<List<RedeemedCoupon>>

    // Consulta el detalle de un cupón canjeado por su identificador
    suspend fun getMyCouponById(redeemedCouponId: String): Result<RedeemedCoupon>
}
