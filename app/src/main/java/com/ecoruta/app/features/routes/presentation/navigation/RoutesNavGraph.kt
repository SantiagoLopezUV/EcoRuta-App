package com.ecoruta.app.features.routes.presentation.navigation

// Subgrafo de navegación para inicio ciudadano, rutas por barrio, radar y notificaciones
// Gestiona el acceso público y autenticado a los horarios de recolección y alertas

import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.core.navigation.navigateToCitizenTab
import com.ecoruta.app.core.navigation.navigateToPublicTab
import com.ecoruta.app.features.routes.presentation.map.AppNotification
import com.ecoruta.app.features.routes.presentation.map.CollectionRoute
import com.ecoruta.app.features.routes.presentation.map.GuestNotificationsScreen
import com.ecoruta.app.features.routes.presentation.map.InicioPublicoActions
import com.ecoruta.app.features.routes.presentation.map.InicioPublicoScreen
import com.ecoruta.app.features.routes.presentation.map.Neighborhood
import com.ecoruta.app.features.routes.presentation.map.NeighborhoodSelectionActions
import com.ecoruta.app.features.routes.presentation.map.NeighborhoodSelectionScreen
import com.ecoruta.app.features.routes.presentation.map.NeighborhoodSelectionUiState
import com.ecoruta.app.features.routes.presentation.map.NotificationKind
import com.ecoruta.app.features.routes.presentation.map.NotificationsActions
import com.ecoruta.app.features.routes.presentation.map.NotificationsScreen
import com.ecoruta.app.features.routes.presentation.map.NotificationsUiState
import com.ecoruta.app.features.routes.presentation.map.PublicService
import com.ecoruta.app.features.routes.presentation.map.RouteActions
import com.ecoruta.app.features.routes.presentation.map.RouteLiveStatus
import com.ecoruta.app.features.routes.presentation.map.RouteScreen
import com.ecoruta.app.features.routes.presentation.map.RouteUiState
import com.ecoruta.app.features.routes.presentation.radar.ProximityAlertScreen
import com.ecoruta.app.features.routes.presentation.radar.RadarActions
import com.ecoruta.app.features.routes.presentation.radar.RadarScreen
import com.ecoruta.app.features.routes.presentation.radar.RadarUiState
import java.util.Calendar

private val SampleNeighborhoods = listOf(
    Neighborhood("ejemplo-la-esperanza", "La Esperanza"),
    Neighborhood("ejemplo-el-principe", "El Príncipe"),
    Neighborhood("ejemplo-san-nicolas", "San Nicolás"),
)

private val SampleNotifications = listOf(
    AppNotification(
        id = "NOT-01",
        kind = NotificationKind.PROXIMIDAD,
        label = "Alerta de proximidad",
        title = "El camión recolector se acerca a La Esperanza",
        body = "Prepara tus residuos ordinarios para la recolección.",
        createdAtMillis = 1_760_100_000_000L,
        read = false,
        referenceId = "ejemplo-la-esperanza",
    ),
)

