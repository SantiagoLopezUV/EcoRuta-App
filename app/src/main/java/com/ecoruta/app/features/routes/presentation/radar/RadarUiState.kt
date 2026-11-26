package com.ecoruta.app.features.routes.presentation.radar

// Estado inmutable de la configuración del radar de proximidad y alertas recibidas
// Registra valor del radio en metros, interruptor de activación, registro de token y alertas

import androidx.annotation.StringRes
import com.ecoruta.app.features.routes.presentation.map.AppNotification

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

sealed interface TokenRegistrationState {
    data object Idle : TokenRegistrationState
    data object Registering : TokenRegistrationState
    data class Success(val token: String) : TokenRegistrationState
    data class Failure(@StringRes val messageRes: Int, val rawError: String? = null) : TokenRegistrationState
}

data class ProximityAlertsUiState(
    val neighborhoodId: String? = null,
    val neighborhoodName: String? = null,
    val radiusMeters: Int = RadarDefaults.RADIUS,
    val notificationsEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val tokenRegistrationState: TokenRegistrationState = TokenRegistrationState.Idle,
    val alerts: List<AppNotification> = emptyList(),
    val citizenLatitude: Double? = null,
    val citizenLongitude: Double? = null,
    val isCurrentlyInsidePass: Boolean = false,
) {
    fun toRadarUiState(): RadarUiState = RadarUiState(
        neighborhoodName = neighborhoodName,
        radiusMeters = radiusMeters,
        notificationsEnabled = notificationsEnabled,
        isSaving = isSaving,
    )
}

sealed interface ProximityAlertEvent {
    data class Saved(val radiusMeters: Int, val notificationsEnabled: Boolean) : ProximityAlertEvent
    data class AlertTriggered(val notification: AppNotification, val distanceMeters: Double) : ProximityAlertEvent
    data class ShowNotice(@StringRes val messageRes: Int) : ProximityAlertEvent
}
