package com.ecoruta.app.features.circular_economy.presentation.my_pickups

// Vista con las recogidas que el reciclador reservó, agrupadas por estado en pestañas
// Las pendientes se pueden cancelar o escanear y las demás solo muestran su resultado

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.core.navigation.components.recyclerTabs
import com.ecoruta.app.core.ui.components.EcoEmptyStateCard
import com.ecoruta.app.core.ui.components.EcoFilterChip
import com.ecoruta.app.core.ui.components.EcoRoleTabScreenLayout
import com.ecoruta.app.core.ui.components.EcoStatusChip
import com.ecoruta.app.core.ui.theme.EcoAccent
import com.ecoruta.app.core.ui.theme.EcoAccentSoft
import com.ecoruta.app.core.ui.theme.EcoBlack
import com.ecoruta.app.core.ui.theme.EcoBorder
import com.ecoruta.app.core.ui.theme.EcoDimens
import com.ecoruta.app.core.ui.theme.EcoError
import com.ecoruta.app.core.ui.theme.EcoFieldBackground
import com.ecoruta.app.core.ui.theme.EcoGreenSecondary
import com.ecoruta.app.core.ui.theme.EcoPrimary
import com.ecoruta.app.core.ui.theme.EcoRutaTheme
import com.ecoruta.app.core.ui.theme.EcoSubtitle
import com.ecoruta.app.core.ui.theme.EcoTextStyles
import com.ecoruta.app.core.ui.theme.EcoTitle
import com.ecoruta.app.core.ui.theme.EcoWhite
import com.ecoruta.app.features.circular_economy.domain.model.PickupStatus

// Pantalla de Mis recogidas que elige entre cargando, vacío o la lista de la pestaña activa
@Composable
fun RecyclerPickupsScreen(
    state: RecyclerPickupsUiState,
    tabs: List<RecyclerTab>,
    actions: RecyclerPickupsActions,
    modifier: Modifier = Modifier,
) {
    EcoRoleTabScreenLayout(
        items = tabs,
        selectedTab = RecyclerTab.MAP,
        onTabClick = actions.onTabClick,
        onBack = actions.onBack,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(EcoDimens.ChipSpacing)) {
            Text(stringResource(R.string.recycler_pickups_title), style = EcoTextStyles.Title, color = EcoTitle)
            Text(stringResource(R.string.recycler_pickups_subtitle), style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        StatusTabs(selected = state.selectedTab, onSelected = actions.onTabSelected)
        when {
            state.isLoading -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = EcoDimens.CardPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = EcoPrimary) }
            state.visiblePickups.isEmpty() -> EcoEmptyStateCard(
                illustrationRes = R.drawable.ic_list,
                heading = stringResource(state.selectedTab.emptyRes()),
                body = stringResource(R.string.recycler_pickups_empty_body),
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(EcoDimens.CardContentSpacing)) {
                state.visiblePickups.forEach { pickup ->
                    PickupCard(
                        pickup = pickup,
                        onCancel = { actions.onCancelClick(pickup.id) },
                        onScanQr = { actions.onScanQrClick(pickup.id) },
                    )
                }
            }
        }
    }
    if (state.showCancelDialog) {
        CancelPickupDialog(onConfirm = actions.onConfirmCancel, onDismiss = actions.onDismissCancel, isCancelling = state.isCancelling)
    }
}

// Fila de chips con una pestaña por cada estado de la recogida
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusTabs(selected: PickupStatus, onSelected: (PickupStatus) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(EcoDimens.ChipSpacing),
        verticalArrangement = Arrangement.spacedBy(EcoDimens.ChipSpacing),
    ) {
        PickupStatus.entries.forEach { status ->
            EcoFilterChip(text = stringResource(status.tabRes()), selected = status == selected, onClick = { onSelected(status) })
        }
    }
}

