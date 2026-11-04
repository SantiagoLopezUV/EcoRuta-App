package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// Estado inmutable del formulario de cupón del comercio
// Lo comparten la creación y la edición de cupones con sus campos, errores y estados de envío

// Campos del formulario que pueden quedar marcados con error de validación
enum class MerchantCouponField {
    TITLE,
    DESCRIPTION,
    POINTS,
    QUANTITY,
    VALID_UNTIL,
}

// Valores escritos por el comercio y estado del envío del formulario
data class MerchantCouponFormUiState(
    val storeName: String = "",
    val title: String = "",
    val description: String = "",
    val pointsCost: String = "",
    val quantity: String = "",
    val validUntil: String = "",
    val isActive: Boolean = true,
    val invalidFields: Set<MerchantCouponField> = emptySet(),
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val isOffline: Boolean = false,
    // Solo aplica en la edición cuando el cupón elegido ya no existe
    val isCouponMissing: Boolean = false,
)

// Acciones del formulario de cupón para crear o editar
data class MerchantCouponFormActions(
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onPointsChange: (String) -> Unit = {},
    val onQuantityChange: (String) -> Unit = {},
    val onValidUntilChange: (String) -> Unit = {},
    val onActiveChange: (Boolean) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onRetry: () -> Unit = {},
)
