package com.ecoruta.app.features.auth.presentation.navigation

// Subgrafo de navegación para autenticación, registro y gestión de contraseñas
// Conecta las vistas de inicio de sesión, recuperación de cuenta y cambio de clave

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordEvent
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordMode
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordScreen
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordUiState
import com.ecoruta.app.features.auth.presentation.first_login.ForceChangePasswordViewModel
import com.ecoruta.app.features.auth.presentation.first_login.PasswordChangedScreen
import com.ecoruta.app.features.auth.presentation.login.LoginActions
import com.ecoruta.app.features.auth.presentation.login.LoginScreen
import com.ecoruta.app.features.auth.presentation.login.LoginUiState
import com.ecoruta.app.features.auth.presentation.password_reset.PasswordRecoveryEvent
import com.ecoruta.app.features.auth.presentation.password_reset.PasswordResetViewModel
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
import com.ecoruta.app.features.auth.presentation.register.RegisterViewModel
import com.ecoruta.app.features.auth.presentation.register.RegistrationEvent
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
    recyclerRegistrationGraph(navController = navController)
    storeRegistrationGraph(navController = navController)
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
        composable(EcoRutaDestination.RECOVER_EMAIL) { entry ->
            val viewModel = recoveryViewModel(navController, entry)
            val state by viewModel.uiState.collectAsState()
            // Pasa a la pantalla del código cuando el servidor confirma el envío del correo
            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        PasswordRecoveryEvent.CodeSent ->
                            navController.navigate(EcoRutaDestination.RETRIEVE_CODE) { launchSingleTop = true }
                    }
                }
            }
            RecoveryEmailScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onSubmit = viewModel::onSubmit,
                onBackClick = backToLogin,
            )
        }
        composable(EcoRutaDestination.RETRIEVE_CODE) { entry ->
            val viewModel = recoveryViewModel(navController, entry)
            val state by viewModel.uiState.collectAsState()
            // Con el código aceptado pasa a la nueva contraseña y saca esta pantalla de la pila para no volver a ella
            LaunchedEffect(state.isCodeVerified) {
                if (state.isCodeVerified) {
                    navController.navigate(EcoRutaDestination.RESET_PASSWORD) {
                        popUpTo(EcoRutaDestination.RETRIEVE_CODE) { inclusive = true }
                    }
                }
            }
            RecoveryCodeScreen(
                state = state,
                onCodeChange = viewModel::onCodeChange,
                onSubmit = viewModel::onSubmitCode,
                onResendClick = viewModel::onResendClick,
            )
        }
        composable(EcoRutaDestination.RESET_PASSWORD) { entry ->
            val viewModel = recoveryViewModel(navController, entry)
            val state by viewModel.uiState.collectAsState()
            // Con la contraseña guardada muestra la confirmación que lleva de vuelta al inicio de sesión
            LaunchedEffect(state.isPasswordUpdated) {
                if (state.isPasswordUpdated) {
                    navController.navigate(EcoRutaDestination.PASSWORD_UPDATED) {
                        popUpTo(EcoRutaDestination.RECOVER_EMAIL) { inclusive = true }
                    }
                }
            }
            ResetPasswordScreen(
                state = state,
                actions = ResetPasswordActions(
                    onNewPasswordChange = viewModel::onNewPasswordChange,
                    onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                    onToggleNewPasswordVisibility = viewModel::onToggleNewPasswordVisibility,
                    onToggleConfirmPasswordVisibility = viewModel::onToggleConfirmPasswordVisibility,
                    onSave = viewModel::onSave,
                    onBackToLogin = backToLogin,
                ),
            )
        }
        composable(EcoRutaDestination.PASSWORD_UPDATED) {
            PasswordUpdatedScreen(onBackToLogin = backToLogin)
        }
    }
}

