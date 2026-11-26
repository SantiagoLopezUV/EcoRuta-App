package com.ecoruta.app.core.testing

// Repositorio de rutas por barrio y radar de proximidad en memoria para pruebas unitarias sin red
// Permite simular consultas de horarios, guardado de configuración y registro de tokens FCM

import com.ecoruta.app.features.routes.domain.model.NeighborhoodRoute
import com.ecoruta.app.features.routes.domain.model.ProximityRadarConfig
import com.ecoruta.app.features.routes.domain.repository.NeighborhoodRouteRepository

class FakeNeighborhoodRouteRepository : NeighborhoodRouteRepository {

    var error: Throwable? = null
    var tokenRegistrationError: Throwable? = null

    val radarConfigs = mutableMapOf<String, ProximityRadarConfig>()
    val registeredTokens = mutableListOf<String>()

    override suspend fun getRouteForNeighborhood(neighborhoodId: String): Result<NeighborhoodRoute?> =
        result {
            NeighborhoodRoute(
                neighborhoodId = neighborhoodId,
                neighborhoodName = "La Esperanza",
                serviceDays = listOf(1, 3, 5),
                startMinute = 360,
                endMinute = 720,
            )
        }

    override suspend fun saveRadarConfig(config: ProximityRadarConfig): Result<Unit> =
        result {
            radarConfigs[config.neighborhoodId] = config
        }

    override suspend fun getRadarConfig(neighborhoodId: String): Result<ProximityRadarConfig?> =
        result {
            radarConfigs[neighborhoodId]
        }

    override suspend fun registerDeviceToken(token: String): Result<Unit> {
        tokenRegistrationError?.let { return Result.failure(it) }
        return result {
            require(token.isNotBlank()) { "Token no puede estar vacío" }
            registeredTokens.add(token)
        }
    }

    private inline fun <T> result(block: () -> T): Result<T> =
        error?.let { Result.failure(it) } ?: runCatching { block() }
}
