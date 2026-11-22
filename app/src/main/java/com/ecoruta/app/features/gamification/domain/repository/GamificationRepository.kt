package com.ecoruta.app.features.gamification.domain.repository

// Contrato de dominio para catálogo de beneficios y gamificación
// Operaciones abstractas de consulta de cupones, canje y validación en caja

import com.ecoruta.app.features.gamification.domain.model.MerchantCouponModel

interface GamificationRepository {

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
