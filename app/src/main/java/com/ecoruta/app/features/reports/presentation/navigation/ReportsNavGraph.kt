package com.ecoruta.app.features.reports.presentation.navigation

// Subgrafo de navegación para la creación y consulta de reportes ciudadanos
// Conecta el formulario guiado en tres pasos, el historial y la vista sin sesión

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
import com.ecoruta.app.features.reports.presentation.create.NewReportUiState
import com.ecoruta.app.features.reports.presentation.create.Report
import com.ecoruta.app.features.reports.presentation.create.ReportLocationActions
import com.ecoruta.app.features.reports.presentation.create.ReportLocationScreen
import com.ecoruta.app.features.reports.presentation.create.ReportPhotoActions
import com.ecoruta.app.features.reports.presentation.create.ReportPhotoScreen
import com.ecoruta.app.features.reports.presentation.create.ReportSentScreen
import com.ecoruta.app.features.reports.presentation.create.ReportStatus
import com.ecoruta.app.features.reports.presentation.create.ReportType
import com.ecoruta.app.features.reports.presentation.create.ReportTypeActions
import com.ecoruta.app.features.reports.presentation.create.ReportTypeScreen
import com.ecoruta.app.features.reports.presentation.list.MyReportsActions
import com.ecoruta.app.features.reports.presentation.list.MyReportsScreen
import com.ecoruta.app.features.reports.presentation.list.MyReportsUiState
import com.ecoruta.app.features.reports.presentation.list.ReportDetailActions
import com.ecoruta.app.features.reports.presentation.list.ReportDetailScreen
import com.ecoruta.app.features.reports.presentation.list.ReportDetailUiState
import com.ecoruta.app.features.reports.presentation.list.ReportsGuestActions
import com.ecoruta.app.features.reports.presentation.list.ReportsGuestScreen

private val SampleReports = listOf(
    Report(
        id = "REP-104",
        type = ReportType.BOTADERO_CLANDESTINO,
        status = ReportStatus.APROBADO,
        address = "Calle 14 #8-20, La Esperanza",
        area = "La Esperanza",
        createdAtMillis = 1_760_100_000_000L,
    ),
)

// Registra las rutas del formulario de nuevo reporte, historial y vista sin sesión
fun NavGraphBuilder.reportsGraph(navController: NavHostController) {
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }

    composable(EcoRutaDestination.GUEST_REPORTS) {
        ReportsGuestScreen(
            actions = ReportsGuestActions(
                onBack = { navController.popBackStack() },
                onLogin = { open(EcoRutaDestination.LOGIN) },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    navigation(
        startDestination = EcoRutaDestination.REPORT_PHOTO,
        route = EcoRutaDestination.NEW_REPORT,
    ) {
        composable(EcoRutaDestination.REPORT_PHOTO) {
            var state by remember { mutableStateOf(NewReportUiState()) }
            ReportPhotoScreen(
                state = state,
                actions = ReportPhotoActions(
                    onPhotoAdded = { uri -> state = state.copy(photos = state.photos + uri, photoError = null) },
                    onPhotoRemoved = { uri -> state = state.copy(photos = state.photos - uri) },
                    onPhotoLimitReached = {},
                    onCameraUnavailable = {},
                    onDescriptionChange = { state = state.copy(description = it) },
                    onContinue = { navController.navigate(EcoRutaDestination.REPORT_TYPE) { launchSingleTop = true } },
                    onBack = { navController.popBackStack(EcoRutaDestination.NEW_REPORT, inclusive = true) },
                ),
            )
        }
        composable(EcoRutaDestination.REPORT_TYPE) {
            var state by remember { mutableStateOf(NewReportUiState(type = ReportType.CONTENEDOR_LLENO)) }
            ReportTypeScreen(
                state = state,
                actions = ReportTypeActions(
                    onTypeSelected = { state = state.copy(type = it, typeError = null) },
                    onSpecialWasteClick = { open(EcoRutaDestination.REQUEST_DEBRIS_PICKUP) },
                    onContinue = { navController.navigate(EcoRutaDestination.REPORT_LOCATION) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.REPORT_LOCATION) {
            var state by remember { mutableStateOf(NewReportUiState(address = "Calle 14 #8-20")) }
            ReportLocationScreen(
                state = state,
                actions = ReportLocationActions(
                    onUseCurrentLocation = {},
                    onLocationPermissionDenied = {},
                    onEditAddress = { state = state.copy(isEditingAddress = true) },
                    onAddressChange = { state = state.copy(address = it, locationError = null) },
                    onAddressDone = { state = state.copy(isEditingAddress = false) },
                    onSubmit = {
                        navController.navigate(EcoRutaDestination.REPORT_SENT) {
                            popUpTo(EcoRutaDestination.REPORT_PHOTO)
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.REPORT_SENT) {
            ReportSentScreen(
                sentWithoutBackend = true,
                onViewMyReports = {
                    navController.navigate(EcoRutaDestination.MY_REPORTS) {
                        popUpTo(EcoRutaDestination.CITIZEN_HOME)
                        launchSingleTop = true
                    }
                },
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.HOME) },
            )
        }
    }

    composable(EcoRutaDestination.MY_REPORTS) {
        MyReportsScreen(
            state = MyReportsUiState.Loaded(reports = SampleReports, isSample = true),
            actions = MyReportsActions(
                onBack = { navController.popBackStack() },
                onReportClick = { report -> open(EcoRutaDestination.reportDetail(report.id)) },
                onCreateReport = { open(EcoRutaDestination.NEW_REPORT) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(
        route = EcoRutaDestination.REPORT_DETAIL,
        arguments = listOf(navArgument(EcoRutaDestination.ARG_REPORT_ID) { type = NavType.StringType }),
    ) { entry ->
        val reportId = entry.arguments?.getString(EcoRutaDestination.ARG_REPORT_ID).orEmpty()
        val report = SampleReports.find { it.id == reportId } ?: SampleReports.first()
        ReportDetailScreen(
            state = ReportDetailUiState.Loaded(report = report, isSample = true),
            actions = ReportDetailActions(
                onBack = { navController.popBackStack() },
                onCreateReport = { open(EcoRutaDestination.NEW_REPORT) },
                onViewOriginal = { id -> navController.navigate(EcoRutaDestination.reportDetail(id)) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }
}
