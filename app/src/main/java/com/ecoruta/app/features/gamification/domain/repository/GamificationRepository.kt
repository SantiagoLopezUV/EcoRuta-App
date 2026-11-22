package com.ecoruta.app.features.gamification.domain.repository

// Contrato de dominio para catálogo de beneficios y gamificación
// Operaciones abstractas de consulta de cupones, canje y validación en caja

import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.MerchantCouponModel
import com.ecoruta.app.features.gamification.domain.model.RedeemedCouponModel

interface GamificationRepository {

    // --- Catálogo ciudadano ---

    // Retorna todos los cupones activos del catálogo de beneficios
    suspend fun getCoupons(): Result<List<BenefitCoupon>>

    // Retorna un cupón individual por su ID para el detalle de redención
    suspend fun getCoupon(id: String): Result<BenefitCoupon>

    // Canjea un cupón y retorna el modelo con el código de caja y saldo resultante
    suspend fun redeemCoupon(id: String): Result<RedeemedCouponModel>

    // --- Gestión del comercio ---

    // Retorna los cupones publicados por el comercio autenticado
    suspend fun getMerchantCoupons(): Result<List<MerchantCouponModel>>

    // Retorna un cupón del comercio por su ID para pre-poblar el formulario de edición
    suspend fun getMerchantCoupon(id: String): Result<MerchantCouponModel>

    // Crea un cupón nuevo con los datos del formulario validados en el ViewModel
    suspend fun createCoupon(
        title: String,
        description: String,
        pointsCost: Int,
        quantity: Int,
        validUntilMillis: Long,
    ): Result<Unit>

    // Actualiza los campos de un cupón existente; solo aplica los campos no nulos
    suspend fun updateCoupon(
        id: String,
        title: String,
        description: String,
        pointsCost: Int,
        quantity: Int,
        validUntilMillis: Long,
        isActive: Boolean,
    ): Result<Unit>

    // Desactiva un cupón activo para que deje de aparecer en el catálogo de ciudadanos
    suspend fun deactivateCoupon(id: String): Result<Unit>
}
