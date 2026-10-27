package com.ecoruta.app.features.reports.presentation.list

// Vista composable del historial de reportes del ciudadano
// Lista tarjetas de reportes con badges: Pendiente, Aprobado, Atendido, Rechazado

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTextLink
import com.ecoruta.app.core.ui.components.GuestServiceActions
import com.ecoruta.app.core.ui.components.GuestServiceContent
import com.ecoruta.app.core.ui.components.GuestServiceScreen
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.reports.presentation.create.Report
import com.ecoruta.app.features.reports.presentation.create.ReportInfoHint
import com.ecoruta.app.features.reports.presentation.create.ReportPhotoPlaceholder
import com.ecoruta.app.features.reports.presentation.create.ReportStatus
import com.ecoruta.app.features.reports.presentation.create.ReportStatusChip
import com.ecoruta.app.features.reports.presentation.create.ReportType
import com.ecoruta.app.features.reports.presentation.create.ReportsTabLayout
import com.ecoruta.app.features.reports.presentation.create.formatReportDate
import com.ecoruta.app.features.reports.presentation.create.labelRes

// Pantalla y acciones del listado de reportes enviados por el ciudadano
data class MyReportsActions(
    val onBack: () -> Unit = {},
    val onReportClick: (Report) -> Unit = {},
    val onCreateReport: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun MyReportsScreen(state: MyReportsUiState, actions: MyReportsActions) {
    ReportsTabLayout(
        selectedTab = CitizenTab.PERFIL,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.report_create), onClick = actions.onCreateReport)
        },
    ) {
        Text(stringResource(R.string.profile_my_reports), style = EcoTextStyles.Titulo, color = EcoTitle)
        when (state) {
            MyReportsUiState.Loading -> LoadingBox()
            MyReportsUiState.Error -> ErrorBox(R.string.report_list_error, actions.onRetry)
            is MyReportsUiState.Loaded -> {
                if (state.reports.isEmpty()) {
                    EmptyReports()
                } else {
                    Text(stringResource(R.string.report_list_description), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
                    if (state.isSample) ReportInfoHint(text = stringResource(R.string.report_sample_data_hint))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.reports.forEach { report ->
                            ReportCard(report = report, onClick = { actions.onReportClick(report) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(report: Report, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.report_view_detail), onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReportPhotoPlaceholder(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EcoWhite),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(report.type.labelRes()), style = EcoTextStyles.Boton, color = EcoTitle, modifier = Modifier.weight(1f))
                ReportStatusChip(report.status)
            }
            Text(report.fullAddress(), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
            Text(formatReportDate(report.createdAtMillis), style = EcoTextStyles.EtiquetaCampo, color = EcoSubtitle)
        }
    }
}

@Composable
private fun EmptyReports() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(EcoFieldBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_file_text), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(48.dp))
        }
        Text(stringResource(R.string.report_empty_title), style = EcoTextStyles.Boton, color = EcoTitle)
        Text(
            stringResource(R.string.report_empty_description),
            style = EcoTextStyles.Subtitulo,
            color = EcoSubtitle,
            textAlign = TextAlign.Center,
        )
    }
}

// Vista de detalle de un reporte ciudadano individual
data class ReportDetailActions(
    val onBack: () -> Unit = {},
    val onCreateReport: () -> Unit = {},
    val onViewOriginal: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun ReportDetailScreen(state: ReportDetailUiState, actions: ReportDetailActions) {
    val report = (state as? ReportDetailUiState.Loaded)?.report
    val canCreateNew = report?.status == ReportStatus.RECHAZADO || report?.status == ReportStatus.DUPLICADO
    val bottomActions: (@Composable ColumnScope.() -> Unit)? = if (canCreateNew) {
        {
            val originalId = report?.originalReportId
            if (report?.status == ReportStatus.DUPLICADO && originalId != null) {
                EcoTextLink(text = stringResource(R.string.report_view_original), onClick = { actions.onViewOriginal(originalId) })
            }
            EcoPrimaryButton(text = stringResource(R.string.report_create_new), onClick = actions.onCreateReport)
        }
    } else {
        null
    }
    ReportsTabLayout(
        selectedTab = CitizenTab.PERFIL,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = bottomActions,
    ) {
        Text(stringResource(R.string.report_detail_title), style = EcoTextStyles.Titulo, color = EcoTitle)
        when (state) {
            ReportDetailUiState.Loading -> LoadingBox()
            ReportDetailUiState.Error -> ErrorBox(R.string.report_detail_error, actions.onRetry)
            ReportDetailUiState.NotFound -> EcoErrorMessage(text = stringResource(R.string.report_not_found))
            is ReportDetailUiState.Loaded -> ReportDetailContent(state.report, state.isSample)
        }
    }
}

@Composable
private fun ReportDetailContent(report: Report, isSample: Boolean) {
    ReportPhotoPlaceholder(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp)),
        iconSize = 32.dp,
        caption = stringResource(if (isSample) R.string.report_sample_photo else R.string.report_photo_pending),
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailLabel(R.string.report_type_title)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(report.type.labelRes()), style = EcoTextStyles.Boton, color = EcoTitle, modifier = Modifier.weight(1f))
            ReportStatusChip(report.status)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailLabel(R.string.report_address_label)
        Text(report.fullAddress(), style = EcoTextStyles.Subtitulo, color = EcoTitle)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { DetailLabel(R.string.report_date_label) }
        Text(formatReportDate(report.createdAtMillis), style = EcoTextStyles.Subtitulo, color = EcoTitle)
    }
    StatusExplanation(report)
}