// Tarjeta de una recogida; la pendiente lleva borde destacado y las acciones de cancelar y escanear
@Composable
private fun PickupCard(pickup: RecyclerPickupItem, onCancel: () -> Unit, onScanQr: () -> Unit) {
    val isPending = pickup.status == PickupStatus.PENDIENTE
    val shape = RoundedCornerShape(EcoDimens.CardCornerRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(EcoWhite)
            .border(
                width = if (isPending) EcoDimens.HighlightBorderWidth else EcoDimens.BorderWidth,
                color = if (isPending) EcoPrimary else EcoBorder,
                shape = shape,
            )
            .padding(EcoDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(EcoDimens.CardContentSpacing),
    ) {
        PickupStatusLabel(pickup.status)
        Column(verticalArrangement = Arrangement.spacedBy(EcoDimens.LabelValueSpacing)) {
            Text(pickup.title, style = EcoTextStyles.ButtonText, color = EcoTitle)
            Text(pickup.location, style = EcoTextStyles.Subtitle, color = EcoSubtitle)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(EcoDimens.CardContentSpacing)) {
            // El volumen siempre se muestra y el segundo dato depende del estado
            val volumeLabel = if (pickup.status == PickupStatus.COMPLETADO) R.string.recycler_pickups_confirmed_volume else R.string.recycler_delivery_volume
            PickupDetail(label = stringResource(volumeLabel), value = pickup.volume)
            when (pickup.status) {
                PickupStatus.PENDIENTE -> PickupDetail(stringResource(R.string.recycler_delivery_distance), pickup.distance)
                PickupStatus.COMPLETADO -> PickupDetail(stringResource(R.string.recycler_pickups_completed_on), pickup.dateLabel.orEmpty())
                PickupStatus.CANCELADO -> PickupDetail(stringResource(R.string.recycler_pickups_cancelled_on), pickup.dateLabel.orEmpty())
            }
        }
        if (isPending) {
            Row(horizontalArrangement = Arrangement.spacedBy(EcoDimens.CardContentSpacing)) {
                PickupActionButton(
                    text = stringResource(R.string.recycler_pickups_cancel),
                    background = EcoWhite,
                    content = EcoError,
                    borderColor = EcoError,
                    onClick = onCancel,
                )
                PickupActionButton(
                    text = stringResource(R.string.recycler_pickups_scan),
                    background = EcoAccent,
                    content = EcoBlack,
                    onClick = onScanQr,
                )
            }
        }
    }
}

// Etiqueta de estado con los colores propios de cada pestaña
@Composable
private fun PickupStatusLabel(status: PickupStatus) {
    when (status) {
        PickupStatus.PENDIENTE -> EcoStatusChip(stringResource(R.string.recycler_pickups_status_pending), EcoAccentSoft, EcoPrimary)
        PickupStatus.COMPLETADO -> EcoStatusChip(stringResource(R.string.recycler_pickups_status_completed), EcoAccentSoft, EcoGreenSecondary)
        PickupStatus.CANCELADO -> EcoStatusChip(stringResource(R.string.recycler_pickups_status_cancelled), EcoFieldBackground, EcoSubtitle)
    }
}

// Dato de la tarjeta con su etiqueta arriba y el valor debajo
@Composable
private fun RowScope.PickupDetail(label: String, value: String) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(EcoDimens.LabelValueSpacing)) {
        Text(label, style = EcoTextStyles.FieldLabel, color = EcoSubtitle)
        Text(value, style = EcoTextStyles.ButtonText, color = EcoTitle)
    }
}

