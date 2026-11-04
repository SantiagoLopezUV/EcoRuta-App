package com.ecoruta.app.features.driver.presentation.active_route

// Vista composable de confirmación tras marcar un punto de la ruta como visitado
// Muestra el punto atendido, el progreso del recorrido y el siguiente punto por visitar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.DriverTab
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.driver.presentation.orders.DriverOrderStatus
import com.ecoruta.app.features.driver.presentation.orders.DriverStatusLabel

// Acciones disponibles tras registrar la visita de un punto
data class PointVisitedActions(
    val onBack: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onTabClick: (DriverTab) -> Unit = {},
)

// Confirmación de la visita con el punto atendido, el avance de la ruta y el siguiente punto
@Composable
fun PointVisitedScreen(state: ActiveRouteUiState, actions: PointVisitedActions) {
    val route = state.route ?: return
    val visited = state.lastVisitedPoint ?: return
    val next = state.nextPoint
    EcoRoleTabScreenLayout(
        items = DriverTab.entries,
        selectedTab = DriverTab.MAP,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
        bottomActions = {
            // Al atender el último punto el botón lleva al cierre de la ruta
            EcoPrimaryButton(
                text = stringResource(if (next != null) R.string.driver_continue_next_point else R.string.driver_finish_route),
                onClick = actions.onContinue,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.driver_point_visited_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(
                stringResource(R.string.driver_route_header, route.code, stringResource(route.type.labelRes), route.zone),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        DriverSuccessHeading(stringResource(R.string.driver_point_visited_heading))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(visited.name, style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
                DriverStatusLabel(DriverOrderStatus.ATTENDED)
            }
            Text(
                stringResource(R.string.driver_point_address_city, visited.address, stringResource(route.pointLabelRes(visited))),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
            visited.visitedAt?.let {
                Text(stringResource(R.string.driver_point_visited_at, it), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                pluralStringResource(R.plurals.driver_route_progress, route.points.size, state.visitedCount, route.points.size),
                style = EcoTextStyles.ButtonText,
                color = EcoPrimary,
            )
            RouteProgressBar(total = route.points.size, completed = state.visitedCount)
        }
        next?.let { point ->
            Column(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EcoFieldBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.driver_next_point, state.visitedCount + 1, route.points.size),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoPrimary,
                )
                Text(point.name, style = EcoTextStyles.ButtonText, color = EcoTitle)
                Text(stringResource(R.string.driver_point_address_city, point.address, state.city), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                Text(
                    stringResource(R.string.driver_point_trip, stringResource(route.pointLabelRes(point)), point.distanceAway, point.travelTime),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoSubtitle,
                )
            }
        }
    }
}

// Icono de verificación dentro de un círculo con el mensaje de confirmación debajo
@Composable
internal fun DriverSuccessHeading(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(EcoFieldBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_circle_check), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(32.dp))
        }
        Text(text, style = EcoTextStyles.ButtonText, color = EcoPrimary, textAlign = TextAlign.Center)
    }
}

// Barra segmentada con un tramo por punto de la ruta, resaltando los ya visitados
@Composable
private fun RouteProgressBar(total: Int, completed: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (index < completed) EcoAccent else EcoBorder),
            )
        }
    }
}

@Preview(name = "Punto visitado · Ruta de limpieza", widthDp = 390, heightDp = 844)
@Composable
private fun PointVisitedCleanupPreview() {
    EcoRutaTheme {
        PointVisitedScreen(state = ActiveRouteUiState(route = SampleCleanupRoute, city = "Tuluá", visitedCount = 1), actions = PointVisitedActions())
    }
}

@Preview(name = "Punto visitado · Ruta ordinaria", widthDp = 390, heightDp = 844)
@Composable
private fun PointVisitedOrdinaryPreview() {
    EcoRutaTheme {
        PointVisitedScreen(state = ActiveRouteUiState(route = SampleOrdinaryRoute, city = "Tuluá", visitedCount = 1), actions = PointVisitedActions())
    }
}
