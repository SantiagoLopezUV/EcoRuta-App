package com.ecoruta.app.features.bulky_waste.presentation.schedule

// Confirmación de solicitud de retiro de escombros enviada

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

@Composable
fun DebrisSentScreen(
    request: DebrisRequest,
    sentWithoutBackend: Boolean,
    onViewTracking: () -> Unit,
    onGoHome: () -> Unit,
    onTabClick: (CitizenTab) -> Unit,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onGoHome)
    }
    EcoTabScreenLayout(
        selectedTab = CitizenTab.HOME,
        onBack = onGoHome,
        onTabClick = onTabClick,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.debris_view_tracking),
                onClick = onViewTracking,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.debris_sent_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.debris_sent_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(EcoFieldBackground)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(EcoAccent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check_large), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(40.dp))
            }
            Text(stringResource(R.string.debris_sent_motto), style = EcoTextStyles.ButtonText, color = EcoTitle, textAlign = TextAlign.Center)
            DebrisStatusChip(DebrisStatus.PENDIENTE)
        }
        DebrisSummaryCard(request)
        EcoInfoHint(text = stringResource(R.string.debris_cost_pending_hint))
        if (sentWithoutBackend) EcoInfoHint(text = stringResource(R.string.debris_design_mode_sent_hint))
    }
}

@Composable
internal fun DebrisSummaryCard(request: DebrisRequest) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryItem(stringResource(R.string.debris_type_label), stringResource(request.type.labelRes()))
        SummaryItem(stringResource(R.string.debris_location_label), stringResource(R.string.debris_address_in_city, request.address))
        SummaryItem(stringResource(R.string.debris_preferred_not_confirmed), formatLongDate(request.preferredDateMillis))
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EcoDataLabel(label)
        Text(value, style = EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

@Preview(name = "Solicitud Enviada", widthDp = 393, heightDp = 852)
@Composable
private fun DebrisSentPreview() {
    EcoRutaTheme {
        DebrisSentScreen(
            request = DebrisRequest(
                id = "rcd-1",
                type = DebrisType.MUEBLES,
                description = "Sofá de tres puestos",
                address = "Cra. 22E #6-40 · Torobajo",
                preferredDateMillis = DayMillis.addDays(previewWeekStart, 3),
                status = DebrisStatus.PENDIENTE,
                createdAtMillis = previewBaseDay,
            ),
            sentWithoutBackend = false,
            onViewTracking = {},
            onGoHome = {},
            onTabClick = {},
        )
    }
}
