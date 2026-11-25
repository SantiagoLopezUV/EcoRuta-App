package com.ecoruta.app.features.gamification.data.repository

// Implementación del repositorio de gamificación y cupones comerciales
// Consulta catálogo, saldo, historial de movimientos y redime cupones con operación atómica en Supabase

import com.ecoruta.app.features.gamification.data.remote.dto.CitizenBalanceDto
import com.ecoruta.app.features.gamification.data.remote.dto.CouponDto
import com.ecoruta.app.features.gamification.data.remote.dto.CouponRedemptionDto
import com.ecoruta.app.features.gamification.data.remote.dto.CreateCouponPayloadDto
import com.ecoruta.app.features.gamification.data.remote.dto.EcoPointsTransactionDto
import com.ecoruta.app.features.gamification.data.remote.dto.RedeemRpcResultDto
import com.ecoruta.app.features.gamification.data.remote.dto.UpdateCouponPayloadDto
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
import java.util.Date
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
            ?: return@runCatching 0

        val row = postgrest.from("citizens")
            .select { filter { eq("profile_id", userId) } }
            .decodeSingleOrNull<CitizenBalanceDto>()

        row?.ecopointsBalance ?: 0
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
            .decodeList<CouponDto>()
            .map { it.toDomain() }
    }

    // Canjea un cupón debitando los puntos del ciudadano de forma atómica (RPC Supabase)
    override suspend fun redeemCoupon(couponId: String): Result<RedeemedBenefitCoupon> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para canjear el cupón")

        val result = postgrest.rpc("redeem_coupon", mapOf("p_coupon_id" to couponId))
            .decodeSingleOrNull<RedeemRpcResultDto>()

        val redemptionId = result?.redemptionId.orEmpty()
        val code = result?.code.orEmpty()

        RedeemedBenefitCoupon(
            id = redemptionId,
            couponId = couponId,
            businessName = "Comercio Aliado",
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
            .decodeList<CouponRedemptionDto>()
            .map { row ->
                RedeemedBenefitCoupon(
                    id = row.id,
                    couponId = row.couponId,
                    businessName = "Comercio Aliado",
                    title = "",
                    code = row.code.orEmpty(),
                    redeemedAtMillis = parseIsoDateToMillis(row.redeemedAt),
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
            .decodeSingleOrNull<CouponRedemptionDto>()
            ?: error("Cupón canjeado no encontrado con id: $redeemedCouponId")

        RedeemedBenefitCoupon(
            id = row.id,
            couponId = row.couponId,
            businessName = "Comercio Aliado",
            title = "",
            code = row.code.orEmpty(),
            redeemedAtMillis = parseIsoDateToMillis(row.redeemedAt),
            pointsSpent = 0,
            validUntilMillis = 0L,
        )
    }

    // Registra un nuevo cupón de descuento en Supabase asociado al comercio autenticado
    override suspend fun createCoupon(
        title: String,
        pointsCost: Int,
        quantityAvailable: Int,
        validUntilMillis: Long?,
        category: String,
        description: String,
    ): Result<BenefitCoupon> = runCatching {
        val businessId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para publicar cupones comerciales")

        require(title.isNotBlank()) { "El título del cupón no puede estar vacío" }
        require(pointsCost > 0) { "El costo en puntos debe ser mayor a cero" }
        require(quantityAvailable >= 0) { "La cantidad disponible no puede ser negativa" }

        val isoValidUntil = formatMillisToIsoDate(validUntilMillis)

        val payload = CreateCouponPayloadDto(
            businessId = businessId,
            title = title.trim(),
            pointsCost = pointsCost,
            quantityAvailable = quantityAvailable,
            validUntil = isoValidUntil,
            isActive = true,
            category = category.trim().ifBlank { "general" },
            description = description.trim(),
        )

        val created = postgrest.from("coupons")
            .insert(payload) {
                select()
            }
            .decodeSingle<CouponDto>()

        created.toDomain()
    }

    // Actualiza los campos de un cupón existente verificando propiedad del comercio
    override suspend fun updateCoupon(
        couponId: String,
        title: String,
        pointsCost: Int,
        quantityAvailable: Int,
        validUntilMillis: Long?,
        category: String,
        description: String,
        isActive: Boolean,
    ): Result<BenefitCoupon> = runCatching {
        val businessId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para modificar cupones comerciales")

        require(couponId.isNotBlank()) { "El identificador del cupón no puede estar vacío" }
        require(title.isNotBlank()) { "El título del cupón no puede estar vacío" }
        require(pointsCost > 0) { "El costo en puntos debe ser mayor a cero" }
        require(quantityAvailable >= 0) { "La cantidad disponible no puede ser negativa" }

        val isoValidUntil = formatMillisToIsoDate(validUntilMillis)
        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val payload = UpdateCouponPayloadDto(
            title = title.trim(),
            pointsCost = pointsCost,
            quantityAvailable = quantityAvailable,
            validUntil = isoValidUntil,
            isActive = isActive,
            category = category.trim().ifBlank { "general" },
            description = description.trim(),
            updatedAt = isoNow,
        )

        val updated = postgrest.from("coupons")
            .update(payload) {
                filter {
                    eq("id", couponId)
                    eq("business_id", businessId)
                }
                select()
            }
            .decodeSingle<CouponDto>()

        updated.toDomain()
    }

    // Desactiva un cupón del comercio mediante baja lógica (is_active = false)
    override suspend fun deactivateCoupon(couponId: String): Result<Unit> = runCatching {
        val businessId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para desactivar cupones comerciales")

        require(couponId.isNotBlank()) { "El identificador del cupón no puede estar vacío" }

        val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        postgrest.from("coupons")
            .update(UpdateCouponPayloadDto(isActive = false, updatedAt = isoNow)) {
                filter {
                    eq("id", couponId)
                    eq("business_id", businessId)
                }
            }
        Unit
    }

    // Consulta todos los cupones creados por el comercio autenticado (activos e inactivos)
    override suspend fun getMerchantCoupons(): Result<List<BenefitCoupon>> = runCatching {
        val businessId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar cupones del comercio")

        postgrest.from("coupons")
            .select {
                filter { eq("business_id", businessId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<CouponDto>()
            .map { it.toDomain() }
    }

    //formato para la fecha del cupon
    private fun formatMillisToIsoDate(millis: Long?): String? {
        if (millis == null || millis <= 0L) return null
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))
    }

    private fun parseIsoDateToMillis(isoDate: String?): Long {
        if (isoDate.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(isoDate.substringBefore("+").substringBefore("Z"))
                ?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    }
}
