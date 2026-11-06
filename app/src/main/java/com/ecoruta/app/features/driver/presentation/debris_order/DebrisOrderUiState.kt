package com.ecoruta.app.features.driver.presentation.debris_order

// Estado inmutable de la orden de recolección de escombros asignada al chofer
// Domicilio, tipo y volumen del escombro, estado de la orden y confirmación de la recolección

import com.ecoruta.app.features.driver.presentation.orders.DriverOrderStatus

// Orden de escombros con el domicilio del ciudadano y lo que se debe recoger
data class DebrisOrder(
    val code: String,
    val dateLabel: String,
    val schedule: String,
    val address: String,
    val locality: String,
    val debrisType: String,
    val volume: String,
    val hasHazardousWaste: Boolean,
    val status: DriverOrderStatus,
    val collectedAt: String? = null,
)

// Estado de las pantallas de la orden de escombros y de su recolección confirmada
data class DebrisOrderUiState(
    val order: DebrisOrder? = null,
    val isConfirming: Boolean = false,
)

// Orden de escombros ilustrativa para el modo diseño y las previsualizaciones
val SampleDebrisOrder = DebrisOrder(
    code = "E-028",
    dateLabel = "Hoy, 14 de octubre",
    schedule = "10:00–11:00",
    address = "Calle 26 # 25-16, casa 2",
    locality = "Centro, Tuluá, Valle del Cauca",
    debrisType = "Ladrillo y concreto",
    volume = "1 m³",
    hasHazardousWaste = false,
    status = DriverOrderStatus.SCHEDULED,
)
