package com.ecoruta.app.features.bulky_waste.presentation.orientation

// Sección de la guía que lista los gestores autorizados para el residuo elegido
// Muestra la carga, la lista con datos de contacto, el aviso sin gestores y el error con reintento

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.bulky_waste.domain.model.RcdManagerOrientation

@Composable
fun AuthorizedManagersSection(
    state: OrientationUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EcoDataLabel(stringResource(R.string.debris_managers_title))
        when (state) {
            OrientationUiState.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EcoAccent, modifier = Modifier.size(32.dp))
            }
            is OrientationUiState.Loaded -> state.managers.forEach { ManagerCard(it) }
            OrientationUiState.Empty -> EcoInfoHint(stringResource(R.string.debris_managers_empty))
            is OrientationUiState.Error -> {
                EcoErrorMessage(text = stringResource(state.messageRes))
                EcoTextLink(text = stringResource(R.string.action_retry), onClick = onRetry)
            }
        }
    }
}

// Tarjeta con el nombre del gestor y los datos de contacto que tenga registrados
@Composable
private fun ManagerCard(manager: RcdManagerOrientation) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(EcoDimens.BorderWidth, EcoBorder, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(manager.name, style = EcoTextStyles.ButtonText, color = EcoTitle)
        listOfNotNull(manager.description, manager.address, manager.phone, manager.operatingHours)
            .filter { it.isNotBlank() }
            .forEach { Text(it, style = EcoTextStyles.Subtitle, color = EcoSubtitle) }
    }
}

@Preview(name = "Gestores autorizados", widthDp = 360, showBackground = true)
@Composable
private fun AuthorizedManagersSectionPreview() {
    EcoRutaTheme {
        AuthorizedManagersSection(
            state = OrientationUiState.Loaded(
                listOf(
                    RcdManagerOrientation(
                        id = "rcd-01",
                        name = "Escombrera Municipal Autorizada",
                        phone = "602 224 1000",
                        address = "Vía Tuluá - Riofrío Km 3",
                        operatingHours = "Lunes a Sábado 6:00 AM - 4:00 PM",
                        description = null,
                    ),
                ),
            ),
            onRetry = {},
        )
    }
}
