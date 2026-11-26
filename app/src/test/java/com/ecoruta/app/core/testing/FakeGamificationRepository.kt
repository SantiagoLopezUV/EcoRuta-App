package com.ecoruta.app.core.testing

// Repositorio de gamificación en memoria para pruebas unitarias sin red ni base de datos
// Permite simular saldo, historial de movimientos, cupones y canjes de EcoPuntos

import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.EcoPointsLedger
import com.ecoruta.app.features.gamification.domain.model.PointsTransaction
import com.ecoruta.app.features.gamification.domain.model.RedeemedBenefitCoupon
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository

class FakeGamificationRepository : GamificationRepository {

    // Permite forzar un error simulado en todas las operaciones
    var error: Throwable? = null

    // Saldo inicial de EcoPuntos simulado para las pruebas
    var balance: Int = 1_250

    // Historial de movimientos de ejemplo en memoria
    val movements = mutableListOf(
        PointsTransaction("MOV-01", "Entrega de material reciclable", 1_760_100_000_000L, 80),
        PointsTransaction("MOV-02", "Canje en Café del Río", 1_760_050_000_000L, -450),
        PointsTransaction("MOV-03", "Módulo educativo completado", 1_759_900_000_000L, 50),
    )

    // Catálogo de cupones disponibles en memoria
    val availableCoupons = mutableListOf(
        BenefitCoupon(
            id = "CUP-01",
            businessName = "Café del Río",
            title = "20 % de descuento en bebidas calientes",
            subtitle = "Válido de lunes a viernes",
            pointsCost = 450,
            availableQuantity = 18,
            validUntilMillis = 1_795_996_800_000L,
            conditions = listOf("Un cupón por compra", "Presenta el código QR en caja"),
            category = "food",
        ),
        BenefitCoupon(
            id = "CUP-02",
            businessName = "Tienda Don Julio",
            title = "5 % de descuento en mercado",
            subtitle = "Aplica en productos de aseo",
            pointsCost = 300,
            availableQuantity = 10,
            validUntilMillis = 1_795_996_800_000L,
            conditions = listOf("Mínimo de compra \$30.000"),
            category = "groceries",
        ),
    )

    // Cupones canjeados en memoria del ciudadano autenticado
    val redeemedCoupons = mutableListOf(
        RedeemedBenefitCoupon(
            id = "RED-01",
            couponId = "CUP-01",
            businessName = "Café del Río",
            title = "20 % de descuento en bebidas calientes",
            code = "ECO-CAFE-8492",
            redeemedAtMillis = 1_760_050_000_000L,
            pointsSpent = 450,
            validUntilMillis = 1_795_996_800_000L,
        ),
    )

    // Cupones propios creados por el comercio en memoria para pruebas de gestión
    val coupons = mutableListOf(
        BenefitCoupon(
            id = "CPN-01",
            businessName = "Comercio de Prueba",
            title = "10% en toda la tienda",
            subtitle = "Aplica para productos no rebajados",
            pointsCost = 500,
            availableQuantity = 20,
            validUntilMillis = 1_800_000_000_000L,
            conditions = emptyList(),
            category = "general",
            isActive = true,
        ),
        BenefitCoupon(
            id = "CPN-02",
            businessName = "Comercio de Prueba",
            title = "Envío gratis",
            subtitle = "Solo pedidos mayores a \$50.000",
            pointsCost = 300,
            availableQuantity = 5,
            validUntilMillis = 1_780_000_000_000L,
            conditions = emptyList(),
            category = "shipping",
            isActive = true,
        ),
    )

    // Listas de seguimiento para verificar llamadas en tests
    val createCalls = mutableListOf<Map<String, Any>>()
    val updateCalls = mutableListOf<Map<String, Any>>()
    val deactivatedIds = mutableListOf<String>()

    override suspend fun getBalance(): Result<Int> = result { balance }

    override suspend fun getPointsLedger(): Result<EcoPointsLedger> = result {
        EcoPointsLedger(balance = balance, movements = movements.toList())
    }

    override suspend fun getAvailableCoupons(): Result<List<BenefitCoupon>> = result {
        availableCoupons.filter { it.availableQuantity > 0 }.toList()
    }

