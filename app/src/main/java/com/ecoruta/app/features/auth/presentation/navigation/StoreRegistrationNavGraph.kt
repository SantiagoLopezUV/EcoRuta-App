package com.ecoruta.app.features.auth.presentation.navigation

// Subgrafo de navegación para el registro del comercio aliado
// Conecta la cuenta del gerente, la confirmación por correo, los datos del comercio y la solicitud en revisión

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.features.auth.presentation.register.StoreDetailsActions
import com.ecoruta.app.features.auth.presentation.register.StoreDetailsScreen
import com.ecoruta.app.features.auth.presentation.register.StoreDetailsUiState
import com.ecoruta.app.features.auth.presentation.register.StoreEmailConfirmationScreen
import com.ecoruta.app.features.auth.presentation.register.StoreEmailConfirmedScreen
import com.ecoruta.app.features.auth.presentation.register.StoreOwnerAccountActions
import com.ecoruta.app.features.auth.presentation.register.StoreOwnerAccountScreen
import com.ecoruta.app.features.auth.presentation.register.StoreOwnerAccountUiState
import com.ecoruta.app.features.auth.presentation.register.StoreRequestStatusScreen
import com.ecoruta.app.features.auth.presentation.register.StoreRequestStatusUiState

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
            var state by remember { mutableStateOf(StoreOwnerAccountUiState()) }
            StoreOwnerAccountScreen(
                state = state,
                actions = StoreOwnerAccountActions(
                    onFullNameChange = { state = state.copy(fullName = it, fullNameErrorRes = null) },
                    onEmailChange = { state = state.copy(email = it, emailErrorRes = null) },
                    onContinue = {
                        navController.navigate(EcoRutaDestination.storeRegistrationConfirmation(state.email.trim())) {
                            popUpTo(EcoRutaDestination.RECORD_TYPE_SELECTION)
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onRetry = { state = state.copy(isOffline = false) },
                ),
            )
        }

        composable(EcoRutaDestination.STORE_REGISTRATION_CONFIRMATION, arguments = emailArgument) { entry ->
            val email = entry.arguments?.getString(EcoRutaDestination.ARG_EMAIL).orEmpty()
            StoreEmailConfirmationScreen(
                email = email,
                // Temporal hasta que exista el ViewModel, reenviar el enlace simula que el gerente validó su correo
                onResendClick = {
                    navController.navigate(EcoRutaDestination.storeRegistrationEmailValidated(email)) {
                        popUpTo(EcoRutaDestination.STORE_REGISTRATION_CONFIRMATION) { inclusive = true }
                    }
                },
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
            var state by remember { mutableStateOf(StoreDetailsUiState()) }
            StoreDetailsScreen(
                state = state,
                actions = StoreDetailsActions(
                    onNameChange = { state = state.copy(name = it, nameErrorRes = null) },
                    onIdTypeChange = { state = state.copy(idType = it, idErrorRes = null) },
                    onIdNumberChange = { state = state.copy(idNumber = it, idErrorRes = null) },
                    onCategoryChange = { state = state.copy(category = it, categoryErrorRes = null) },
                    onAddressChange = { state = state.copy(address = it, addressErrorRes = null) },
                    onPhoneChange = { state = state.copy(phone = it, phoneErrorRes = null) },
                    onDataPolicyChange = { state = state.copy(acceptedDataPolicy = it) },
                    onViewPolicyClick = {},
                    onSubmit = {
                        navController.navigate(EcoRutaDestination.STORE_REGISTRATION_IN_REVIEW) {
                            popUpTo(EcoRutaDestination.RECORD) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onRetry = { state = state.copy(isOffline = false) },
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
    }
}
