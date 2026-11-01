package com.ecoruta.app.features.bulky_waste.presentation.tracking

// Vista composable de seguimiento y línea de tiempo de escombros
// Visualiza el avance paso a paso: Pendiente, Cotizado, Programado y Recolectado

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.ecoruta.app.core.ui.components.EcoDataLabel
import com.ecoruta.app.core.ui.components.EcoErrorMessage
import com.ecoruta.app.core.ui.components.EcoInfoHint
import com.ecoruta.app.core.ui.components.EcoPrimaryButton
import com.ecoruta.app.core.ui.components.EcoTabScreenLayout
import com.ecoruta.app.core.ui.components.GuestServiceActions
import com.ecoruta.app.core.ui.components.GuestServiceContent
import com.ecoruta.app.core.ui.components.GuestServiceScreen
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisRequest
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatusChip
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType
import com.ecoruta.app.features.bulky_waste.presentation.schedule.formatCop
import com.ecoruta.app.features.bulky_waste.presentation.schedule.formatShortDate
import com.ecoruta.app.features.bulky_waste.presentation.schedule.iconRes
import com.ecoruta.app.features.bulky_waste.presentation.schedule.labelRes

// Pantalla de seguimiento detallado y línea de progreso para una solicitud de retiro

data class DebrisTrackingActions(
    val onBack: () -> Unit = {},
    val onCancelRequest: () -> Unit = {},
    val onApproveQuote: () -> Unit = {},
    val onGoHome: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun DebrisTrackingScreen(
    state: DebrisTrackingUiState,
    isWorking: Boolean,
    showGoHome: Boolean,
    showNotSentHint: Boolean,
    actions: DebrisTrackingActions,
) {
    val request = (state as? DebrisTrackingUiState.Loaded)?.request
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    val canCancel = request?.status == DebrisStatus.PENDIENTE || request?.status == DebrisStatus.COTIZADO
    val bottomActions: (@Composable ColumnScope.() -> Unit)? =
        if (request == null && !showGoHome) {
            null
        } else {
            {
                if (request?.status == DebrisStatus.COTIZADO) {
                    EcoPrimaryButton(
                        text = stringResource(R.string.debris_approve_quote),
                        onClick = actions.onApproveQuote,
                        isLoading = isWorking,
                        height = 48.dp,
                    )
                }
                if (canCancel) {
                    OutlinedButton(
                        onClick = { confirmCancel = true },
                        enabled = !isWorking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EcoTitle),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = EcoWhite, contentColor = EcoTitle),
                    ) { Text(stringResource(R.string.debris_cancel_request), style = EcoTextStyles.ButtonText) }
                }
                if (showGoHome) {
                    EcoPrimaryButton(
                        text = stringResource(R.string.report_back_home),
                        onClick = actions.onGoHome,
                        trailingIconRes = R.drawable.ic_arrow_right,
                    )
                }
            }
        }

    EcoTabScreenLayout(
        selectedTab = if (showGoHome) CitizenTab.HOME else CitizenTab.PROFILE,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = bottomActions,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.debris_tracking_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.debris_tracking_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        when (state) {
            DebrisTrackingUiState.Loading -> LoadingBox()
            DebrisTrackingUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(R.string.debris_tracking_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            DebrisTrackingUiState.NotFound -> EcoErrorMessage(text = stringResource(R.string.debris_not_found))
            is DebrisTrackingUiState.Loaded -> {
                if (state.isSample) EcoInfoHint(text = stringResource(R.string.debris_sample_data_hint))
                if (showNotSentHint) EcoInfoHint(text = stringResource(R.string.debris_design_mode_sent_hint))
                TrackingHeader(state.request)
                if (state.request.status == DebrisStatus.CANCELADO) CancelledBox() else Timeline(state.request.status)
                CostCard(state.request)
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.debris_cancel_title), style = EcoTextStyles.ButtonText, color = EcoTitle) },
            text = { Text(stringResource(R.string.debris_cancel_message), style = EcoTextStyles.Subtitle, color = EcoSubtitle) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    actions.onCancelRequest()
                }) { Text(stringResource(R.string.debris_cancel_confirm), style = EcoTextStyles.ButtonText, color = EcoError) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) {
                    Text(stringResource(R.string.debris_cancel_dismiss), style = EcoTextStyles.ButtonText, color = EcoPrimary)
                }
            },
            containerColor = EcoWhite,
        )
    }
}

