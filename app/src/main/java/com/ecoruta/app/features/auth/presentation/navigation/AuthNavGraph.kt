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

    composable(EcoRutaDestination.LOGIN) {
        var state by remember { mutableStateOf(LoginUiState()) }
        LoginScreen(
            state = state,
            actions = LoginActions(
                onEmailChange = { state = state.copy(email = it, emailErrorRes = null, showInvalidCredentials = false) },
                onPasswordChange = { state = state.copy(password = it, passwordErrorRes = null, showInvalidCredentials = false) },
                onTogglePasswordVisibility = { state = state.copy(isPasswordVisible = !state.isPasswordVisible) },
                onForgotPasswordClick = { navigateTo(EcoRutaDestination.RECOVERY) },
                onSubmit = {
                    navController.navigate(EcoRutaDestination.CITIZEN_HOME) {
                        popUpTo(EcoRutaDestination.GUEST_USER) { inclusive = true }
                    }
                },
                onGoogleClick = {
                    navController.navigate(EcoRutaDestination.CITIZEN_HOME) {
                        popUpTo(EcoRutaDestination.GUEST_USER) { inclusive = true }
                    }
                },
                onRegisterClick = { navigateTo(EcoRutaDestination.RECORD) },
            ),
        )
    }

    composable(EcoRutaDestination.WEB_ONLY_ACCESS) {
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
        navController.popBackStack(EcoRutaDestination.RECOVERY, inclusive = true)
    }

    navigation(
        startDestination = EcoRutaDestination.RECOVER_EMAIL,
        route = EcoRutaDestination.RECOVERY,
    ) {
        composable(EcoRutaDestination.RECOVER_EMAIL) {
            var state by remember { mutableStateOf(PasswordRecoveryUiState()) }
            RecoveryEmailScreen(
                state = state,
                onEmailChange = { state = state.copy(email = it, emailErrorRes = null) },
                onSubmit = { navController.navigate(EcoRutaDestination.RETRIEVE_CODE) { launchSingleTop = true } },
                onBackClick = backToLogin,
            )
        }
        composable(EcoRutaDestination.RETRIEVE_CODE) {
            var state by remember { mutableStateOf(PasswordRecoveryUiState(email = "ciudadano@ecoruta.co")) }
            RecoveryCodeScreen(
                state = state,
                onCodeChange = { state = state.copy(code = it, codeErrorRes = null) },
                onSubmit = { navController.navigate(EcoRutaDestination.RESET_PASSWORD) { launchSingleTop = true } },
                onResendClick = {},
            )
        }
        composable(EcoRutaDestination.RESET_PASSWORD) {
            var state by remember { mutableStateOf(PasswordRecoveryUiState()) }
            ResetPasswordScreen(
                state = state,
                actions = ResetPasswordActions(
                    onNewPasswordChange = { state = state.copy(newPassword = it, newPasswordErrorRes = null) },
                    onConfirmPasswordChange = { state = state.copy(confirmPassword = it, confirmPasswordErrorRes = null) },
                    onToggleNewPasswordVisibility = { state = state.copy(isNewPasswordVisible = !state.isNewPasswordVisible) },
                    onToggleConfirmPasswordVisibility = { state = state.copy(isConfirmPasswordVisible = !state.isConfirmPasswordVisible) },
                    onSave = { navController.navigate(EcoRutaDestination.PASSWORD_UPDATED) { launchSingleTop = true } },
                    onBackToLogin = backToLogin,
                ),
            )
        }
        composable(EcoRutaDestination.PASSWORD_UPDATED) {
            PasswordUpdatedScreen(onBackToLogin = backToLogin)
        }
    }
}

