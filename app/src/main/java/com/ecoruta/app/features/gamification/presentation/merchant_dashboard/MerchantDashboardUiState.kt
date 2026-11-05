package com.ecoruta.app.features.gamification.presentation.merchant_dashboard

// Estado inmutable del panel del comercio aliado
// Modela el resumen de cupones, los cupones activos, los canjes recientes y los estados de carga

// Cupón publicado por el comercio tal como se muestra en el panel y en sus listas
data class MerchantCoupon(
    val id: String,
    val title: String,
    val description: String,
    val pointsCost: Int,
    val availableQuantity: Int,
    val validUntil: String,
    val isActive: Boolean,
)

// Canje validado en caja que aparece en el historial reciente del comercio
data class MerchantRedemption(
    val code: String,
    val discountPercent: Int,
    val time: String,
)

// Estado del panel, vacío por defecto mientras no haya datos del servidor
data class MerchantDashboardUiState(
    val storeName: String = "",
    val activeCoupons: List<MerchantCoupon> = emptyList(),
    val availableTotal: Int = 0,
    val redemptionsToday: Int = 0,
    val recentRedemptions: List<MerchantRedemption> = emptyList(),
    val redemptionsDateLabel: String = "",
    // Cupón que espera la confirmación del comercio antes de desactivarse
    val couponToDeactivate: MerchantCoupon? = null,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val isOffline: Boolean = false,
) {
    val showDeactivateConfirm: Boolean get() = couponToDeactivate != null

    // El panel se considera vacío cuando no hay cupones ni canjes para mostrar
    val isEmpty: Boolean get() = activeCoupons.isEmpty() && recentRedemptions.isEmpty()
}
