package com.ecoruta.app.features.auth.presentation.register

// Paso de creación de la cuenta del ciudadano: correo y contraseña

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSecondaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles

data class CitizenAccountActions(
    val onFullNameChange: (String) -> Unit = {},
    val onEmailChange: (String) -> Unit = {},
    val onGoogleClick: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onChangeRegistrationType: () -> Unit = {},
)

@Composable
fun CitizenAccountScreen(
    state: CitizenRegistrationUiState,
    actions: CitizenAccountActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        actions.onContinue()
    }

    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_step_1_of_2),
        stageLabel = stringResource(R.string.registration_stage_account),
        progress = 0.5f,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.registration_continue),
                onClick = submit,
                isLoading = state.isCheckingEmail,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            EcoTextLink(text = stringResource(R.string.registration_change_type), onClick = actions.onChangeRegistrationType)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_account_title),
            description = stringResource(R.string.registration_account_description),
        )
        EcoSecondaryButton(
            text = stringResource(R.string.login_google),
            onClick = actions.onGoogleClick,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = EcoBorder)
            Text(stringResource(R.string.registration_or_email), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = EcoBorder)
        }
        EcoTextField(
            value = state.fullName,
            onValueChange = actions.onFullNameChange,
            label = stringResource(R.string.registration_full_name_label),
            placeholder = stringResource(R.string.registration_full_name_placeholder),
            errorMessage = state.fullNameErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.email,
            onValueChange = actions.onEmailChange,
            label = stringResource(R.string.login_email_label),
            placeholder = stringResource(R.string.login_email_placeholder),
            errorMessage = state.emailErrorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
    }
}

@Preview(name = "Paso 1 Cuenta", widthDp = 393, heightDp = 852)
@Composable
private fun CitizenAccountPreview() {
    EcoRutaTheme {
        CitizenAccountScreen(
            state = CitizenRegistrationUiState(
                fullName = "Camila Andrea Gómez",
                email = "camila.gomez@ecoruta.co",
            ),
            actions = CitizenAccountActions(),
        )
    }
}
