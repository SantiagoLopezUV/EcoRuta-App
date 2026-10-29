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
import com.ecoruta.app.features.routes.presentation.map.HomeActions
import com.ecoruta.app.features.routes.presentation.map.HomeScreen
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
        route = EcoRutaDestination.PUBLIC_HOME,
        enterTransition = { publicHomeEnterTransition },
    ) {
        val helpPendingMessage = stringResource(R.string.notice_help_pending)
        HomeScreen(
            actions = HomeActions(
                onHelpClick = { showNotice(helpPendingMessage) },
                onNotificationsClick = { open(EcoRutaDestination.GUEST_NOTIFICATIONS) },
                onLoginClick = { open(EcoRutaDestination.LOGIN) },
                onServiceClick = { service ->
                    open(
                        when (service) {
                            PublicService.SPECIAL_WASTE -> EcoRutaDestination.GUEST_SPECIAL_WASTE
                            PublicService.GARBAGE -> EcoRutaDestination.GUEST_NEIGHBORHOOD_SELECTION
                            PublicService.RECYCLING -> EcoRutaDestination.GUEST_RECYCLING
                            PublicService.REPORTS -> EcoRutaDestination.GUEST_REPORTS
                        },
                    )
                },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.CITIZEN_HOME) {
        HomeScreen(
            isSignedIn = true,
            userName = "Camila Restrepo",
            actions = HomeActions(
                onNotificationsClick = { open(EcoRutaDestination.NOTIFICATIONS) },
                onLogoutClick = { navController.goToPublicHomeAfterSignOut() },
                onServiceClick = { service ->
                    open(
                        when (service) {
                            PublicService.SPECIAL_WASTE -> EcoRutaDestination.REQUEST_DEBRIS_PICKUP
                            PublicService.GARBAGE -> EcoRutaDestination.NEIGHBORHOOD_SELECTION
                            PublicService.RECYCLING -> EcoRutaDestination.RECYCLING
                            PublicService.REPORTS -> EcoRutaDestination.NEW_REPORT
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
        composable(if (signedIn) EcoRutaDestination.NEIGHBORHOOD_SELECTION else EcoRutaDestination.GUEST_NEIGHBORHOOD_SELECTION) {
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
                            if (signedIn) EcoRutaDestination.neighborhoodRoutes(id) else EcoRutaDestination.guestNeighborhoodRoutes(id),
                        ) { launchSingleTop = true }
                    },
                    onRetry = {},
                    onTabClick = tabClick(signedIn),
                ),
            )
        }

        composable(
            route = if (signedIn) EcoRutaDestination.NEIGHBORHOOD_ROUTES else EcoRutaDestination.GUEST_NEIGHBORHOOD_ROUTES,
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
                        navController.navigate(if (signedIn) EcoRutaDestination.radar(id) else EcoRutaDestination.LOGIN) {
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
                    if (!navController.popBackStack(EcoRutaDestination.NEIGHBORHOOD_SELECTION, inclusive = false)) {
                        navController.navigate(EcoRutaDestination.NEIGHBORHOOD_SELECTION) { launchSingleTop = true }
                    }
                },
                onRadiusChange = { state = state.copy(radiusMeters = it) },
                onNotificationsChange = { state = state.copy(notificationsEnabled = it) },
                onSave = {
                    navController.navigate(
                        EcoRutaDestination.proximityAlert(id, state.radiusMeters, state.notificationsEnabled),
                    ) { launchSingleTop = true }
                },
                onNotificationsDenied = { showNotice(context.getString(R.string.radar_notifications_denied)) },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(
        route = EcoRutaDestination.PROXIMITY_ALERT,
        arguments = listOf(
            navArgument(EcoRutaDestination.ARG_ID) { type = NavType.StringType },
            navArgument(EcoRutaDestination.ARG_RADIUS) { type = NavType.IntType },
            navArgument(EcoRutaDestination.ARG_NOTIFICATIONS_ENABLED) { type = NavType.BoolType },
        ),
    ) { entry ->
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val radius = entry.arguments?.getInt(EcoRutaDestination.ARG_RADIUS) ?: 500
        val enabled = entry.arguments?.getBoolean(EcoRutaDestination.ARG_NOTIFICATIONS_ENABLED) ?: true
        val neighborhood = SampleNeighborhoods.find { it.id == id } ?: SampleNeighborhoods.first()
        ProximityAlertScreen(
            neighborhoodName = neighborhood.name,
            radiusMeters = radius,
            notificationsEnabled = enabled,
            onViewRoute = {
                if (!navController.popBackStack(EcoRutaDestination.NEIGHBORHOOD_ROUTES, inclusive = false)) {
                    navController.navigate(EcoRutaDestination.neighborhoodRoutes(id)) { launchSingleTop = true }
                }
            },
            onBack = { navController.popBackStack() },
            onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
        )
    }
}

private fun NavGraphBuilder.notificationsDestinations(navController: NavHostController) {
    composable(EcoRutaDestination.NOTIFICATIONS) {
        NotificationsScreen(
            state = NotificationsUiState.Loaded(notifications = SampleNotifications, isSample = true),
            actions = NotificationsActions(
                onBack = { navController.popBackStack() },
                onNotificationClick = { notification ->
                    val route = when (notification.kind) {
                        NotificationKind.PROXIMIDAD ->
                            notification.referenceId?.let { EcoRutaDestination.neighborhoodRoutes(it) } ?: EcoRutaDestination.NEIGHBORHOOD_SELECTION
                        NotificationKind.SOLICITUD_ESCOMBROS -> EcoRutaDestination.MY_DEBRIS_REQUESTS
                        NotificationKind.REPORTE -> EcoRutaDestination.MY_REPORTS
                    }
                    navController.navigate(route) { launchSingleTop = true }
                },
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.HOME) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.GUEST_NOTIFICATIONS) {
        GuestNotificationsScreen(
            onBack = { navController.popBackStack() },
            onLogin = { navController.navigate(EcoRutaDestination.LOGIN) { launchSingleTop = true } },
            onTabClick = { tab -> navController.navigateToPublicTab(tab) },
        )
    }
}
