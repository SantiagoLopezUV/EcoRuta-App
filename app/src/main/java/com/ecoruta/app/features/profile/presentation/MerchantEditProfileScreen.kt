package com.ecoruta.app.features.profile.presentation

// Vista para que el comercio aliado actualice los datos de su establecimiento
// Reutiliza los campos del registro y deja el NIT visible pero sin opción de cambiarlo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleBrandHeader
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoAccentSoft
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.profile.presentation.components.StoreAddressField
import com.ecoruta.app.features.profile.presentation.components.StoreCategoryField
import com.ecoruta.app.features.profile.presentation.components.StoreNameField
import com.ecoruta.app.features.profile.presentation.components.StorePhoneField

// Pantalla de edición sin barra inferior, con cabecera de regreso y las acciones fijas abajo
@Composable
fun MerchantEditProfileScreen(state: MerchantEditProfileUiState, actions: MerchantEditProfileActions) {
    val isEditable = !state.isSaving
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EcoRoleBrandHeader(onBack = actions.onBack)
            // Ruta de ubicación que indica que la edición parte de Mi perfil
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.my_profile), style = EcoTextStyles.Subtitle, color = EcoTitle)
                Text(stringResource(R.string.merchant_profile_edit_data), style = EcoTextStyles.Subtitle, color = EcoTitle)
            }
            if (state.isSaved) SavedBanner()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.merchant_edit_profile_title), style = EcoTextStyles.Title, color = EcoTitle)
                Text(stringResource(R.string.merchant_edit_profile_description), style = EcoTextStyles.Subtitle, color = EcoTitle)
            }
            StoreNameField(
                value = state.name,
                onValueChange = actions.onNameChange,
                errorMessage = state.nameErrorRes?.let { stringResource(it) },
                enabled = isEditable,
            )
            // El NIT se muestra deshabilitado porque solo la administración puede corregirlo
            EcoTextField(
                value = state.nit,
                onValueChange = {},
                label = stringResource(R.string.store_id_nit),
                enabled = false,
                supportingText = stringResource(R.string.merchant_edit_profile_nit_hint),
            )
            StoreCategoryField(
                selected = state.category,
                onSelected = actions.onCategoryChange,
                errorMessage = state.categoryErrorRes?.let { stringResource(it) },
                enabled = isEditable,
            )
            StoreAddressField(
                value = state.address,
                onValueChange = actions.onAddressChange,
                errorMessage = state.addressErrorRes?.let { stringResource(it) },
                enabled = isEditable,
            )
            StorePhoneField(
                value = state.phone,
                onValueChange = actions.onPhoneChange,
                errorMessage = state.phoneErrorRes?.let { stringResource(it) },
                enabled = isEditable,
            )
            // Después de guardar el aviso de revisión se reemplaza por el mensaje de cambios guardados
            if (!state.isSaved) {
                Text(stringResource(R.string.merchant_edit_profile_review_notice), style = EcoTextStyles.Subtitle, color = EcoTitle)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            EcoPrimaryButton(
                text = stringResource(R.string.merchant_edit_save),
                onClick = actions.onSave,
                enabled = !state.hasErrors,
                isLoading = state.isSaving,
            )
            EcoTextLink(text = stringResource(R.string.action_cancel), onClick = { if (isEditable) actions.onCancel() })
        }
    }
}

// Aviso verde que confirma que los cambios se guardaron
@Composable
private fun SavedBanner() {
    val shape = RoundedCornerShape(12.dp)
    Text(
        stringResource(R.string.merchant_edit_profile_saved),
        style = EcoTextStyles.Subtitle,
        color = EcoPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoAccentSoft)
            .border(1.dp, EcoAccent, shape)
            .padding(12.dp),
    )
}

// Datos ilustrativos del comercio usados solo en las vistas previas
private val PreviewEditProfile = MerchantEditProfileUiState(
    name = "Tienda Semilla de Ejemplo",
    nit = "900123456-7",
    category = StoreCategory.GROCERY,
    address = "Calle de Ejemplo # 12-34, Tuluá",
    phone = "300 000 0000",
)

@Preview(name = "Editar perfil del comercio", widthDp = 393, heightDp = 900)
@Composable
private fun MerchantEditProfilePreview() {
    EcoRutaTheme { MerchantEditProfileScreen(state = PreviewEditProfile, actions = MerchantEditProfileActions()) }
}

@Preview(name = "Editar perfil del comercio · Validación", widthDp = 393, heightDp = 950)
@Composable
private fun MerchantEditProfileValidationPreview() {
    EcoRutaTheme {
        MerchantEditProfileScreen(
            state = PreviewEditProfile.copy(
                name = "",
                address = "",
                nameErrorRes = R.string.merchant_edit_profile_error_name,
                addressErrorRes = R.string.store_registration_error_address,
            ),
            actions = MerchantEditProfileActions(),
        )
    }
}

@Preview(name = "Editar perfil del comercio · Guardando", widthDp = 393, heightDp = 900)
@Composable
private fun MerchantEditProfileSavingPreview() {
    EcoRutaTheme { MerchantEditProfileScreen(state = PreviewEditProfile.copy(isSaving = true), actions = MerchantEditProfileActions()) }
}

@Preview(name = "Editar perfil del comercio · Guardado", widthDp = 393, heightDp = 950)
@Composable
private fun MerchantEditProfileSavedPreview() {
    EcoRutaTheme { MerchantEditProfileScreen(state = PreviewEditProfile.copy(isSaved = true), actions = MerchantEditProfileActions()) }
}
