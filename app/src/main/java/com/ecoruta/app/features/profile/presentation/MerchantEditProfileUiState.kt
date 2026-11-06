package com.ecoruta.app.features.profile.presentation

// Estado inmutable de la edición del perfil del comercio aliado
// Guarda los datos editables, el NIT de solo lectura, los errores de cada campo y el avance del guardado

import androidx.annotation.StringRes

// Datos del establecimiento con la dirección completa para que el comercio la pueda corregir
data class MerchantEditProfileUiState(
    val name: String = "",
    val nit: String = "",
    val category: StoreCategory? = null,
    val address: String = "",
    val phone: String = "",
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val categoryErrorRes: Int? = null,
    @StringRes val addressErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    // El botón de guardar se deshabilita mientras algún campo tenga un error pendiente
    val hasErrors: Boolean
        get() = listOf(nameErrorRes, categoryErrorRes, addressErrorRes, phoneErrorRes).any { it != null }
}

// Acciones de la edición del perfil del comercio
data class MerchantEditProfileActions(
    val onNameChange: (String) -> Unit = {},
    val onCategoryChange: (StoreCategory) -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onBack: () -> Unit = {},
)
