package com.ecoruta.app.features.circular_economy.presentation.navigation

// Subgrafo de navegación del módulo reciclador
// Conecta el mapa de entregas con las pestañas de rutas, escáner y perfil del reciclador

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.core.navigation.rememberRouteLocationGate
import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.features.circular_economy.presentation.assigned_route.AssignedRouteActions
import com.ecoruta.app.features.circular_economy.presentation.assigned_route.RecyclerAssignedRouteScreen
import com.ecoruta.app.features.circular_economy.presentation.assigned_route.SampleAssignedRoute
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.RecyclerDeliveriesActions
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.RecyclerDeliveriesMapScreen
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.RecyclerDeliveriesUiState
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.RecyclerDelivery
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.SampleRecyclerDeliveries
import com.ecoruta.app.features.circular_economy.presentation.scanner.ConfirmVolumeScreen
import com.ecoruta.app.features.circular_economy.presentation.scanner.DeliveredVolume
import com.ecoruta.app.features.circular_economy.presentation.scanner.RecyclerScanResultScreen
import com.ecoruta.app.features.circular_economy.presentation.scanner.RecyclerScannerScreen
import com.ecoruta.app.features.circular_economy.presentation.scanner.RecyclerScannerUiState
import com.ecoruta.app.features.profile.presentation.RecyclerChangeProfileScreen
import com.ecoruta.app.features.profile.presentation.RecyclerEditProfileActions
import com.ecoruta.app.features.profile.presentation.RecyclerEditProfileScreen
import com.ecoruta.app.features.profile.presentation.RecyclerEditProfileUiState
import com.ecoruta.app.features.profile.presentation.RecyclerProfileActions
import com.ecoruta.app.features.profile.presentation.RecyclerProfileScreen
import com.ecoruta.app.features.profile.presentation.SampleRecyclerDetails
import com.ecoruta.app.features.profile.presentation.SampleRecyclerProfile
import com.ecoruta.app.features.profile.presentation.UserRole
import com.ecoruta.app.features.profile.presentation.components.ChangeProfileActions
import android.net.Uri

