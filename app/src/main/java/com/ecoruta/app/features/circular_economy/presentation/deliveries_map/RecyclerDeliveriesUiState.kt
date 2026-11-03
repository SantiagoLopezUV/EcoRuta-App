package com.ecoruta.app.features.circular_economy.presentation.deliveries_map

// Estado inmutable del mapa de ofertas de reciclaje
// Lista de solicitudes cercanas, filtro de material activo, reclamos y errores

import com.ecoruta.app.features.circular_economy.presentation.publish.MaterialType
import com.ecoruta.app.features.circular_economy.presentation.publish.PublicationStatus

// Entrega de material publicada por un ciudadano y visible para el reciclador en el mapa
data class RecyclerDelivery(
    val id: String,
    val title: String,
    val area: String,
    val zone: String,
    val volume: String,
    val distance: String,
    val material: MaterialType,
    val status: PublicationStatus,
    // Posición relativa del marcador dentro del mapa ilustrativo, entre 0 y 1
    val mapX: Float,
    val mapY: Float,
)

// Estado visual del mapa de entregas con filtros, entrega seleccionada y lista desplegada
data class RecyclerDeliveriesUiState(
    val deliveries: List<RecyclerDelivery> = emptyList(),
    val materialFilter: MaterialType? = null,
    val statusFilter: PublicationStatus = PublicationStatus.DISPONIBLE,
    val selectedId: String? = null,
    val isListExpanded: Boolean = false,
) {
    // Entregas que cumplen los filtros de material y estado activos
    val visibleDeliveries: List<RecyclerDelivery>
        get() = deliveries.filter { delivery ->
            (materialFilter == null || delivery.material == materialFilter) && delivery.status == statusFilter
        }

    // Entrega destacada en la tarjeta; si no hay una elegida se toma la primera visible
    val selectedDelivery: RecyclerDelivery?
        get() = visibleDeliveries.firstOrNull { it.id == selectedId } ?: visibleDeliveries.firstOrNull()
}
