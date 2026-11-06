package com.ecoruta.app.features.driver.presentation.navigation

// Subgrafo de navegación del módulo chofer
// Conecta Mis órdenes con el detalle y la ejecución de rutas, las órdenes de escombros y el perfil

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.components.DriverTab
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordMode
import com.ecoruta.app.features.driver.presentation.active_route.ActiveRouteActions
import com.ecoruta.app.features.driver.presentation.active_route.ActiveRouteTrackingScreen
import com.ecoruta.app.features.driver.presentation.active_route.ActiveRouteUiState
import com.ecoruta.app.features.driver.presentation.active_route.PointVisitedActions
import com.ecoruta.app.features.driver.presentation.active_route.PointVisitedScreen
import com.ecoruta.app.features.driver.presentation.active_route.RouteFinishedActions
import com.ecoruta.app.features.driver.presentation.active_route.RouteFinishedScreen
import com.ecoruta.app.features.driver.presentation.active_route.SampleCleanupRoute
import com.ecoruta.app.features.driver.presentation.active_route.SampleOrdinaryRoute
import com.ecoruta.app.features.driver.presentation.debris_order.DebrisCollectedActions
import com.ecoruta.app.features.driver.presentation.debris_order.DebrisCollectedScreen
import com.ecoruta.app.features.driver.presentation.debris_order.DebrisOrderActions
import com.ecoruta.app.features.driver.presentation.debris_order.DebrisOrderScreen
import com.ecoruta.app.features.driver.presentation.debris_order.DebrisOrderUiState
import com.ecoruta.app.features.driver.presentation.debris_order.SampleDebrisOrder
import com.ecoruta.app.features.driver.presentation.orders.DriverOrderKind
import com.ecoruta.app.features.driver.presentation.orders.DriverOrderStatus
import com.ecoruta.app.features.driver.presentation.orders.DriverOrdersActions
import com.ecoruta.app.features.driver.presentation.orders.DriverOrdersListScreen
import com.ecoruta.app.features.driver.presentation.orders.DriverRouteDetailActions
import com.ecoruta.app.features.driver.presentation.orders.DriverRouteDetailScreen
import com.ecoruta.app.features.driver.presentation.orders.SampleDriverOrders
import com.ecoruta.app.features.driver.presentation.orders.SampleDriverOrdersState
import com.ecoruta.app.features.profile.presentation.DriverEditProfileActions
import com.ecoruta.app.features.profile.presentation.DriverEditProfileScreen
import com.ecoruta.app.features.profile.presentation.DriverEditProfileUiState
import com.ecoruta.app.features.profile.presentation.DriverProfileActions
import com.ecoruta.app.features.profile.presentation.DriverProfileScreen
import com.ecoruta.app.features.profile.presentation.SampleDriverDetails
import com.ecoruta.app.features.profile.presentation.SampleDriverProfile
import com.ecoruta.app.features.profile.presentation.UserRole

