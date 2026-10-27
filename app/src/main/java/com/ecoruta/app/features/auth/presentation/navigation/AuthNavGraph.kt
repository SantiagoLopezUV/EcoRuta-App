package com.ecoruta.app.features.auth.presentation.navigation

// Subgrafo de navegación para autenticación, registro y gestión de contraseñas
// Conecta las vistas de inicio de sesión, recuperación de cuenta y cambio de clave

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.core.ui.components.WebOnlyAccessScreen
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordActions
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordMode
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordScreen
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordUiState
import com.ecoruta.app.features.auth.presentation.first_login.PasswordChangedScreen
import com.ecoruta.app.features.auth.presentation.login.LoginActions
import com.ecoruta.app.features.auth.presentation.login.LoginScreen
import com.ecoruta.app.features.auth.presentation.login.LoginUiState
import com.ecoruta.app.features.auth.presentation.password_reset.PasswordRecoveryUiState
import com.ecoruta.app.features.auth.presentation.password_reset.PasswordUpdatedScreen
import com.ecoruta.app.features.auth.presentation.password_reset.RecoveryCodeScreen
import com.ecoruta.app.features.auth.presentation.password_reset.RecoveryEmailScreen
import com.ecoruta.app.features.auth.presentation.password_reset.ResetPasswordActions
import com.ecoruta.app.features.auth.presentation.password_reset.ResetPasswordScreen
import com.ecoruta.app.features.auth.presentation.register.CitizenAccessReadyScreen
import com.ecoruta.app.features.auth.presentation.register.CitizenAccountActions
import com.ecoruta.app.features.auth.presentation.register.CitizenAccountScreen
import com.ecoruta.app.features.auth.presentation.register.CitizenEmailConfirmationScreen
import com.ecoruta.app.features.auth.presentation.register.CitizenProfileActions
import com.ecoruta.app.features.auth.presentation.register.CitizenProfileScreen
import com.ecoruta.app.features.auth.presentation.register.CitizenRegistrationUiState
import com.ecoruta.app.features.auth.presentation.register.RegistrationType
import com.ecoruta.app.features.auth.presentation.register.RegistrationTypeScreen
import com.ecoruta.app.features.profile.presentation.UserRole

// Registra las rutas de inicio de sesión, recuperación, registro y cambio de contraseña
fun NavGraphBuilder.authGraph(navController: NavHostController) {
    fun navigateTo(route: String) = navController.navigate(route) { launchSingleTop = true }

    composable(EcoRutaDestination.INICIAR_SESION) {
        var state by remember { mutableStateOf(LoginUiState()) }
        LoginScreen(
            state = state,
            actions = LoginActions(
                onEmailChange = { state = state.copy(email = it, emailErrorRes = null, showInvalidCredentials = false) },
                onPasswordChange = { state = state.copy(password = it, passwordErrorRes = null, showInvalidCredentials = false) },
                onTogglePasswordVisibility = { state = state.copy(isPasswordVisible = !state.isPasswordVisible) },
                onForgotPasswordClick = { navigateTo(EcoRutaDestination.RECUPERACION) },
                onSubmit = {
                    navController.navigate(EcoRutaDestination.INICIO_CIUDADANO) {
                        popUpTo(EcoRutaDestination.INICIO_PUBLICO) { inclusive = true }
                    }
                },
                onGoogleClick = {
                    navController.navigate(EcoRutaDestination.INICIO_CIUDADANO) {
                        popUpTo(EcoRutaDestination.INICIO_PUBLICO) { inclusive = true }
                    }
                },
                onRegisterClick = { navigateTo(EcoRutaDestination.REGISTRO) },
            ),
        )
    }

    composable(EcoRutaDestination.ACCESO_NO_DISPONIBLE) {
        WebOnlyAccessScreen(
            isSigningOut = false,
            onSignOut = { navController.goToPublicHomeAfterSignOut() },
        )
    }

    passwordRecoveryGraph(navController = navController)
    registrationGraph(navController = navController)
    changePasswordDestinations(navController = navController)
}

