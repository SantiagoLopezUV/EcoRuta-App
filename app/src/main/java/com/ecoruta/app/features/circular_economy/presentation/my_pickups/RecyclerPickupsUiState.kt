package com.ecoruta.app.features.circular_economy.presentation.my_pickups

// Estado inmutable de la lista de recogidas del reciclador
// Pestaña de estado elegida, recogidas listas para mostrar y confirmación de cancelación

import com.ecoruta.app.core.navigation.components.RecyclerTab
import com.ecoruta.app.features.circular_economy.domain.model.PickupStatus

// Recogida con los textos ya formateados para la tarjeta
// El volumen es el estimado o el confirmado y la fecha es la de completado o cancelado según el estado
data class RecyclerPickupItem(
    val id: String,
    val title: String,
    val location: String,
    val volume: String,
    val distance: String,
    val status: PickupStatus,
    val dateLabel: String? = null,
)

// Estado visual de Mis recogidas con la pestaña activa, la carga y el diálogo de cancelar
data class RecyclerPickupsUiState(
    val pickups: List<RecyclerPickupItem> = emptyList(),
    val selectedTab: PickupStatus = PickupStatus.PENDIENTE,
    val isLoading: Boolean = false,
    val pickupToCancelId: String? = null,
    val isCancelling: Boolean = false,
) {
    // Recogidas que pertenecen a la pestaña de estado elegida
    val visiblePickups: List<RecyclerPickupItem>
        get() = pickups.filter { it.status == selectedTab }

    // El diálogo de cancelar se muestra mientras haya una recogida elegida para cancelar
    val showCancelDialog: Boolean
        get() = pickupToCancelId != null
}

// Acciones disponibles en Mis recogidas
data class RecyclerPickupsActions(
    val onTabSelected: (PickupStatus) -> Unit = {},
    val onCancelClick: (String) -> Unit = {},
    val onConfirmCancel: () -> Unit = {},
    val onDismissCancel: () -> Unit = {},
    val onScanQrClick: (String) -> Unit = {},
    val onBack: () -> Unit = {},
    val onTabClick: (RecyclerTab) -> Unit = {},
)