// Registra las pantallas del chofer con sesión iniciada
fun NavGraphBuilder.driverGraph(navController: NavHostController) {
    val tabClick: (DriverTab) -> Unit = { tab -> navController.navigateToDriverTab(tab) }
    val idArgument = listOf(navArgument(EcoRutaDestination.ARG_ID) { type = NavType.StringType })
    // Ruta iniciada y orden de escombros abierta, compartidas por las pantallas de cada flujo
    var routeState by mutableStateOf(ActiveRouteUiState(city = "Tuluá"))
    var debrisState by mutableStateOf(DebrisOrderUiState(order = SampleDebrisOrder))

    // Abre el detalle de la orden según sea una ruta o una recolección de escombros
    val openOrder: (String, DriverOrderKind) -> Unit = { id, kind ->
        val route = if (kind == DriverOrderKind.DEBRIS) EcoRutaDestination.driverDebrisOrder(id) else EcoRutaDestination.driverRouteDetail(id)
        navController.navigate(route) { launchSingleTop = true }
    }

    composable(EcoRutaDestination.MY_ORDERS) {
        DriverOrdersListScreen(
            state = SampleDriverOrdersState,
            actions = DriverOrdersActions(
                onOrderClick = { order -> openOrder(order.id, order.kind) },
                onViewRouteDetail = {
                    SampleDriverOrders.firstOrNull { it.kind != DriverOrderKind.DEBRIS }?.let { openOrder(it.id, it.kind) }
                },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.DRIVER_ROUTE_DETAIL, arguments = idArgument) { entry ->
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID)
        val route = if (id == SampleOrdinaryRoute.code) SampleOrdinaryRoute else SampleCleanupRoute
        DriverRouteDetailScreen(
            state = ActiveRouteUiState(route = route),
            actions = DriverRouteDetailActions(
                onBack = { navController.popBackStack() },
                onStartRoute = {
                    routeState = ActiveRouteUiState(
                        route = route.copy(status = DriverOrderStatus.IN_PROGRESS),
                        city = "Tuluá",
                        isSharingLocation = true,
                    )
                    navController.navigate(EcoRutaDestination.DRIVER_ACTIVE_ROUTE) {
                        popUpTo(EcoRutaDestination.MY_ORDERS)
                        launchSingleTop = true
                    }
                },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.DRIVER_ACTIVE_ROUTE) {
        ActiveRouteTrackingScreen(
            state = routeState,
            actions = ActiveRouteActions(
                onBack = { navController.popBackStack() },
                onMarkVisited = {
                    routeState = routeState.copy(visitedCount = routeState.visitedCount + 1)
                    navController.navigate(EcoRutaDestination.DRIVER_POINT_VISITED) { launchSingleTop = true }
                },
                onGoToOrders = { tabClick(DriverTab.ORDERS) },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.DRIVER_POINT_VISITED) {
        PointVisitedScreen(
            state = routeState,
            actions = PointVisitedActions(
                onBack = { navController.popBackStack() },
                onContinue = {
                    if (routeState.nextPoint != null) {
                        navController.popBackStack(EcoRutaDestination.DRIVER_ACTIVE_ROUTE, inclusive = false)
                    } else {
                        // Al cerrar la ruta se deja de compartir la ubicación y se usa una duración ilustrativa
                        routeState = routeState.copy(
                            isSharingLocation = false,
                            completedSchedule = routeState.route?.schedule.orEmpty(),
                            duration = "52 min",
                        )
                        navController.navigate(EcoRutaDestination.DRIVER_ROUTE_FINISHED) {
                            popUpTo(EcoRutaDestination.MY_ORDERS)
                            launchSingleTop = true
                        }
                    }
                },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.DRIVER_ROUTE_FINISHED) {
        RouteFinishedScreen(
            state = routeState,
            actions = RouteFinishedActions(
                onBack = { navController.popBackStack() },
                onBackToOrders = {
                    routeState = ActiveRouteUiState(city = "Tuluá")
                    tabClick(DriverTab.ORDERS)
                },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.DRIVER_DEBRIS_ORDER, arguments = idArgument) { entry ->
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        DebrisOrderScreen(
            state = debrisState,
            actions = DebrisOrderActions(
                onBack = { navController.popBackStack() },
                onMarkCollected = {
                    // Hora ilustrativa de la recolección mientras no exista el registro real
                    debrisState = debrisState.copy(
                        order = debrisState.order?.copy(status = DriverOrderStatus.COLLECTED, collectedAt = "10:24"),
                    )
                    navController.navigate(EcoRutaDestination.driverDebrisCollected(id)) {
                        popUpTo(EcoRutaDestination.MY_ORDERS)
                        launchSingleTop = true
                    }
                },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.DRIVER_DEBRIS_COLLECTED, arguments = idArgument) {
        DebrisCollectedScreen(
            state = debrisState,
            actions = DebrisCollectedActions(
                onBack = { navController.popBackStack() },
                onBackToOrders = { tabClick(DriverTab.ORDERS) },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.DRIVER_PROFILE) {
        var localPhoto by remember { mutableStateOf<Uri?>(null) }
        DriverProfileScreen(
            profile = SampleDriverProfile,
            details = SampleDriverDetails,
            localPhoto = localPhoto,
            actions = DriverProfileActions(
                onPhotoSelected = { localPhoto = it },
                onRemovePhoto = { localPhoto = null },
                onChangePassword = {
                    navController.navigate(EcoRutaDestination.changePassword(ChangePasswordMode.PROFILE, UserRole.CHOFER)) { launchSingleTop = true }
                },
                onEditProfile = { navController.navigate(EcoRutaDestination.DRIVER_EDIT_PROFILE) { launchSingleTop = true } },
                onSignOut = { navController.goToPublicHomeAfterSignOut() },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.DRIVER_EDIT_PROFILE) {
        // Arranca con los datos del perfil ilustrativo y guardar no hace nada hasta que exista el ViewModel
        var state by remember {
            mutableStateOf(
                DriverEditProfileUiState(
                    fullName = SampleDriverProfile.fullName,
                    email = SampleDriverProfile.email,
                    company = SampleDriverDetails.company,
                ),
            )
        }
        DriverEditProfileScreen(
            state = state,
            actions = DriverEditProfileActions(
                onFullNameChange = { state = state.copy(fullName = it, fullNameErrorRes = null) },
                onPhoneChange = { state = state.copy(phone = it, phoneErrorRes = null) },
                onCancel = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            ),
        )
    }
}

// Navega entre las pestañas del chofer conservando Mis órdenes como base de la pila
fun NavHostController.navigateToDriverTab(tab: DriverTab) {
    if (tab == DriverTab.ORDERS) {
        popBackStack(EcoRutaDestination.MY_ORDERS, inclusive = false)
        return
    }
    val route = when (tab) {
        DriverTab.ORDERS -> EcoRutaDestination.MY_ORDERS
        DriverTab.MAP -> EcoRutaDestination.DRIVER_ACTIVE_ROUTE
        DriverTab.PROFILE -> EcoRutaDestination.DRIVER_PROFILE
    }
    navigate(route) {
        popUpTo(EcoRutaDestination.MY_ORDERS)
        launchSingleTop = true
    }
}
