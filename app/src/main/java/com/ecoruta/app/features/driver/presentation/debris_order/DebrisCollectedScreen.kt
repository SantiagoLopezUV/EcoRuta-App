package com.ecoruta.app.features.driver.presentation.debris_order

// Vista composable de confirmación tras marcar como recolectada una orden de escombros
// Muestra el resumen de lo recogido y el aviso de que la orden quedó cerrada

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.driver.presentation.active_route.DriverSuccessHeading
import com.ecoruta.app.features.driver.presentation.orders.DriverOrderStatus
import com.ecoruta.app.features.driver.presentation.orders.DriverStatusLabel

// Acciones disponibles tras confirmar la recolección de escombros
data class DebrisCollectedActions(
    val onBack: () -> Unit = {},
    val onBackToOrders: () -> Unit = {},
    val onTabClick: (DriverTab) -> Unit = {},
)

// Confirmación de la recolección con el resumen de la orden cerrada y el regreso a las órdenes
@Composable
fun DebrisCollectedScreen(state: DebrisOrderUiState, actions: DebrisCollectedActions) {
    val order = state.order ?: return
    EcoRoleTabScreenLayout(
        items = DriverTab.entries,
        selectedTab = DriverTab.ORDERS,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.driver_debris_collected_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.driver_debris_collected_subtitle, order.code), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        DriverSuccessHeading(stringResource(R.string.driver_debris_collected_heading))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_truck), contentDescription = null, tint = EcoTitle, modifier = Modifier.size(24.dp))
                Box(Modifier.weight(1f))
                DriverStatusLabel(order.status)
            }
            Text(order.debrisType, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(order.address, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            Text(order.locality, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            Text(
                listOfNotNull(order.dateLabel, order.collectedAt, order.volume).joinToString(" · "),
                style = EcoTextStyles.FieldLabel,
                color = EcoSubtitle,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.driver_debris_collected_confirmed), style = EcoTextStyles.ButtonText, color = EcoPrimary)
            Text(
                stringResource(R.string.driver_debris_collected_body, order.code, stringResource(DriverOrderStatus.COLLECTED.labelRes)),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        Text(
            stringResource(R.string.driver_debris_collected_saved),
            style = EcoTextStyles.Subtitle,
            color = EcoSubtitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        EcoPrimaryButton(text = stringResource(R.string.driver_back_to_orders), onClick = actions.onBackToOrders)
    }
}

@Preview(name = "Escombros recolectados · Chofer", widthDp = 390, heightDp = 844)
@Composable
private fun DebrisCollectedPreview() {
    EcoRutaTheme {
        DebrisCollectedScreen(
            state = DebrisOrderUiState(order = SampleDebrisOrder.copy(status = DriverOrderStatus.COLLECTED, collectedAt = "10:24")),
            actions = DebrisCollectedActions(),
        )
    }
}
