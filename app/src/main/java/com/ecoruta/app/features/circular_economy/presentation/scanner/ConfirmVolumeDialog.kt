package com.ecoruta.app.features.circular_economy.presentation.scanner

// Pantalla composable para selección y confirmación del volumen real recibido
// Escalas: Cartón seco y plegado, Bolsa pequeña <2kg, Bolsa grande 2-10kg, Costal >10kg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.core.navigation.components.recyclerTabs
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.SampleRecyclerDeliveries

// Pantalla para elegir la categoría de volumen recibido y confirmar la entrega
@Composable
fun ConfirmVolumeScreen(
    state: RecyclerScannerUiState,
    onVolumeSelected: (DeliveredVolume) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onTabClick: (RecyclerTab) -> Unit,
    tabs: List<RecyclerTab>,
    modifier: Modifier = Modifier,
) {
    EcoRoleTabScreenLayout(
        items = tabs,
        selectedTab = RecyclerTab.SCANNER,
        onTabClick = onTabClick,
        onBack = onBack,
        modifier = modifier,
        bottomActions = {
            Text(stringResource(R.string.recycler_volume_note), style = EcoTextStyles.Subtitle, color = EcoSubtitle, modifier = Modifier.fillMaxWidth())
            EcoPrimaryButton(text = stringResource(R.string.recycler_volume_confirm), onClick = onConfirm, height = 56.dp)
        },
    ) {
        ScreenTitle(
            title = stringResource(R.string.recycler_volume_title),
            description = stringResource(R.string.recycler_volume_description),
        )
        state.delivery?.let { delivery ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EcoFieldBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.recycler_volume_material_label), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
                Text(delivery.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
                Text(stringResource(R.string.recycler_volume_material_place, delivery.area), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
            }
        }
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeliveredVolume.entries.forEach { volume ->
                VolumeOption(
                    volume = volume,
                    selected = volume == state.selectedVolume,
                    onClick = { onVolumeSelected(volume) },
                )
            }
        }
    }
}

// Opción de volumen con nombre, rango de peso e indicador circular de selección
@Composable
private fun VolumeOption(volume: DeliveredVolume, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(EcoFieldBackground)
            .border(if (selected) 2.dp else 1.dp, if (selected) EcoPrimary else EcoFieldBackground, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(volume.labelRes()), style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(
                stringResource(if (selected) R.string.recycler_volume_selected else volume.rangeRes()),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
            )
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) EcoPrimary else EcoWhite)
                .border(1.5.dp, if (selected) EcoPrimary else EcoSubtitle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_checkbox_check), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun DeliveredVolume.labelRes(): Int = when (this) {
    DeliveredVolume.SMALL_BAG -> R.string.volume_small_bag
    DeliveredVolume.LARGE_BAG -> R.string.recycler_volume_large_bag
    DeliveredVolume.SACK -> R.string.recycler_volume_sack
}

private fun DeliveredVolume.rangeRes(): Int = when (this) {
    DeliveredVolume.SMALL_BAG -> R.string.recycler_volume_small_range
    DeliveredVolume.LARGE_BAG -> R.string.recycler_volume_large_range
    DeliveredVolume.SACK -> R.string.recycler_volume_sack_range
}

@Preview(name = "Confirmar volumen entregado", widthDp = 393, heightDp = 852)
@Composable
private fun ConfirmVolumePreview() {
    EcoRutaTheme {
        ConfirmVolumeScreen(
            state = RecyclerScannerUiState(delivery = SampleRecyclerDeliveries.first()),
            onVolumeSelected = {},
            onConfirm = {},
            onBack = {},
            onTabClick = {},
            tabs = recyclerTabs(isCompanyLinked = false),
        )
    }
}
