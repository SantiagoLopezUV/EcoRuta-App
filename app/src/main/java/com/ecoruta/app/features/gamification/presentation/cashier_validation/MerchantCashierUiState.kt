package com.ecoruta.app.features.gamification.presentation.cashier_validation

// Estado inmutable de la validación de cupones en caja
// Modela código digitado, detalle del cupón, porcentaje, total a pagar y mensaje

import com.ecoruta.app.core.navigation.components.MerchantTab

// Longitud del código que se escribe después del prefijo fijo ECO
const val COUPON_CODE_LENGTH = 8

// Motivos por los que un código no se puede validar en caja
enum class CashierValidationError {
    INCOMPLETE_CODE,
    NOT_FOUND,
    ALREADY_REDEEMED,
    EXPIRED,
    OTHER_STORE,
}

// Detalle del cupón validado con el descuento aplicado y el total que paga el cliente
data class ValidatedCoupon(
    val code: String,
    val title: String,
    val storeName: String,
    val pointsCost: Int,
    val redeemedAt: String,
    val discountPercent: Int? = null,
    val purchaseAmount: Long? = null,
    val discountAmount: Long? = null,
    val totalToPay: Long? = null,
)

// Estado de la pantalla de caja, vacío por defecto mientras no haya un código escrito
data class MerchantCashierUiState(
    val storeName: String = "",
    val code: String = "",
    val isScanning: Boolean = false,
    val isVerifying: Boolean = false,
    val validationError: CashierValidationError? = null,
    val validatedCoupon: ValidatedCoupon? = null,
    val hasError: Boolean = false,
    val isOffline: Boolean = false,
) {
    // El botón de verificar solo se habilita con los ocho caracteres completos
    val isCodeComplete: Boolean get() = code.length == COUPON_CODE_LENGTH
}

// Acciones de la pantalla de validación en caja
data class MerchantCashierActions(
    val onStartScan: () -> Unit = {},
    val onStopScan: () -> Unit = {},
    val onQrDetected: (String) -> Unit = {},
    val onCodeChange: (String) -> Unit = {},
    val onVerify: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (MerchantTab) -> Unit = {},
)
