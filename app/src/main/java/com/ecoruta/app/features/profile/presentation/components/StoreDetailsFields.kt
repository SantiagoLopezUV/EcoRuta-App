package com.ecoruta.app.features.profile.presentation.components

// Campos con los datos del establecimiento del comercio aliado
// Mantienen las mismas etiquetas, teclados y mensajes del registro para reutilizarlos al editar el perfil

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.features.profile.presentation.StoreCategory

// Nombre comercial con mayúscula inicial en cada palabra
@Composable
internal fun StoreNameField(value: String, onValueChange: (String) -> Unit, errorMessage: String?, enabled: Boolean = true) {
    EcoTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.store_registration_name_label),
        placeholder = stringResource(R.string.store_registration_name_placeholder),
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
    )
}

// Lista desplegable con las categorías de comercio disponibles
@Composable
internal fun StoreCategoryField(selected: StoreCategory?, onSelected: (StoreCategory) -> Unit, errorMessage: String?, enabled: Boolean = true) {
    SelectField(
        label = stringResource(R.string.store_registration_category_label),
        placeholder = stringResource(R.string.store_application_category_placeholder),
        iconRes = R.drawable.ic_tag,
        options = StoreCategory.entries,
        selected = selected,
        optionLabel = { stringResource(it.labelRes()) },
        onSelected = onSelected,
        errorMessage = errorMessage,
        enabled = enabled,
    )
}

// Dirección física completa del establecimiento en Tuluá
@Composable
internal fun StoreAddressField(value: String, onValueChange: (String) -> Unit, errorMessage: String?, enabled: Boolean = true) {
    EcoTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.store_registration_address_label),
        placeholder = stringResource(R.string.store_registration_address_placeholder),
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
    )
}

// Teléfono de contacto que cierra el teclado al terminar de escribir
@Composable
internal fun StorePhoneField(value: String, onValueChange: (String) -> Unit, errorMessage: String?, enabled: Boolean = true) {
    val focusManager = LocalFocusManager.current
    EcoTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(R.string.store_registration_phone_label),
        placeholder = stringResource(R.string.store_registration_phone_placeholder),
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}
