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
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.SampleRecyclerDeliveries

// Registra las pantallas del reciclador con sesión iniciada
fun NavGraphBuilder.recyclerGraph(navController: NavHostController) {
    val tabClick: (RecyclerTab) -> Unit = { tab -> navController.navigateToRecyclerTab(tab) }

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
                onTakeDelivery = { tabClick(RecyclerTab.SCANNER) },
                onTabClick = tabClick,
            ),
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
