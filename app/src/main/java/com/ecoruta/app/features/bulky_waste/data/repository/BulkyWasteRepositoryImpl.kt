package com.ecoruta.app.features.bulky_waste.data.repository

// Implementación concreta del repositorio de escombros y voluminosos (RCD)
// Envía solicitudes de retiro, consulta historial, gestiona cancelaciones y obtiene gestores autorizados

import com.ecoruta.app.features.bulky_waste.data.remote.dto.AuthorizedRcdManagerDto
import com.ecoruta.app.features.bulky_waste.data.remote.dto.BulkyWasteCreateDto
import com.ecoruta.app.features.bulky_waste.data.remote.dto.BulkyWasteRequestDto
import com.ecoruta.app.features.bulky_waste.data.remote.dto.toDomain
import com.ecoruta.app.features.bulky_waste.domain.model.BulkyWasteRequest
import com.ecoruta.app.features.bulky_waste.domain.model.RcdManagerOrientation
import com.ecoruta.app.features.bulky_waste.domain.model.toDbCode
import com.ecoruta.app.features.bulky_waste.domain.repository.BulkyWasteRepository
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType
import com.ecoruta.app.features.reports.data.remote.dto.formatPostgisPoint
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

class BulkyWasteRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
) : BulkyWasteRepository {

    // Registra una nueva solicitud de escombros con las coordenadas PostGIS y la tipología seleccionada
    override suspend fun createRequest(
        type: DebrisType,
        description: String,
        latitude: Double,
        longitude: Double,
        address: String,
        scheduledDateMillis: Long?,
    ): Result<BulkyWasteRequest> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para radicar la solicitud de escombros")

        val scheduledDateStr = scheduledDateMillis?.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(it))
        }

        val createDto = BulkyWasteCreateDto(
            citizenId = userId,
            itemType = type.toDbCode(),
            description = description.trim(),
            location = formatPostgisPoint(latitude, longitude),
            scheduledDate = scheduledDateStr,
            status = "pendiente",
        )

        val createdDto = postgrest.from("bulky_waste_requests")
            .insert(createDto) {
                select()
            }
            .decodeSingle<BulkyWasteRequestDto>()

        createdDto.toDomain().copy(address = address)
    }

    // Consulta todas las solicitudes de escombros del ciudadano autenticado
    override suspend fun getMyRequests(): Result<List<BulkyWasteRequest>> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar solicitudes de escombros")

        postgrest.from("bulky_waste_requests")
            .select {
                filter { eq("citizen_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<BulkyWasteRequestDto>()
            .map { it.toDomain() }
    }

    // Consulta una solicitud específica por su identificador
    override suspend fun getRequestById(requestId: String): Result<BulkyWasteRequest> = runCatching {
        postgrest.from("bulky_waste_requests")
            .select {
                filter { eq("id", requestId) }
            }
            .decodeSingle<BulkyWasteRequestDto>()
            .toDomain()
    }

    // Cancela una solicitud en estado pendiente actualizando su estado a cancelado
    override suspend fun cancelRequest(requestId: String): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para cancelar la solicitud de escombros")

        postgrest.from("bulky_waste_requests")
            .update(mapOf("status" to "cancelado")) {
                filter {
                    eq("id", requestId)
                    eq("citizen_id", userId)
                    eq("status", "pendiente")
                }
            }
        Unit
    }

    // Consulta el directorio de gestores autorizados y orientación oficial de escombros en Tuluá
    override suspend fun getAuthorizedManagers(): Result<List<RcdManagerOrientation>> = runCatching {
        postgrest.from("authorized_rcd_managers")
            .select {
                filter { eq("is_active", true) }
            }
            .decodeList<AuthorizedRcdManagerDto>()
            .map { it.toDomain() }
    }
}
