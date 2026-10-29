package com.ecoruta.app.features.routes.presentation.map

// Pantalla para elegir el barrio y consultar sus rutas de recolección

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoTextField
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Pantalla de búsqueda y selección de barrio para consultar rutas de recolección
data class NeighborhoodSelectionActions(
    val onBack: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onNeighborhoodSelected: (String) -> Unit = {},
    val onContinue: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun NeighborhoodSelectionScreen(state: NeighborhoodSelectionUiState, actions: NeighborhoodSelectionActions) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.MAP,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = actions.onContinue,
                enabled = !state.isLoading,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.neighborhood_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.neighborhood_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        EcoTextField(
            value = state.query,
            onValueChange = actions.onQueryChange,
            label = stringResource(R.string.registration_neighborhood_label),
            placeholder = stringResource(R.string.neighborhood_search_placeholder),
            leadingIcon = {
                Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
            },
        )
        Text(stringResource(R.string.neighborhood_list_label), style = EcoTextStyles.FieldLabel, color = EcoTitle)
        when {
            state.isLoading -> CenteredLoading()
            state.loadError != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EcoErrorMessage(text = stringResource(state.loadError))
                EcoTextLink(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            else -> {
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.neighborhood_sample_hint))
                val visible = state.visibleNeighborhoods
                if (visible.isEmpty()) {
                    Text(stringResource(R.string.neighborhood_no_results), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visible.forEach { neighborhood ->
                        NeighborhoodRow(
                            neighborhood = neighborhood,
                            selected = neighborhood.id == state.selectedId,
                            onClick = { actions.onNeighborhoodSelected(neighborhood.id) },
                        )
                    }
                }
            }
        }
        state.selectionError?.let { EcoErrorMessage(text = stringResource(it)) }
    }
}

@Composable
private fun NeighborhoodRow(neighborhood: Neighborhood, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (selected) EcoFieldBackground else EcoWhite)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) EcoPrimary else EcoBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_map_pin), contentDescription = null, tint = if (selected) EcoPrimary else EcoSubtitle, modifier = Modifier.size(20.dp))
        Text(neighborhood.name, style = EcoTextStyles.LinkText, color = EcoTitle, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) EcoPrimary else EcoWhite)
                .border(1.5.dp, if (selected) EcoPrimary else EcoSubtitle, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(painterResource(R.drawable.ic_checkbox_check), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Preview(name = "Selección de Barrio", widthDp = 393, heightDp = 852)
@Composable
private fun NeighborhoodSelectionPreview() {
    EcoRutaTheme {
        val list = listOf(
            Neighborhood("la-esperanza", "La Esperanza"),
            Neighborhood("torobajo", "Torobajo"),
            Neighborhood("maridiaz", "Maridíaz"),
        )
        NeighborhoodSelectionScreen(
            state = NeighborhoodSelectionUiState(
                isLoading = false,
                neighborhoods = list,
                selectedId = "torobajo",
            ),
            actions = NeighborhoodSelectionActions(),
        )
    }
}