private fun NavGraphBuilder.passwordRecoveryGraph(navController: NavHostController) {
    val backToLogin: () -> Unit = {
        navController.popBackStack(EcoRutaDestination.RECUPERACION, inclusive = true)
    }

    navigation(
        startDestination = EcoRutaDestination.RECUPERAR_CORREO,
        route = EcoRutaDestination.RECUPERACION,
    ) {
        composable(EcoRutaDestination.RECUPERAR_CORREO) {
            var state by remember { mutableStateOf(PasswordRecoveryUiState()) }
            RecoveryEmailScreen(
                state = state,
                onEmailChange = { state = state.copy(email = it, emailErrorRes = null) },
                onSubmit = { navController.navigate(EcoRutaDestination.RECUPERAR_CODIGO) { launchSingleTop = true } },
                onBackClick = backToLogin,
            )
        }
        composable(EcoRutaDestination.RECUPERAR_CODIGO) {
            var state by remember { mutableStateOf(PasswordRecoveryUiState(email = "ciudadano@ecoruta.co")) }
            RecoveryCodeScreen(
                state = state,
                onCodeChange = { state = state.copy(code = it, codeErrorRes = null) },
                onSubmit = { navController.navigate(EcoRutaDestination.RESTABLECER_CONTRASENA) { launchSingleTop = true } },
                onResendClick = {},
            )
        }
        composable(EcoRutaDestination.RESTABLECER_CONTRASENA) {
            var state by remember { mutableStateOf(PasswordRecoveryUiState()) }
            ResetPasswordScreen(
                state = state,
                actions = ResetPasswordActions(
                    onNewPasswordChange = { state = state.copy(newPassword = it, newPasswordErrorRes = null) },
                    onConfirmPasswordChange = { state = state.copy(confirmPassword = it, confirmPasswordErrorRes = null) },
                    onToggleNewPasswordVisibility = { state = state.copy(isNewPasswordVisible = !state.isNewPasswordVisible) },
                    onToggleConfirmPasswordVisibility = { state = state.copy(isConfirmPasswordVisible = !state.isConfirmPasswordVisible) },
                    onSave = { navController.navigate(EcoRutaDestination.CONTRASENA_ACTUALIZADA) { launchSingleTop = true } },
                    onBackToLogin = backToLogin,
                ),
            )
        }
        composable(EcoRutaDestination.CONTRASENA_ACTUALIZADA) {
            PasswordUpdatedScreen(onBackToLogin = backToLogin)
        }
    }
}

