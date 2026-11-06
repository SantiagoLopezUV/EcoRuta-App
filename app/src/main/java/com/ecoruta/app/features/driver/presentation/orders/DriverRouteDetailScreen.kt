package com.ecoruta.app.features.driver.presentation.orders

// Vista composable del detalle de una ruta asignada antes de iniciarla
// Muestra el recorrido en el mapa ilustrativo, los puntos en orden y el estado de la ubicación

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.DriverTab
import com.ecoruta.app.core.ui.components.EcoLocationStatusCard
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoRouteMap
import com.ecoruta.app.core.ui.components.EcoRouteMapPoint
import com.ecoruta.app.core.ui.components.RouteLocationStatus
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.driver.presentation.active_route.ActiveRouteUiState
import com.ecoruta.app.features.driver.presentation.active_route.SampleCleanupRoute
import com.ecoruta.app.features.driver.presentation.active_route.SampleOrdinaryRoute

// Acciones disponibles en el detalle de la ruta
data class DriverRouteDetailActions(
    val onBack: () -> Unit = {},
    val onStartRoute: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onEnableLocation: () -> Unit = {},
    val onTabClick: (DriverTab) -> Unit = {},
)

// Detalle de la ruta con su mapa, la secuencia de puntos y el aviso sobre la ubicación compartida
@Composable
fun DriverRouteDetailScreen(state: ActiveRouteUiState, actions: DriverRouteDetailActions) {
    val route = state.route ?: return
    EcoRoleTabScreenLayout(
        items = DriverTab.entries,
        selectedTab = DriverTab.ORDERS,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
        contentSpacing = 12.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.driver_route_detail_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(
                stringResource(R.string.driver_route_header, route.code, stringResource(route.type.labelRes), route.zone),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val points = pluralStringResource(R.plurals.driver_points_count, route.points.size, route.points.size)
            Text(
                listOf(route.schedule, points, route.distance).joinToString(" · "),
                style = EcoTextStyles.FieldLabel,
                color = EcoSubtitle,
                modifier = Modifier.weight(1f),
            )
            DriverStatusLabel(route.status)
        }
        EcoRouteMap(
            points = route.points.map { EcoRouteMapPoint(it.mapX, it.mapY) },
            description = stringResource(R.string.driver_route_map_description),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            route.points.forEachIndexed { index, point ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((index + 1).toString(), style = EcoTextStyles.FieldLabel, color = EcoPrimary, modifier = Modifier.width(44.dp).padding(start = 12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(point.name, style = EcoTextStyles.Subtitle, color = EcoTitle)
                        Text(point.address, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                    }
                }
            }
        }
        EcoLocationStatusCard(
            status = state.locationStatus,
            infoBody = stringResource(R.string.driver_route_location_body),
            onOpenSettings = actions.onOpenSettings,
            onEnableLocation = actions.onEnableLocation,
        )
        // El botón siempre está activo y la comprobación de la ubicación ocurre al pulsarlo
        EcoPrimaryButton(
            text = stringResource(R.string.driver_route_start),
            onClick = actions.onStartRoute,
        )
    }
}

@Preview(name = "Detalle de ruta · Ruta de limpieza", widthDp = 390, heightDp = 844)
@Composable
private fun DriverRouteDetailCleanupPreview() {
    EcoRutaTheme { DriverRouteDetailScreen(state = ActiveRouteUiState(route = SampleCleanupRoute), actions = DriverRouteDetailActions()) }
}

@Preview(name = "Detalle de ruta · Ruta ordinaria", widthDp = 390, heightDp = 844)
@Composable
private fun DriverRouteDetailOrdinaryPreview() {
    EcoRutaTheme { DriverRouteDetailScreen(state = ActiveRouteUiState(route = SampleOrdinaryRoute), actions = DriverRouteDetailActions()) }
}

@Preview(name = "Detalle de ruta · Ubicación no permitida", widthDp = 390, heightDp = 844)
@Composable
private fun DriverRouteDetailDeniedPreview() {
    EcoRutaTheme {
        DriverRouteDetailScreen(
            state = ActiveRouteUiState(route = SampleCleanupRoute, locationStatus = RouteLocationStatus.PERMISSION_DENIED),
            actions = DriverRouteDetailActions(),
        )
    }
}

@Preview(name = "Detalle de ruta · Ubicación desactivada", widthDp = 390, heightDp = 844)
@Composable
private fun DriverRouteDetailGpsOffPreview() {
    EcoRutaTheme {
        DriverRouteDetailScreen(
            state = ActiveRouteUiState(route = SampleCleanupRoute, locationStatus = RouteLocationStatus.GPS_DISABLED),
            actions = DriverRouteDetailActions(),
        )
    }
}