// Registra las pantallas del reciclador con sesión iniciada
fun NavGraphBuilder.recyclerGraph(navController: NavHostController) {
    val tabClick: (RecyclerTab) -> Unit = { tab -> navController.navigateToRecyclerTab(tab) }
    // Entrega tomada en el mapa y volumen elegido, compartidos por las pantallas del escaneo
    var activeDelivery by mutableStateOf<RecyclerDelivery?>(SampleRecyclerDeliveries.first())
    var deliveredVolume by mutableStateOf(DeliveredVolume.LARGE_BAG)
    var scannedCode by mutableStateOf<String?>(null)

    composable(EcoRutaDestination.DELIVERY_MAP) {
        var state by remember { mutableStateOf(RecyclerDeliveriesUiState(deliveries = SampleRecyclerDeliveries)) }
        RecyclerDeliveriesMapScreen(
            state = state,
            actions = RecyclerDeliveriesActions(
                onMaterialFilterChange = { state = state.copy(materialFilter = it, selectedId = null) },
                onStatusFilterChange = { state = state.copy(statusFilter = it, selectedId = null) },
                onDeliverySelected = { state = state.copy(selectedId = it) },
                onExpandList = { state = state.copy(isListExpanded = true) },
                onCollapseList = { state = state.copy(isListExpanded = false) },
                onTakeDelivery = { delivery ->
                    activeDelivery = delivery
                    tabClick(RecyclerTab.SCANNER)
                },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.RECYCLER_SCANNER) {
        RecyclerScannerScreen(
            state = RecyclerScannerUiState(delivery = activeDelivery),
            // Al leer un código QR con la cámara se continúa con la confirmación del volumen
            onQrDetected = { code ->
                scannedCode = code
                navController.navigate(EcoRutaDestination.RECYCLER_CONFIRM_VOLUME) { launchSingleTop = true }
            },
            onBack = { navController.popBackStack() },
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.RECYCLER_CONFIRM_VOLUME) {
        ConfirmVolumeScreen(
            state = RecyclerScannerUiState(delivery = activeDelivery, scannedCode = scannedCode, selectedVolume = deliveredVolume),
            onVolumeSelected = { deliveredVolume = it },
            onConfirm = {
                navController.navigate(EcoRutaDestination.RECYCLER_SCAN_RESULT) {
                    popUpTo(EcoRutaDestination.RECYCLER_SCANNER)
                    launchSingleTop = true
                }
            },
            onBack = { navController.popBackStack() },
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.RECYCLER_SCAN_RESULT) {
        RecyclerScanResultScreen(
            state = RecyclerScannerUiState(delivery = activeDelivery, selectedVolume = deliveredVolume),
            onBackToMap = { tabClick(RecyclerTab.MAP) },
            onBack = { navController.popBackStack() },
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.RECYCLER_ASSIGNED_ROUTE) { entry ->
        // Los puntos en el mapa solo se abren con permiso de ubicación precisa y el GPS encendido
        val locationGate = rememberRouteLocationGate(entry.lifecycle) { tabClick(RecyclerTab.MAP) }
        RecyclerAssignedRouteScreen(
            route = SampleAssignedRoute,
            locationStatus = locationGate.status,
            actions = AssignedRouteActions(
                onBack = { navController.popBackStack() },
                onViewOnMap = locationGate.onStart,
                onOpenSettings = locationGate.onOpenSettings,
                onEnableLocation = locationGate.onEnableLocation,
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.RECYCLER_PROFILE) {
        var localPhoto by remember { mutableStateOf<Uri?>(null) }
        RecyclerProfileScreen(
            profile = SampleRecyclerProfile,
            details = SampleRecyclerDetails,
            localPhoto = localPhoto,
            actions = RecyclerProfileActions(
                onBack = { navController.popBackStack() },
                onPhotoSelected = { localPhoto = it },
                onRemovePhoto = { localPhoto = null },
                onChangeProfile = { navController.navigate(EcoRutaDestination.RECYCLER_CHANGE_PROFILE) { launchSingleTop = true } },
                onEditProfile = { navController.navigate(EcoRutaDestination.RECYCLER_EDIT_PROFILE) { launchSingleTop = true } },
                onSignOut = { navController.goToPublicHomeAfterSignOut() },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.RECYCLER_EDIT_PROFILE) {
        // Arranca con el nombre del perfil ilustrativo y guardar no hace nada hasta que exista el ViewModel
        var state by remember { mutableStateOf(RecyclerEditProfileUiState(fullName = SampleRecyclerProfile.fullName)) }
        RecyclerEditProfileScreen(
            state = state,
            actions = RecyclerEditProfileActions(
                onFullNameChange = { state = state.copy(fullName = it, fullNameErrorRes = null) },
                onPhoneChange = { state = state.copy(phone = it, phoneErrorRes = null) },
                onAssociationChange = { state = state.copy(association = it) },
                onDocumentPicked = { state = state.copy(idDocument = it, documentErrorRes = null) },
                onDocumentRemoved = { state = state.copy(idDocument = null) },
                onCancel = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            ),
        )
    }

    composable(EcoRutaDestination.RECYCLER_CHANGE_PROFILE) {
        RecyclerChangeProfileScreen(
            profile = SampleRecyclerProfile,
            actions = ChangeProfileActions(
                onBack = { navController.popBackStack() },
                onSwitchRole = { role ->
                    navController.navigate(EcoRutaDestination.homeForRole(role)) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRequestRole = { role ->
                    val route = if (role == UserRole.COMERCIO) EcoRutaDestination.STORE_REQUEST else EcoRutaDestination.RECYCLER_APPLICATION
                    navController.navigate(route) { launchSingleTop = true }
                },
            ),
            onTabClick = tabClick,
        )
    }
}

// Navega entre las pestañas del reciclador conservando el mapa de entregas como base de la pila
fun NavHostController.navigateToRecyclerTab(tab: RecyclerTab) {
    if (tab == RecyclerTab.MAP) {
        popBackStack(EcoRutaDestination.DELIVERY_MAP, inclusive = false)
        return
    }
    val route = when (tab) {
        RecyclerTab.MAP -> EcoRutaDestination.DELIVERY_MAP
        RecyclerTab.ROUTES -> EcoRutaDestination.RECYCLER_ASSIGNED_ROUTE
        RecyclerTab.SCANNER -> EcoRutaDestination.RECYCLER_SCANNER
        RecyclerTab.PROFILE -> EcoRutaDestination.RECYCLER_PROFILE
    }
    navigate(route) {
        popUpTo(EcoRutaDestination.DELIVERY_MAP)
        launchSingleTop = true
    }
}
