package com.ecoruta.app.features.bulky_waste

// Pruebas unitarias para los modelos de dominio de escombros/voluminosos y FakeBulkyWasteRepository
// Valida conversión a modelos visuales de UI, control de cancelación, códigos de base de datos y flujos en memoria

import com.ecoruta.app.core.testing.FakeBulkyWasteRepository
import com.ecoruta.app.features.bulky_waste.domain.model.BulkyWasteRequest
import com.ecoruta.app.features.bulky_waste.domain.model.debrisStatusFromDb
import com.ecoruta.app.features.bulky_waste.domain.model.debrisTypeFromDb
import com.ecoruta.app.features.bulky_waste.domain.model.toDbCode
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkyWasteDomainModelTest {

    // Valida que BulkyWasteRequest.toUiRequest conserve todos los atributos requeridos por la vista
    @Test
    fun testBulkyWasteRequestToUiRequestMapping() {
        val domainRequest = BulkyWasteRequest(
            id = "req-001",
            citizenId = "citizen-001",
            type = DebrisType.FURNITURE,
            description = "Comedor con 4 sillas",
            latitude = 4.0847,
            longitude = -76.1953,
            address = "Calle 25 #14-10",
            scheduledDateMillis = 1_794_000_000_000L,
            status = DebrisStatus.PENDIENTE,
            quotedCop = 50_000L,
            createdAtMillis = 1_793_950_000_000L,
            driverName = "Carlos Conductor",
            statusNote = "En ruta de asignación",
        )

        val ui = domainRequest.toUiRequest()

        assertEquals("req-001", ui.id)
        assertEquals(DebrisType.FURNITURE, ui.type)
        assertEquals("Comedor con 4 sillas", ui.description)
        assertEquals("Calle 25 #14-10", ui.address)
        assertEquals(DebrisStatus.PENDIENTE, ui.status)
        assertEquals(50_000L, ui.quotedCop)
        assertEquals("Carlos Conductor", ui.driverName)
        assertEquals("En ruta de asignación", ui.statusNote)
    }

    // Valida que si la dirección está vacía, toUiRequest utilice las coordenadas como respaldo
    @Test
    fun testBulkyWasteRequestFallbackAddress() {
        val domainRequest = BulkyWasteRequest(
            id = "req-002",
            citizenId = "citizen-001",
            type = DebrisType.RENOVATION_DEBRIS,
            description = "Escombros",
            latitude = 4.0847,
            longitude = -76.1953,
            address = "",
        )

        val ui = domainRequest.toUiRequest()
        assertEquals("Lat: 4.0847, Lng: -76.1953", ui.address)
    }

    // Valida que solo las solicitudes en estado PENDIENTE puedan cancelarse
    @Test
    fun testCanCancelLogic() {
        val pending = BulkyWasteRequest(
            id = "req-1",
            citizenId = "c1",
            type = DebrisType.FURNITURE,
            description = "",
            latitude = 0.0,
            longitude = 0.0,
            status = DebrisStatus.PENDIENTE,
        )
        assertTrue(pending.canCancel)

        val quoted = pending.copy(status = DebrisStatus.COTIZADO)
        assertFalse(quoted.canCancel)

        val scheduled = pending.copy(status = DebrisStatus.PROGRAMADO)
        assertFalse(scheduled.canCancel)

        val collected = pending.copy(status = DebrisStatus.RECOLECTADO)
        assertFalse(collected.canCancel)

        val cancelled = pending.copy(status = DebrisStatus.CANCELADO)
        assertFalse(cancelled.canCancel)
    }

    // Valida mapeo bidireccional entre DebrisType y códigos de base de datos
    @Test
    fun testDebrisTypeDatabaseCodeMapping() {
        assertEquals("muebles", DebrisType.FURNITURE.toDbCode())
        assertEquals("colchones", DebrisType.MATTRESSES.toDbCode())
        assertEquals("escombros", DebrisType.RENOVATION_DEBRIS.toDbCode())

        assertEquals(DebrisType.FURNITURE, debrisTypeFromDb("muebles"))
        assertEquals(DebrisType.FURNITURE, debrisTypeFromDb("furniture"))
        assertEquals(DebrisType.MATTRESSES, debrisTypeFromDb("colchones"))
        assertEquals(DebrisType.MATTRESSES, debrisTypeFromDb("mattresses"))
        assertEquals(DebrisType.RENOVATION_DEBRIS, debrisTypeFromDb("escombros"))
        assertEquals(DebrisType.RENOVATION_DEBRIS, debrisTypeFromDb("desconocido"))
    }

    // Valida mapeo bidireccional entre DebrisStatus y códigos de base de datos
    @Test
    fun testDebrisStatusDatabaseCodeMapping() {
        assertEquals("pendiente", DebrisStatus.PENDIENTE.toDbCode())
        assertEquals("cotizado", DebrisStatus.COTIZADO.toDbCode())
        assertEquals("programado", DebrisStatus.PROGRAMADO.toDbCode())
        assertEquals("recolectado", DebrisStatus.RECOLECTADO.toDbCode())
        assertEquals("cancelado", DebrisStatus.CANCELADO.toDbCode())

        assertEquals(DebrisStatus.PENDIENTE, debrisStatusFromDb("pendiente"))
        assertEquals(DebrisStatus.COTIZADO, debrisStatusFromDb("cotizado"))
        assertEquals(DebrisStatus.PROGRAMADO, debrisStatusFromDb("programado"))
        assertEquals(DebrisStatus.RECOLECTADO, debrisStatusFromDb("recolectado"))
        assertEquals(DebrisStatus.CANCELADO, debrisStatusFromDb("cancelado"))
        assertEquals(DebrisStatus.PENDIENTE, debrisStatusFromDb("desconocido"))
    }

    // Valida que FakeBulkyWasteRepository permita crear una solicitud, listarla y cancelarla
    @Test
    fun testFakeBulkyWasteRepositoryCreateAndCancel(): Unit = runBlocking {
        val repo = FakeBulkyWasteRepository()

        val createResult = repo.createRequest(
            type = DebrisType.MATTRESSES,
            description = "Colchón semidoble para disposición",
            latitude = 4.0812,
            longitude = -76.1988,
            address = "Calle 20 #10-05",
            scheduledDateMillis = 1_795_000_000_000L,
        )
        assertTrue(createResult.isSuccess)
        val created = createResult.getOrThrow()
        assertEquals(DebrisType.MATTRESSES, created.type)
        assertEquals(DebrisStatus.PENDIENTE, created.status)

        // Comprueba que figure en la lista de solicitudes
        val listResult = repo.getMyRequests()
        assertTrue(listResult.isSuccess)
        assertTrue(listResult.getOrThrow().any { it.id == created.id })

        // Cancela la solicitud pendiente
        val cancelResult = repo.cancelRequest(created.id)
        assertTrue(cancelResult.isSuccess)

        val updated = repo.getRequestById(created.id).getOrThrow()
        assertEquals(DebrisStatus.CANCELADO, updated.status)
        assertFalse(updated.canCancel)
    }

    // Valida que FakeBulkyWasteRepository rechace cancelar una solicitud no pendiente
    @Test
    fun testFakeBulkyWasteRepositoryCannotCancelNonPending(): Unit = runBlocking {
        val repo = FakeBulkyWasteRepository()
        // req-102 en el fake está en estado COTIZADO
        val cancelResult = repo.cancelRequest("req-102")
        assertTrue(cancelResult.isFailure)
    }

    // Valida consulta de gestores autorizados en FakeBulkyWasteRepository
    @Test
    fun testFakeBulkyWasteRepositoryGetAuthorizedManagers(): Unit = runBlocking {
        val repo = FakeBulkyWasteRepository()
        val result = repo.getAuthorizedManagers()
        assertTrue(result.isSuccess)

        val managers = result.getOrThrow()
        assertTrue(managers.isNotEmpty())
        assertNotNull(managers.first().name)
        assertTrue(managers.first().acceptedTypes.isNotEmpty())
    }
}
