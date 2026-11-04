package com.ecoruta.app.features.driver.presentation.active_route

// Vista composable del cierre de una ruta cuando todos los puntos fueron atendidos
// Resume puntos visitados, duración, recorrido completo y el fin de la ubicación compartida

import androidx.compose.foundation.background
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.DriverTab
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.driver.presentation.orders.DriverOrderStatus
import com.ecoruta.app.features.driver.presentation.orders.DriverStatusLabel

// Acciones disponibles en el cierre de la ruta
data class RouteFinishedActions(
    val onBack: () -> Unit = {},
    val onBackToOrders: () -> Unit = {},
    val onTabClick: (DriverTab) -> Unit = {},
)

// Resumen de la ruta finalizada con sus cifras, los puntos atendidos y el regreso a las órdenes
@Composable
fun RouteFinishedScreen(state: ActiveRouteUiState, actions: RouteFinishedActions) {
    val route = state.route ?: return
    EcoRoleTabScreenLayout(
        items = DriverTab.entries,
        selectedTab = DriverTab.ORDERS,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.driver_route_finished_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(
                stringResource(R.string.driver_route_header, route.code, stringResource(route.type.labelRes), route.zone),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        DriverSuccessHeading(stringResource(R.string.driver_route_finished_heading))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FinishedStat(
                label = stringResource(R.string.driver_route_finished_points),
                value = stringResource(R.string.driver_points_ratio, state.visitedCount, route.points.size),
                modifier = Modifier.weight(1f),
            )
            FinishedStat(
                label = stringResource(R.string.driver_route_finished_duration),
                value = state.duration,
                modifier = Modifier.weight(1f),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.driver_route_finished_trip), style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
                DriverStatusLabel(DriverOrderStatus.ATTENDED)
            }
            Text(
                stringResource(R.string.driver_route_finished_traveled, state.completedSchedule, route.distance),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                route.points.forEach { point ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EcoAccent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_circle_check), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(point.name, style = EcoTextStyles.Subtitle, color = EcoTitle)
                            Text(point.address, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                        }
                    }
                }
            }
        }
        Text(
            stringResource(R.string.driver_location_stopped),
            style = EcoTextStyles.Subtitle,
            color = EcoTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        EcoPrimaryButton(text = stringResource(R.string.driver_back_to_orders), onClick = actions.onBackToOrders)
    }
}

// Tarjeta con una cifra del resumen de la ruta y su etiqueta
@Composable
private fun FinishedStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Text(value, style = EcoTextStyles.Numbers, color = EcoPrimary)
    }
}

@Preview(name = "Ruta finalizada · Ruta de limpieza", widthDp = 390, heightDp = 844)
@Composable
private fun RouteFinishedCleanupPreview() {
    EcoRutaTheme {
        RouteFinishedScreen(
            state = ActiveRouteUiState(route = SampleCleanupRoute, visitedCount = 3, completedSchedule = "08:00–08:52", duration = "52 min"),
            actions = RouteFinishedActions(),
        )
    }
}

@Preview(name = "Ruta finalizada · Ruta ordinaria", widthDp = 390, heightDp = 844)
@Composable
private fun RouteFinishedOrdinaryPreview() {
    EcoRutaTheme {
        RouteFinishedScreen(
            state = ActiveRouteUiState(route = SampleOrdinaryRoute, visitedCount = 3, completedSchedule = "06:00–06:52", duration = "52 min"),
            actions = RouteFinishedActions(),
        )
    }
}
