package com.ecoruta.app.features.auth.presentation.first_login

// Vista composable para cambio obligatorio de contraseña provisional
// Pantalla de bloqueo para operadores o recicladores con must_change_password = true

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoInputIcon
import com.ecoruta.app.core.ui.components.EcoPasswordRequirements
import com.ecoruta.app.core.ui.components.EcoPasswordVisibilityToggle
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.features.auth.presentation.register.CenteredBrand
import com.ecoruta.app.features.auth.presentation.register.RegistrationLayout
import com.ecoruta.app.features.auth.presentation.register.RegistrationTitle
import com.ecoruta.app.features.profile.presentation.UserRole

@StringRes
private fun UserRole.accountNameRes(): Int = when (this) {
    UserRole.CIUDADANO -> R.string.account_citizen
    UserRole.RECICLADOR -> R.string.account_recycler
    UserRole.CHOFER -> R.string.account_driver
    UserRole.COMERCIO -> R.string.account_store
}

data class ChangePasswordActions(
    val onCurrentChange: (String) -> Unit = {},
    val onNewChange: (String) -> Unit = {},
    val onConfirmChange: (String) -> Unit = {},
    val onToggleCurrentVisibility: () -> Unit = {},
    val onToggleNewVisibility: () -> Unit = {},
    val onToggleConfirmVisibility: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun ChangePasswordScreen(
    mode: ChangePasswordMode,
    role: UserRole,
    state: ChangePasswordUiState,
    actions: ChangePasswordActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val save = {
        focusManager.clearFocus()
        actions.onSave()
    }
    val isFirstLogin = mode == ChangePasswordMode.FIRST_LOGIN

    RegistrationLayout(
        stepLabel = stringResource(if (isFirstLogin) R.string.first_login else R.string.my_profile),
        stageLabel = stringResource(R.string.change_password_stage),
        progress = 0.5f,
        onBackClick = actions.onBack,
        modifier = modifier,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.reset_save), onClick = save, isLoading = state.isSaving)
        },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.change_password_title),
            description = if (isFirstLogin) {
                stringResource(R.string.change_password_first_login_description)
            } else {
                stringResource(R.string.change_password_profile_description, stringResource(role.accountNameRes()))
            },
        )
        EcoTextField(
            value = state.currentPassword,
            onValueChange = actions.onCurrentChange,
            label = stringResource(R.string.change_password_current_label),
            leadingIcon = { EcoInputIcon(R.drawable.ic_lock) },
            trailingIcon = { EcoPasswordVisibilityToggle(state.isCurrentVisible, actions.onToggleCurrentVisibility) },
            errorMessage = state.currentErrorRes?.let { stringResource(it) },
            supportingText = stringResource(
                if (isFirstLogin) R.string.change_password_current_hint_temporary else R.string.change_password_current_hint_profile,
            ),
            visualTransformation = if (state.isCurrentVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        )
        EcoTextField(
            value = state.newPassword,
            onValueChange = actions.onNewChange,
            label = stringResource(R.string.reset_new_password_label),
            placeholder = stringResource(R.string.reset_new_password_placeholder),
            leadingIcon = { EcoInputIcon(R.drawable.ic_lock) },
            trailingIcon = { EcoPasswordVisibilityToggle(state.isNewVisible, actions.onToggleNewVisibility) },
            errorMessage = state.newErrorRes?.let { stringResource(it) },
            visualTransformation = if (state.isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
        )
        EcoPasswordRequirements(password = state.newPassword)
        EcoTextField(
            value = state.confirmPassword,
            onValueChange = actions.onConfirmChange,
            label = stringResource(R.string.reset_confirm_password_label),
            placeholder = stringResource(R.string.reset_confirm_password_placeholder),
            leadingIcon = { EcoInputIcon(R.drawable.ic_lock) },
            trailingIcon = { EcoPasswordVisibilityToggle(state.isConfirmVisible, actions.onToggleConfirmVisibility) },
            errorMessage = state.confirmErrorRes?.let { stringResource(it) },
            visualTransformation = if (state.isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
    }
}

@Composable
fun PasswordChangedScreen(
    mode: ChangePasswordMode,
    role: UserRole,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onContinue)
    }
    val isFirstLogin = mode == ChangePasswordMode.FIRST_LOGIN
    val (descriptionRes, buttonRes) = if (isFirstLogin) {
        when (role) {
            UserRole.CIUDADANO -> R.string.password_changed_citizen to R.string.password_changed_go_home
            UserRole.RECICLADOR -> R.string.password_changed_recycler to R.string.password_changed_go_recycler
            UserRole.CHOFER -> R.string.password_changed_driver to R.string.password_changed_go_orders
            UserRole.COMERCIO -> R.string.password_changed_store to R.string.password_changed_go_store_panel
        }
    } else {
        R.string.password_changed_profile to R.string.password_changed_back_to_profile
    }

    RegistrationLayout(
        stepLabel = stringResource(if (isFirstLogin) R.string.first_login else R.string.my_profile),
        stageLabel = stringResource(R.string.registration_stage_finished),
        progress = 1f,
        modifier = modifier,
        bottomActions = { EcoPrimaryButton(text = stringResource(buttonRes), onClick = onContinue) },
    ) {
        RegistrationTitle(
            title = stringResource(R.string.password_changed_title),
            description = stringResource(descriptionRes),
        )
        CenteredBrand(modifier = Modifier.padding(top = 120.dp))
    }
}

@Preview(name = "Cambio obligatorio", widthDp = 393, heightDp = 900)
@Composable
private fun ChangePasswordFirstLoginPreview() {
    EcoRutaTheme {
        ChangePasswordScreen(ChangePasswordMode.FIRST_LOGIN, UserRole.RECICLADOR, ChangePasswordUiState(newPassword = "eco2026"), ChangePasswordActions())
    }
}

@Preview(name = "Contraseña actualizada", widthDp = 393, heightDp = 852)
@Composable
private fun PasswordChangedPreview() {
    EcoRutaTheme {
        PasswordChangedScreen(
            mode = ChangePasswordMode.PROFILE,
            role = UserRole.CIUDADANO,
            onContinue = {},
        )
    }
}
