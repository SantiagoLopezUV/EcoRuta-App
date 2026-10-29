package com.ecoruta.app.features.reports.presentation.create

// Paso 2 del reporte: selección del tipo de afectación

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoSelectionCard
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle

// Segundo paso del formulario de reporte para seleccionar el tipo de afectación
data class ReportTypeActions(
    val onTypeSelected: (ReportType) -> Unit = {},
    val onSpecialWasteClick: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun ReportTypeScreen(state: NewReportUiState, actions: ReportTypeActions) {
    ReportStepLayout(
        step = 2,
        stageLabel = stringResource(R.string.report_stage_type),
        title = stringResource(R.string.report_type_title),
        description = stringResource(R.string.report_type_description),
        onBack = actions.onBack,
        bottomActions = {
            EcoPrimaryButton(
                text = stringResource(R.string.action_continue),
                onClick = actions.onContinue,
                height = 48.dp,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
        },
    ) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EcoSelectionCard(
                title = stringResource(R.string.report_type_full_container),
                description = stringResource(R.string.report_type_full_container_description),
                iconRes = R.drawable.ic_trash,
                iconSize = 24.dp,
                selected = state.type == ReportType.CONTENEDOR_LLENO,
                onClick = { actions.onTypeSelected(ReportType.CONTENEDOR_LLENO) },
            )
            EcoSelectionCard(
                title = stringResource(R.string.report_type_illegal_dump),
                description = stringResource(R.string.report_type_illegal_dump_description),
                iconRes = R.drawable.ic_report_alert,
                iconSize = 24.dp,
                selected = state.type == ReportType.BOTADERO_CLANDESTINO,
                onClick = { actions.onTypeSelected(ReportType.BOTADERO_CLANDESTINO) },
            )
        }
        state.typeError?.let { EcoErrorMessage(text = stringResource(it)) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EcoFieldBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.report_special_waste_hint), style = EcoTextStyles.Subtitle, color = EcoTitle)
            }
            EcoTextLink(
                text = stringResource(R.string.report_special_waste_link),
                onClick = actions.onSpecialWasteClick,
                modifier = Modifier.padding(start = 30.dp),
            )
        }
    }
}

@Preview(name = "Paso 2 Tipo", widthDp = 393, heightDp = 852)
@Composable
private fun ReportTypePreview() {
    EcoRutaTheme {
        ReportTypeScreen(
            state = NewReportUiState(type = ReportType.CONTENEDOR_LLENO),
            actions = ReportTypeActions(),
        )
    }
}
