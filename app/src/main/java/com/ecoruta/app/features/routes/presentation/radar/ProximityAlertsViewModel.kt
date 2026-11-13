package com.ecoruta.app.features.routes.presentation.radar

// ViewModel para la configuración del radar de proximidad y gestión de alertas
// Modifica el radio de cobertura (100 m a 1000 m), gestiona token FCM, cálculo de distancia geodésica
// y emisión de alertas de proximidad (una única vez por pasada)

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.routes.domain.model.ProximityRadarConfig
import com.ecoruta.app.features.routes.domain.repository.NeighborhoodRouteRepository
import com.ecoruta.app.features.routes.presentation.map.AppNotification
import com.ecoruta.app.features.routes.presentation.map.NotificationKind
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProximityAlertsViewModel @Inject constructor(
    private val repository: NeighborhoodRouteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProximityAlertsUiState())
    val uiState: StateFlow<ProximityAlertsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProximityAlertEvent>(Channel.BUFFERED)
    val events: Flow<ProximityAlertEvent> = _events.receiveAsFlow()

    fun setNeighborhood(id: String, name: String) {
        _uiState.update { it.copy(neighborhoodId = id, neighborhoodName = name) }
        viewModelScope.launch {
            repository.getRadarConfig(id).onSuccess { config ->
                if (config != null) {
                    _uiState.update {
                        it.copy(
                            radiusMeters = config.radiusMeters,
                            notificationsEnabled = config.notificationsEnabled,
                            citizenLatitude = config.referenceLatitude ?: it.citizenLatitude,
                            citizenLongitude = config.referenceLongitude ?: it.citizenLongitude,
                        )
                    }
                }
            }
        }
    }

    fun onRadiusChange(radiusMeters: Int) {
        val clamped = radiusMeters.coerceIn(RadarDefaults.MIN_RADIUS, RadarDefaults.MAX_RADIUS)
        _uiState.update { it.copy(radiusMeters = clamped) }
    }

    fun onNotificationsChange(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setCitizenLocation(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(citizenLatitude = latitude, citizenLongitude = longitude) }
    }

    fun onSave() {
        val current = _uiState.value
        if (current.isSaving) return

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val config = ProximityRadarConfig(
                neighborhoodId = current.neighborhoodId.orEmpty(),
                neighborhoodName = current.neighborhoodName.orEmpty(),
                radiusMeters = current.radiusMeters,
                notificationsEnabled = current.notificationsEnabled,
                referenceLatitude = current.citizenLatitude,
                referenceLongitude = current.citizenLongitude,
            )

            val result = repository.saveRadarConfig(config)
            _uiState.update { it.copy(isSaving = false) }

            result.fold(
                onSuccess = {
                    _events.send(ProximityAlertEvent.Saved(current.radiusMeters, current.notificationsEnabled))
                },
                onFailure = {
                    _events.send(ProximityAlertEvent.ShowNotice(R.string.radar_save_error))
                },
            )
        }
    }

    fun registerPushToken(token: String) {
        if (token.isBlank()) {
            _uiState.update {
                it.copy(
                    tokenRegistrationState = TokenRegistrationState.Failure(
                        R.string.radar_save_error,
                        rawError = "Token no puede estar vacío",
                    ),
                )
            }
            return
        }

        _uiState.update { it.copy(tokenRegistrationState = TokenRegistrationState.Registering) }

        viewModelScope.launch {
            repository.registerDeviceToken(token)
                .onSuccess {
                    _uiState.update {
                        it.copy(tokenRegistrationState = TokenRegistrationState.Success(token))
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            tokenRegistrationState = TokenRegistrationState.Failure(
                                R.string.radar_save_error,
                                rawError = error.message,
                            ),
                        )
                    }
                }
        }
    }

    fun onTruckLocationUpdated(
        truckId: String,
        truckLatitude: Double,
        truckLongitude: Double,
        timestampMillis: Long = System.currentTimeMillis(),
    ) {
        val current = _uiState.value
        val citizenLat = current.citizenLatitude
        val citizenLon = current.citizenLongitude

        // Sin ubicación ciudadana de referencia no se calcula proximidad
        if (citizenLat == null || citizenLon == null) return

        val distanceMeters = calculateDistanceMeters(
            lat1 = citizenLat,
            lon1 = citizenLon,
            lat2 = truckLatitude,
            lon2 = truckLongitude,
        )

        val isInside = distanceMeters <= current.radiusMeters

        if (isInside) {
            // Solo se dispara la alerta al ingresar por primera vez en esta pasada
            if (!current.isCurrentlyInsidePass) {
                _uiState.update { it.copy(isCurrentlyInsidePass = true) }

                // Solo se crea y emite la alerta si las notificaciones están habilitadas
                if (current.notificationsEnabled) {
                    val notification = AppNotification(
                        id = "PROX-$timestampMillis-$truckId",
                        kind = NotificationKind.PROXIMITY,
                        label = "Alerta de proximidad",
                        title = "El camión recolector está cerca",
                        body = "El camión recolector se encuentra a unos ${distanceMeters.toInt()} metros de tu ubicación.",
                        createdAtMillis = timestampMillis,
                        read = false,
                        referenceId = current.neighborhoodId,
                    )

                    _uiState.update { it.copy(alerts = listOf(notification) + it.alerts) }
                    viewModelScope.launch {
                        _events.send(ProximityAlertEvent.AlertTriggered(notification, distanceMeters))
                    }
                }
            }
        } else {
            // Al salir del radio se reinicia la pasada para permitir futuras alertas si vuelve a entrar
            if (current.isCurrentlyInsidePass) {
                _uiState.update { it.copy(isCurrentlyInsidePass = false) }
            }
        }
    }

    fun markAlertAsRead(notificationId: String) {
        _uiState.update { state ->
            state.copy(
                alerts = state.alerts.map { alert ->
                    if (alert.id == notificationId) alert.copy(read = true) else alert
                },
            )
        }
    }

    fun clearAlerts() {
        _uiState.update { it.copy(alerts = emptyList()) }
    }

    companion object {
        // Cálculo de distancia ortodrómica con fórmula de Haversine en O(1)
        fun calculateDistanceMeters(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double,
        ): Double {
            val earthRadiusMeters = 6_371_000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadiusMeters * c
        }
    }
}
