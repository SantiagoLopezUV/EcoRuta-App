package com.ecoruta.app.features.auth.presentation.navigation

// Subgrafo de navegación para el registro del reciclador independiente y vinculado a una empresa
// Conecta el formulario, la confirmación por correo, la búsqueda de empresa y los estados finales

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.features.auth.presentation.register.RecyclerEmailConfirmationScreen
import com.ecoruta.app.features.auth.presentation.register.RecyclerEmailConfirmedScreen
import com.ecoruta.app.features.auth.presentation.register.RecyclerPendingActivationScreen
import com.ecoruta.app.features.auth.presentation.register.RecyclerRegistrationActions
import com.ecoruta.app.features.auth.presentation.register.RecyclerRegistrationFormScreen
import com.ecoruta.app.features.auth.presentation.register.RecyclerRegistrationUiState
import com.ecoruta.app.features.auth.presentation.register.RegistrationType
import com.ecoruta.app.features.profile.presentation.Company
import com.ecoruta.app.features.profile.presentation.RecyclerApplicationUiState
import com.ecoruta.app.features.profile.presentation.components.CompanySearchScreen
import com.ecoruta.app.features.profile.presentation.components.SelectedCompanyScreen

// Correo de ejemplo mostrado mientras el registro no está conectado al backend
private const val SampleRecyclerEmail = "ana.torres@example.com"

// Empresas gestoras de ejemplo para la búsqueda del reciclador vinculado
private val SampleRecyclerCompanies = listOf(
    Company("EMP-01", "Asorecicladores del Oriente", "Asociación"),
    Company("EMP-02", "Cooperativa EcoVida", "Cooperativa"),
)

