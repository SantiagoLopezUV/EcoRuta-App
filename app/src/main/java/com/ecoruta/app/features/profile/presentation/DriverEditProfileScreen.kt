package com.ecoruta.app.features.profile.presentation

// Vista para que el chofer actualice su nombre y su teléfono de contacto
// El correo, la empresa y la placa del vehículo los administra la empresa y se muestran deshabilitados

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
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.profile.presentation.components.EditProfileLayout

// Pantalla de edición del chofer sin barra inferior, con cabecera de regreso y acciones fijas abajo
@Composable
fun DriverEditProfileScreen(state: DriverEditProfileUiState, actions: DriverEditProfileActions) {
    val isEditable = !state.isSaving
    val focusManager = LocalFocusManager.current
    EditProfileLayout(
        description = stringResource(R.string.driver_edit_profile_description),
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        EcoTextField(
            value = state.email,
            onValueChange = {},
            label = stringResource(R.string.login_email_label),
            enabled = false,
        )
        EcoTextField(
            value = state.company,
            onValueChange = {},
            label = stringResource(R.string.driver_edit_profile_company_label),
            enabled = false,
        )
        // La ayuda va debajo de la placa porque explica los tres datos que administra la empresa
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EcoTextField(
                value = state.vehiclePlate,
                onValueChange = {},
                label = stringResource(R.string.driver_edit_profile_plate_label),
                enabled = false,
            )
            Text(stringResource(R.string.driver_edit_profile_company_hint), style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
    }
}

// Datos ilustrativos del chofer usados solo en las vistas previas
private val PreviewDriverEditProfile = DriverEditProfileUiState(
    fullName = "Luis Fernando Mora",
    phone = "300 000 0000",
    email = "luis@correo.com",
    company = "Empresa de Aseo de Ejemplo",
    vehiclePlate = "ABC123",
)

@Preview(name = "Editar perfil del chofer", widthDp = 393, heightDp = 900)
@Composable
private fun DriverEditProfilePreview() {
    EcoRutaTheme { DriverEditProfileScreen(state = PreviewDriverEditProfile, actions = DriverEditProfileActions()) }
}

@Preview(name = "Editar perfil del chofer · Validación", widthDp = 393, heightDp = 950)
@Composable
private fun DriverEditProfileValidationPreview() {
    EcoRutaTheme {
        DriverEditProfileScreen(
            state = PreviewDriverEditProfile.copy(
                fullName = "",
                phone = "300",
                fullNameErrorRes = R.string.registration_error_name_required,
                phoneErrorRes = R.string.registration_error_phone_invalid,
            ),
            actions = DriverEditProfileActions(),
        )
    }
}

@Preview(name = "Editar perfil del chofer · Guardando", widthDp = 393, heightDp = 900)
@Composable
private fun DriverEditProfileSavingPreview() {
    EcoRutaTheme { DriverEditProfileScreen(state = PreviewDriverEditProfile.copy(isSaving = true), actions = DriverEditProfileActions()) }
}

@Preview(name = "Editar perfil del chofer · Guardado", widthDp = 393, heightDp = 950)
@Composable
private fun DriverEditProfileSavedPreview() {
    EcoRutaTheme { DriverEditProfileScreen(state = PreviewDriverEditProfile.copy(isSaved = true), actions = DriverEditProfileActions()) }
}
