package com.ecoruta.app.features.auth.presentation.navigation

// Subgrafo de navegación para el registro del comercio aliado
// Conecta la cuenta del gerente, la confirmación por correo, los datos del comercio y la solicitud en revisión

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.core.ui.components.rememberPdfPicker
import com.ecoruta.app.features.auth.presentation.register.ResendLinkStatus
import com.ecoruta.app.features.auth.presentation.register.StoreDetailsActions
import com.ecoruta.app.features.auth.presentation.register.StoreDetailsScreen
import com.ecoruta.app.features.auth.presentation.register.StoreDetailsEvent
import com.ecoruta.app.features.auth.presentation.register.StoreDetailsViewModel
import com.ecoruta.app.features.auth.presentation.register.StoreEmailConfirmationScreen
import com.ecoruta.app.features.auth.presentation.register.StoreEmailConfirmationUiState
import com.ecoruta.app.features.auth.presentation.register.StoreEmailConfirmedScreen
import com.ecoruta.app.features.auth.presentation.register.StoreOwnerAccountActions
import com.ecoruta.app.features.auth.presentation.register.StoreOwnerAccountScreen
import com.ecoruta.app.features.auth.presentation.register.StoreOwnerAccountEvent
import com.ecoruta.app.features.auth.presentation.register.StoreOwnerAccountViewModel
import com.ecoruta.app.features.auth.presentation.register.StoreRequestStatusScreen
import com.ecoruta.app.features.auth.presentation.register.StoreRequestStatusUiState
import kotlinx.coroutines.delay

// Tiempos de la simulación temporal del reenvío y de la apertura del enlace
private const val SIMULATED_RESEND_MILLIS = 1_500L
private const val SIMULATED_CONFIRMATION_MILLIS = 3_000L

// Registra las rutas del registro de comercio bajo su propio subgrafo
fun NavGraphBuilder.storeRegistrationGraph(navController: NavHostController) {
    // El correo escrito por el gerente viaja como argumento entre las pantallas de confirmación
    val emailArgument = listOf(
        navArgument(EcoRutaDestination.ARG_EMAIL) {
            type = NavType.StringType
            defaultValue = ""
        },
    )

    navigation(
        startDestination = EcoRutaDestination.STORE_REGISTRATION_ACCOUNT,
        route = EcoRutaDestination.STORE_REGISTRATION,
    ) {
        composable(EcoRutaDestination.STORE_REGISTRATION_ACCOUNT) {
            val viewModel: StoreOwnerAccountViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // Con la cuenta validada pasa a la confirmación por correo usando el correo limpio que entrega el ViewModel
            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is StoreOwnerAccountEvent.FormValid ->
                            navController.navigate(EcoRutaDestination.storeRegistrationConfirmation(event.email)) {
                                popUpTo(EcoRutaDestination.RECORD_TYPE_SELECTION)
                                launchSingleTop = true
                            }
                    }
                }
            }
            StoreOwnerAccountScreen(
                state = state,
                actions = StoreOwnerAccountActions(
                    onFullNameChange = viewModel::onFullNameChange,
                    onEmailChange = viewModel::onEmailChange,
                    onContinue = viewModel::onContinue,
                    onBack = { navController.popBackStack() },
                    onRetry = viewModel::onRetry,
                ),
            )
        }

        composable(EcoRutaDestination.STORE_REGISTRATION_CONFIRMATION, arguments = emailArgument) { entry ->
            val email = entry.arguments?.getString(EcoRutaDestination.ARG_EMAIL).orEmpty()
            var resendStatus by rememberSaveable { mutableStateOf(ResendLinkStatus.IDLE) }
            // Sin ViewModel para el reenvío, se simula el envío del enlace y luego que el gerente lo abrió
            LaunchedEffect(resendStatus) {
                when (resendStatus) {
                    ResendLinkStatus.SENDING -> {
                        delay(SIMULATED_RESEND_MILLIS)
                        resendStatus = ResendLinkStatus.SENT
                    }
                    ResendLinkStatus.SENT -> {
                        delay(SIMULATED_CONFIRMATION_MILLIS)
                        navController.navigate(EcoRutaDestination.storeRegistrationEmailValidated(email)) {
                            popUpTo(EcoRutaDestination.STORE_REGISTRATION_CONFIRMATION) { inclusive = true }
                        }
                    }
                    ResendLinkStatus.IDLE -> Unit
                }
            }
            StoreEmailConfirmationScreen(
                state = StoreEmailConfirmationUiState(email = email, resendStatus = resendStatus),
                onResendClick = { resendStatus = ResendLinkStatus.SENDING },
                onExit = { navController.popBackStack(EcoRutaDestination.RECORD, inclusive = true) },
            )
        }

        composable(EcoRutaDestination.STORE_REGISTRATION_EMAIL_VALIDATED, arguments = emailArgument) { entry ->
            StoreEmailConfirmedScreen(
                email = entry.arguments?.getString(EcoRutaDestination.ARG_EMAIL).orEmpty(),
                onContinue = {
                    navController.navigate(EcoRutaDestination.STORE_REGISTRATION_DETAILS) {
                        popUpTo(EcoRutaDestination.STORE_REGISTRATION_EMAIL_VALIDATED) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(EcoRutaDestination.STORE_REGISTRATION_DETAILS) {
            val viewModel: StoreDetailsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // El selector del sistema se registra aquí para que la pantalla solo reciba el nombre del RUT
            val pickRut = rememberPdfPicker { document -> viewModel.onRutPicked(document.name) }
            // Con los datos validados la solicitud pasa a revisión y el registro sale de la pila
            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        StoreDetailsEvent.FormValid ->
                            navController.navigate(EcoRutaDestination.STORE_REGISTRATION_IN_REVIEW) {
                                popUpTo(EcoRutaDestination.RECORD) { inclusive = true }
                            }
                    }
                }
            }
            StoreDetailsScreen(
                state = state,
                actions = StoreDetailsActions(
                    onNameChange = viewModel::onNameChange,
                    onIdTypeChange = viewModel::onIdTypeChange,
                    onIdNumberChange = viewModel::onIdNumberChange,
                    onCategoryChange = viewModel::onCategoryChange,
                    onAddressChange = viewModel::onAddressChange,
                    onPhoneChange = viewModel::onPhoneChange,
                    onPickPdf = pickRut,
                    onRemovePdf = viewModel::onRemovePdf,
                    onDataPolicyChange = viewModel::onDataPolicyChange,
                    onViewPolicyClick = {},
                    onSubmit = viewModel::onSubmit,
                    onBack = { navController.popBackStack() },
                    onRetry = viewModel::onRetry,
                ),
            )
        }

        composable(EcoRutaDestination.STORE_REGISTRATION_IN_REVIEW) {
            StoreRequestStatusScreen(
                state = StoreRequestStatusUiState.InReview(),
                onLoginClick = { navController.navigate(EcoRutaDestination.LOGIN) { launchSingleTop = true } },
                onGoHome = { navController.goToPublicHomeAfterSignOut() },
                onRetry = {},
            )
        }

        // Con la solicitud aprobada el gerente entra con la contraseña provisional y el registro sale de la pila
        composable(EcoRutaDestination.STORE_REGISTRATION_APPROVED) {
            StoreRequestStatusScreen(
                state = StoreRequestStatusUiState.Approved(),
                onLoginClick = {
                    navController.navigate(EcoRutaDestination.LOGIN) {
                        popUpTo(EcoRutaDestination.STORE_REGISTRATION) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoHome = { navController.goToPublicHomeAfterSignOut() },
                onRetry = {},
            )
        }
    }
}
