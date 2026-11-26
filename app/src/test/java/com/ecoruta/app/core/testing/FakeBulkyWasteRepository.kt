package com.ecoruta.app.core.testing

// Repositorio de retiro de escombros y voluminosos en memoria para pruebas unitarias sin red ni base de datos
// Permite registrar solicitudes, simular cotizaciones, cancelar solicitudes y consultar gestores RCD autorizados

import com.ecoruta.app.features.bulky_waste.domain.model.BulkyWasteRequest
import com.ecoruta.app.features.bulky_waste.domain.model.RcdManagerOrientation
import com.ecoruta.app.features.bulky_waste.domain.repository.BulkyWasteRepository
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DayMillis
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType

class FakeBulkyWasteRepository : BulkyWasteRepository {

    // Identificador del ciudadano autenticado para simular aislamiento RLS (Row-Level Security)
    var currentUserId: String = "citizen-test-01"

    // Permite forzar excepciones simuladas para probar control de errores en los ViewModels
    var error: Throwable? = null

    // Lista mutable de solicitudes en memoria para pruebas
    val requests = mutableListOf(
        BulkyWasteRequest(
            id = "req-101",
            citizenId = "citizen-test-01",
            type = DebrisType.FURNITURE,
            description = "Sofá viejo de 3 puestos y sillón auxiliar",
            latitude = 4.0847,
            longitude = -76.1953,
            address = "Calle 25 #14-10, Barrio Centro",
            scheduledDateMillis = 1_794_000_000_000L,
            status = DebrisStatus.PENDIENTE,
            quotedCop = null,
            createdAtMillis = 1_793_950_000_000L,
        ),
        BulkyWasteRequest(
            id = "req-102",
            citizenId = "citizen-test-01",
            type = DebrisType.RENOVATION_DEBRIS,
            description = "8 bultos de escombros de remodelación de baño",
            latitude = 4.0900,
            longitude = -76.2000,
            address = "Carrera 30 #20-15, La Esperanza",
            scheduledDateMillis = 1_794_100_000_000L,
            status = DebrisStatus.COTIZADO,
            quotedCop = 85_000L,
            createdAtMillis = 1_793_900_000_000L,
        ),
    )

    // Lista mutable de gestores autorizados y orientación oficial de Tuluá
    val managers = mutableListOf(
        RcdManagerOrientation(
            id = "rcd-01",
            name = "Veolia Aseo Tuluá - Servicio Especial RCD",
            phone = "315 123 4567",
            address = "Carrera 40 #26-10",
            operatingHours = "Lunes a Viernes 7:00 AM - 5:00 PM",
            description = "Empresa prestadora del servicio público de aseo para recolección de voluminosos y escombros domiciliarios.",
            acceptedTypes = listOf(DebrisType.FURNITURE, DebrisType.MATTRESSES, DebrisType.RENOVATION_DEBRIS),
        ),
        RcdManagerOrientation(
            id = "rcd-02",
            name = "EcoEscombros del Valle S.A.S.",
            phone = "318 765 4321",
            address = "Km 3 Vía Tuluá - Riofrío",
            operatingHours = "Lunes a Sábado 6:00 AM - 4:00 PM",
            description = "Gestor ambiental autorizado CVC para aprovechamiento y disposición de RCD.",
            acceptedTypes = listOf(DebrisType.RENOVATION_DEBRIS),
        ),
    )

    override suspend fun createRequest(
        type: DebrisType,
        description: String,
        latitude: Double,
        longitude: Double,
        address: String,
        scheduledDateMillis: Long?,
    ): Result<BulkyWasteRequest> = result {
        val newId = "req-${requests.size + 101}"
        val created = BulkyWasteRequest(
            id = newId,
            citizenId = currentUserId,
            type = type,
            description = description,
            latitude = latitude,
            longitude = longitude,
            address = address,
            scheduledDateMillis = scheduledDateMillis,
            status = DebrisStatus.PENDIENTE,
            quotedCop = null,
            createdAtMillis = System.currentTimeMillis(),
        )
        requests.add(0, created)
        created
    }

    override suspend fun getMyRequests(): Result<List<BulkyWasteRequest>> = result {
        requests
            .filter { it.citizenId == currentUserId }
            .sortedByDescending { it.createdAtMillis }
    }

    override suspend fun getRequestById(requestId: String): Result<BulkyWasteRequest> = result {
        require(requestId.isNotBlank()) { "El identificador de la solicitud no puede estar vacío" }
        requests.firstOrNull { it.id == requestId }
            ?: error("Solicitud de escombros no encontrada con id: $requestId")
    }

    override suspend fun cancelRequest(requestId: String): Result<Unit> = result {
        require(requestId.isNotBlank()) { "El identificador de la solicitud no puede estar vacío" }
        val index = requests.indexOfFirst { it.id == requestId }
        if (index < 0) error("Solicitud de escombros no encontrada con id: $requestId")

        val current = requests[index]
        require(current.citizenId == currentUserId) {
            "No autorizado para cancelar una solicitud perteneciente a otro ciudadano"
        }
        require(current.canCancel) {
            "Solo se pueden cancelar solicitudes en estado pendiente (estado actual: ${current.status})"
        }
        requests[index] = current.copy(status = DebrisStatus.CANCELADO)
    }

    override suspend fun getAuthorizedManagers(): Result<List<RcdManagerOrientation>> = result {
        managers.toList()
    }

    // Configuración de capacidades semanales por día ISO (1=Lunes a 7=Domingo)
    var weeklyCapacities: Map<Int, Int> = mapOf(
        1 to 10, 2 to 10, 3 to 10, 4 to 10, 5 to 10, 6 to 10, 7 to 0
    )

    // Consulta de cupos disponibles basada en la capacidad semanal y solicitudes no canceladas
    override suspend fun getAvailablePickupDates(
        startDateMillis: Long,
        daysCount: Int,
    ): Result<Set<Long>> = result {
        val available = mutableSetOf<Long>()
        val calendar = java.util.Calendar.getInstance()

        val startDay = DayMillis.startOfDay(startDateMillis)

        val bookedCounts = requests
            .filter { it.status != DebrisStatus.CANCELADO && it.scheduledDateMillis != null }
            .mapNotNull { it.scheduledDateMillis?.let { ms -> DayMillis.startOfDay(ms) } }
            .groupingBy { it }
            .eachCount()

        for (i in 0 until daysCount) {
            val currentDayMillis = DayMillis.addDays(startDay, i)
            calendar.timeInMillis = currentDayMillis

            val calDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            val isoDayOfWeek = if (calDay == java.util.Calendar.SUNDAY) 7 else calDay - 1

            val maxSlots = weeklyCapacities[isoDayOfWeek] ?: if (isoDayOfWeek == 7) 0 else 10
            val booked = bookedCounts[currentDayMillis] ?: 0

            if (maxSlots > 0 && booked < maxSlots) {
                available.add(currentDayMillis)
            }
        }
        available
    }

    // Asigna la fecha programada a una solicitud existente en memoria
    override suspend fun scheduleRequestDate(
        requestId: String,
        scheduledDateMillis: Long,
    ): Result<Unit> = result {
        require(requestId.isNotBlank()) { "El identificador de la solicitud no puede estar vacío" }
        val index = requests.indexOfFirst { it.id == requestId }
        if (index < 0) error("Solicitud de escombros no encontrada con id: $requestId")
        val current = requests[index]
        requests[index] = current.copy(scheduledDateMillis = scheduledDateMillis)
    }

    private inline fun <T> result(block: () -> T): Result<T> =
        error?.let { Result.failure(it) } ?: runCatching(block)
}
