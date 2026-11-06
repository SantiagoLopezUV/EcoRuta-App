package com.ecoruta.app.features.profile.presentation

// Vista para que el reciclador actualice sus datos de contacto, su asociación y su cédula en PDF
// La empresa vinculada se muestra sin opción de cambiarla porque la aprueba su administrador

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoPdfField
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.PickedDocument
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.profile.presentation.components.EditProfileLayout

// Pantalla de edición del reciclador sin barra inferior, con cabecera de regreso y acciones fijas abajo
@Composable
fun RecyclerEditProfileScreen(state: RecyclerEditProfileUiState, actions: RecyclerEditProfileActions) {
    val isEditable = !state.isSaving
    val focusManager = LocalFocusManager.current
    EditProfileLayout(
        description = stringResource(R.string.recycler_edit_profile_description),
        isSaving = state.isSaving,
        isSaved = state.isSaved,
        canSave = !state.hasErrors,
        onSave = actions.onSave,
        onCancel = actions.onCancel,
        onBack = actions.onBack,
    ) {
        EcoTextField(
            value = state.fullName,
            onValueChange = actions.onFullNameChange,
            label = stringResource(R.string.registration_full_name_label),
            errorMessage = state.fullNameErrorRes?.let { stringResource(it) },
            enabled = isEditable,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.phone,
            onValueChange = actions.onPhoneChange,
            label = stringResource(R.string.registration_phone_label),
            errorMessage = state.phoneErrorRes?.let { stringResource(it) },
            enabled = isEditable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.association,
            onValueChange = actions.onAssociationChange,
            label = stringResource(R.string.recycler_edit_profile_association_label),
            enabled = isEditable,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        // Sin empresa vinculada el campo indica que el reciclador es independiente
        EcoTextField(
            value = state.linkedCompany ?: stringResource(R.string.recycler_modality_independent),
            onValueChange = {},
            label = stringResource(R.string.recycler_edit_profile_company_label),
            enabled = false,
            supportingText = stringResource(R.string.recycler_edit_profile_company_hint),
        )
        // Mientras se guarda el campo ignora el documento elegido o el retiro del actual
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EcoPdfField(
                label = stringResource(R.string.recycler_edit_profile_document_label),
                hint = stringResource(R.string.recycler_application_document_pdf_hint),
                document = state.idDocument,
                onPicked = { if (isEditable) actions.onDocumentPicked(it) },
                onRemove = { if (isEditable) actions.onDocumentRemoved() },
                errorMessage = state.documentErrorRes?.let { stringResource(it) },
            )
            Text(stringResource(R.string.recycler_edit_profile_document_hint), style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
    }
}

// Datos ilustrativos del reciclador usados solo en las vistas previas
private val PreviewRecyclerEditProfile = RecyclerEditProfileUiState(
    fullName = "Carlos Andrés Ruiz",
    phone = "300 000 0000",
    association = "Asociación de Recicladores de Ejemplo",
    idDocument = PickedDocument(uri = Uri.EMPTY, name = "cedula_ejemplo.pdf"),
)

@Preview(name = "Editar perfil del reciclador", widthDp = 393, heightDp = 900)
@Composable
private fun RecyclerEditProfilePreview() {
    EcoRutaTheme { RecyclerEditProfileScreen(state = PreviewRecyclerEditProfile, actions = RecyclerEditProfileActions()) }
}

@Preview(name = "Editar perfil del reciclador · Validación", widthDp = 393, heightDp = 950)
@Composable
private fun RecyclerEditProfileValidationPreview() {
    EcoRutaTheme {
        RecyclerEditProfileScreen(
            state = PreviewRecyclerEditProfile.copy(
                fullName = "",
                phone = "300",
                idDocument = null,
                fullNameErrorRes = R.string.registration_error_name_required,
                phoneErrorRes = R.string.registration_error_phone_invalid,
                documentErrorRes = R.string.recycler_application_error_document,
            ),
            actions = RecyclerEditProfileActions(),
        )
    }
}

@Preview(name = "Editar perfil del reciclador · Guardando", widthDp = 393, heightDp = 900)
@Composable
private fun RecyclerEditProfileSavingPreview() {
    EcoRutaTheme { RecyclerEditProfileScreen(state = PreviewRecyclerEditProfile.copy(isSaving = true), actions = RecyclerEditProfileActions()) }
}

@Preview(name = "Editar perfil del reciclador · Guardado", widthDp = 393, heightDp = 950)
@Composable
private fun RecyclerEditProfileSavedPreview() {
    EcoRutaTheme { RecyclerEditProfileScreen(state = PreviewRecyclerEditProfile.copy(isSaved = true), actions = RecyclerEditProfileActions()) }
}
