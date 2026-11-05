package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Estado inmutable de la lista de cupones del comercio aliado
// Modela los cupones activos e inactivos junto con los estados de carga, error y conexión

import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantCoupon

// Lista de cupones del comercio, vacía por defecto mientras no haya datos del servidor
data class MerchantCouponsUiState(
    val storeName: String = "",
    val coupons: List<MerchantCoupon> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val isOffline: Boolean = false,
)

// Acciones disponibles en la lista de cupones del comercio
data class MerchantCouponsActions(
    val onEditCoupon: (MerchantCoupon) -> Unit = {},
    val onDeactivateCoupon: (MerchantCoupon) -> Unit = {},
    val onCreateCoupon: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (MerchantTab) -> Unit = {},
)
