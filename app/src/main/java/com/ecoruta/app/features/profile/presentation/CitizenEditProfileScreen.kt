package com.ecoruta.app.features.profile.presentation

// Vista para que el ciudadano actualice sus datos personales, su barrio y el radio del radar
// El correo se muestra sin opción de cambiarlo y la foto se sigue cambiando desde el perfil

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
import com.ecoruta.app.features.profile.presentation.components.SelectField
import com.ecoruta.app.features.routes.presentation.map.Neighborhood
import com.ecoruta.app.features.routes.presentation.map.formatMeters

// Pantalla de edición del ciudadano sin barra inferior, con cabecera de regreso y acciones fijas abajo
@Composable
fun CitizenEditProfileScreen(state: CitizenEditProfileUiState, actions: CitizenEditProfileActions) {
    val isEditable = !state.isSaving
    val focusManager = LocalFocusManager.current
    EditProfileLayout(
        description = stringResource(R.string.citizen_edit_profile_description),
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
        // El correo identifica la cuenta y por eso se muestra deshabilitado
        EcoTextField(
            value = state.email,
            onValueChange = {},
            label = stringResource(R.string.login_email_label),
            enabled = false,
            supportingText = stringResource(R.string.citizen_edit_profile_email_hint),
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
        SelectField(
            label = stringResource(R.string.registration_neighborhood_label),
            placeholder = stringResource(R.string.registration_neighborhood_placeholder),
            iconRes = R.drawable.ic_map_pin,
            options = state.neighborhoods,
            selected = state.neighborhood,
            optionLabel = { it.name },
            onSelected = actions.onNeighborhoodChange,
            errorMessage = state.neighborhoodErrorRes?.let { stringResource(it) },
            enabled = isEditable,
        )
        // El radio se elige entre las distancias permitidas y debajo se explica para qué sirve
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectField(
                label = stringResource(R.string.citizen_edit_profile_radar_label),
                placeholder = formatMeters(state.radarRadiusMeters),
                iconRes = R.drawable.ic_bell,
                options = state.radarRadiusOptions,
                selected = state.radarRadiusMeters,
                optionLabel = { formatMeters(it) },
                onSelected = actions.onRadarRadiusChange,
                enabled = isEditable,
            )
            Text(stringResource(R.string.citizen_edit_profile_radar_hint), style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        Text(stringResource(R.string.citizen_edit_profile_photo_note), style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

// Barrios ilustrativos usados solo en las vistas previas
private val PreviewNeighborhoods = listOf(
    Neighborhood("ejemplo-centro", "Centro"),
    Neighborhood("ejemplo-la-esperanza", "La Esperanza"),
    Neighborhood("ejemplo-el-principe", "El Príncipe"),
)

// Datos ilustrativos del ciudadano usados solo en las vistas previas
private val PreviewCitizenEditProfile = CitizenEditProfileUiState(
    fullName = "Ana María Gómez",
    email = "ana@correo.com",
    phone = "300 000 0000",
    neighborhood = PreviewNeighborhoods.first(),
    neighborhoods = PreviewNeighborhoods,
    radarRadiusMeters = 500,
)

@Preview(name = "Editar perfil del ciudadano", widthDp = 393, heightDp = 900)
@Composable
private fun CitizenEditProfilePreview() {
    EcoRutaTheme { CitizenEditProfileScreen(state = PreviewCitizenEditProfile, actions = CitizenEditProfileActions()) }
}

@Preview(name = "Editar perfil del ciudadano · Validación", widthDp = 393, heightDp = 950)
@Composable
private fun CitizenEditProfileValidationPreview() {
    EcoRutaTheme {
        CitizenEditProfileScreen(
            state = PreviewCitizenEditProfile.copy(
                fullName = "",
                phone = "300",
                neighborhood = null,
                fullNameErrorRes = R.string.registration_error_name_required,
                phoneErrorRes = R.string.registration_error_phone_invalid,
                neighborhoodErrorRes = R.string.neighborhood_required,
            ),
            actions = CitizenEditProfileActions(),
        )
    }
}

@Preview(name = "Editar perfil del ciudadano · Guardando", widthDp = 393, heightDp = 900)
@Composable
private fun CitizenEditProfileSavingPreview() {
    EcoRutaTheme { CitizenEditProfileScreen(state = PreviewCitizenEditProfile.copy(isSaving = true), actions = CitizenEditProfileActions()) }
}

@Preview(name = "Editar perfil del ciudadano · Guardado", widthDp = 393, heightDp = 950)
@Composable
private fun CitizenEditProfileSavedPreview() {
    EcoRutaTheme { CitizenEditProfileScreen(state = PreviewCitizenEditProfile.copy(isSaved = true), actions = CitizenEditProfileActions()) }
}