// Comparte un solo ViewModel entre las pantallas del subgrafo de recuperación para conservar el correo y el código
@Composable
private fun recoveryViewModel(navController: NavHostController, entry: NavBackStackEntry): PasswordResetViewModel {
    val recoveryEntry = remember(entry) { navController.getBackStackEntry(EcoRutaDestination.RECOVERY) }
    return hiltViewModel(recoveryEntry)
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
        composable(EcoRutaDestination.CITIZEN_REGISTRATION_ACCOUNT) { entry ->
            val parentEntry = remember(entry) {
                runCatching { navController.getBackStackEntry(EcoRutaDestination.RECORD) }.getOrNull()
            }
            val viewModel: RegisterViewModel = if (parentEntry != null) hiltViewModel(parentEntry) else hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is RegistrationEvent.Step1Valid -> {
                            navController.navigate(EcoRutaDestination.CITIZEN_REGISTRATION_PROFILE) { launchSingleTop = true }
                        }
                        else -> Unit
                    }
                }
            }

            CitizenAccountScreen(
                state = state,
                actions = CitizenAccountActions(
                    onFullNameChange = viewModel::onFullNameChange,
                    onEmailChange = viewModel::onEmailChange,
                    onGoogleClick = { navController.navigate(EcoRutaDestination.CITIZEN_REGISTRATION_PROFILE) { launchSingleTop = true } },
                    onContinue = viewModel::onStep1Submit,
                    onChangeRegistrationType = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.CITIZEN_REGISTRATION_PROFILE) { entry ->
            val parentEntry = remember(entry) {
                runCatching { navController.getBackStackEntry(EcoRutaDestination.RECORD) }.getOrNull()
            }
            val viewModel: RegisterViewModel = if (parentEntry != null) hiltViewModel(parentEntry) else hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is RegistrationEvent.AccountCreated -> {
                            navController.navigate(EcoRutaDestination.CITIZEN_REGISTRATION_CONFIRMATION) {
                                popUpTo(EcoRutaDestination.RECORD_TYPE_SELECTION)
                                launchSingleTop = true
                            }
                        }
                        else -> Unit
                    }
                }
            }

            CitizenProfileScreen(
                state = state,
                actions = CitizenProfileActions(
                    onDocumentTypeChange = viewModel::onDocumentTypeChange,
                    onDocumentNumberChange = viewModel::onDocumentNumberChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onNeighborhoodChange = viewModel::onNeighborhoodChange,
                    onDataPolicyChange = viewModel::onDataPolicyChange,
                    onViewPolicyClick = {},
                    onCreateAccount = { viewModel.onCreateAccount() },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.CITIZEN_REGISTRATION_CONFIRMATION) { entry ->
            val parentEntry = remember(entry) {
                runCatching { navController.getBackStackEntry(EcoRutaDestination.RECORD) }.getOrNull()
            }
            val viewModel: RegisterViewModel = if (parentEntry != null) hiltViewModel(parentEntry) else hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            CitizenEmailConfirmationScreen(
                email = state.email.ifBlank { "ciudadano@ecoruta.co" },
                isResending = state.isResendingEmail,
                onResendClick = viewModel::onResendEmail,
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
        val viewModel: ForceChangePasswordViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            viewModel.events.collect { event ->
                when (event) {
                    is ChangePasswordEvent.Done -> {
                        navController.navigate(EcoRutaDestination.passwordChanged(mode, role)) {
                            popUpTo(EcoRutaDestination.CHANGE_PASSWORD) { inclusive = true }
                        }
                    }
                    is ChangePasswordEvent.ShowNotice -> Unit
                }
            }
        }

        ChangePasswordScreen(
            mode = mode,
            role = role,
            state = state,
            actions = ChangePasswordActions(
                onCurrentChange = viewModel::onCurrentChange,
                onNewChange = viewModel::onNewChange,
                onConfirmChange = viewModel::onConfirmChange,
                onToggleCurrentVisibility = viewModel::onToggleCurrentVisibility,
                onToggleNewVisibility = viewModel::onToggleNewVisibility,
                onToggleConfirmVisibility = viewModel::onToggleConfirmVisibility,
                onSave = viewModel::onSave,
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