    override suspend fun redeemCoupon(couponId: String): Result<RedeemedBenefitCoupon> = result {
        require(couponId.isNotBlank()) { "El identificador del cupón no puede estar vacío" }
        val coupon = availableCoupons.find { it.id == couponId }
            ?: error("Cupón no encontrado con id: $couponId")

        require(balance >= coupon.pointsCost) {
            "Saldo insuficiente: tienes $balance EcoPuntos y el cupón cuesta ${coupon.pointsCost}"
        }

        balance -= coupon.pointsCost

        val redeemed = RedeemedBenefitCoupon(
            id = "RED-${redeemedCoupons.size + 1}",
            couponId = coupon.id,
            businessName = coupon.businessName,
            title = coupon.title,
            code = "ECO-${coupon.businessName.take(4).uppercase()}-${(1000..9999).random()}",
            redeemedAtMillis = System.currentTimeMillis(),
            pointsSpent = coupon.pointsCost,
            validUntilMillis = coupon.validUntilMillis,
        )
        redeemedCoupons.add(redeemed)
        redeemed
    }

    override suspend fun getMyCoupons(): Result<List<RedeemedBenefitCoupon>> = result {
        redeemedCoupons.sortedByDescending { it.redeemedAtMillis }
    }

    override suspend fun getMyCouponById(redeemedCouponId: String): Result<RedeemedBenefitCoupon> = result {
        require(redeemedCouponId.isNotBlank()) { "El identificador del cupón canjeado no puede estar vacío" }
        redeemedCoupons.find { it.id == redeemedCouponId }
            ?: error("Cupón canjeado no encontrado con id: $redeemedCouponId")
    }

    override suspend fun createCoupon(
        title: String,
        pointsCost: Int,
        quantityAvailable: Int,
        validUntilMillis: Long?,
        category: String,
        description: String,
    ): Result<BenefitCoupon> = result {
        require(title.isNotBlank()) { "El título del cupón no puede estar vacío" }
        require(pointsCost > 0) { "El costo en puntos debe ser mayor a cero" }
        require(quantityAvailable >= 0) { "La cantidad disponible no puede ser negativa" }

        createCalls.add(mapOf("title" to title, "pointsCost" to pointsCost, "quantityAvailable" to quantityAvailable))

        val created = BenefitCoupon(
            id = "CPN-${coupons.size + 100}",
            businessName = "Comercio de Prueba",
            title = title,
            subtitle = description,
            pointsCost = pointsCost,
            availableQuantity = quantityAvailable,
            validUntilMillis = validUntilMillis ?: (System.currentTimeMillis() + 86400000L * 30),
            conditions = emptyList(),
            category = category,
            isActive = true,
        )
        coupons.add(created)
        availableCoupons.add(created)
        created
    }

    override suspend fun updateCoupon(
        couponId: String,
        title: String,
        pointsCost: Int,
        quantityAvailable: Int,
        validUntilMillis: Long?,
        category: String,
        description: String,
        isActive: Boolean,
    ): Result<BenefitCoupon> = result {
        require(couponId.isNotBlank()) { "El identificador del cupón no puede estar vacío" }
        require(title.isNotBlank()) { "El título del cupón no puede estar vacío" }
        require(pointsCost > 0) { "El costo en puntos debe ser mayor a cero" }
        require(quantityAvailable >= 0) { "La cantidad disponible no puede ser negativa" }

        updateCalls.add(mapOf("id" to couponId, "title" to title, "isActive" to isActive))

        val index = coupons.indexOfFirst { it.id == couponId }
        val existing = if (index != -1) coupons[index] else availableCoupons.find { it.id == couponId }
            ?: error("Cupón no encontrado con id: $couponId")

        val updated = existing.copy(
            title = title,
            subtitle = description,
            pointsCost = pointsCost,
            availableQuantity = quantityAvailable,
            validUntilMillis = validUntilMillis ?: existing.validUntilMillis,
            category = category,
            isActive = isActive,
        )
        if (index != -1) {
            coupons[index] = updated
        } else {
            coupons.add(updated)
        }
        val availIndex = availableCoupons.indexOfFirst { it.id == couponId }
        if (availIndex != -1) availableCoupons[availIndex] = updated
        updated
    }

    override suspend fun deactivateCoupon(couponId: String): Result<Unit> = result {
        require(couponId.isNotBlank()) { "El identificador del cupón no puede estar vacío" }
        deactivatedIds.add(couponId)
        val index = coupons.indexOfFirst { it.id == couponId }
        if (index != -1) coupons[index] = coupons[index].copy(isActive = false)
        val availIndex = availableCoupons.indexOfFirst { it.id == couponId }
        if (availIndex != -1) availableCoupons[availIndex] = availableCoupons[availIndex].copy(isActive = false)
        Unit
    }

    override suspend fun getMerchantCoupons(): Result<List<BenefitCoupon>> = result {
        coupons.toList()
    }

    override suspend fun getMerchantCoupon(couponId: String): Result<BenefitCoupon> = result {
        coupons.find { it.id == couponId }
            ?: error("Cupón no encontrado con id: $couponId")
    }

    private inline fun <T> result(block: () -> T): Result<T> =
        error?.let { Result.failure(it) } ?: runCatching(block)
}
