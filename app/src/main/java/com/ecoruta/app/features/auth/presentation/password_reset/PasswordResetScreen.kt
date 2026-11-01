package com.ecoruta.app.features.auth.presentation.password_reset

// Vista composable para el restablecimiento de contraseña
// Flujo de 4 pasos: ingreso de correo, código de verificación y nueva contraseña

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoBrandHeader
import com.ecoruta.app.core.ui.components.EcoCodeInput
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoIconBadge
import com.ecoruta.app.core.ui.components.EcoInputIcon
import com.ecoruta.app.core.ui.components.EcoPasswordRequirements
import com.ecoruta.app.core.ui.components.EcoPasswordVisibilityToggle
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.auth.presentation.login.LoginActions
import com.ecoruta.app.features.auth.presentation.login.LoginContent
import com.ecoruta.app.features.auth.presentation.login.LoginUiState

@Composable
fun RecoveryEmailScreen(
    state: PasswordRecoveryUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSubmit()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
            .padding(bottom = 32.dp),
    ) {
        EcoBrandHeader(onBackClick = onBackClick)

        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.recovery_email_title), style = EcoTextStyles.Title, color = EcoTitle)
                Text(stringResource(R.string.recovery_email_subtitle), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EcoTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.login_email_label),
                    placeholder = stringResource(R.string.login_email_placeholder),
                    leadingIcon = { EcoInputIcon(R.drawable.ic_mail) },
                    errorMessage = state.emailErrorRes?.let { stringResource(it) },
                    isError = state.emailErrorRes != null || state.requestFailed,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                if (state.requestFailed) {
                    EcoErrorMessage(text = stringResource(R.string.recovery_error_request_failed))
                }
                EcoPrimaryButton(
                    text = stringResource(R.string.recovery_verify_continue),
                    onClick = submit,
                    isLoading = state.isRequestingCode,
                )
            }
        }
    }
}

@Composable
fun RecoveryCodeScreen(
    state: PasswordRecoveryUiState,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onResendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val submit = {
        focusManager.clearFocus()
        onSubmit()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
            .padding(bottom = 32.dp),
    ) {
        EcoBrandHeader()

        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(EcoFieldBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(EcoPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mail_badge),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(33.dp),
                        )
                    }
                }
                Text(stringResource(R.string.recovery_code_overline), style = EcoTextStyles.Subtitle, color = EcoPrimary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(stringResource(R.string.recovery_code_title), style = EcoTextStyles.Title, color = EcoTitle)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.recovery_code_subtitle, state.email.trim()),
                        style = EcoTextStyles.Subtitle.copy(fontSize = 12.sp),
                        color = EcoSubtitle,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        EcoCodeInput(
                            value = state.code,
                            onValueChange = onCodeChange,
                            length = RECOVERY_CODE_LENGTH,
                            accessibilityLabel = stringResource(R.string.recovery_code_accessibility),
                            isError = state.codeErrorRes != null,
                            onDone = submit,
                        )
                        state.codeErrorRes?.let {
                            EcoErrorMessage(
                                text = stringResource(it),
                                iconRes = R.drawable.ic_alert_triangle,
                                iconSize = DpSize(18.dp, 20.dp),
                            )
                        }
                    }
                }

                EcoPrimaryButton(
                    text = stringResource(
                        if (state.isCodeExpired) R.string.recovery_request_new_code else R.string.recovery_verify_continue,
                    ),
                    onClick = submit,
                    isLoading = state.isVerifyingCode || (state.isCodeExpired && state.isRequestingCode),
                )
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.resendSecondsLeft > 0 && !state.isCodeExpired) {
                    Text(
                        text = stringResource(R.string.recovery_resend_in, formatSeconds(state.resendSecondsLeft)),
                        style = EcoTextStyles.Subtitle,
                        color = EcoPrimary,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    EcoTextLink(text = stringResource(R.string.recovery_resend_code), onClick = onResendClick)
                }
            }
        }
    }
}

private fun formatSeconds(totalSeconds: Int): String =
    "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)

