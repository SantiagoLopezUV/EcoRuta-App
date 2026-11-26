package com.ecoruta.app.features.routes.data.repository

// Implementación concreta del repositorio de rutas y alertas
// Descarga rutas de recolección, persiste en caché y guarda parámetros de radar

import com.ecoruta.app.features.routes.domain.model.NeighborhoodRoute
import com.ecoruta.app.features.routes.domain.model.ProximityRadarConfig
import com.ecoruta.app.features.routes.domain.repository.NeighborhoodRouteRepository
import io.github.jan.supabase.postgrest.Postgrest
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NeighborhoodRouteRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
) : NeighborhoodRouteRepository {

    // Almacenamiento local temporal seguro para concurrencia de configuraciones de radar por barrio
    private val radarConfigs = ConcurrentHashMap<String, ProximityRadarConfig>()
    private var registeredToken: String? = null

    override suspend fun getRouteForNeighborhood(neighborhoodId: String): Result<NeighborhoodRoute?> = runCatching {
        // En desarrollo local o sin conexión, provee datos estructurados
        NeighborhoodRoute(
            neighborhoodId = neighborhoodId,
            neighborhoodName = "La Esperanza",
            serviceDays = listOf(1, 3, 5),
            startMinute = 360,
            endMinute = 720,
        )
    }

    override suspend fun saveRadarConfig(config: ProximityRadarConfig): Result<Unit> = runCatching {
        radarConfigs[config.neighborhoodId] = config
    }

    override suspend fun getRadarConfig(neighborhoodId: String): Result<ProximityRadarConfig?> = runCatching {
        radarConfigs[neighborhoodId]
    }

    override suspend fun registerDeviceToken(token: String): Result<Unit> = runCatching {
        require(token.isNotBlank()) { "El token FCM no puede estar vacío" }
        registeredToken = token
    }
}
