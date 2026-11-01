package com.ecoruta.app.features.gamification.presentation.benefits

// Estado inmutable del catálogo de beneficios comerciales
// Contiene lista de cupones, saldo de puntos del usuario, filtro y estado de canje

import androidx.annotation.StringRes

enum class CouponCategory {
    FOOD,
    GROCERIES,
    STATIONERY,
}

data class Coupon(
    val id: String,
    val businessName: String,
    val title: String,
    val subtitle: String,
    val category: CouponCategory,
    val pointsCost: Int,
    val availableQuantity: Int,
    val validUntilMillis: Long,
    val conditions: List<String>,
)

data class RedeemedCoupon(
    val id: String,
    val coupon: Coupon,
    val code: String,
    val redeemedAtMillis: Long,
    val pointsSpent: Int,
)

data class RedemptionReceipt(
    val redeemedCoupon: RedeemedCoupon,
    val balanceAfter: Int,
)

data class PointsMovement(
    val id: String,
    val description: String,
    val dateMillis: Long,
    val points: Int,
)

data class BenefitsUiState(
    val isLoading: Boolean = true,
    val balance: Int? = null,
    val coupons: List<Coupon> = emptyList(),
    val category: CouponCategory? = null,
    val isSample: Boolean = false,
    @param:StringRes val error: Int? = null,
) {
    val visibleCoupons: List<Coupon>
        get() = if (category == null) coupons else coupons.filter { it.category == category }
}

enum class RedemptionStep {
    DETAIL,
    CONFIRM,
    INSUFFICIENT,
    REDEEMED,
}

data class CouponRedemptionUiState(
    val isLoading: Boolean = true,
    val coupon: Coupon? = null,
    val balance: Int? = null,
    val step: RedemptionStep = RedemptionStep.DETAIL,
    val isRedeeming: Boolean = false,
    val receipt: RedemptionReceipt? = null,
    val redeemedWithoutBackend: Boolean = false,
    val isSample: Boolean = false,
    @param:StringRes val error: Int? = null,
) {
    val balanceAfterPreview: Int?
        get() = if (balance != null && coupon != null) balance - coupon.pointsCost else null

    val missingPoints: Int
        get() = ((coupon?.pointsCost ?: 0) - (balance ?: 0)).coerceAtLeast(0)
}

sealed interface PointsHistoryUiState {
    data object Loading : PointsHistoryUiState
    data object Error : PointsHistoryUiState
    data class Loaded(
        val balance: Int?,
        val movements: List<PointsMovement>,
        val isSample: Boolean = false,
    ) : PointsHistoryUiState {
        val netMovement: Int get() = movements.sumOf { it.points }
        val initialBalance: Int? get() = balance?.minus(netMovement)
    }
}

data class MyCouponsUiState(
    val isLoading: Boolean = true,
    val coupons: List<RedeemedCoupon> = emptyList(),
    val category: CouponCategory? = null,
    val expandedId: String? = null,
    val isSample: Boolean = false,
    @param:StringRes val error: Int? = null,
) {
    val visibleCoupons: List<RedeemedCoupon>
        get() = if (category == null) coupons else coupons.filter { it.coupon.category == category }
}

sealed interface MyCouponUiState {
    data object Loading : MyCouponUiState
    data object NotFound : MyCouponUiState
    data object Error : MyCouponUiState
    data class Loaded(val coupon: RedeemedCoupon, val isSample: Boolean = false) : MyCouponUiState
}
