package com.ecoruta.app.core.testing

// Repositorio de gamificación en memoria para pruebas unitarias sin red ni base de datos

import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.MerchantCouponModel
import com.ecoruta.app.features.gamification.domain.model.RedeemedCouponModel
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository

class FakeGamificationRepository : GamificationRepository {

    var error: Throwable? = null
    var redeemError: Throwable? = null

    val catalogCoupons = mutableListOf(
        BenefitCoupon(
            id = "BEN-01",
            title = "10% en Naturalia",
            description = "Solo en frutas y verduras",
            storeName = "Naturalia",
            category = "FOOD",
            pointsCost = 400,
            availableQuantity = 15,
            validUntilMillis = 1_800_000_000_000L,
            discountPercent = 10,
        ),
        BenefitCoupon(
            id = "BEN-02",
            title = "Envío gratis en Merkado",
            description = "Pedidos mayores a $50.000",
            storeName = "Merkado",
            category = "GROCERIES",
            pointsCost = 250,
            availableQuantity = 8,
            validUntilMillis = 1_780_000_000_000L,
            discountPercent = 0,
        ),
    )

    val merchantCoupons = mutableListOf(
        MerchantCouponModel(
            id = "CPN-01",
            title = "10% en toda la tienda",
            description = "Aplica para productos no rebajados",
            pointsCost = 500,
            availableQuantity = 20,
            validUntilMillis = 1_800_000_000_000L,
            isActive = true,
        ),
    )

    var nextRedemption: RedeemedCouponModel = RedeemedCouponModel(
        id = "RDEM-TEST-001",
        couponId = "BEN-01",
        couponTitle = "10% en Naturalia",
        storeName = "Naturalia",
        category = "FOOD",
        pointsCost = 400,
        code = "ECONAT-0001",
        redeemedAtMillis = 1_760_200_000_000L,
        balanceAfter = 850,
    )

    val createCalls = mutableListOf<Map<String, Any>>()
    val updateCalls = mutableListOf<Map<String, Any>>()
    val deactivatedIds = mutableListOf<String>()

    // --- Catálogo ciudadano ---

    override suspend fun getCoupons(): Result<List<BenefitCoupon>> =
        error?.let { Result.failure(it) } ?: Result.success(catalogCoupons.toList())

    override suspend fun getCoupon(id: String): Result<BenefitCoupon> =
        error?.let { Result.failure(it) }
            ?: catalogCoupons.find { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Coupon $id not found"))

    override suspend fun redeemCoupon(id: String): Result<RedeemedCouponModel> =
        redeemError?.let { Result.failure(it) } ?: Result.success(nextRedemption)

    // --- Gestión del comercio ---

    override suspend fun getMerchantCoupons(): Result<List<MerchantCouponModel>> =
        error?.let { Result.failure(it) } ?: Result.success(merchantCoupons.toList())

    override suspend fun getMerchantCoupon(id: String): Result<MerchantCouponModel> =
        error?.let { Result.failure(it) }
            ?: merchantCoupons.find { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("Merchant coupon $id not found"))

    override suspend fun createCoupon(title: String, description: String, pointsCost: Int, quantity: Int, validUntilMillis: Long): Result<Unit> {
        if (error != null) return Result.failure(error!!)
        createCalls.add(mapOf("title" to title, "pointsCost" to pointsCost))
        merchantCoupons.add(MerchantCouponModel("CPN-NEW-${createCalls.size}", title, description, pointsCost, quantity, validUntilMillis, true))
        return Result.success(Unit)
    }

    override suspend fun updateCoupon(id: String, title: String, description: String, pointsCost: Int, quantity: Int, validUntilMillis: Long, isActive: Boolean): Result<Unit> {
        if (error != null) return Result.failure(error!!)
        updateCalls.add(mapOf("id" to id, "title" to title))
        val idx = merchantCoupons.indexOfFirst { it.id == id }
        if (idx >= 0) merchantCoupons[idx] = merchantCoupons[idx].copy(title = title, description = description, pointsCost = pointsCost, availableQuantity = quantity, validUntilMillis = validUntilMillis, isActive = isActive)
        return Result.success(Unit)
    }

    override suspend fun deactivateCoupon(id: String): Result<Unit> {
        if (error != null) return Result.failure(error!!)
        deactivatedIds.add(id)
        return Result.success(Unit)
    }
}
