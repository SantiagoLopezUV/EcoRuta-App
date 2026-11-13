package com.ecoruta.app.core.testing

// Repositorio de gamificación en memoria para pruebas unitarias sin red ni base de datos
// Permite simular saldo, historial de movimientos, cupones y canjes de EcoPuntos

import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.EcoPointsLedger
import com.ecoruta.app.features.gamification.domain.model.PointsTransaction
import com.ecoruta.app.features.gamification.domain.model.RedeemedBenefitCoupon
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository
import com.ecoruta.app.features.gamification.presentation.benefits.Coupon
import com.ecoruta.app.features.gamification.presentation.benefits.CouponCategory
import com.ecoruta.app.features.gamification.presentation.benefits.RedeemedCoupon

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
        RedeemedCoupon(
            id = "RED-01",
            coupon = Coupon(
                id = "CUP-01",
                businessName = "Café del Río",
                title = "20 % de descuento en bebidas calientes",
                subtitle = "Válido de lunes a viernes",
                category = CouponCategory.FOOD,
                pointsCost = 450,
                availableQuantity = 18,
                validUntilMillis = 1_795_996_800_000L,
                conditions = listOf("Un cupón por compra"),
            ),
            code = "ECO-CAFE-8492",
            redeemedAtMillis = 1_760_050_000_000L,
            pointsSpent = 450,
        ),
    )

    override suspend fun getBalance(): Result<Int> = result { balance }

    override suspend fun getPointsLedger(): Result<EcoPointsLedger> = result {
        EcoPointsLedger(balance = balance, movements = movements.toList())
    }

    override suspend fun getAvailableCoupons(): Result<List<BenefitCoupon>> = result {
        availableCoupons.filter { it.availableQuantity > 0 }.toList()
    }

    override suspend fun redeemCoupon(couponId: String): Result<RedeemedCoupon> = result {
        require(couponId.isNotBlank()) { "El identificador del cupón no puede estar vacío" }
        val coupon = availableCoupons.find { it.id == couponId }
            ?: error("Cupón no encontrado con id: $couponId")

        require(balance >= coupon.pointsCost) {
            "Saldo insuficiente: tienes $balance EcoPuntos y el cupón cuesta ${coupon.pointsCost}"
        }

        balance -= coupon.pointsCost

        val uiCoupon = Coupon(
            id = coupon.id,
            businessName = coupon.businessName,
            title = coupon.title,
            subtitle = coupon.subtitle,
            category = CouponCategory.FOOD,
            pointsCost = coupon.pointsCost,
            availableQuantity = coupon.availableQuantity,
            validUntilMillis = coupon.validUntilMillis,
            conditions = coupon.conditions,
        )
        val redeemed = RedeemedCoupon(
            id = "RED-${redeemedCoupons.size + 1:02d}",
            coupon = uiCoupon,
            code = "ECO-${coupon.businessName.take(4).uppercase()}-${(1000..9999).random()}",
            redeemedAtMillis = System.currentTimeMillis(),
            pointsSpent = coupon.pointsCost,
        )
        redeemedCoupons.add(redeemed)
        redeemed
    }

    override suspend fun getMyCoupons(): Result<List<RedeemedCoupon>> = result {
        redeemedCoupons.sortedByDescending { it.redeemedAtMillis }
    }

    override suspend fun getMyCouponById(redeemedCouponId: String): Result<RedeemedCoupon> = result {
        require(redeemedCouponId.isNotBlank()) { "El identificador del cupón canjeado no puede estar vacío" }
        redeemedCoupons.find { it.id == redeemedCouponId }
            ?: error("Cupón canjeado no encontrado con id: $redeemedCouponId")
    }

    private inline fun <T> result(block: () -> T): Result<T> =
        error?.let { Result.failure(it) } ?: runCatching(block)
}
