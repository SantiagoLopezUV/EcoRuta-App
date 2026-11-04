package com.ecoruta.app.features.driver.presentation.debris_order

// Vista composable de la orden de escombros que el chofer debe recoger en un domicilio
// Muestra la dirección, el tipo y volumen del escombro, una foto de referencia y la confirmación

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.DriverTab
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.features.driver.presentation.orders.DriverStatusLabel

// Acciones disponibles en la orden de escombros
data class DebrisOrderActions(
    val onBack: () -> Unit = {},
    val onMarkCollected: () -> Unit = {},
    val onTabClick: (DriverTab) -> Unit = {},
)

// Detalle de la orden de escombros con la confirmación de que ya se retiraron del domicilio
@Composable
fun DebrisOrderScreen(state: DebrisOrderUiState, actions: DebrisOrderActions) {
    val order = state.order ?: return
    EcoRoleTabScreenLayout(
        items = DriverTab.entries,
        selectedTab = DriverTab.ORDERS,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.driver_debris_order_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(
                stringResource(R.string.driver_debris_order_subtitle, order.code, order.dateLabel, order.schedule),
                style = EcoTextStyles.Subtitle,
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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_truck), contentDescription = null, tint = EcoTitle, modifier = Modifier.size(24.dp))
                Box(Modifier.weight(1f))
                DriverStatusLabel(order.status)
            }
            Text(stringResource(R.string.driver_debris_address_label), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            Text(order.address, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(order.locality, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            Text(stringResource(R.string.driver_debris_type_label), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
            Text(order.debrisType, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(
                stringResource(
                    R.string.driver_debris_volume,
                    order.volume,
                    stringResource(if (order.hasHazardousWaste) R.string.driver_debris_hazardous else R.string.driver_debris_not_hazardous),
                ),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        // Recuadro de referencia con el icono de escombros mientras no se cargue la foto real
        val photoDescription = stringResource(R.string.driver_debris_photo_description)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(EcoFieldBackground)
                .semantics { contentDescription = photoDescription },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_bricks), contentDescription = null, tint = EcoSubtitle, modifier = Modifier.size(56.dp))
        }
        Text(stringResource(R.string.driver_debris_confirm_hint), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        EcoPrimaryButton(
            text = stringResource(R.string.driver_debris_mark_collected),
            onClick = actions.onMarkCollected,
            isLoading = state.isConfirming,
        )
    }
}

@Preview(name = "Orden de escombros · Chofer", widthDp = 390, heightDp = 844)
@Composable
private fun DebrisOrderPreview() {
    EcoRutaTheme { DebrisOrderScreen(state = DebrisOrderUiState(order = SampleDebrisOrder), actions = DebrisOrderActions()) }
}
