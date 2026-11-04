package com.ecoruta.app.features.driver.presentation.orders

// Vista composable de la hoja de trabajo diaria del chofer
// Lista órdenes asignadas para el turno indicando zona, horario y cantidad de puntos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.DriverTab
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

// Acciones disponibles en la pantalla Mis órdenes
data class DriverOrdersActions(
    val onOrderClick: (DriverOrder) -> Unit = {},
    val onViewRouteDetail: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (DriverTab) -> Unit = {},
)

// Hoja de trabajo del día con las órdenes asignadas o el aviso de que no hay ninguna
@Composable
fun DriverOrdersListScreen(state: DriverOrdersUiState, actions: DriverOrdersActions) {
    EcoRoleTabScreenLayout(
        items = DriverTab.entries,
        selectedTab = DriverTab.ORDERS,
        onTabClick = actions.onTabClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.driver_orders_title), style = EcoTextStyles.Title, color = EcoTitle)
            val subtitle = state.dateLabel?.let { stringResource(R.string.driver_orders_subtitle, it, state.city) } ?: state.city
            Text(subtitle, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        if (state.orders.isEmpty()) {
            DriverOrdersEmpty()
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(R.plurals.driver_orders_count, state.orders.size, state.orders.size),
                    style = EcoTextStyles.ButtonText,
                    color = EcoTitle,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    pluralStringResource(R.plurals.driver_orders_attended, state.attendedCount, state.attendedCount),
                    style = EcoTextStyles.FieldLabel,
                    color = EcoGreenSecondary,
                )
            }
            state.orders.forEach { order ->
                DriverOrderCard(order = order, onClick = { actions.onOrderClick(order) })
            }
            if (state.syncFailed) {
                SyncFailedNotice(lastSyncTime = state.lastSyncTime, onRetry = actions.onRetry)
            }
            // El botón principal solo aparece cuando hay alguna ruta entre las órdenes del día
            if (state.orders.any { it.kind != DriverOrderKind.DEBRIS }) {
                EcoPrimaryButton(text = stringResource(R.string.driver_orders_view_route), onClick = actions.onViewRouteDetail)
            }
        }
    }
}

// Tarjeta de una orden con su código, estado, tipo de jornada y resumen de horario y lugar
@Composable
private fun DriverOrderCard(order: DriverOrder, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    // Las rutas pendientes se resaltan con borde porque son las que el chofer debe iniciar
    val highlighted = order.kind != DriverOrderKind.DEBRIS && order.status == DriverOrderStatus.PENDING
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoFieldBackground)
            .then(if (highlighted) Modifier.border(1.dp, EcoGreenSecondary, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(order.code, style = EcoTextStyles.FieldLabel, color = EcoSubtitle, modifier = Modifier.weight(1f))
            DriverStatusLabel(order.status)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_truck), contentDescription = null, tint = EcoTitle, modifier = Modifier.size(22.dp))
            Text(stringResource(order.kind.labelRes), style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = EcoTitle, modifier = Modifier.size(20.dp))
        }
        val points = order.pointCount?.let { pluralStringResource(R.plurals.driver_points_count, it, it) }
        Text(
            listOfNotNull(order.schedule, order.place, points).joinToString(" · "),
            style = EcoTextStyles.Subtitle,
            color = EcoSubtitle,
        )
    }
}

// Texto de estado en verde sin fondo, usado en las tarjetas del módulo chofer
@Composable
internal fun DriverStatusLabel(status: DriverOrderStatus, modifier: Modifier = Modifier) {
    EcoStatusChip(
        text = stringResource(status.labelRes),
        background = Color.Transparent,
        content = EcoGreenSecondary,
        modifier = modifier,
    )
}

// Aviso de que no se pudo sincronizar con la hora de las órdenes guardadas y la opción de reintentar
@Composable
private fun SyncFailedNotice(lastSyncTime: String?, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EcoDimens.ControlCornerRadius))
            .background(EcoFieldBackground)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.driver_orders_sync_failed), style = EcoTextStyles.FieldLabel, color = EcoTitle)
            if (lastSyncTime != null) {
                Text(stringResource(R.string.driver_orders_saved_at, lastSyncTime), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            }
        }
        // Área táctil mínima de 48 dp aunque el texto sea más pequeño
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onRetry)
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.action_retry), style = EcoTextStyles.LinkText, color = EcoTitle)
        }
    }
}

// Estado vacío cuando el chofer no tiene órdenes asignadas para la jornada
@Composable
private fun DriverOrdersEmpty() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 140.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(152.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EcoFieldBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_profile_clipboard), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(80.dp))
        }
        Text(stringResource(R.string.driver_orders_empty_title), style = EcoTextStyles.ButtonText, color = EcoTitle, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.driver_orders_empty_body),
            style = EcoTextStyles.Subtitle,
            color = EcoSubtitle,
            textAlign = TextAlign.Center,
        )
    }
}

// Órdenes ilustrativas del día para el modo diseño y las previsualizaciones
val SampleDriverOrders = listOf(
    DriverOrder("R-013", "R-013", DriverOrderKind.ORDINARY_ROUTE, DriverOrderStatus.PENDING, "06:00–07:00", "Norte", pointCount = 3),
    DriverOrder("R-014", "R-014", DriverOrderKind.CLEANUP_ROUTE, DriverOrderStatus.PENDING, "08:00–09:00", "Centro", pointCount = 3),
    DriverOrder("E-028", "E-028", DriverOrderKind.DEBRIS, DriverOrderStatus.SCHEDULED, "10:00–11:00", "Calle 26 # 25-16"),
)

// Jornada ilustrativa del chofer con el aviso de sincronización fallida
val SampleDriverOrdersState = DriverOrdersUiState(
    dateLabel = "Hoy, 14 de octubre",
    city = "Tuluá",
    orders = SampleDriverOrders,
    syncFailed = true,
    lastSyncTime = "07:45",
)

@Preview(name = "Mis órdenes · Chofer", widthDp = 390, heightDp = 844)
@Composable
private fun DriverOrdersPreview() {
    EcoRutaTheme { DriverOrdersListScreen(state = SampleDriverOrdersState, actions = DriverOrdersActions()) }
}

@Preview(name = "Mis órdenes vacío · Chofer", widthDp = 390, heightDp = 844)
@Composable
private fun DriverOrdersEmptyPreview() {
    EcoRutaTheme { DriverOrdersListScreen(state = DriverOrdersUiState(city = "Tuluá"), actions = DriverOrdersActions()) }
}
