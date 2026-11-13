package com.ecoruta.app.features.bulky_waste.presentation.navigation

// Subgrafo de navegación para solicitudes de retiro de escombros y voluminosos (RCD)
// Conecta el flujo guiado de agendamiento en 3 pasos, el seguimiento y la vista sin sesión

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisScheduleActions
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisScheduleScreen
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisSentScreen
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType
import com.ecoruta.app.features.bulky_waste.presentation.schedule.ScheduleRcdEvent
import com.ecoruta.app.features.bulky_waste.presentation.schedule.ScheduleRcdViewModel
import com.ecoruta.app.features.bulky_waste.presentation.tracking.DebrisGuestScreen
import com.ecoruta.app.features.bulky_waste.presentation.tracking.DebrisTrackingActions
import com.ecoruta.app.features.bulky_waste.presentation.tracking.DebrisTrackingScreen
import com.ecoruta.app.features.bulky_waste.presentation.tracking.DebrisTrackingUiState
import com.ecoruta.app.features.bulky_waste.presentation.tracking.MyDebrisRequestsActions
import com.ecoruta.app.features.bulky_waste.presentation.tracking.MyDebrisRequestsScreen
import com.ecoruta.app.features.bulky_waste.presentation.tracking.RcdTrackingViewModel

private val SampleDayMillis = DayMillis.startOfDay(1_760_500_000_000L)

private val FallbackDebrisRequest = DebrisRequest(
    id = "RCD-201",
    type = DebrisType.RENOVATION_DEBRIS,
    description = "4 lonas de escombros de remodelación",
    address = "Calle 14 #8-20, Tuluá",
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
        composable(EcoRutaDestination.DEBRIS_REQUEST_FORM) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(EcoRutaDestination.REQUEST_DEBRIS_PICKUP)
            }
            val viewModel: ScheduleRcdViewModel = hiltViewModel(parentEntry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            DebrisRequestScreen(
                state = state,
                actions = DebrisRequestActions(
                    onTypeSelected = viewModel::onTypeSelected,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onAddressChange = viewModel::onAddressChange,
                    onContinue = {
                        if (viewModel.validateStep1()) {
                            navController.navigate(EcoRutaDestination.DEBRIS_GUIDE) { launchSingleTop = true }
                        }
                    },
                    onBack = { navController.popBackStack(EcoRutaDestination.REQUEST_DEBRIS_PICKUP, inclusive = true) },
                ),
            )
        }

        composable(EcoRutaDestination.DEBRIS_GUIDE) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(EcoRutaDestination.REQUEST_DEBRIS_PICKUP)
            }
            val viewModel: ScheduleRcdViewModel = hiltViewModel(parentEntry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            DebrisGuideScreen(
                selectedType = state.type ?: DebrisType.RENOVATION_DEBRIS,
                onContinue = { navController.navigate(EcoRutaDestination.DEBRIS_DATE) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
            )
        }

        composable(EcoRutaDestination.DEBRIS_DATE) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(EcoRutaDestination.REQUEST_DEBRIS_PICKUP)
            }
            val viewModel: ScheduleRcdViewModel = hiltViewModel(parentEntry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ScheduleRcdEvent.SubmitSuccess -> {
                            navController.navigate(EcoRutaDestination.DEBRIS_REQUEST_SENT) {
                                popUpTo(EcoRutaDestination.DEBRIS_REQUEST_FORM)
                                launchSingleTop = true
                            }
                        }
                        else -> Unit
                    }
                }
            }

            DebrisScheduleScreen(
                state = state,
                actions = DebrisScheduleActions(
                    onPreviousWeek = viewModel::onPreviousWeek,
                    onNextWeek = viewModel::onNextWeek,
                    onDaySelected = viewModel::onDaySelected,
                    onRetryDays = viewModel::onRetryDays,
                    onSubmit = { viewModel.onSubmit() },
                    onBack = { navController.popBackStack() },
                ),
            )
        }

        composable(EcoRutaDestination.DEBRIS_REQUEST_SENT) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(EcoRutaDestination.REQUEST_DEBRIS_PICKUP)
            }
            val viewModel: ScheduleRcdViewModel = hiltViewModel(parentEntry)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val request = state.submitted ?: FallbackDebrisRequest

            DebrisSentScreen(
                request = request,
                sentWithoutBackend = state.submitted == null,
                onViewTracking = {
                    val reqId = request.id
                    if (reqId != null) {
                        navController.navigate(EcoRutaDestination.debrisTracking(reqId)) {
                            popUpTo(EcoRutaDestination.REQUEST_DEBRIS_PICKUP) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        open(EcoRutaDestination.NEW_DEBRIS_TRACKING)
                    }
                },
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.HOME) },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            )
        }

        composable(EcoRutaDestination.NEW_DEBRIS_TRACKING) {
            DebrisTrackingScreen(
                state = DebrisTrackingUiState.Loaded(FallbackDebrisRequest, isSample = true),
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
        val viewModel: RcdTrackingViewModel = hiltViewModel()
        val state by viewModel.myRequestsState.collectAsStateWithLifecycle()

        MyDebrisRequestsScreen(
            state = state,
            actions = MyDebrisRequestsActions(
                onBack = { navController.popBackStack() },
                onRequestClick = { request -> request.id?.let { open(EcoRutaDestination.debrisTracking(it)) } },
                onNewRequest = { open(EcoRutaDestination.REQUEST_DEBRIS_PICKUP) },
                onRetry = { viewModel.loadMyRequests() },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(
        route = EcoRutaDestination.DEBRIS_TRACKING,
        arguments = listOf(navArgument(EcoRutaDestination.ARG_REPORT_ID) { type = NavType.StringType }),
    ) { backStackEntry ->
        val reportId = backStackEntry.arguments?.getString(EcoRutaDestination.ARG_REPORT_ID) ?: ""
        val viewModel: RcdTrackingViewModel = hiltViewModel()
        val state by viewModel.trackingState.collectAsStateWithLifecycle()
        val isWorking by viewModel.isWorking.collectAsStateWithLifecycle()

        LaunchedEffect(reportId) {
            if (reportId.isNotBlank()) {
                viewModel.loadRequest(reportId)
            }
        }

        DebrisTrackingScreen(
            state = state,
            isWorking = isWorking,
            showGoHome = false,
            showNotSentHint = false,
            actions = DebrisTrackingActions(
                onBack = { navController.popBackStack() },
                onCancelRequest = { viewModel.cancelRequest(reportId) },
                onApproveQuote = { viewModel.approveQuote(reportId) },
                onRetry = { viewModel.loadRequest(reportId) },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }
}
