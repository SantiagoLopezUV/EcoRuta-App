package com.ecoruta.app.features.routes.domain.model

// Modelo de dominio para la configuración del radar de proximidad
// Modela barrio de referencia, radio de cobertura (100 m a 1000 m) y estado activo

data class ProximityRadarConfig(
    val neighborhoodId: String,
    val neighborhoodName: String,
    val radiusMeters: Int = 500,
    val notificationsEnabled: Boolean = true,
    val referenceLatitude: Double? = null,
    val referenceLongitude: Double? = null,
)