data class ResetPasswordActions(
    val onNewPasswordChange: (String) -> Unit = {},
    val onConfirmPasswordChange: (String) -> Unit = {},
    val onToggleNewPasswordVisibility: () -> Unit = {},
    val onToggleConfirmPasswordVisibility: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onBackToLogin: () -> Unit = {},
)

@Composable
fun ResetPasswordScreen(
    state: PasswordRecoveryUiState,
    actions: ResetPasswordActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val save = {
        focusManager.clearFocus()
        actions.onSave()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
            .padding(bottom = 32.dp),
    ) {
        EcoBrandHeader()

        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EcoIconBadge(iconRes = R.drawable.ic_shield_check)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.reset_title), style = EcoTextStyles.Title, color = EcoTitle)
                    Text(stringResource(R.string.reset_subtitle), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EcoTextField(
                    value = state.newPassword,
                    onValueChange = actions.onNewPasswordChange,
                    label = stringResource(R.string.reset_new_password_label),
                    placeholder = stringResource(R.string.reset_new_password_placeholder),
                    leadingIcon = { EcoInputIcon(R.drawable.ic_lock) },
                    trailingIcon = {
                        EcoPasswordVisibilityToggle(state.isNewPasswordVisible, actions.onToggleNewPasswordVisibility)
                    },
                    errorMessage = state.newPasswordErrorRes?.let { stringResource(it) },
                    visualTransformation = if (state.isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                )
                EcoPasswordRequirements(password = state.newPassword)
                EcoTextField(
                    value = state.confirmPassword,
                    onValueChange = actions.onConfirmPasswordChange,
                    label = stringResource(R.string.reset_confirm_password_label),
                    placeholder = stringResource(R.string.reset_confirm_password_placeholder),
                    leadingIcon = { EcoInputIcon(R.drawable.ic_lock) },
                    trailingIcon = {
                        EcoPasswordVisibilityToggle(state.isConfirmPasswordVisible, actions.onToggleConfirmPasswordVisibility)
                    },
                    errorMessage = state.confirmPasswordErrorRes?.let { stringResource(it) },
                    visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EcoPrimaryButton(
                    text = stringResource(R.string.reset_save),
                    onClick = save,
                    isLoading = state.isSavingPassword,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EcoTextLink(
                        text = stringResource(R.string.recovery_back_to_login),
                        onClick = actions.onBackToLogin,
                        leadingIconRes = R.drawable.ic_arrow_left,
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordUpdatedScreen(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onBackToLogin)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
            .padding(bottom = 32.dp),
    ) {
        EcoBrandHeader()
        Column(
            modifier = Modifier.padding(top = 136.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EcoIconBadge(iconRes = R.drawable.ic_shield_check)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.password_updated_title), style = EcoTextStyles.Title, color = EcoTitle)
                    Text(stringResource(R.string.password_updated_message), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
            }
            EcoPrimaryButton(text = stringResource(R.string.recovery_back_to_login), onClick = onBackToLogin)
        }
    }
}

@Preview(name = "Correo", widthDp = 393, heightDp = 852)
@Composable
private fun RecoveryEmailPreview() {
    EcoRutaTheme {
        RecoveryEmailScreen(
            state = PasswordResetUiState(email = "camila.gomez@ecoruta.co"),
            onEmailChange = {},
            onSubmit = {},
            onBackClick = {},
        )
    }
}

@Preview(name = "Código OTP", widthDp = 393, heightDp = 852)
@Composable
private fun RecoveryCodePreview() {
    EcoRutaTheme {
        RecoveryCodeScreen(
            state = PasswordResetUiState(email = "camila.gomez@ecoruta.co", code = "482910", resendSecondsLeft = 45),
            onCodeChange = {},
            onSubmit = {},
            onResendClick = {},
        )
    }
}

@Preview(name = "Nueva Clave", widthDp = 393, heightDp = 1048)
@Composable
private fun PasswordRecoveryPreview() {
    EcoRutaTheme { ResetPasswordScreen(state = PasswordResetUiState(), actions = ResetPasswordActions()) }
}

@Preview(name = "Actualizada", widthDp = 393, heightDp = 852)
@Composable
private fun PasswordUpdatedPreview() {
    EcoRutaTheme {
        PasswordUpdatedScreen(onBackToLogin = {})
    }
}