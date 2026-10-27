package com.ecoruta.app.features.auth.presentation.first_login

// Estado inmutable de la pantalla de primer ingreso
// Modela campos de contraseña nueva, confirmación, indicadores de fortaleza y carga

import androidx.annotation.StringRes

// Define el origen desde el cual se solicita el cambio de contraseña
enum class ChangePasswordMode {
    // Primer ingreso utilizando la contraseña provisional enviada por correo
    PRIMER_INGRESO,

    // Solicitud iniciada voluntariamente desde las opciones del perfil
    PERFIL,
}

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isCurrentVisible: Boolean = false,
    val isNewVisible: Boolean = false,
    val isConfirmVisible: Boolean = false,
    @StringRes val currentErrorRes: Int? = null,
    @StringRes val newErrorRes: Int? = null,
    @StringRes val confirmErrorRes: Int? = null,
    val isSaving: Boolean = false,
)

typealias ForceChangePasswordUiState = ChangePasswordUiState

sealed interface ChangePasswordEvent {
    data object Done : ChangePasswordEvent
    data class ShowNotice(@StringRes val messageRes: Int) : ChangePasswordEvent
}
