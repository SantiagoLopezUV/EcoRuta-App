package com.ecoruta.app.features.routes.domain.repository

// Contrato de dominio para consulta de rutas de recolección y radar
// Declaración abstracta de operaciones de trazados, horarios y notificaciones FCM

import com.ecoruta.app.features.routes.domain.model.NeighborhoodRoute
import com.ecoruta.app.features.routes.domain.model.ProximityRadarConfig

interface NeighborhoodRouteRepository {
    // Consulta la información de ruta y horario para un barrio específico
    suspend fun getRouteForNeighborhood(neighborhoodId: String): Result<NeighborhoodRoute?>

    // Guarda los parámetros de configuración del radar de proximidad
    suspend fun saveRadarConfig(config: ProximityRadarConfig): Result<Unit>

    // Obtiene la configuración guardada del radar para un barrio
    suspend fun getRadarConfig(neighborhoodId: String): Result<ProximityRadarConfig?>

    // Registra el token del dispositivo (FCM) para recibir alertas push de proximidad
    suspend fun registerDeviceToken(token: String): Result<Unit>
}