// Botón de la tarjeta que ocupa la mitad de la fila, con borde opcional
@Composable
private fun RowScope.PickupActionButton(
    text: String,
    background: Color,
    content: Color,
    onClick: () -> Unit,
    borderColor: Color? = null,
) {
    val shape = RoundedCornerShape(EcoDimens.ControlCornerRadius)
    Box(
        modifier = Modifier
            .weight(1f)
            .height(EcoDimens.SecondaryControlHeight)
            .clip(shape)
            .background(background)
            .then(if (borderColor != null) Modifier.border(EcoDimens.BorderWidth, borderColor, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = EcoTextStyles.LinkText, color = content)
    }
}

@StringRes
private fun PickupStatus.tabRes(): Int = when (this) {
    PickupStatus.PENDIENTE -> R.string.recycler_pickups_tab_pending
    PickupStatus.COMPLETADO -> R.string.recycler_pickups_tab_completed
    PickupStatus.CANCELADO -> R.string.recycler_pickups_tab_cancelled
}

@StringRes
private fun PickupStatus.emptyRes(): Int = when (this) {
    PickupStatus.PENDIENTE -> R.string.recycler_pickups_empty_pending
    PickupStatus.COMPLETADO -> R.string.recycler_pickups_empty_completed
    PickupStatus.CANCELADO -> R.string.recycler_pickups_empty_cancelled
}

// Recogidas de ejemplo en Tuluá usadas en las previsualizaciones y en el modo diseño
val SampleRecyclerPickups = listOf(
    RecyclerPickupItem(
        id = "REC-01",
        title = "Cartón seco y plegado",
        location = "Centro, Tuluá",
        volume = "0,3 m³",
        distance = "0,8 km",
        status = PickupStatus.PENDIENTE,
    ),
    RecyclerPickupItem(
        id = "REC-02",
        title = "Botellas PET limpias",
        location = "Barrio Boyacá, Tuluá",
        volume = "0,2 m³",
        distance = "1,2 km",
        status = PickupStatus.PENDIENTE,
    ),
    RecyclerPickupItem(
        id = "REC-03",
        title = "Vidrio de envases",
        location = "La Esperanza, Tuluá",
        volume = "0,4 m³",
        distance = "2,1 km",
        status = PickupStatus.COMPLETADO,
        dateLabel = "5 nov.",
    ),
    RecyclerPickupItem(
        id = "REC-04",
        title = "Cartón corrugado",
        location = "Victoria, Tuluá",
        volume = "0,5 m³",
        distance = "1,6 km",
        status = PickupStatus.CANCELADO,
        dateLabel = "3 nov.",
    ),
)

private val PreviewTabs = recyclerTabs(isCompanyLinked = false)

@Preview(name = "Mis recogidas · Pendientes", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsPendingPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(state = RecyclerPickupsUiState(pickups = SampleRecyclerPickups), tabs = PreviewTabs, actions = RecyclerPickupsActions())
    }
}

@Preview(name = "Mis recogidas · Pendientes vacía", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsPendingEmptyPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(state = RecyclerPickupsUiState(), tabs = PreviewTabs, actions = RecyclerPickupsActions())
    }
}

@Preview(name = "Mis recogidas · Completadas", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsCompletedPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(
            state = RecyclerPickupsUiState(pickups = SampleRecyclerPickups, selectedTab = PickupStatus.COMPLETADO),
            tabs = PreviewTabs,
            actions = RecyclerPickupsActions(),
        )
    }
}

@Preview(name = "Mis recogidas · Completadas vacía", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsCompletedEmptyPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(state = RecyclerPickupsUiState(selectedTab = PickupStatus.COMPLETADO), tabs = PreviewTabs, actions = RecyclerPickupsActions())
    }
}

@Preview(name = "Mis recogidas · Canceladas", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsCancelledPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(
            state = RecyclerPickupsUiState(pickups = SampleRecyclerPickups, selectedTab = PickupStatus.CANCELADO),
            tabs = PreviewTabs,
            actions = RecyclerPickupsActions(),
        )
    }
}

@Preview(name = "Mis recogidas · Canceladas vacía", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsCancelledEmptyPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(state = RecyclerPickupsUiState(selectedTab = PickupStatus.CANCELADO), tabs = PreviewTabs, actions = RecyclerPickupsActions())
    }
}

@Preview(name = "Mis recogidas · Diálogo de cancelar", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsCancelDialogPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(
            state = RecyclerPickupsUiState(pickups = SampleRecyclerPickups, pickupToCancelId = SampleRecyclerPickups.first().id),
            tabs = PreviewTabs,
            actions = RecyclerPickupsActions(),
        )
    }
}

@Preview(name = "Mis recogidas · Cargando", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsLoadingPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(state = RecyclerPickupsUiState(isLoading = true), tabs = PreviewTabs, actions = RecyclerPickupsActions())
    }
}

@Preview(name = "Mis recogidas · Reciclador vinculado", widthDp = 393, heightDp = 852)
@Composable
private fun RecyclerPickupsLinkedPreview() {
    EcoRutaTheme {
        RecyclerPickupsScreen(
            state = RecyclerPickupsUiState(pickups = SampleRecyclerPickups),
            tabs = recyclerTabs(isCompanyLinked = true),
            actions = RecyclerPickupsActions(),
        )
    }
}
