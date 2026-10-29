package com.ecoruta.app.features.auth.presentation.register

// Pantallas finales del registro ciudadano: confirmación de correo y acceso listo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

@Composable
fun CitizenEmailConfirmationScreen(
    email: String,
    isResending: Boolean,
    onResendClick: () -> Unit,
    onGoToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onGoToLogin)
    }
    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_citizen),
        stageLabel = stringResource(R.string.registration_stage_confirmation),
        progress = 1f,
        contentSpacing = 20.dp,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.registration_go_to_login), onClick = onGoToLogin)
            EcoTextLink(
                text = stringResource(
                    if (isResending) R.string.registration_resending_email else R.string.registration_resend_email,
                ),
                onClick = { if (!isResending) onResendClick() },
            )
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_confirm_title),
            description = stringResource(R.string.registration_confirm_description),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.registration_delivery_email), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            Text(email, style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
        Text(stringResource(R.string.registration_spam_hint), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        CenteredBrand(modifier = Modifier.padding(top = 48.dp))
    }
}

@Composable
fun CitizenAccessReadyScreen(
    onEnterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onEnterClick)
    }
    RegistrationLayout(
        stepLabel = stringResource(R.string.registration_citizen),
        stageLabel = stringResource(R.string.registration_stage_finished),
        progress = 1f,
        contentSpacing = 20.dp,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.registration_enter), onClick = onEnterClick)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.registration_ready_title),
            description = stringResource(R.string.registration_ready_description),
        )
        CenteredBrand(modifier = Modifier.padding(top = 120.dp))
    }
}

@Preview(name = "Confirmar correo", widthDp = 393, heightDp = 852)
@Composable
private fun CitizenEmailConfirmationPreview() {
    EcoRutaTheme {
        CitizenEmailConfirmationScreen(
            email = "camila.gomez@ecoruta.co",
            isResending = false,
            onResendClick = {},
            onGoToLogin = {},
        )
    }
}

@Preview(name = "Acceso listo", widthDp = 393, heightDp = 852)
@Composable
private fun CitizenAccessReadyPreview() {
    EcoRutaTheme {
        CitizenAccessReadyScreen(onEnterClick = {})
    }
}
