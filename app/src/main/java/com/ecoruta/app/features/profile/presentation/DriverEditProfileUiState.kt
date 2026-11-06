package com.ecoruta.app.features.profile.presentation

// Estado inmutable de la edición del perfil del chofer
// Guarda el nombre y el teléfono editables, los datos que administra la empresa y el avance del guardado

import androidx.annotation.StringRes

// Datos del chofer donde el correo, la empresa y la placa solo se muestran
data class DriverEditProfileUiState(
    val fullName: String = "",
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val vehiclePlate: String = "",
    @StringRes val fullNameErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    // El botón de guardar se deshabilita mientras algún campo tenga un error pendiente
    val hasErrors: Boolean
        get() = listOf(fullNameErrorRes, phoneErrorRes).any { it != null }
}

// Acciones de la edición del perfil del chofer
data class DriverEditProfileActions(
    val onFullNameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onBack: () -> Unit = {},
)
