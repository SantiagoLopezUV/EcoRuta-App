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

    composable(EcoRutaDestination.REPORTES_SIN_SESION) {
        ReportsGuestScreen(
            actions = ReportsGuestActions(
                onBack = { navController.popBackStack() },
                onLogin = { open(EcoRutaDestination.INICIAR_SESION) },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    navigation(
        startDestination = EcoRutaDestination.REPORTE_FOTO,
        route = EcoRutaDestination.NUEVO_REPORTE,
    ) {
        composable(EcoRutaDestination.REPORTE_FOTO) {
            var state by remember { mutableStateOf(NewReportUiState()) }
            ReportPhotoScreen(
                state = state,
                actions = ReportPhotoActions(
                    onPhotoAdded = { uri -> state = state.copy(photos = state.photos + uri, photoError = null) },
                    onPhotoRemoved = { uri -> state = state.copy(photos = state.photos - uri) },
                    onPhotoLimitReached = {},
                    onCameraUnavailable = {},
                    onDescriptionChange = { state = state.copy(description = it) },
                    onContinue = { navController.navigate(EcoRutaDestination.REPORTE_TIPO) { launchSingleTop = true } },
                    onBack = { navController.popBackStack(EcoRutaDestination.NUEVO_REPORTE, inclusive = true) },
                ),
            )
        }
        composable(EcoRutaDestination.REPORTE_TIPO) {
            var state by remember { mutableStateOf(NewReportUiState(type = ReportType.CONTENEDOR_LLENO)) }
            ReportTypeScreen(
                state = state,
                actions = ReportTypeActions(
                    onTypeSelected = { state = state.copy(type = it, typeError = null) },
                    onSpecialWasteClick = { open(EcoRutaDestination.SOLICITAR_RETIRO) },
                    onContinue = { navController.navigate(EcoRutaDestination.REPORTE_UBICACION) { launchSingleTop = true } },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.REPORTE_UBICACION) {
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
                        navController.navigate(EcoRutaDestination.REPORTE_ENVIADO) {
                            popUpTo(EcoRutaDestination.REPORTE_FOTO)
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                ),
            )
        }
        composable(EcoRutaDestination.REPORTE_ENVIADO) {
            ReportSentScreen(
                sentWithoutBackend = true,
                onViewMyReports = {
                    navController.navigate(EcoRutaDestination.MIS_REPORTES) {
                        popUpTo(EcoRutaDestination.INICIO_CIUDADANO)
                        launchSingleTop = true
                    }
                },
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.INICIO) },
            )
        }
    }

    composable(EcoRutaDestination.MIS_REPORTES) {
        MyReportsScreen(
            state = MyReportsUiState.Loaded(reports = SampleReports, isSample = true),
            actions = MyReportsActions(
                onBack = { navController.popBackStack() },
                onReportClick = { report -> open(EcoRutaDestination.detalleReporte(report.id)) },
                onCreateReport = { open(EcoRutaDestination.NUEVO_REPORTE) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(
        route = EcoRutaDestination.DETALLE_REPORTE,
        arguments = listOf(navArgument(EcoRutaDestination.ARG_REPORT_ID) { type = NavType.StringType }),
    ) { entry ->
        val reportId = entry.arguments?.getString(EcoRutaDestination.ARG_REPORT_ID).orEmpty()
        val report = SampleReports.find { it.id == reportId } ?: SampleReports.first()
        ReportDetailScreen(
            state = ReportDetailUiState.Loaded(report = report, isSample = true),
            actions = ReportDetailActions(
                onBack = { navController.popBackStack() },
                onCreateReport = { open(EcoRutaDestination.NUEVO_REPORTE) },
                onViewOriginal = { id -> navController.navigate(EcoRutaDestination.detalleReporte(id)) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }
}
