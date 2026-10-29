package com.ecoruta.app.features.reports.presentation.create

// Confirmación que se muestra al enviar un reporte ciudadano

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.EcoTopBar
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite

// Pantalla de confirmación tras enviar el reporte ciudadano
@Composable
fun ReportSentScreen(
    sentWithoutBackend: Boolean,
    onViewMyReports: () -> Unit,
    onGoHome: () -> Unit,
) {
    if (!LocalInspectionMode.current) {
        BackHandler(onBack = onGoHome)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EcoWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
    ) {
        EcoTopBar(onBackClick = onGoHome)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(EcoPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_check_large), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(56.dp))
            }
            Text(stringResource(R.string.report_sent_title), style = EcoTextStyles.Title, color = EcoTitle, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.report_sent_description),
                style = EcoTextStyles.Subtitle,
                color = EcoSubtitle,
                textAlign = TextAlign.Center,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.report_current_status), style = EcoTextStyles.FieldLabel, color = EcoTitle)
                ReportStatusChip(ReportStatus.PENDIENTE)
            }
            if (sentWithoutBackend) {
                ReportInfoHint(text = stringResource(R.string.report_design_mode_sent_hint))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            EcoPrimaryButton(
                text = stringResource(R.string.report_view_my_reports),
                onClick = onViewMyReports,
                trailingIconRes = R.drawable.ic_arrow_right,
            )
            EcoTextLink(text = stringResource(R.string.report_back_home), onClick = onGoHome)
        }
    }
}

@Preview(name = "Reporte Enviado", widthDp = 393, heightDp = 852)
@Composable
private fun ReportSentPreview() {
    EcoRutaTheme {
        ReportSentScreen(
            sentWithoutBackend = false,
            onViewMyReports = {},
            onGoHome = {},
        )
    }
}
