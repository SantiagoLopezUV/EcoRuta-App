package com.ecoruta.app.features.auth.presentation.login

// Estado inmutable de la pantalla de inicio de sesión
// Representa campos de texto, visibilidad de clave, indicador de carga y errores

import androidx.annotation.StringRes
import com.ecoruta.app.features.profile.presentation.UserRole

// Estado visual de los campos y banderas de navegación de la pantalla de inicio de sesión
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null,
    // Activa el borde de error en los campos y muestra el aviso de credenciales inválidas
    val showInvalidCredentials: Boolean = false,
    val isSubmitting: Boolean = false,
    // Rol activo cuando el usuario completa el inicio de sesión satisfactoriamente
    val signedInRole: UserRole? = null,
    // Rol activo cuando el usuario requiere cambiar su contraseña provisional en el primer ingreso
    val passwordChangeRole: UserRole? = null,
    // Indica que la cuenta autenticada solo tiene acceso permitido desde el panel web
    val webOnlyAccess: Boolean = false,
    // Controla la apertura del panel inferior para seleccionar el rol en modo de prueba
    val showDesignModeSheet: Boolean = false,
    @StringRes val noticeRes: Int? = null,
)
