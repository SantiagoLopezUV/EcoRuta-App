package com.ecoruta.app.features.bulky_waste.presentation.navigation

// Subgrafo de navegación para solicitudes de retiro de escombros y voluminosos
// Conecta el agendamiento en tres pasos, el seguimiento y la vista sin sesión

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
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.navigateToCitizenTab
import com.ecoruta.app.core.navigation.navigateToPublicTab
import com.ecoruta.app.core.ui.components.GuestServiceActions
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DayMillis
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisGuideScreen
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisRequest
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisRequestActions
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisRequestScreen
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisRequestUiState
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisScheduleActions
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisScheduleScreen
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisSentScreen
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType
import com.ecoruta.app.features.bulky_waste.presentation.tracking.DebrisGuestScreen
import com.ecoruta.app.features.bulky_waste.presentation.tracking.DebrisTrackingActions
import com.ecoruta.app.features.bulky_waste.presentation.tracking.DebrisTrackingScreen
import com.ecoruta.app.features.bulky_waste.presentation.tracking.DebrisTrackingUiState
import com.ecoruta.app.features.bulky_waste.presentation.tracking.MyDebrisRequestsActions
import com.ecoruta.app.features.bulky_waste.presentation.tracking.MyDebrisRequestsScreen
import com.ecoruta.app.features.bulky_waste.presentation.tracking.MyDebrisRequestsUiState

private val SampleDayMillis = DayMillis.startOfDay(1_760_500_000_000L)

private val SampleDebrisRequest = DebrisRequest(
    id = "RCD-201",
    type = DebrisType.ESCOMBROS_REMODELACION,
    description = "4 lonas de escombros de remodelación",
    address = "Calle 14 #8-20",
    status = DebrisStatus.PENDIENTE,
    createdAtMillis = 1_760_100_000_000L,
    preferredDateMillis = SampleDayMillis,
    scheduledDateMillis = SampleDayMillis,
)

// Registra las rutas de agendamiento y seguimiento de retiro de residuos especiales
fun NavGraphBuilder.debrisGraph(navController: NavHostController) {
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }

    composable(EcoRutaDestination.RESIDUOS_ESPECIALES_SIN_SESION) {
        DebrisGuestScreen(
            actions = GuestServiceActions(
                onBack = { navController.popBackStack() },
                onLogin = { open(EcoRutaDestination.INICIAR_SESION) },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    navigation(
        startDestination = EcoRutaDestination.RETIRO_SOLICITUD,
        route = EcoRutaDestination.SOLICITAR_RETIRO,
    ) {
        composable(EcoRutaDestination.RETIRO_SOLICITUD) {
            var state by remember { mutableStateOf(DebrisRequestUiState(type = DebrisType.ESCOMBROS_REMODELACION)) }
            DebrisRequestScreen(
                state = state,
                actions = DebrisRequestActions(
                    onTypeSelected = { state = state.copy(type = it, typeError = null) },
                    onDescriptionChange = { state = state.copy(description = it, descriptionError = null) },
                    onAddressChange = { state = state.copy(address = it, addressError = null) },
                    onContinue = { navController.navigate(EcoRutaDestination.RETIRO_GUIA) { launchSingleTop = true } },
                    onBack = { navController.popBackStack(EcoRutaDestination.SOLICITAR_RETIRO, inclusive = true) },
                ),
            )
        }
        composable(EcoRutaDestination.RETIRO_GUIA) {
            DebrisGuideScreen(
                selectedType = DebrisType.ESCOMBROS_REMODELACION,
                onContinue = { navController.navigate(EcoRutaDestination.RETIRO_FECHA) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
            )
        }
        composable(EcoRutaDestination.RETIRO_FECHA) {
            val weekStart = DayMillis.mondayOf(SampleDayMillis)
            var state by remember {
                mutableStateOf(
                    DebrisRequestUiState(
                        firstSelectableDay = weekStart,
                        lastSelectableDay = DayMillis.addDays(weekStart, 21),
                        weekStart = weekStart,
                        isLoadingDays = false,
                        availableDays = setOf(SampleDayMillis),
                        selectedDay = SampleDayMillis,
                    ),
                )
            }
            DebrisScheduleScreen(
                state = state,
                actions = DebrisScheduleActions(
                    onPreviousWeek = {},
                    onNextWeek = {},
                    onDaySelected = { state = state.copy(selectedDay = it, dateError = null) },
                    onRetryDays = {},
                    onSubmit = {
                        navController.navigate(EcoRutaDestination.RETIRO_ENVIADA) {
                            popUpTo(EcoRutaDestination.RETIRO_SOLICITUD)
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.RETIRO_ENVIADA) {
            DebrisSentScreen(
                request = SampleDebrisRequest,
                sentWithoutBackend = true,
                onViewTracking = { open(EcoRutaDestination.RETIRO_SEGUIMIENTO_NUEVA) },
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.INICIO) },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            )
        }
        composable(EcoRutaDestination.RETIRO_SEGUIMIENTO_NUEVA) {
            DebrisTrackingScreen(
                state = DebrisTrackingUiState.Loaded(SampleDebrisRequest, isSample = true),
                isWorking = false,
                showGoHome = true,
                showNotSentHint = true,
                actions = DebrisTrackingActions(
                    onBack = { navController.popBackStack() },
                    onCancelRequest = {},
                    onApproveQuote = {},
                    onGoHome = { navController.navigateToCitizenTab(CitizenTab.INICIO) },
                    onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
                ),
            )
        }
    }

    composable(EcoRutaDestination.MIS_SOLICITUDES_ESCOMBROS) {
        MyDebrisRequestsScreen(
            state = MyDebrisRequestsUiState.Loaded(requests = listOf(SampleDebrisRequest), isSample = true),
            actions = MyDebrisRequestsActions(
                onBack = { navController.popBackStack() },
                onRequestClick = { request -> request.id?.let { open(EcoRutaDestination.seguimientoRetiro(it)) } },
                onNewRequest = { open(EcoRutaDestination.SOLICITAR_RETIRO) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(
        route = EcoRutaDestination.SEGUIMIENTO_RETIRO,
        arguments = listOf(navArgument(EcoRutaDestination.ARG_REPORT_ID) { type = NavType.StringType }),
    ) {
        DebrisTrackingScreen(
            state = DebrisTrackingUiState.Loaded(SampleDebrisRequest, isSample = true),
            isWorking = false,
            showGoHome = false,
            showNotSentHint = false,
            actions = DebrisTrackingActions(
                onBack = { navController.popBackStack() },
                onCancelRequest = {},
                onApproveQuote = {},
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }
}
