package com.ecoruta.app.features.routes.domain.model

// Modelo de dominio puro para rutas de aseo ordinario por barrio
// Define nombre del barrio, días de servicio (ej. Lunes/Miércoles) y franjas horarias

data class NeighborhoodRoute(
    val neighborhoodId: String,
    val neighborhoodName: String,
    val serviceDays: List<Int>,
    val startMinute: Int,
    val endMinute: Int,
    val routeCoordinates: List<Pair<Double, Double>> = emptyList(),
)