// Registra las rutas del registro de reciclador en ambas modalidades
fun NavGraphBuilder.recyclerRegistrationGraph(navController: NavHostController) {
    // Empresa elegida en la búsqueda, compartida con la pantalla de empresa seleccionada y la activación
    var selectedCompany by mutableStateOf<Company?>(null)

    composable(EcoRutaDestination.INDEPENDENT_RECYCLER_REGISTRATION) {
        var state by remember { mutableStateOf(RecyclerRegistrationUiState()) }
        val goToConfirmation = {
            navController.navigate(EcoRutaDestination.INDEPENDENT_RECYCLER_CONFIRMATION) {
                popUpTo(EcoRutaDestination.RECORD_TYPE_SELECTION)
                launchSingleTop = true
            }
        }
        RecyclerRegistrationFormScreen(
            type = RegistrationType.INDEPENDENT_RECYCLER,
            state = state,
            actions = recyclerFormActions(
                state = { state },
                update = { state = it },
                onValid = goToConfirmation,
                onGoogleClick = goToConfirmation,
                onBack = { navController.popBackStack() },
            ),
        )
    }

    composable(EcoRutaDestination.INDEPENDENT_RECYCLER_CONFIRMATION) {
        RecyclerEmailConfirmationScreen(
            type = RegistrationType.INDEPENDENT_RECYCLER,
            email = SampleRecyclerEmail,
            // Sin backend, reenviar el enlace simula que el reciclador abrió el correo de confirmación
            onResendClick = {
                navController.navigate(EcoRutaDestination.INDEPENDENT_RECYCLER_CONFIRMED) {
                    popUpTo(EcoRutaDestination.INDEPENDENT_RECYCLER_CONFIRMATION) { inclusive = true }
                }
            },
            onExit = { navController.popBackStack(EcoRutaDestination.RECORD, inclusive = true) },
        )
    }

    composable(EcoRutaDestination.INDEPENDENT_RECYCLER_CONFIRMED) {
        RecyclerEmailConfirmedScreen(
            type = RegistrationType.INDEPENDENT_RECYCLER,
            email = SampleRecyclerEmail,
            onContinue = {
                navController.navigate(EcoRutaDestination.DELIVERY_MAP) {
                    popUpTo(EcoRutaDestination.GUEST_USER) { inclusive = true }
                }
            },
        )
    }

    composable(EcoRutaDestination.COMPANY_RECYCLER_REGISTRATION) {
        var state by remember { mutableStateOf(RecyclerRegistrationUiState()) }
        RecyclerRegistrationFormScreen(
            type = RegistrationType.COMPANY_RECYCLER,
            state = state,
            actions = recyclerFormActions(
                state = { state },
                update = { state = it },
                onValid = {
                    navController.navigate(EcoRutaDestination.COMPANY_RECYCLER_CONFIRMATION) {
                        popUpTo(EcoRutaDestination.RECORD_TYPE_SELECTION)
                        launchSingleTop = true
                    }
                },
                onGoogleClick = {},
                onBack = { navController.popBackStack() },
            ),
        )
    }

    composable(EcoRutaDestination.COMPANY_RECYCLER_CONFIRMATION) {
        RecyclerEmailConfirmationScreen(
            type = RegistrationType.COMPANY_RECYCLER,
            email = SampleRecyclerEmail,
            // Sin backend, reenviar el enlace simula que el reciclador validó su correo
            onResendClick = {
                navController.navigate(EcoRutaDestination.COMPANY_RECYCLER_EMAIL_VALIDATED) {
                    popUpTo(EcoRutaDestination.COMPANY_RECYCLER_CONFIRMATION) { inclusive = true }
                }
            },
            onExit = { navController.popBackStack(EcoRutaDestination.RECORD, inclusive = true) },
        )
    }

    composable(EcoRutaDestination.COMPANY_RECYCLER_EMAIL_VALIDATED) {
        RecyclerEmailConfirmedScreen(
            type = RegistrationType.COMPANY_RECYCLER,
            email = SampleRecyclerEmail,
            onContinue = {
                navController.navigate(EcoRutaDestination.COMPANY_RECYCLER_COMPANY_SEARCH) {
                    popUpTo(EcoRutaDestination.COMPANY_RECYCLER_EMAIL_VALIDATED)
                    launchSingleTop = true
                }
            },
        )
    }

    composable(EcoRutaDestination.COMPANY_RECYCLER_COMPANY_SEARCH) {
        var state by remember { mutableStateOf(RecyclerApplicationUiState()) }
        CompanySearchScreen(
            state = state,
            onQueryChange = { query ->
                val results = if (query.isBlank()) {
                    emptyList()
                } else {
                    SampleRecyclerCompanies.filter { it.name.contains(query.trim(), ignoreCase = true) }
                }
                state = state.copy(query = query, results = results)
            },
            onCompanySelected = { company ->
                selectedCompany = company
                navController.navigate(EcoRutaDestination.COMPANY_RECYCLER_SELECTED_COMPANY) { launchSingleTop = true }
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable(EcoRutaDestination.COMPANY_RECYCLER_SELECTED_COMPANY) {
        SelectedCompanyScreen(
            state = RecyclerApplicationUiState(company = selectedCompany),
            onChangeCompany = { navController.popBackStack() },
            onComplete = {
                navController.navigate(EcoRutaDestination.COMPANY_RECYCLER_PENDING_ACTIVATION) {
                    popUpTo(EcoRutaDestination.RECORD) { inclusive = true }
                }
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable(EcoRutaDestination.COMPANY_RECYCLER_PENDING_ACTIVATION) {
        RecyclerPendingActivationScreen(
            company = selectedCompany,
            onGoHome = { navController.goToPublicHomeAfterSignOut() },
        )
    }
}

// Construye las acciones del formulario; las validaciones de los campos quedan a cargo de la capa de lógica
private fun recyclerFormActions(
    state: () -> RecyclerRegistrationUiState,
    update: (RecyclerRegistrationUiState) -> Unit,
    onValid: () -> Unit,
    onGoogleClick: () -> Unit,
    onBack: () -> Unit,
) = RecyclerRegistrationActions(
    onFullNameChange = { update(state().copy(fullName = it, fullNameErrorRes = null)) },
    onEmailChange = { update(state().copy(email = it, emailErrorRes = null)) },
    onDocumentTypeChange = { update(state().copy(documentType = it, documentErrorRes = null)) },
    onDocumentNumberChange = { update(state().copy(documentNumber = it, documentErrorRes = null)) },
    onIdDocumentPicked = { update(state().copy(idDocument = it, idDocumentErrorRes = null)) },
    onIdDocumentRemoved = { update(state().copy(idDocument = null)) },
    onPhoneChange = { update(state().copy(phone = it, phoneErrorRes = null)) },
    onGoogleClick = onGoogleClick,
    onSubmit = onValid,
    onBack = onBack,
)