@Composable
private fun TrackingHeader(request: DebrisRequest) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(request.type.labelRes()), style = EcoTextStyles.ButtonText, color = EcoTitle, modifier = Modifier.weight(1f))
            DebrisStatusChip(request.status)
        }
        Text(stringResource(R.string.debris_address_in_city, request.address), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        Text(
            text = request.scheduledDateMillis?.let { stringResource(R.string.debris_scheduled_for, formatShortDate(it)) }
                ?: stringResource(R.string.debris_preferred_for, formatShortDate(request.preferredDateMillis)),
            style = EcoTextStyles.FieldLabel,
            color = EcoSubtitle,
        )
    }
}

private val TimelineSteps = listOf(
    Triple(DebrisStatus.PENDIENTE, R.string.debris_status_pending, R.string.debris_timeline_pending),
    Triple(DebrisStatus.COTIZADO, R.string.debris_status_quoted, R.string.debris_timeline_quoted),
    Triple(DebrisStatus.PROGRAMADO, R.string.debris_status_scheduled, R.string.debris_timeline_scheduled),
    Triple(DebrisStatus.RECOLECTADO, R.string.debris_status_collected, R.string.debris_timeline_collected),
)

@Composable
private fun Timeline(status: DebrisStatus) {
    val currentIndex = TimelineSteps.indexOfFirst { it.first == status }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TimelineSteps.forEachIndexed { index, (_, titleRes, bodyRes) ->
            val isDone = index < currentIndex || status == DebrisStatus.RECOLECTADO
            val isCurrent = index == currentIndex && status != DebrisStatus.RECOLECTADO
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> EcoAccent
                                isCurrent -> EcoPrimary
                                else -> EcoWhite
                            },
                        )
                        .border(1.dp, if (isDone || isCurrent) Color.Transparent else EcoBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) {
                        Icon(painterResource(R.drawable.ic_checkbox_check), contentDescription = null, tint = EcoWhite, modifier = Modifier.size(16.dp))
                    } else if (isCurrent) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EcoWhite),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isCurrent) stringResource(R.string.debris_timeline_current, stringResource(titleRes)) else stringResource(titleRes),
                        style = EcoTextStyles.ButtonText,
                        color = if (isDone || isCurrent) EcoTitle else EcoSubtitle,
                    )
                    Text(stringResource(bodyRes), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
                }
            }
        }
    }
}

@Composable
private fun CancelledBox() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, EcoBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.debris_cancelled_title), style = EcoTextStyles.ButtonText, color = EcoTitle)
        Text(stringResource(R.string.debris_cancelled_body), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
    }
}

@Composable
private fun CostCard(request: DebrisRequest) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CostRow(stringResource(R.string.debris_cost_label), request.costText(), emphasized = true)
        CostRow(
            stringResource(R.string.debris_pickup_date_label),
            request.scheduledDateMillis?.let(::formatShortDate) ?: stringResource(R.string.debris_to_be_confirmed),
        )
        CostRow(stringResource(R.string.debris_driver_label), request.driverName ?: stringResource(R.string.debris_no_driver))
    }
}

@Composable
private fun CostRow(label: String, value: String, emphasized: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EcoDataLabel(label, Modifier.weight(1f))
        Text(value, style = if (emphasized) EcoTextStyles.ButtonText else EcoTextStyles.Subtitle, color = EcoTitle)
    }
}

@Composable
private fun DebrisRequest.costText(): String = when {
    status == DebrisStatus.CANCELADO -> stringResource(R.string.debris_no_charge)
    costCop != null -> formatCop(costCop)
    else -> stringResource(R.string.debris_to_be_quoted)
}

@Composable
private fun LoadingBox() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) { CircularProgressIndicator(color = EcoPrimary) }
}

// Pantalla que lista el historial de solicitudes de retiro de escombros del ciudadano

data class MyDebrisRequestsActions(
    val onBack: () -> Unit = {},
    val onRequestClick: (DebrisRequest) -> Unit = {},
    val onNewRequest: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onTabClick: (CitizenTab) -> Unit = {},
)

