package com.ecoruta.app.features.circular_economy.presentation.assigned_route

// Vista composable de la ruta de reciclaje asignada al reciclador vinculado a una empresa
// Muestra el recorrido en el mapa ilustrativo, las paradas ordenadas y el estado de la ubicación

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.core.navigation.components.recyclerTabs
import com.ecoruta.app.core.ui.components.EcoLocationStatusCard
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoRouteMap
import com.ecoruta.app.core.ui.components.EcoRouteMapPoint
import com.ecoruta.app.core.ui.components.RouteLocationStatus
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Parada de la ruta con su barrio, dirección y posición relativa dentro del mapa ilustrativo
data class RouteStop(
    val neighborhood: String,
    val address: String,
    val mapX: Float,
    val mapY: Float,
)

// Ruta de reciclaje asignada por la empresa al reciclador vinculado
data class AssignedRecyclingRoute(
    val code: String,
    val name: String,
    val date: String,
    val company: String,
    val stops: List<RouteStop>,
)

// Acciones disponibles en la pantalla de ruta asignada
data class AssignedRouteActions(
    val onBack: () -> Unit = {},
    val onViewOnMap: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onEnableLocation: () -> Unit = {},
    val onTabClick: (RecyclerTab) -> Unit = {},
)

// Pantalla con la ruta del día, sus paradas y el aviso sobre la ubicación compartida
@Composable
fun RecyclerAssignedRouteScreen(
    route: AssignedRecyclingRoute,
    locationStatus: RouteLocationStatus,
    tabs: List<RecyclerTab>,
    actions: AssignedRouteActions,
) {
    EcoRoleTabScreenLayout(
        items = tabs,
        selectedTab = RecyclerTab.ROUTES,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
        contentSpacing = 12.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recycler_route_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.recycler_route_subtitle, route.code, route.name), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.recycler_route_summary, route.date, route.stops.size, route.company),
                style = EcoTextStyles.FieldLabel,
                color = EcoSubtitle,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.recycler_route_assigned), style = EcoTextStyles.FieldLabel, color = EcoGreenSecondary)
        }
        EcoRouteMap(points = route.stops.map { EcoRouteMapPoint(it.mapX, it.mapY) })
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            route.stops.forEachIndexed { index, stop ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EcoWhite),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", style = EcoTextStyles.FieldLabel, color = EcoPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(stop.neighborhood, style = EcoTextStyles.LinkText, color = EcoTitle)
                        Text(stop.address, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                    }
                }
            }
        }
        EcoLocationStatusCard(
            status = locationStatus,
            infoBody = stringResource(R.string.recycler_route_location_body),
            onOpenSettings = actions.onOpenSettings,
            onEnableLocation = actions.onEnableLocation,
        )
        EcoPrimaryButton(text = stringResource(R.string.recycler_route_view_on_map), onClick = actions.onViewOnMap, height = 56.dp)
    }
}

// Ruta ilustrativa de reciclaje en Tuluá para el modo diseño y las previsualizaciones
val SampleAssignedRoute = AssignedRecyclingRoute(
    code = "RT-031",
    name = "Ruta Sajonia",
    date = "17 oct. 2026",
    company = "Aseo Tuluá S. A. S.",
    stops = listOf(
        RouteStop("Sajonia", "Carrera 27 # 32-14", 0.237f, 0.294f),
        RouteStop("La Campiña", "Calle 25 # 28-09", 0.477f, 0.494f),
        RouteStop("Centro", "Calle 27 # 26-35", 0.237f, 0.814f),
    ),
)

@Preview(name = "Ruta asignada · Reciclador vinculado", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerAssignedRoutePreview() {
    EcoRutaTheme {
        RecyclerAssignedRouteScreen(
            route = SampleAssignedRoute,
            locationStatus = RouteLocationStatus.OK,
            tabs = recyclerTabs(isCompanyLinked = true),
            actions = AssignedRouteActions(),
        )
    }
}
