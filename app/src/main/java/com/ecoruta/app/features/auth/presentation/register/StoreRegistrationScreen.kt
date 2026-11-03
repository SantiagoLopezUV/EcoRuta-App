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
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.OfflineScreen
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

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
