package com.ecoruta.app.features.driver.presentation.active_route

// Vista composable de ruta en ejecución con mapa y secuencia de puntos
// Trazado de ruta en curso, indicador de ubicación compartida y botón para marcar el punto como visitado

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.DriverTab
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoRouteMap
import com.ecoruta.app.core.ui.components.EcoRouteMapPoint
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.driver.presentation.orders.DriverOrderStatus
import com.ecoruta.app.features.driver.presentation.orders.DriverStatusLabel

// Acciones disponibles en la ruta en curso
data class ActiveRouteActions(
    val onBack: () -> Unit = {},
    val onMarkVisited: () -> Unit = {},
    val onCenterLocation: () -> Unit = {},
    val onGoToOrders: () -> Unit = {},
    val onTabClick: (DriverTab) -> Unit = {},
)

// Ruta en curso con el mapa, el aviso de ubicación compartida y la tarjeta del siguiente punto
@Composable
fun ActiveRouteTrackingScreen(state: ActiveRouteUiState, actions: ActiveRouteActions) {
    val route = state.route
    if (route == null) {
        NoActiveRouteScreen(onGoToOrders = actions.onGoToOrders)
        return
    }
    EcoRoleTabScreenLayout(
        items = DriverTab.entries,
        selectedTab = DriverTab.MAP,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
        contentSpacing = 12.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.driver_active_route_title), style = EcoTextStyles.Title, color = EcoTitle)
            val progress = pluralStringResource(R.plurals.driver_route_progress, route.points.size, state.visitedCount, route.points.size)
            Text(
                stringResource(R.string.driver_route_header, route.code, route.zone, progress),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        if (state.isSharingLocation) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EcoAccent),
                )
                Text(stringResource(R.string.driver_sharing_location), style = EcoTextStyles.FieldLabel, color = EcoPrimary)
            }
        }
        EcoRouteMap(
            points = route.points.map { EcoRouteMapPoint(it.mapX, it.mapY) },
            description = stringResource(R.string.driver_route_map_description),
            mapHeight = 250.dp,
            showLiveLocation = state.isSharingLocation,
        ) {
            val centerLabel = stringResource(R.string.recycler_map_my_location)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EcoWhite)
                    .clickable(role = Role.Button, onClickLabel = centerLabel, onClick = actions.onCenterLocation),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_map_pin), contentDescription = centerLabel, tint = EcoTitle, modifier = Modifier.size(24.dp))
            }
        }
        state.nextPoint?.let { point ->
            Column(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EcoFieldBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.driver_next_point, state.visitedCount + 1, route.points.size),
                        style = EcoTextStyles.FieldLabel,
                        color = EcoSubtitle,
                        modifier = Modifier.weight(1f),
                    )
                    DriverStatusLabel(DriverOrderStatus.PENDING)
                }
                Text(point.name, style = EcoTextStyles.ButtonText, color = EcoTitle)
                Text(stringResource(R.string.driver_point_address_city, point.address, state.city), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                Text(
                    stringResource(R.string.driver_point_trip, stringResource(route.pointLabelRes(point)), point.distanceAway, point.travelTime),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoSubtitle,
                )
                Text(point.directions, style = EcoTextStyles.Subtitle, color = EcoTitle)
                EcoPrimaryButton(text = stringResource(R.string.driver_mark_visited), onClick = actions.onMarkVisited)
            }
        }
    }
}

// Aviso cuando el chofer abre el mapa sin haber iniciado ninguna ruta
@Composable
private fun NoActiveRouteScreen(onGoToOrders: () -> Unit) {
    EcoSystemStateScreen(
        title = stringResource(R.string.driver_active_route_title),
        subtitle = stringResource(R.string.driver_no_active_route_subtitle),
        illustrationRes = R.drawable.ill_state_location,
        heading = stringResource(R.string.driver_no_active_route_heading),
        body = stringResource(R.string.driver_no_active_route_body),
        actionText = stringResource(R.string.driver_go_to_orders),
        onAction = onGoToOrders,
    )
}

@Preview(name = "Ruta en curso · Ruta de limpieza", widthDp = 390, heightDp = 844)
@Composable
private fun ActiveRouteCleanupPreview() {
    EcoRutaTheme {
        ActiveRouteTrackingScreen(
            state = ActiveRouteUiState(route = SampleCleanupRoute, city = "Tuluá", isSharingLocation = true),
            actions = ActiveRouteActions(),
        )
    }
}

@Preview(name = "Ruta en curso · Ruta ordinaria", widthDp = 390, heightDp = 844)
@Composable
private fun ActiveRouteOrdinaryPreview() {
    EcoRutaTheme {
        ActiveRouteTrackingScreen(
            state = ActiveRouteUiState(route = SampleOrdinaryRoute, city = "Tuluá", isSharingLocation = true),
            actions = ActiveRouteActions(),
        )
    }
}

@Preview(name = "Ruta en curso · Sin ruta activa", widthDp = 390, heightDp = 844)
@Composable
private fun NoActiveRoutePreview() {
    EcoRutaTheme { ActiveRouteTrackingScreen(state = ActiveRouteUiState(), actions = ActiveRouteActions()) }
}
