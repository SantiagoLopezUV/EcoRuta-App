package com.ecoruta.app.features.auth.presentation.register

// Pantallas del registro del comercio aliado
// Incluye la cuenta del gerente, la confirmación por correo, los datos del comercio y el estado de la solicitud

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoCheckboxRow
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.profile.presentation.StoreCategory
import com.ecoruta.app.features.profile.presentation.StoreIdType
import com.ecoruta.app.features.profile.presentation.components.IdTypeSelector
import com.ecoruta.app.features.profile.presentation.components.SelectField
import com.ecoruta.app.features.profile.presentation.components.labelRes

// Acciones disponibles en el paso de cuenta del gerente
data class StoreOwnerAccountActions(
    val onFullNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

// Primer paso del registro donde el gerente crea la cuenta con su nombre y correo
@Composable
fun StoreOwnerAccountScreen(
    state: StoreOwnerAccountUiState,
    actions: StoreOwnerAccountActions,
    modifier: Modifier = Modifier,
) {
    if (state.isOffline) {
        OfflineScreen(onRetry = actions.onRetry)
        return
    }
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        actions.onContinue()
    }

    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_step_1_of_2),
        stageLabel = stringResource(R.string.store_registration_stage_owner),
        progress = 0.5f,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.registration_continue),
                onClick = submit,
                isLoading = state.isSubmitting,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            EcoTextLink(text = stringResource(R.string.registration_back), onClick = actions.onBack)
        },
    ) {
        // Con errores el título invita a revisar los datos sin perder lo que ya se escribió
        RegistrationTitle(
            title = stringResource(if (state.hasErrors) R.string.store_registration_owner_review_title else R.string.store_registration_owner_title),
            description = stringResource(
                if (state.hasErrors) R.string.store_registration_owner_review_description else R.string.store_registration_owner_description,
            ),
        )
        EcoTextField(
            value = state.fullName,
            onValueChange = actions.onFullNameChange,
            label = stringResource(R.string.store_registration_owner_name_label),
            placeholder = stringResource(R.string.store_registration_owner_name_placeholder),
            errorMessage = state.fullNameErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.email,
            onValueChange = actions.onEmailChange,
            label = stringResource(R.string.login_email_label),
            placeholder = stringResource(R.string.store_registration_email_placeholder),
            errorMessage = state.emailErrorRes?.let { stringResource(it) },
            supportingText = stringResource(R.string.store_registration_email_hint),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        Text(
            stringResource(if (state.hasErrors) R.string.store_registration_complete_required else R.string.store_registration_owner_info),
            style = EcoTextStyles.Subtitle,
            color = EcoTitle,
        )
    }
}

// Pantalla que pide al gerente abrir el enlace enviado a su correo
@Composable
fun StoreEmailConfirmationScreen(
    email: String,
    onResendClick: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecyclerEmailConfirmationScreen(
        type = RegistrationType.STORE,
        email = email,
        onResendClick = onResendClick,
        onExit = onExit,
        modifier = modifier,
    )
}

// Pantalla que confirma el correo del gerente y lo lleva a los datos del comercio
@Composable
fun StoreEmailConfirmedScreen(
    email: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RecyclerEmailConfirmedScreen(
        type = RegistrationType.STORE,
        email = email,
        onContinue = onContinue,
        modifier = modifier,
    )
}

