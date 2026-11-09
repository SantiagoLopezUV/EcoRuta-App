package com.ecoruta.app.features.bulky_waste.domain.repository

// Contrato de dominio para gestión de residuos especiales y escombros (RCD)
// Operaciones abstractas de agendamiento, seguimiento, cancelación y consulta de gestores autorizados

import com.ecoruta.app.features.bulky_waste.domain.model.BulkyWasteRequest
import com.ecoruta.app.features.bulky_waste.domain.model.RcdManagerOrientation
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType

interface BulkyWasteRepository {

    // Registra una nueva solicitud de retiro de escombros o voluminosos en el servidor
    suspend fun createRequest(
        type: DebrisType,
        description: String,
        latitude: Double,
        longitude: Double,
        address: String = "",
        scheduledDateMillis: Long? = null,
    ): Result<BulkyWasteRequest>

    // Consulta las solicitudes de escombros radicadas por el ciudadano autenticado
    suspend fun getMyRequests(): Result<List<BulkyWasteRequest>>

    // Consulta el detalle de una solicitud específica por su identificador
    suspend fun getRequestById(requestId: String): Result<BulkyWasteRequest>

    // Cancela una solicitud en estado pendiente
    suspend fun cancelRequest(requestId: String): Result<Unit>

    // Consulta el directorio de gestores autorizados y orientación oficial de escombros en Tuluá
    suspend fun getAuthorizedManagers(): Result<List<RcdManagerOrientation>>
}
