package com.ecoruta.app.features.gamification.data.remote.dto

// DTO serializable para cupones comerciales y saldos recibidos desde Supabase
// Mapea id, negocio, título, costo en puntos, cantidad disponible y vigencia

import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class CouponDto(
    @SerialName("id") val id: String,
    @SerialName("business_id") val businessId: String? = null,
    @SerialName("title") val title: String,
    @SerialName("points_cost") val pointsCost: Int = 0,
    @SerialName("quantity_available") val quantityAvailable: Int = 0,
    @SerialName("valid_until") val validUntil: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("category") val category: String? = null,
    @SerialName("description") val description: String? = null,
)

// Convierte el DTO remoto de cupón comercial a modelo de dominio
fun CouponDto.toDomain(): BenefitCoupon = BenefitCoupon(
    id = id,
    businessName = "Comercio Aliado",
    title = title,
    subtitle = description.orEmpty(),
    pointsCost = pointsCost,
    availableQuantity = quantityAvailable,
    validUntilMillis = parseDateToMillis(validUntil),
    conditions = emptyList(),
    category = category.orEmpty(),
)

@Serializable
data class CitizenBalanceDto(
    @SerialName("ecopoints_balance") val ecopointsBalance: Int = 0,
)

@Serializable
data class CouponRedemptionDto(
    @SerialName("id") val id: String,
    @SerialName("coupon_id") val couponId: String,
    @SerialName("code") val code: String? = null,
    @SerialName("redeemed_at") val redeemedAt: String? = null,
)

@Serializable
data class RedeemRpcResultDto(
    @SerialName("redemption_id") val redemptionId: String? = null,
    @SerialName("code") val code: String? = null,
)

private fun parseDateToMillis(dateStr: String?): Long = runCatching {
    if (dateStr == null) return@runCatching 0L
    val pattern = if (dateStr.length == 10) "yyyy-MM-dd" else "yyyy-MM-dd'T'HH:mm:ss"
    SimpleDateFormat(pattern, Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(dateStr.substringBefore("+").substringBefore("Z"))
        ?.time ?: 0L
}.getOrDefault(0L)
