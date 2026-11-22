package com.ecoruta.app.core.testing

// Repositorio de gamificación en memoria para pruebas unitarias sin red ni base de datos

import com.ecoruta.app.features.gamification.domain.model.MerchantCouponModel
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository

class FakeGamificationRepository : GamificationRepository {

    var error: Throwable? = null

    val coupons = mutableListOf(
        MerchantCouponModel(
            id = "CPN-01",
            title = "10% en toda la tienda",
            description = "Aplica para productos no rebajados",
            pointsCost = 500,
            availableQuantity = 20,
            validUntilMillis = 1_800_000_000_000L,
            isActive = true,
        ),
        MerchantCouponModel(
            id = "CPN-02",
            title = "Envío gratis",
            description = "Solo pedidos mayores a $50.000",
            pointsCost = 300,
            availableQuantity = 5,
            validUntilMillis = 1_780_000_000_000L,
            isActive = true,
        ),
    )

    val createCalls = mutableListOf<Map<String, Any>>()
    val updateCalls = mutableListOf<Map<String, Any>>()
    val deactivatedIds = mutableListOf<String>()

    override suspend fun getMerchantCoupons(): Result<List<MerchantCouponModel>> =
        error?.let { Result.failure(it) } ?: Result.success(coupons.toList())

    override suspend fun getMerchantCoupon(id: String): Result<MerchantCouponModel> =
        error?.let { Result.failure(it) }
            ?: coupons.find { it.id == id }
                ?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Coupon $id not found"))

    override suspend fun createCoupon(
        title: String,
        description: String,
        pointsCost: Int,
        quantity: Int,
        validUntilMillis: Long,
    ): Result<Unit> {
        if (error != null) return Result.failure(error!!)
        createCalls.add(mapOf("title" to title, "pointsCost" to pointsCost, "quantity" to quantity))
        coupons.add(
            MerchantCouponModel(
                id = "CPN-NEW-${createCalls.size}",
                title = title,
                description = description,
                pointsCost = pointsCost,
                availableQuantity = quantity,
                validUntilMillis = validUntilMillis,
                isActive = true,
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun updateCoupon(
        id: String,
        title: String,
        description: String,
        pointsCost: Int,
        quantity: Int,
        validUntilMillis: Long,
        isActive: Boolean,
    ): Result<Unit> {
        if (error != null) return Result.failure(error!!)
        updateCalls.add(mapOf("id" to id, "title" to title, "isActive" to isActive))
        val idx = coupons.indexOfFirst { it.id == id }
        if (idx >= 0) coupons[idx] = coupons[idx].copy(title = title, description = description, pointsCost = pointsCost, availableQuantity = quantity, validUntilMillis = validUntilMillis, isActive = isActive)
        return Result.success(Unit)
    }

    override suspend fun deactivateCoupon(id: String): Result<Unit> {
        if (error != null) return Result.failure(error!!)
        deactivatedIds.add(id)
        val idx = coupons.indexOfFirst { it.id == id }
        if (idx >= 0) coupons[idx] = coupons[idx].copy(isActive = false)
        return Result.success(Unit)
    }
}
