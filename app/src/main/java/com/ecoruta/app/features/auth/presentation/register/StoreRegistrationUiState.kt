package com.ecoruta.app.features.auth.presentation.register

// Estados inmutables de las pantallas de registro del comercio aliado
// Guardan los campos de cada paso, sus mensajes de error y el estado de envío

import androidx.annotation.StringRes

// Estado del primer paso con los datos de la cuenta del gerente del comercio
data class StoreOwnerAccountUiState(
    val fullName: String = "",
    val email: String = "",
    @StringRes val fullNameErrorRes: Int? = null,
    @StringRes val emailErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
) {
    val hasErrors: Boolean get() = fullNameErrorRes != null || emailErrorRes != null
}