@Composable
fun MyDebrisRequestsScreen(state: MyDebrisRequestsUiState, actions: MyDebrisRequestsActions) {
    EcoTabScreenLayout(
        selectedTab = CitizenTab.PROFILE,
        onBack = actions.onBack,
        onTabClick = actions.onTabClick,
        bottomActions = {
            EcoPrimaryButton(text = stringResource(R.string.debris_request_title), onClick = actions.onNewRequest)
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.profile_my_debris_requests), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.debris_list_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        when (state) {
            MyDebrisRequestsUiState.Loading -> LoadingBox()
            MyDebrisRequestsUiState.Error -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EcoErrorMessage(text = stringResource(R.string.debris_list_error))
                EcoPrimaryButton(text = stringResource(R.string.action_retry), onClick = actions.onRetry)
            }
            is MyDebrisRequestsUiState.Loaded ->
                if (state.requests.isEmpty()) {
                    EmptyRequests()
                } else {
                    if (state.isSample) EcoInfoHint(text = stringResource(R.string.debris_sample_data_hint))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.requests.forEach { request ->
                            RequestCard(request = request, onClick = { actions.onRequestClick(request) })
                        }
                    }
                }
        }
    }
}

@Composable
private fun RequestCard(request: DebrisRequest, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EcoFieldBackground)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.debris_view_tracking), onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(EcoWhite),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(request.type.iconRes()), contentDescription = null, tint = EcoPrimary, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(request.type.labelRes()), style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(formatShortDate(request.createdAtMillis), style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DebrisStatusChip(request.status)
            Text(request.costText(), style = EcoTextStyles.Subtitle, color = EcoTitle)
        }
    }
}

@Composable
private fun EmptyRequests() {
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
            Icon(painterResource(R.drawable.ic_bricks), contentDescription = null, tint = EcoGreenSecondary, modifier = Modifier.size(44.dp))
        }
        Text(stringResource(R.string.debris_empty_title), style = EcoTextStyles.ButtonText, color = EcoTitle)
        Text(stringResource(R.string.debris_empty_description), style = EcoTextStyles.Subtitle, color = EcoSubtitle, textAlign = TextAlign.Center)
    }
}

// Vista informativa del servicio de retiro de residuos especiales para usuarios sin sesión

@Composable
fun DebrisGuestScreen(actions: GuestServiceActions) {
    GuestServiceScreen(
        content = GuestServiceContent(
            banner = R.string.debris_guest_banner,
            lockedSection = R.string.debris_guest_locked,
            title = R.string.debris_guest_title,
            description = R.string.debris_guest_description,
            benefits = listOf(R.string.debris_guest_can_1, R.string.debris_guest_can_2, R.string.debris_guest_can_3),
        ),
        actions = actions,
    )
}

private val previewDebrisRequests = listOf(
    DebrisRequest(
        id = "rcd-1",
        type = DebrisType.RENOVATION_DEBRIS,
        description = "8 lonas de escombros de obra menor en cocina.",
        address = "Cra. 22E #6-40 · Torobajo",
        preferredDateMillis = 1_745_000_000_000L,
        scheduledDateMillis = 1_745_000_000_000L,
        status = DebrisStatus.COTIZADO,
        costCop = 45_000L,
        driverName = "Héctor Narváez",
        createdAtMillis = 1_744_800_000_000L,
    ),
    DebrisRequest(
        id = "rcd-2",
        type = DebrisType.FURNITURE,
        description = "Sofá de tres puestos.",
        address = "Cra. 22E #6-40 · Torobajo",
        preferredDateMillis = 1_744_500_000_000L,
        scheduledDateMillis = 1_744_500_000_000L,
        status = DebrisStatus.RECOLECTADO,
        costCop = 25_000L,
        driverName = "Carlos Benavides",
        createdAtMillis = 1_744_400_000_000L,
    ),
)

@Preview(name = "Seguimiento de Solicitud RCD", widthDp = 393, heightDp = 900)
@Composable
private fun DebrisTrackingPreview() {
    EcoRutaTheme {
        DebrisTrackingScreen(
            state = DebrisTrackingUiState.Loaded(request = previewDebrisRequests.first(), isSample = false),
            isWorking = false,
            showGoHome = false,
            showNotSentHint = false,
            actions = DebrisTrackingActions(),
        )
    }
}

@Preview(name = "Mis Solicitudes de Escombros", widthDp = 393, heightDp = 852)
@Composable
private fun MyDebrisRequestsPreview() {
    EcoRutaTheme {
        MyDebrisRequestsScreen(
            state = MyDebrisRequestsUiState.Loaded(requests = previewDebrisRequests, isSample = false),
            actions = MyDebrisRequestsActions(),
        )
    }
}

@Preview(name = "Residuos Especiales sin sesión", widthDp = 393, heightDp = 852)
@Composable
private fun DebrisGuestPreview() {
    EcoRutaTheme {
        DebrisGuestScreen(actions = GuestServiceActions())
    }
}
