package com.ecoruta.app.features.profile.presentation

// Estado inmutable de la edición del perfil del reciclador
// Guarda los datos de contacto, la asociación, la empresa de solo lectura, la cédula en PDF y el avance del guardado

import androidx.annotation.StringRes
import com.ecoruta.app.core.ui.components.PickedDocument

// Datos editables del reciclador, la empresa vinculada es nula cuando trabaja de forma independiente
data class RecyclerEditProfileUiState(
    val fullName: String = "",
    val phone: String = "",
    val association: String = "",
    val linkedCompany: String? = null,
    val idDocument: PickedDocument? = null,
    @StringRes val fullNameErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    @StringRes val documentErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    // El botón de guardar se deshabilita mientras algún campo tenga un error pendiente
    val hasErrors: Boolean
        get() = listOf(fullNameErrorRes, phoneErrorRes, documentErrorRes).any { it != null }
}

// Acciones de la edición del perfil del reciclador
data class RecyclerEditProfileActions(
    val onFullNameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onAssociationChange: (String) -> Unit = {},
    val onPickDocument: () -> Unit = {},
    val onDocumentRemoved: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onBack: () -> Unit = {},
)
