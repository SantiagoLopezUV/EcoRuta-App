package com.ecoruta.app.features.circular_economy.domain.model

// Modelo de dominio para los estados de una recogida tomada por un reciclador
// Estados: pendiente, completado y cancelado

// Ciclo de una recogida desde que el reciclador reserva la entrega hasta que la recibe o la cancela
enum class PickupStatus {
    PENDIENTE,
    COMPLETADO,
    CANCELADO,
}