private fun NavGraphBuilder.registrationGraph(navController: NavHostController) {
    val backToLogin: () -> Unit = {
        navController.popBackStack(EcoRutaDestination.RECORD, inclusive = true)
    }

    navigation(
        startDestination = EcoRutaDestination.RECORD_TYPE_SELECTION,
        route = EcoRutaDestination.RECORD,
    ) {
        composable(EcoRutaDestination.RECORD_TYPE_SELECTION) {
            var selectedType by rememberSaveable { mutableStateOf<RegistrationType?>(null) }
            RegistrationTypeScreen(
                selectedType = selectedType,
                onTypeSelected = { selectedType = it },
                onContinue = { type ->
                    val route = when (type) {
                        RegistrationType.CITIZEN -> EcoRutaDestination.CITIZEN_REGISTRATION_ACCOUNT
                        RegistrationType.INDEPENDENT_RECYCLER -> EcoRutaDestination.INDEPENDENT_RECYCLER_REGISTRATION
                        RegistrationType.COMPANY_RECYCLER -> EcoRutaDestination.COMPANY_RECYCLER_REGISTRATION
                        RegistrationType.STORE -> EcoRutaDestination.STORE_REGISTRATION
                    }
                    navController.navigate(route) { launchSingleTop = true }
                },
                onLoginClick = backToLogin,
            )
        }
        composable(EcoRutaDestination.CITIZEN_REGISTRATION_ACCOUNT) {
            var state by remember { mutableStateOf(CitizenRegistrationUiState()) }
            CitizenAccountScreen(
                state = state,
                actions = CitizenAccountActions(
                    onFullNameChange = { state = state.copy(fullName = it, fullNameErrorRes = null) },
                    onEmailChange = { state = state.copy(email = it, emailErrorRes = null) },
                    onGoogleClick = { navController.navigate(EcoRutaDestination.CITIZEN_REGISTRATION_PROFILE) { launchSingleTop = true } },
                    onContinue = { navController.navigate(EcoRutaDestination.CITIZEN_REGISTRATION_PROFILE) { launchSingleTop = true } },
                    onChangeRegistrationType = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.CITIZEN_REGISTRATION_PROFILE) {
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
                        navController.navigate(EcoRutaDestination.CITIZEN_REGISTRATION_CONFIRMATION) {
                            popUpTo(EcoRutaDestination.RECORD_TYPE_SELECTION)
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.CITIZEN_REGISTRATION_CONFIRMATION) {
            CitizenEmailConfirmationScreen(
                email = "ciudadano@ecoruta.co",
                isResending = false,
                onResendClick = {},
                onGoToLogin = backToLogin,
            )
        }
        composable(EcoRutaDestination.CITIZEN_REGISTRATION_DONE) {
            CitizenAccessReadyScreen(onEnterClick = backToLogin)
        }
    }
}

private fun NavGraphBuilder.changePasswordDestinations(navController: NavHostController) {
    val arguments = listOf(
        navArgument(EcoRutaDestination.ARG_MODE) { type = NavType.StringType },
        navArgument(EcoRutaDestination.ARG_ROLE) { type = NavType.StringType },
    )

    composable(EcoRutaDestination.CHANGE_PASSWORD, arguments = arguments) { entry ->
        val mode = ChangePasswordMode.valueOf(entry.arguments?.getString(EcoRutaDestination.ARG_MODE).orEmpty())
        val role = UserRole.valueOf(entry.arguments?.getString(EcoRutaDestination.ARG_ROLE).orEmpty())
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
                    navController.navigate(EcoRutaDestination.passwordChanged(mode, role)) {
                        popUpTo(EcoRutaDestination.CHANGE_PASSWORD) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            ),
        )
    }

    composable(EcoRutaDestination.PASSWORD_CHANGED, arguments = arguments) { entry ->
        val mode = ChangePasswordMode.valueOf(entry.arguments?.getString(EcoRutaDestination.ARG_MODE).orEmpty())
        val role = UserRole.valueOf(entry.arguments?.getString(EcoRutaDestination.ARG_ROLE).orEmpty())
        PasswordChangedScreen(
            mode = mode,
            role = role,
            onContinue = {
                if (mode == ChangePasswordMode.FIRST_LOGIN) {
                    navController.navigate(EcoRutaDestination.homeForRole(role)) {
                        popUpTo(EcoRutaDestination.GUEST_USER) { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            },
        )
    }
}
