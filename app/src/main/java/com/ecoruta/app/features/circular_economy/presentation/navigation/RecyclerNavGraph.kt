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
import com.ecoruta.app.core.navigation.components.RecyclerTab
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

// Registra las pantallas del reciclador con sesión iniciada
fun NavGraphBuilder.recyclerGraph(navController: NavHostController) {
    val tabClick: (RecyclerTab) -> Unit = { tab -> navController.navigateToRecyclerTab(tab) }
    // Entrega tomada en el mapa y volumen elegido, compartidos por las pantallas del escaneo
    var activeDelivery by mutableStateOf<RecyclerDelivery?>(SampleRecyclerDeliveries.first())
    var deliveredVolume by mutableStateOf(DeliveredVolume.LARGE_BAG)

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
            // Sin cámara conectada, tocar el visor simula la lectura del código QR
            onScan = { navController.navigate(EcoRutaDestination.RECYCLER_CONFIRM_VOLUME) { launchSingleTop = true } },
            onBack = { navController.popBackStack() },
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.RECYCLER_CONFIRM_VOLUME) {
        ConfirmVolumeScreen(
            state = RecyclerScannerUiState(delivery = activeDelivery, selectedVolume = deliveredVolume),
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
