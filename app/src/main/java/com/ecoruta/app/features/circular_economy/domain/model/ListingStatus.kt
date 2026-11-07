package com.ecoruta.app.features.circular_economy.domain.model

// Modelo de dominio para los estados de una entrega de material publicada por un ciudadano
// Estados: disponible, reservado, completado y cancelado

// Ciclo de una entrega publicada, que vuelve a disponible cuando el reciclador cancela la recogida
enum class ListingStatus {
    DISPONIBLE,
    RESERVADO,
    COMPLETADO,
    CANCELADO,
}
