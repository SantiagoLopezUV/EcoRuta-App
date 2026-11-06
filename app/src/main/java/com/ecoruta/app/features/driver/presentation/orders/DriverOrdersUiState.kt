package com.ecoruta.app.features.driver.presentation.orders

// Estado inmutable de las órdenes diarias del chofer
// Lista de órdenes asignadas, estado de la jornada, orden seleccionada y errores

import androidx.annotation.StringRes
import com.ecoruta.app.R

// Tipo de jornada que representa cada orden asignada al chofer
enum class DriverOrderKind(@StringRes val labelRes: Int) {
    ORDINARY_ROUTE(R.string.driver_order_kind_ordinary),
    CLEANUP_ROUTE(R.string.driver_order_kind_cleanup),
    DEBRIS(R.string.driver_order_kind_debris),
}

// Estado visible de una orden, de un punto de la ruta o de una recolección de escombros
enum class DriverOrderStatus(@StringRes val labelRes: Int) {
    PENDING(R.string.driver_status_pending),
    SCHEDULED(R.string.driver_status_scheduled),
    IN_PROGRESS(R.string.driver_status_in_progress),
    ATTENDED(R.string.driver_status_attended),
    COLLECTED(R.string.driver_status_collected),
}

// Orden de trabajo del día con su código, horario y lugar o zona de atención
data class DriverOrder(
    val id: String,
    val code: String,
    val kind: DriverOrderKind,
    val status: DriverOrderStatus,
    val schedule: String,
    val place: String,
    val pointCount: Int? = null,
)

// Estado de la pantalla Mis órdenes con la jornada del día y el aviso de sincronización
data class DriverOrdersUiState(
    val dateLabel: String? = null,
    val city: String = "",
    val orders: List<DriverOrder> = emptyList(),
    val attendedCount: Int = 0,
    val selectedOrderId: String? = null,
    val syncFailed: Boolean = false,
    val lastSyncTime: String? = null,
)