// Acciones disponibles en el paso de datos del comercio
data class StoreDetailsActions(
    val onNameChange: (String) -> Unit = {},
    val onIdTypeChange: (StoreIdType) -> Unit = {},
    val onIdNumberChange: (String) -> Unit = {},
    val onCategoryChange: (StoreCategory) -> Unit = {},
    val onAddressChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onDataPolicyChange: (Boolean) -> Unit = {},
    val onViewPolicyClick: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

// Segundo paso del registro con los datos del establecimiento en Tuluá
@Composable
fun StoreDetailsScreen(
    state: StoreDetailsUiState,
    actions: StoreDetailsActions,
    modifier: Modifier = Modifier,
) {
    if (state.isOffline) {
        OfflineScreen(onRetry = actions.onRetry)
        return
    }
    val focusManager = LocalFocusManager.current

    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_step_2_of_2),
        stageLabel = stringResource(R.string.store_registration_stage_details),
        progress = 1f,
        modifier = modifier,
        bottomActions = {
            EcoCheckboxRow(
                checked = state.acceptedDataPolicy,
                onCheckedChange = actions.onDataPolicyChange,
                text = stringResource(R.string.registration_policy_consent),
                linkText = stringResource(R.string.registration_view_policy),
                onLinkClick = actions.onViewPolicyClick,
            )
            // La solicitud solo se puede enviar después de aceptar la política de datos
            EcoPrimaryButton(
                text = stringResource(R.string.store_registration_submit),
                onClick = {
                    focusManager.clearFocus()
                    actions.onSubmit()
                },
                enabled = state.acceptedDataPolicy,
                isLoading = state.isSubmitting,
            )
            EcoTextLink(text = stringResource(R.string.registration_back), onClick = actions.onBack)
        },
    ) {
        RegistrationTitle(
            title = stringResource(if (state.hasErrors) R.string.store_registration_details_review_title else R.string.store_registration_details_title),
            description = stringResource(
                if (state.hasErrors) R.string.store_registration_details_review_description else R.string.store_registration_details_description,
            ),
        )
        EcoTextField(
            value = state.name,
            onValueChange = actions.onNameChange,
            label = stringResource(R.string.store_registration_name_label),
            placeholder = stringResource(R.string.store_registration_name_placeholder),
            errorMessage = state.nameErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.idNumber,
            onValueChange = actions.onIdNumberChange,
            label = stringResource(R.string.store_registration_id_label),
            placeholder = stringResource(R.string.store_registration_id_placeholder),
            leadingIcon = { IdTypeSelector(selected = state.idType, onSelected = actions.onIdTypeChange) },
            errorMessage = state.idErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )
        SelectField(
            label = stringResource(R.string.store_registration_category_label),
            placeholder = stringResource(R.string.store_application_category_placeholder),
            iconRes = R.drawable.ic_tag,
            options = StoreCategory.entries,
            selected = state.category,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = actions.onCategoryChange,
            errorMessage = state.categoryErrorRes?.let { stringResource(it) },
        )
        Text(stringResource(R.string.store_registration_category_hint), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        EcoTextField(
            value = state.address,
            onValueChange = actions.onAddressChange,
            label = stringResource(R.string.store_registration_address_label),
            placeholder = stringResource(R.string.store_registration_address_placeholder),
            errorMessage = state.addressErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.phone,
            onValueChange = actions.onPhoneChange,
            label = stringResource(R.string.store_registration_phone_label),
            placeholder = stringResource(R.string.store_registration_phone_placeholder),
            errorMessage = state.phoneErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        Text(
            stringResource(if (state.hasErrors) R.string.store_registration_details_review_info else R.string.store_registration_details_info),
            style = EcoTextStyles.Subtitle,
            color = EcoTitle,
        )
    }
}

@Preview(name = "Comercio · Cuenta del dueño", widthDp = 393, heightDp = 852)
@Composable
private fun StoreOwnerAccountPreview() {
    EcoRutaTheme {
        StoreOwnerAccountScreen(state = StoreOwnerAccountUiState(), actions = StoreOwnerAccountActions())
    }
}

@Preview(name = "Comercio · Cuenta del dueño con validación", widthDp = 393, heightDp = 852)
@Composable
private fun StoreOwnerAccountValidationPreview() {
    EcoRutaTheme {
        StoreOwnerAccountScreen(
            state = StoreOwnerAccountUiState(
                email = "contacto@",
                fullNameErrorRes = R.string.store_registration_error_owner_name,
                emailErrorRes = R.string.store_registration_error_email,
            ),
            actions = StoreOwnerAccountActions(),
        )
    }
}

@Preview(name = "Comercio · Cuenta sin conexión", widthDp = 393, heightDp = 852)
@Composable
private fun StoreOwnerAccountOfflinePreview() {
    EcoRutaTheme {
        StoreOwnerAccountScreen(state = StoreOwnerAccountUiState(isOffline = true), actions = StoreOwnerAccountActions())
    }
}

@Preview(name = "Comercio · Confirmación de correo pendiente", widthDp = 393, heightDp = 852)
@Composable
private fun StoreEmailConfirmationPreview() {
    EcoRutaTheme {
        StoreEmailConfirmationScreen(email = "gerente@example.com", onResendClick = {}, onExit = {})
    }
}

@Preview(name = "Comercio · Correo validado", widthDp = 393, heightDp = 852)
@Composable
private fun StoreEmailConfirmedPreview() {
    EcoRutaTheme {
        StoreEmailConfirmedScreen(email = "gerente@example.com", onContinue = {})
    }
}

@Preview(name = "Comercio · Datos del comercio", widthDp = 393, heightDp = 1000)
@Composable
private fun StoreDetailsPreview() {
    EcoRutaTheme {
        StoreDetailsScreen(state = StoreDetailsUiState(), actions = StoreDetailsActions())
    }
}

@Preview(name = "Comercio · Datos del comercio con validación", widthDp = 393, heightDp = 1100)
@Composable
private fun StoreDetailsValidationPreview() {
    EcoRutaTheme {
        StoreDetailsScreen(
            state = StoreDetailsUiState(
                name = "Papelería El Lápiz",
                category = StoreCategory.STATIONERY_BOOKSTORE,
                idErrorRes = R.string.store_registration_error_id,
                addressErrorRes = R.string.store_registration_error_address,
                phoneErrorRes = R.string.store_registration_error_phone,
            ),
            actions = StoreDetailsActions(),
        )
    }
}
