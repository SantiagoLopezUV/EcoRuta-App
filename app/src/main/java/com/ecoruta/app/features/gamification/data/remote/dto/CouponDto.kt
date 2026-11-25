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
)

// Convierte el DTO remoto de cupón comercial a modelo de dominio sin depender de columnas inexistentes
fun CouponDto.toDomain(businessName: String = "Comercio Aliado"): BenefitCoupon = BenefitCoupon(
    id = id,
    businessName = businessName,
    title = title,
    subtitle = title,
    pointsCost = pointsCost,
    availableQuantity = quantityAvailable,
    validUntilMillis = parseDateToMillis(validUntil),
    conditions = emptyList(),
    category = "general",
    isActive = isActive,
)

// DTO serializable para crear un cupón en Supabase ajustado estrictamente al esquema de la tabla coupons
@Serializable
data class CreateCouponPayloadDto(
    @SerialName("business_id") val businessId: String,
    @SerialName("title") val title: String,
    @SerialName("points_cost") val pointsCost: Int,
    @SerialName("quantity_available") val quantityAvailable: Int,
    @SerialName("valid_until") val validUntil: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

// DTO serializable para actualizar campos de un cupón existente en Supabase según columnas reales
@Serializable
data class UpdateCouponPayloadDto(
    @SerialName("title") val title: String? = null,
    @SerialName("points_cost") val pointsCost: Int? = null,
    @SerialName("quantity_available") val quantityAvailable: Int? = null,
    @SerialName("valid_until") val validUntil: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
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
    @SerialName("success") val success: Boolean = true,
    @SerialName("redemption_id") val redemptionId: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
)

@Serializable
data class BusinessInfoDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("business_name") val businessName: String,
)

internal fun parseDateToMillis(dateStr: String?): Long = runCatching {
    if (dateStr.isNullOrBlank()) return@runCatching 0L
    val clean = dateStr.trim().replace(' ', 'T')
    val withoutOffset = clean.substringBefore("+").substringBefore("Z")
    if (withoutOffset.length == 10) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(withoutOffset)?.time ?: 0L
    } else {
        val base = withoutOffset.substringBefore(".")
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(base)?.time ?: 0L
    }
}.getOrDefault(0L)
