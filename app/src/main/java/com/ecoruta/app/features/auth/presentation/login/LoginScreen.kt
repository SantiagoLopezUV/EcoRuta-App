package com.ecoruta.app.features.auth.presentation.login

// Vista composable de la pantalla de inicio de sesión
// Renderiza campos de correo, contraseña, botón de acceso y opciones alternativas

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoBrandHeader
import com.ecoruta.app.core.ui.components.EcoCheckboxRow
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInputIcon
import com.ecoruta.app.core.ui.components.EcoPasswordVisibilityToggle
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSecondaryButton
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.profile.presentation.UserRole

@get:StringRes
internal val UserRole.labelRes: Int
    get() = when (this) {
        UserRole.CIUDADANO -> R.string.role_citizen
        UserRole.RECICLADOR -> R.string.role_recycler
        UserRole.CHOFER -> R.string.role_driver
        UserRole.COMERCIO -> R.string.role_store
    }

// Eventos y acciones que la pantalla de inicio de sesión expone hacia el grafo de navegación
data class LoginActions(
    val onEmailChange: (String) -> Unit = {},
    val onPasswordChange: (String) -> Unit = {},
    val onTogglePasswordVisibility: () -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onForgotPasswordClick: () -> Unit = {},
    val onGoogleClick: () -> Unit = {},
    val onRegisterClick: () -> Unit = {},
)

// Pantalla principal de inicio de sesión con soporte para estados de validación y error
@Composable
fun LoginScreen(
    state: LoginUiState = LoginUiState(),
    actions: LoginActions = LoginActions(),
    modifier: Modifier = Modifier,
) {
    LoginContent(state = state, actions = actions, modifier = modifier)
}

@Composable
fun LoginContent(
    state: LoginUiState,
    actions: LoginActions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

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
            LoginHero()

            // Bienvenida
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.login_title), style = EcoTextStyles.Titulo, color = EcoTitle)
                Text(stringResource(R.string.login_subtitle), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
            }

            // Credenciales
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                EcoTextField(
                    value = state.email,
                    onValueChange = actions.onEmailChange,
                    label = stringResource(R.string.login_email_label),
                    placeholder = stringResource(R.string.login_email_placeholder),
                    leadingIcon = { EcoInputIcon(R.drawable.ic_mail) },
                    errorMessage = state.emailErrorRes?.let { stringResource(it) },
                    isError = state.emailErrorRes != null || state.showInvalidCredentials,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EcoTextField(
                        value = state.password,
                        onValueChange = actions.onPasswordChange,
                        label = stringResource(R.string.login_password_label),
                        placeholder = stringResource(R.string.login_password_placeholder),
                        leadingIcon = { EcoInputIcon(R.drawable.ic_lock) },
                        trailingIcon = {
                            EcoPasswordVisibilityToggle(
                                isVisible = state.isPasswordVisible,
                                onToggle = actions.onTogglePasswordVisibility,
                            )
                        },
                        errorMessage = state.passwordErrorRes?.let { stringResource(it) },
                        isError = state.passwordErrorRes != null || state.showInvalidCredentials,
                        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            actions.onSubmit()
                        }),
                    )
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        EcoTextLink(
                            text = stringResource(R.string.login_forgot_password),
                            onClick = actions.onForgotPasswordClick,
                        )
                    }
                }

                if (state.showInvalidCredentials) {
                    EcoErrorMessage(text = stringResource(R.string.login_error_invalid_credentials))
                }

                EcoPrimaryButton(
                    text = stringResource(R.string.login_submit),
                    onClick = {
                        focusManager.clearFocus()
                        actions.onSubmit()
                    },
                    isLoading = state.isSubmitting,
                )
            }

            // Alternativas de acceso
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = EcoBorder)
                    Text(stringResource(R.string.login_divider_or), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
                    HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = EcoBorder)
                }
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
            }

            // Crear cuenta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.login_no_account), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
                EcoTextLink(text = stringResource(R.string.login_register), onClick = actions.onRegisterClick)
            }
        }
    }
}

// Tarjeta visual de cabecera con imagen de fondo, degradado de contraste y mensaje de bienvenida
@Composable
private fun LoginHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(24.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.login_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.35f to EcoPrimary.copy(alpha = 0f),
                        1f to EcoPrimary.copy(alpha = 0.72f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.login_hero_title), style = EcoTextStyles.Titulo, color = EcoWhite)
            Text(stringResource(R.string.login_hero_description), style = EcoTextStyles.Subtitulo, color = EcoWhite)
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_leaf),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp),
                )
                Text(stringResource(R.string.login_hero_benefit), style = EcoTextStyles.EtiquetaCampo, color = EcoWhite)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignModeLoginSheet(
    onContinue: (role: UserRole, simulateFirstLogin: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onWebOnly: () -> Unit = {},
) {
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.CIUDADANO) }
    var simulateFirstLogin by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = EcoWhite,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = EcoDimens.ScreenHorizontalPadding)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.design_mode_title), style = EcoTextStyles.Titulo, color = EcoTitle)
                Text(stringResource(R.string.design_mode_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
            }
            Text(stringResource(R.string.design_mode_role_label), style = EcoTextStyles.EtiquetaCampo, color = EcoTitle)
            Column(Modifier.selectableGroup()) {
                UserRole.entries.forEach { role ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(selected = role == selectedRole, role = Role.RadioButton) { selectedRole = role }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = role == selectedRole,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = EcoAccent, unselectedColor = EcoSubtitle),
                        )
                        Text(stringResource(role.labelRes), style = EcoTextStyles.Subtitulo, color = EcoTitle)
                    }
                }
            }
            EcoCheckboxRow(
                checked = simulateFirstLogin,
                onCheckedChange = { simulateFirstLogin = it },
                text = stringResource(R.string.design_mode_first_login),
            )
            EcoPrimaryButton(
                text = stringResource(R.string.registration_continue),
                onClick = { onContinue(selectedRole, simulateFirstLogin) },
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EcoTextLink(text = stringResource(R.string.design_mode_web_only), onClick = onWebOnly)
            }
        }
    }
}

@Preview(name = "Iniciar sesión", widthDp = 393, heightDp = 1048)
@Composable
private fun LoginPreview() {
    EcoRutaTheme { LoginContent(state = LoginUiState(), actions = LoginActions()) }
}