// Registra las rutas de inicio público, inicio ciudadano, rutas por barrio, radar y notificaciones
fun NavGraphBuilder.routesGraph(
    navController: NavHostController,
    showNotice: (String) -> Unit,
    publicHomeEnterTransition: EnterTransition,
) {
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }

    composable(
        route = EcoRutaDestination.INICIO_PUBLICO,
        enterTransition = { publicHomeEnterTransition },
    ) {
        val helpPendingMessage = stringResource(R.string.notice_help_pending)
        InicioPublicoScreen(
            actions = InicioPublicoActions(
                onHelpClick = { showNotice(helpPendingMessage) },
                onNotificationsClick = { open(EcoRutaDestination.NOTIFICACIONES_SIN_SESION) },
                onLoginClick = { open(EcoRutaDestination.INICIAR_SESION) },
                onServiceClick = { service ->
                    open(
                        when (service) {
                            PublicService.RESIDUOS_ESPECIALES -> EcoRutaDestination.RESIDUOS_ESPECIALES_SIN_SESION
                            PublicService.BASURA -> EcoRutaDestination.SELECCION_BARRIO_SIN_SESION
                            PublicService.RECICLAJE -> EcoRutaDestination.RECICLAJE_SIN_SESION
                            PublicService.REPORTES -> EcoRutaDestination.REPORTES_SIN_SESION
                        },
                    )
                },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.INICIO_CIUDADANO) {
        InicioPublicoScreen(
            isSignedIn = true,
            userName = "Camila Restrepo",
            actions = InicioPublicoActions(
                onNotificationsClick = { open(EcoRutaDestination.NOTIFICACIONES) },
                onLogoutClick = { navController.goToPublicHomeAfterSignOut() },
                onServiceClick = { service ->
                    open(
                        when (service) {
                            PublicService.RESIDUOS_ESPECIALES -> EcoRutaDestination.SOLICITAR_RETIRO
                            PublicService.BASURA -> EcoRutaDestination.SELECCION_BARRIO
                            PublicService.RECICLAJE -> EcoRutaDestination.RECICLAJE
                            PublicService.REPORTES -> EcoRutaDestination.NUEVO_REPORTE
                        },
                    )
                },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    neighborhoodDestinations(navController = navController, showNotice = showNotice)
    notificationsDestinations(navController = navController)
}

private fun NavGraphBuilder.neighborhoodDestinations(
    navController: NavHostController,
    showNotice: (String) -> Unit,
) {
    val idArgument = listOf(navArgument(EcoRutaDestination.ARG_ID) { type = NavType.StringType })
    fun tabClick(signedIn: Boolean): (CitizenTab) -> Unit = { tab ->
        if (signedIn) navController.navigateToCitizenTab(tab) else navController.navigateToPublicTab(tab)
    }

    listOf(true, false).forEach { signedIn ->
        composable(if (signedIn) EcoRutaDestination.SELECCION_BARRIO else EcoRutaDestination.SELECCION_BARRIO_SIN_SESION) {
            var state by remember {
                mutableStateOf(
                    NeighborhoodSelectionUiState(
                        isLoading = false,
                        neighborhoods = SampleNeighborhoods,
                        selectedId = SampleNeighborhoods.first().id,
                        isSample = true,
                    ),
                )
            }
            NeighborhoodSelectionScreen(
                state = state,
                actions = NeighborhoodSelectionActions(
                    onBack = { navController.popBackStack() },
                    onQueryChange = { state = state.copy(query = it) },
                    onNeighborhoodSelected = { state = state.copy(selectedId = it, selectionError = null) },
                    onContinue = {
                        val id = state.selectedId ?: SampleNeighborhoods.first().id
                        navController.navigate(
                            if (signedIn) EcoRutaDestination.rutasBarrio(id) else EcoRutaDestination.rutasBarrioSinSesion(id),
                        ) { launchSingleTop = true }
                    },
                    onRetry = {},
                    onTabClick = tabClick(signedIn),
                ),
            )
        }

        composable(
            route = if (signedIn) EcoRutaDestination.RUTAS_BARRIO else EcoRutaDestination.RUTAS_BARRIO_SIN_SESION,
            arguments = idArgument,
        ) { entry ->
            val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
            val neighborhood = SampleNeighborhoods.find { it.id == id } ?: SampleNeighborhoods.first()
            val route = CollectionRoute(
                neighborhood = neighborhood,
                days = listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY),
                startMinute = 7 * 60,
                endMinute = 11 * 60,
                liveStatus = RouteLiveStatus.EN_CURSO,
            )
            RouteScreen(
                state = RouteUiState.Loaded(route = route, isSample = true),
                isSignedIn = signedIn,
                actions = RouteActions(
                    onBack = { navController.popBackStack() },
                    onPrimaryAction = {
                        navController.navigate(if (signedIn) EcoRutaDestination.radar(id) else EcoRutaDestination.INICIAR_SESION) {
                            launchSingleTop = true
                        }
                    },
                    onRetry = {},
                    onTabClick = tabClick(signedIn),
                ),
            )
        }
    }

    composable(EcoRutaDestination.RADAR, arguments = idArgument) { entry ->
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val neighborhood = SampleNeighborhoods.find { it.id == id } ?: SampleNeighborhoods.first()
        var state by remember { mutableStateOf(RadarUiState(neighborhoodName = neighborhood.name)) }
        val context = LocalContext.current
        RadarScreen(
            state = state,
            actions = RadarActions(
                onBack = { navController.popBackStack() },
                onChangeNeighborhood = {
                    if (!navController.popBackStack(EcoRutaDestination.SELECCION_BARRIO, inclusive = false)) {
                        navController.navigate(EcoRutaDestination.SELECCION_BARRIO) { launchSingleTop = true }
                    }
                },
                onRadiusChange = { state = state.copy(radiusMeters = it) },
                onNotificationsChange = { state = state.copy(notificationsEnabled = it) },
                onSave = {
                    navController.navigate(
                        EcoRutaDestination.alertaProximidad(id, state.radiusMeters, state.notificationsEnabled),
                    ) { launchSingleTop = true }
                },
                onNotificationsDenied = { showNotice(context.getString(R.string.radar_notifications_denied)) },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(
        route = EcoRutaDestination.ALERTA_PROXIMIDAD,
        arguments = listOf(
            navArgument(EcoRutaDestination.ARG_ID) { type = NavType.StringType },
            navArgument(EcoRutaDestination.ARG_RADIO) { type = NavType.IntType },
            navArgument(EcoRutaDestination.ARG_AVISOS) { type = NavType.BoolType },
        ),
    ) { entry ->
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val radius = entry.arguments?.getInt(EcoRutaDestination.ARG_RADIO) ?: 500
        val enabled = entry.arguments?.getBoolean(EcoRutaDestination.ARG_AVISOS) ?: true
        val neighborhood = SampleNeighborhoods.find { it.id == id } ?: SampleNeighborhoods.first()
        ProximityAlertScreen(
            neighborhoodName = neighborhood.name,
            radiusMeters = radius,
            notificationsEnabled = enabled,
            onViewRoute = {
                if (!navController.popBackStack(EcoRutaDestination.RUTAS_BARRIO, inclusive = false)) {
                    navController.navigate(EcoRutaDestination.rutasBarrio(id)) { launchSingleTop = true }
                }
            },
            onBack = { navController.popBackStack() },
            onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
        )
    }
}

private fun NavGraphBuilder.notificationsDestinations(navController: NavHostController) {
    composable(EcoRutaDestination.NOTIFICACIONES) {
        NotificationsScreen(
            state = NotificationsUiState.Loaded(notifications = SampleNotifications, isSample = true),
            actions = NotificationsActions(
                onBack = { navController.popBackStack() },
                onNotificationClick = { notification ->
                    val route = when (notification.kind) {
                        NotificationKind.PROXIMIDAD ->
                            notification.referenceId?.let { EcoRutaDestination.rutasBarrio(it) } ?: EcoRutaDestination.SELECCION_BARRIO
                        NotificationKind.SOLICITUD_ESCOMBROS -> EcoRutaDestination.MIS_SOLICITUDES_ESCOMBROS
                        NotificationKind.REPORTE -> EcoRutaDestination.MIS_REPORTES
                    }
                    navController.navigate(route) { launchSingleTop = true }
                },
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.INICIO) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.NOTIFICACIONES_SIN_SESION) {
        GuestNotificationsScreen(
            onBack = { navController.popBackStack() },
            onLogin = { navController.navigate(EcoRutaDestination.INICIAR_SESION) { launchSingleTop = true } },
            onTabClick = { tab -> navController.navigateToPublicTab(tab) },
        )
    }
}
