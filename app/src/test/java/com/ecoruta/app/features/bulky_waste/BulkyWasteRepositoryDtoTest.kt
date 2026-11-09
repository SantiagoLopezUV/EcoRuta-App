package com.ecoruta.app.features.bulky_waste

// Pruebas unitarias de serialización DTO y mapeo de PostGIS para escombros y voluminosos
// Valida compatibilidad con la tabla bulky_waste_requests y authorized_rcd_managers en PostgreSQL

import com.ecoruta.app.features.bulky_waste.data.remote.dto.AuthorizedRcdManagerDto
import com.ecoruta.app.features.bulky_waste.data.remote.dto.BulkyWasteCreateDto
import com.ecoruta.app.features.bulky_waste.data.remote.dto.BulkyWasteRequestDto
import com.ecoruta.app.features.bulky_waste.data.remote.dto.toDomain
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType
import com.ecoruta.app.features.reports.data.remote.dto.formatPostgisPoint
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkyWasteRepositoryDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Valida deserialización de BulkyWasteRequestDto y mapeo PostGIS al modelo de dominio puro
    @Test
    fun testBulkyWasteRequestDtoDeserializationAndToDomain() {
        val rawJson = """
            {
                "id": "b1a2c3d4-1111-4444-8888-000000000001",
                "citizen_id": "citizen-123",
                "item_type": "muebles",
                "description": "Comedor de madera con 4 sillas",
                "location": "SRID=4326;POINT(-76.1953 4.0847)",
                "scheduled_date": "2026-11-10T14:30:00Z",
                "status": "cotizado",
                "cost": 65000.0,
                "created_at": "2026-11-08T18:00:00.123456Z",
                "company_id": "comp-456"
            }
        """.trimIndent()

        val dto = json.decodeFromString<BulkyWasteRequestDto>(rawJson)
        val domain = dto.toDomain()

        assertEquals("b1a2c3d4-1111-4444-8888-000000000001", domain.id)
        assertEquals("citizen-123", domain.citizenId)
        assertEquals(DebrisType.FURNITURE, domain.type)
        assertEquals("Comedor de madera con 4 sillas", domain.description)
        assertEquals(4.0847, domain.latitude, 0.0001)
        assertEquals(-76.1953, domain.longitude, 0.0001)
        assertEquals(DebrisStatus.COTIZADO, domain.status)
        assertEquals(65_000L, domain.quotedCop)
        assertNotNull(domain.scheduledDateMillis)
        assertEquals("comp-456", domain.companyId)
    }

    // Valida serialización de BulkyWasteCreateDto con nombres de columna snake_case exigidos por PostgREST
    @Test
    fun testBulkyWasteCreateDtoSerialization() {
        val pointWkt = formatPostgisPoint(latitude = 4.0847, longitude = -76.1953)
        val createDto = BulkyWasteCreateDto(
            citizenId = "citizen-uuid-1",
            itemType = "escombros",
            description = "10 bultos de desmonte de muro",
            location = pointWkt,
            scheduledDate = "2026-11-12",
            status = "pendiente",
        )

        val encoded = json.encodeToString(createDto)
        assertTrue(encoded.contains("\"citizen_id\":\"citizen-uuid-1\""))
        assertTrue(encoded.contains("\"item_type\":\"escombros\""))
        assertTrue(encoded.contains("\"location\":\"$pointWkt\""))
        assertTrue(encoded.contains("\"status\":\"pendiente\""))
    }

    // Valida deserialización de AuthorizedRcdManagerDto y conversión de tipos aceptados a dominio
    @Test
    fun testAuthorizedRcdManagerDtoToDomain() {
        val rawJson = """
            {
                "id": "mgr-01",
                "name": "Veolia Aseo Tuluá",
                "item_types": ["muebles", "colchones", "escombros"],
                "phone": "315 123 4567",
                "address": "Carrera 40 #26-10",
                "operating_hours": "Lunes a Viernes 7:00 AM - 5:00 PM",
                "description": "Prestador de servicio público de aseo",
                "is_active": true
            }
        """.trimIndent()

        val dto = json.decodeFromString<AuthorizedRcdManagerDto>(rawJson)
        val domain = dto.toDomain()

        assertEquals("mgr-01", domain.id)
        assertEquals("Veolia Aseo Tuluá", domain.name)
        assertEquals(3, domain.acceptedTypes.size)
        assertTrue(domain.acceptedTypes.contains(DebrisType.FURNITURE))
        assertTrue(domain.acceptedTypes.contains(DebrisType.MATTRESSES))
        assertTrue(domain.acceptedTypes.contains(DebrisType.RENOVATION_DEBRIS))
        assertEquals("315 123 4567", domain.phone)
        assertEquals("Carrera 40 #26-10", domain.address)
        assertEquals("Lunes a Viernes 7:00 AM - 5:00 PM", domain.operatingHours)
    }
}