private fun NavGraphBuilder.registrationGraph(navController: NavHostController) {
    val backToLogin: () -> Unit = {
        navController.popBackStack(EcoRutaDestination.REGISTRO, inclusive = true)
    }

    navigation(
        startDestination = EcoRutaDestination.SELECCION_TIPO_REGISTRO,
        route = EcoRutaDestination.REGISTRO,
    ) {
        composable(EcoRutaDestination.SELECCION_TIPO_REGISTRO) {
            var selectedType by rememberSaveable { mutableStateOf<RegistrationType?>(null) }
            RegistrationTypeScreen(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                onContinue = { type ->
                    val route = when (type) {
                        RegistrationType.CIUDADANO -> EcoRutaDestination.REGISTRO_CIUDADANO_CUENTA
                        RegistrationType.RECICLADOR_INDEPENDIENTE -> EcoRutaDestination.REGISTRO_RECICLADOR_INDEPENDIENTE
                        RegistrationType.RECICLADOR_EMPRESA -> EcoRutaDestination.REGISTRO_RECICLADOR_EMPRESA
                        RegistrationType.COMERCIO -> EcoRutaDestination.REGISTRO_COMERCIO
                    }
                    navController.navigate(route) { launchSingleTop = true }
                },
                onLoginClick = backToLogin,
            )
        }
        composable(EcoRutaDestination.REGISTRO_CIUDADANO_CUENTA) {
            var state by remember { mutableStateOf(CitizenRegistrationUiState()) }
            CitizenAccountScreen(
                state = state,
                actions = CitizenAccountActions(
                    onFullNameChange = { state = state.copy(fullName = it, fullNameErrorRes = null) },
                    onEmailChange = { state = state.copy(email = it, emailErrorRes = null) },
                    onGoogleClick = { navController.navigate(EcoRutaDestination.REGISTRO_CIUDADANO_PERFIL) { launchSingleTop = true } },
                    onContinue = { navController.navigate(EcoRutaDestination.REGISTRO_CIUDADANO_PERFIL) { launchSingleTop = true } },
                    onChangeRegistrationType = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.REGISTRO_CIUDADANO_PERFIL) {
            var state by remember { mutableStateOf(CitizenRegistrationUiState()) }
            CitizenProfileScreen(
                state = state,
                actions = CitizenProfileActions(
                    onDocumentTypeChange = { state = state.copy(documentType = it, documentErrorRes = null) },
                    onDocumentNumberChange = { state = state.copy(documentNumber = it, documentErrorRes = null) },
                    onPhoneChange = { state = state.copy(phone = it, phoneErrorRes = null) },
                    onNeighborhoodChange = { state = state.copy(neighborhood = it, neighborhoodErrorRes = null) },
                    onDataPolicyChange = { state = state.copy(acceptedDataPolicy = it) },
                    onViewPolicyClick = {},
                    onCreateAccount = {
                        navController.navigate(EcoRutaDestination.REGISTRO_CIUDADANO_CONFIRMACION) {
                            popUpTo(EcoRutaDestination.SELECCION_TIPO_REGISTRO)
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.REGISTRO_CIUDADANO_CONFIRMACION) {
            CitizenEmailConfirmationScreen(
                email = "ciudadano@ecoruta.co",
                isResending = false,
                onResendClick = {},
                onGoToLogin = backToLogin,
            )
        }
        composable(EcoRutaDestination.REGISTRO_CIUDADANO_LISTO) {
            CitizenAccessReadyScreen(onEnterClick = backToLogin)
        }
    }
}

private fun NavGraphBuilder.changePasswordDestinations(navController: NavHostController) {
    val arguments = listOf(
        navArgument(EcoRutaDestination.ARG_MODO) { type = NavType.StringType },
        navArgument(EcoRutaDestination.ARG_ROL) { type = NavType.StringType },
    )

    composable(EcoRutaDestination.CAMBIAR_CONTRASENA, arguments = arguments) { entry ->
        val mode = ChangePasswordMode.valueOf(entry.arguments?.getString(EcoRutaDestination.ARG_MODO).orEmpty())
        val role = UserRole.valueOf(entry.arguments?.getString(EcoRutaDestination.ARG_ROL).orEmpty())
        var state by remember { mutableStateOf(ChangePasswordUiState()) }
        ChangePasswordScreen(
            mode = mode,
            role = role,
            state = state,
            actions = ChangePasswordActions(
                onCurrentChange = { state = state.copy(currentPassword = it, currentErrorRes = null) },
                onNewChange = { state = state.copy(newPassword = it, newErrorRes = null) },
                onConfirmChange = { state = state.copy(confirmPassword = it, confirmErrorRes = null) },
                onToggleCurrentVisibility = { state = state.copy(isCurrentVisible = !state.isCurrentVisible) },
                onToggleNewVisibility = { state = state.copy(isNewVisible = !state.isNewVisible) },
                onToggleConfirmVisibility = { state = state.copy(isConfirmVisible = !state.isConfirmVisible) },
                onSave = {
                    navController.navigate(EcoRutaDestination.contrasenaCambiada(mode, role)) {
                        popUpTo(EcoRutaDestination.CAMBIAR_CONTRASENA) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            ),
        )
    }

    composable(EcoRutaDestination.CONTRASENA_CAMBIADA, arguments = arguments) { entry ->
        val mode = ChangePasswordMode.valueOf(entry.arguments?.getString(EcoRutaDestination.ARG_MODO).orEmpty())
        val role = UserRole.valueOf(entry.arguments?.getString(EcoRutaDestination.ARG_ROL).orEmpty())
        PasswordChangedScreen(
            mode = mode,
            role = role,
            onContinue = {
                if (mode == ChangePasswordMode.PRIMER_INGRESO) {
                    navController.navigate(EcoRutaDestination.inicioDelRol(role)) {
                        popUpTo(EcoRutaDestination.INICIO_PUBLICO) { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            },
        )
    }
}
