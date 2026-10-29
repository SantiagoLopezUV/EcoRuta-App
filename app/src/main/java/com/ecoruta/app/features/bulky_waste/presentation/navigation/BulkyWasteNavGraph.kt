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

    composable(EcoRutaDestination.GUEST_SPECIAL_WASTE) {
        DebrisGuestScreen(
            actions = GuestServiceActions(
                onBack = { navController.popBackStack() },
                onLogin = { open(EcoRutaDestination.LOGIN) },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    navigation(
        startDestination = EcoRutaDestination.DEBRIS_REQUEST_FORM,
        route = EcoRutaDestination.REQUEST_DEBRIS_PICKUP,
    ) {
        composable(EcoRutaDestination.DEBRIS_REQUEST_FORM) {
            var state by remember { mutableStateOf(DebrisRequestUiState(type = DebrisType.ESCOMBROS_REMODELACION)) }
            DebrisRequestScreen(
                state = state,
                actions = DebrisRequestActions(
                    onTypeSelected = { state = state.copy(type = it, typeError = null) },
                    onDescriptionChange = { state = state.copy(description = it, descriptionError = null) },
                    onAddressChange = { state = state.copy(address = it, addressError = null) },
                    onContinue = { navController.navigate(EcoRutaDestination.DEBRIS_GUIDE) { launchSingleTop = true } },
                    onBack = { navController.popBackStack(EcoRutaDestination.REQUEST_DEBRIS_PICKUP, inclusive = true) },
                ),
            )
        }
        composable(EcoRutaDestination.DEBRIS_GUIDE) {
            DebrisGuideScreen(
                selectedType = DebrisType.ESCOMBROS_REMODELACION,
                onContinue = { navController.navigate(EcoRutaDestination.DEBRIS_DATE) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
            )
        }
        composable(EcoRutaDestination.DEBRIS_DATE) {
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
                        navController.navigate(EcoRutaDestination.DEBRIS_REQUEST_SENT) {
                            popUpTo(EcoRutaDestination.DEBRIS_REQUEST_FORM)
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.DEBRIS_REQUEST_SENT) {
            DebrisSentScreen(
                request = SampleDebrisRequest,
                sentWithoutBackend = true,
                onViewTracking = { open(EcoRutaDestination.NEW_DEBRIS_TRACKING) },
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.HOME) },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            )
        }
        composable(EcoRutaDestination.NEW_DEBRIS_TRACKING) {
            DebrisTrackingScreen(
                state = DebrisTrackingUiState.Loaded(SampleDebrisRequest, isSample = true),
                isWorking = false,
                showGoHome = true,
                showNotSentHint = true,
                actions = DebrisTrackingActions(
                    onBack = { navController.popBackStack() },
                    onCancelRequest = {},
                    onApproveQuote = {},
                    onGoHome = { navController.navigateToCitizenTab(CitizenTab.HOME) },
                    onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
                ),
            )
        }
    }

    composable(EcoRutaDestination.MY_DEBRIS_REQUESTS) {
        MyDebrisRequestsScreen(
            state = MyDebrisRequestsUiState.Loaded(requests = listOf(SampleDebrisRequest), isSample = true),
            actions = MyDebrisRequestsActions(
                onBack = { navController.popBackStack() },
                onRequestClick = { request -> request.id?.let { open(EcoRutaDestination.debrisTracking(it)) } },
                onNewRequest = { open(EcoRutaDestination.REQUEST_DEBRIS_PICKUP) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(
        route = EcoRutaDestination.DEBRIS_TRACKING,
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
