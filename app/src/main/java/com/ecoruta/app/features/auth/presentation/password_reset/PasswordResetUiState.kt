package com.ecoruta.app.features.auth.presentation.password_reset

// Estado inmutable de la recuperación de contraseña
// Registra el paso activo del flujo, correo objetivo, código ingresado y errores

import androidx.annotation.StringRes

const val RECOVERY_CODE_LENGTH = 6

// Estado compartido entre las pantallas de ingreso de correo, validación de código y nueva clave
data class PasswordRecoveryUiState(
    // Datos correspondientes al paso de solicitud por correo electrónico
    val email: String = "",
    @StringRes val emailErrorRes: Int? = null,
    val requestFailed: Boolean = false,
    val isRequestingCode: Boolean = false,
    // Datos correspondientes al paso de verificación del código temporal
    val code: String = "",
    @StringRes val codeErrorRes: Int? = null,
    val isCodeExpired: Boolean = false,
    val isVerifyingCode: Boolean = false,
    val resendSecondsLeft: Int = 0,
    // Datos correspondientes al paso de definición de la nueva contraseña
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    @StringRes val newPasswordErrorRes: Int? = null,
    @StringRes val confirmPasswordErrorRes: Int? = null,
    val isSavingPassword: Boolean = false,
)

typealias PasswordResetUiState = PasswordRecoveryUiState
