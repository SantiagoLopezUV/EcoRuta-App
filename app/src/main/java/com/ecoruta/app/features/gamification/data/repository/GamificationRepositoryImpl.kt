package com.ecoruta.app.features.gamification.data.repository

// Implementación del repositorio de gamificación y cupones comerciales
// Consulta catálogo, saldo, historial de movimientos y redime cupones con operación atómica en Supabase

import com.ecoruta.app.features.gamification.data.remote.dto.EcoPointsTransactionDto
import com.ecoruta.app.features.gamification.data.remote.dto.toDomain
import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.EcoPointsLedger
import com.ecoruta.app.features.gamification.domain.model.RedeemedBenefitCoupon
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class GamificationRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
) : GamificationRepository {

    // Consulta el saldo disponible de EcoPuntos del ciudadano autenticado en la tabla citizens
    override suspend fun getBalance(): Result<Int> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar el saldo de EcoPuntos")

        val row = postgrest.from("citizens")
            .select { filter { eq("profile_id", userId) } }
            .decodeSingleOrNull<Map<String, Any?>>()

        (row?.get("ecopoints_balance") as? Number)?.toInt() ?: 0
    }

    // Consulta el historial completo de movimientos de EcoPuntos (abonos y débitos)
    override suspend fun getPointsLedger(): Result<EcoPointsLedger> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar el historial de EcoPuntos")

        val transactions = postgrest.from("ecopoints_transactions")
            .select {
                filter { eq("citizen_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<EcoPointsTransactionDto>()
            .map { it.toDomain() }

        val balance = transactions.sumOf { it.delta }

        EcoPointsLedger(balance = balance, movements = transactions)
    }

    // Consulta el catálogo de cupones activos disponibles con stock mayor a cero
    override suspend fun getAvailableCoupons(): Result<List<BenefitCoupon>> = runCatching {
        postgrest.from("coupons")
            .select {
                filter {
                    eq("is_active", true)
                    gt("quantity_available", 0)
                }
            }
            .decodeList<Map<String, Any?>>()
            .map { row ->
                BenefitCoupon(
                    id = row["id"].toString(),
                    businessName = row["business_name"]?.toString() ?: "Comercio Aliado",
                    title = row["title"].toString(),
                    subtitle = row["subtitle"]?.toString().orEmpty(),
                    pointsCost = (row["points_cost"] as? Number)?.toInt() ?: 0,
                    availableQuantity = (row["quantity_available"] as? Number)?.toInt() ?: 0,
                    validUntilMillis = parseDateToMillis(row["valid_until"]?.toString()),
                    conditions = emptyList(),
                    category = row["category"]?.toString().orEmpty(),
                )
            }
    }

    // Canjea un cupón debitando los puntos del ciudadano de forma atómica (RPC Supabase)
    override suspend fun redeemCoupon(couponId: String): Result<RedeemedBenefitCoupon> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para canjear el cupón")

        val result = postgrest.rpc("redeem_coupon", mapOf("p_coupon_id" to couponId))
            .decodeSingleOrNull<Map<String, Any?>>()

        val redemptionId = result?.get("redemption_id")?.toString() ?: ""
        val code = result?.get("code")?.toString() ?: ""

        RedeemedBenefitCoupon(
            id = redemptionId,
            couponId = couponId,
            businessName = "",
            title = "",
            code = code,
            redeemedAtMillis = System.currentTimeMillis(),
            pointsSpent = 0,
            validUntilMillis = 0L,
        )
    }

    // Consulta el historial de cupones canjeados por el ciudadano autenticado
    override suspend fun getMyCoupons(): Result<List<RedeemedBenefitCoupon>> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar mis cupones")

        postgrest.from("coupon_redemptions")
            .select {
                filter { eq("citizen_id", userId) }
                order("redeemed_at", Order.DESCENDING)
            }
            .decodeList<Map<String, Any?>>()
            .map { row ->
                RedeemedBenefitCoupon(
                    id = row["id"]?.toString() ?: "",
                    couponId = row["coupon_id"]?.toString() ?: "",
                    businessName = "",
                    title = "",
                    code = row["code"]?.toString() ?: "",
                    redeemedAtMillis = parseIsoDateToMillis(row["redeemed_at"]?.toString()),
                    pointsSpent = 0,
                    validUntilMillis = 0L,
                )
            }
    }

    // Consulta el detalle de un cupón canjeado por su identificador único
    override suspend fun getMyCouponById(redeemedCouponId: String): Result<RedeemedBenefitCoupon> = runCatching {
        require(redeemedCouponId.isNotBlank()) { "El identificador del cupón canjeado no puede estar vacío" }

        val row = postgrest.from("coupon_redemptions")
            .select { filter { eq("id", redeemedCouponId) } }
            .decodeSingleOrNull<Map<String, Any?>>()
            ?: error("Cupón canjeado no encontrado con id: $redeemedCouponId")

        RedeemedBenefitCoupon(
            id = row["id"]?.toString() ?: "",
            couponId = row["coupon_id"]?.toString() ?: "",
            businessName = "",
            title = "",
            code = row["code"]?.toString() ?: "",
            redeemedAtMillis = parseIsoDateToMillis(row["redeemed_at"]?.toString()),
            pointsSpent = 0,
            validUntilMillis = 0L,
        )
    }

    // Funciones para convertir fechas en formato como lo devuelve Supabase a milisegundos
    //Se usa para el campo valid_until de la tabla coupons
    private fun parseDateToMillis(dateStr: String?): Long
    {
        if (dateStr.isNullOrBlank()) return 0L
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(dateStr)?.time ?: 0L
        }.getOrDefault(0L) // evitar el cierre si el dato viene mal
    }

    // Se usa para created_at en ecopoints_transactions o redeemed_at en coupon_redemptions
    private fun parseIsoDateToMillis(isoDate: String?): Long
    {
        if (isoDate.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(isoDate.substringBefore("+").substringBefore("Z"))
                ?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }
}