@Composable
private fun DetailLabel(textRes: Int) {
    Text(stringResource(textRes), style = EcoTextStyles.EtiquetaCampo, color = EcoSubtitle)
}

@Composable
private fun StatusExplanation(report: Report) {
    val (titleRes, bodyRes) = when (report.status) {
        ReportStatus.PENDIENTE -> R.string.report_explain_pending_title to R.string.report_explain_pending
        ReportStatus.APROBADO -> R.string.report_explain_approved_title to R.string.report_explain_approved
        ReportStatus.ATENDIDO -> R.string.report_explain_attended_title to R.string.report_explain_attended
        ReportStatus.RECHAZADO -> R.string.report_explain_rejected_title to R.string.report_explain_rejected
        ReportStatus.DUPLICADO -> R.string.report_explain_duplicate_title to R.string.report_explain_duplicate
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(titleRes), style = EcoTextStyles.Boton, color = EcoTitle)
        Text(report.statusNote ?: stringResource(bodyRes), style = EcoTextStyles.Subtitulo, color = EcoSubtitle)
    }
}

private fun Report.fullAddress(): String = if (area.isBlank()) address else "$address · $area"

@Composable
private fun LoadingBox() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = EcoPrimary) }
}

@Composable
private fun ErrorBox(messageRes: Int, onRetry: () -> Unit) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EcoErrorMessage(text = stringResource(messageRes))
        EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = onRetry)
    }
}

// Vista informativa de reportes para visitantes sin sesión activa
data class ReportsGuestActions(
    val onBack: () -> Unit = {},
    val onLogin: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun ReportsGuestScreen(actions: ReportsGuestActions) {
    GuestServiceScreen(
        content = GuestServiceContent(
            banner = R.string.report_guest_banner,
            lockedSection = R.string.report_guest_my_reports_locked,
            title = R.string.service_reports_title,
            description = R.string.report_guest_description,
            benefits = listOf(R.string.report_guest_can_1, R.string.report_guest_can_2, R.string.report_guest_can_3),
        ),
        actions = GuestServiceActions(onBack = actions.onBack, onLogin = actions.onLogin, onTabClick = actions.onTabClick),
    )
}

private val previewReports = listOf(
    Report(
        id = "rep-1",
        type = ReportType.CONTENEDOR_LLENO,
        address = "Cra. 22E #6-40",
        area = "Torobajo",
        status = ReportStatus.APROBADO,
        createdAtMillis = 1_744_800_000_000L,
    ),
    Report(
        id = "rep-2",
        type = ReportType.BOTADERO_CLANDESTINO,
        address = "Cl. 18 #34-12",
        area = "Maridíaz",
        status = ReportStatus.PENDIENTE,
        createdAtMillis = 1_744_900_000_000L,
    ),
)

@Preview(name = "Mis Reportes", widthDp = 393, heightDp = 852)
@Composable
private fun MyReportsPreview() {
    EcoRutaTheme {
        MyReportsScreen(
            state = MyReportsUiState.Loaded(reports = previewReports),
            actions = MyReportsActions(),
        )
    }
}

@Preview(name = "Detalle de Reporte", widthDp = 393, heightDp = 852)
@Composable
private fun ReportDetailPreview() {
    EcoRutaTheme {
        ReportDetailScreen(
            state = ReportDetailUiState.Loaded(report = previewReports.first(), isSample = false),
            actions = ReportDetailActions(),
        )
    }
}

@Preview(name = "Reportes sin sesión", widthDp = 393, heightDp = 852)
@Composable
private fun ReportsGuestPreview() {
    EcoRutaTheme {
        ReportsGuestScreen(actions = ReportsGuestActions())
    }
}
