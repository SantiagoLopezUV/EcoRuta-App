package com.ecoruta.app.features.routes.presentation.radar

// Estado inmutable de la configuración del radar de proximidad
// Registra valor del radio en metros, interruptor de activación y estado de guardado

import androidx.annotation.StringRes

data class RadarUiState(
    val neighborhoodName: String? = null,
    val radiusMeters: Int = RadarDefaults.RADIUS,
    val notificationsEnabled: Boolean = true,
    val isSaving: Boolean = false,
)

object RadarDefaults {
    const val MIN_RADIUS = 100
    const val MAX_RADIUS = 1_000
    const val STEP = 100
    const val RADIUS = 500
}

sealed interface RadarEvent {
    // Evento emitido al guardar la configuración para avanzar a la vista de alerta
    data class Saved(val radiusMeters: Int, val notificationsEnabled: Boolean) : RadarEvent
    data class ShowNotice(@StringRes val messageRes: Int) : RadarEvent
}
