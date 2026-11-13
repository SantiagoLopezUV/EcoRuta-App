package com.ecoruta.app.features.gamification.data.repository

// Implementación del repositorio de gamificación y cupones comerciales
// Consulta catálogo, saldo, historial de movimientos y redime cupones con operación atómica en Supabase

import com.ecoruta.app.features.gamification.data.remote.dto.EcoPointsTransactionDto
import com.ecoruta.app.features.gamification.data.remote.dto.toDomain
import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.EcoPointsLedger
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository
import com.ecoruta.app.features.gamification.presentation.benefits.CouponCategory
import com.ecoruta.app.features.gamification.presentation.benefits.RedeemedCoupon
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class GamificationRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
) : GamificationRepository {

    // Consulta el saldo disponible de EcoPuntos del ciudadano autenticado en la tabla de perfiles
    override suspend fun getBalance(): Result<Int> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar el saldo de EcoPuntos")

        val row = postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Map<String, Any?>>()

        (row?.get("eco_points") as? Number)?.toInt() ?: 0
    }

    // Consulta el historial completo de movimientos de EcoPuntos (abonos y débitos)
    override suspend fun getPointsLedger(): Result<EcoPointsLedger> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar el historial de EcoPuntos")

        val transactions = postgrest.from("eco_points_transactions")
            .select {
                filter { eq("citizen_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<EcoPointsTransactionDto>()
            .map { it.toDomain() }

        // Calcula el saldo actual como suma de todos los deltas
        val balance = transactions.sumOf { it.delta }

        EcoPointsLedger(balance = balance, movements = transactions)
    }

    // Consulta el catálogo de cupones disponibles con stock mayor a cero
    override suspend fun getAvailableCoupons(): Result<List<BenefitCoupon>> = runCatching {
        // Nota: La conversión de categoría del DB al enum se realiza con fallback a FOOD
        postgrest.from("coupons")
            .select { filter { gt("available_quantity", 0) } }
            .decodeList<Map<String, Any?>>()
            .map { row ->
                BenefitCoupon(
                    id = row["id"].toString(),
                    businessName = row["business_name"].toString(),
                    title = row["title"].toString(),
                    subtitle = row["subtitle"]?.toString().orEmpty(),
                    pointsCost = (row["points_cost"] as? Number)?.toInt() ?: 0,
                    availableQuantity = (row["available_quantity"] as? Number)?.toInt() ?: 0,
                    validUntilMillis = (row["valid_until_millis"] as? Number)?.toLong() ?: 0L,
                    conditions = emptyList(),
                    category = row["category"]?.toString().orEmpty(),
                )
            }
    }

    // Canjea un cupón debitando los puntos del ciudadano de forma atómica (RPC Supabase)
    override suspend fun redeemCoupon(couponId: String): Result<RedeemedCoupon> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para canjear el cupón")

        // Llama a una función RPC atómica en Supabase que valida puntos y registra el canje
        val result = postgrest.rpc("redeem_coupon", mapOf("p_citizen_id" to userId, "p_coupon_id" to couponId))
            .decodeSingleOrNull<Map<String, Any?>>()

        val couponId2 = result?.get("coupon_id")?.toString() ?: couponId
        RedeemedCoupon(
            id = result?.get("id")?.toString() ?: "",
            coupon = com.ecoruta.app.features.gamification.presentation.benefits.Coupon(
                id = couponId2,
                businessName = "",
                title = "",
                subtitle = "",
                category = CouponCategory.FOOD,
                pointsCost = 0,
                availableQuantity = 0,
                validUntilMillis = 0L,
                conditions = emptyList(),
            ),
            code = result?.get("code")?.toString() ?: "",
            redeemedAtMillis = System.currentTimeMillis(),
            pointsSpent = (result?.get("points_spent") as? Number)?.toInt() ?: 0,
        )
    }

    // Consulta el historial de cupones canjeados por el ciudadano autenticado
    override suspend fun getMyCoupons(): Result<List<RedeemedCoupon>> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar mis cupones")

        // Consulta una vista que une redeemed_coupons con la tabla de cupones originales
        postgrest.from("citizen_redeemed_coupons")
            .select {
                filter { eq("citizen_id", userId) }
                order("redeemed_at", Order.DESCENDING)
            }
            .decodeList<Map<String, Any?>>()
            .map { row ->
                val coupon = com.ecoruta.app.features.gamification.presentation.benefits.Coupon(
                    id = row["coupon_id"]?.toString() ?: "",
                    businessName = row["business_name"]?.toString() ?: "",
                    title = row["title"]?.toString() ?: "",
                    subtitle = row["subtitle"]?.toString() ?: "",
                    category = CouponCategory.FOOD,
                    pointsCost = (row["points_cost"] as? Number)?.toInt() ?: 0,
                    availableQuantity = 0,
                    validUntilMillis = (row["valid_until_millis"] as? Number)?.toLong() ?: 0L,
                    conditions = emptyList(),
                )
                RedeemedCoupon(
                    id = row["id"]?.toString() ?: "",
                    coupon = coupon,
                    code = row["code"]?.toString() ?: "",
                    redeemedAtMillis = (row["redeemed_at_millis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    pointsSpent = (row["points_spent"] as? Number)?.toInt() ?: 0,
                )
            }
    }

    // Consulta el detalle de un cupón canjeado por su identificador único
    override suspend fun getMyCouponById(redeemedCouponId: String): Result<RedeemedCoupon> = runCatching {
        require(redeemedCouponId.isNotBlank()) { "El identificador del cupón canjeado no puede estar vacío" }

        val row = postgrest.from("citizen_redeemed_coupons")
            .select { filter { eq("id", redeemedCouponId) } }
            .decodeSingleOrNull<Map<String, Any?>>()
            ?: error("Cupón canjeado no encontrado con id: $redeemedCouponId")

        val coupon = com.ecoruta.app.features.gamification.presentation.benefits.Coupon(
            id = row["coupon_id"]?.toString() ?: "",
            businessName = row["business_name"]?.toString() ?: "",
            title = row["title"]?.toString() ?: "",
            subtitle = row["subtitle"]?.toString() ?: "",
            category = CouponCategory.FOOD,
            pointsCost = (row["points_cost"] as? Number)?.toInt() ?: 0,
            availableQuantity = 0,
            validUntilMillis = (row["valid_until_millis"] as? Number)?.toLong() ?: 0L,
            conditions = emptyList(),
        )
        RedeemedCoupon(
            id = row["id"]?.toString() ?: "",
            coupon = coupon,
            code = row["code"]?.toString() ?: "",
            redeemedAtMillis = (row["redeemed_at_millis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            pointsSpent = (row["points_spent"] as? Number)?.toInt() ?: 0,
        )
    }
}
