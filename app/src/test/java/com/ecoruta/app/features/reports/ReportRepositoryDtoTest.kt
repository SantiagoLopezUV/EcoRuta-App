package com.ecoruta.app.features.reports

// Pruebas unitarias de serialización JSON y conversión PostGIS para los DTOs de reportes
// Valida ReportCreateDto, ReportDto, ReportPhotoDto, parsePostgisPoint y parseIsoTimestampToMillis

import com.ecoruta.app.features.reports.data.remote.dto.ReportCreateDto
import com.ecoruta.app.features.reports.data.remote.dto.ReportDto
import com.ecoruta.app.features.reports.data.remote.dto.ReportPhotoCreateDto
import com.ecoruta.app.features.reports.data.remote.dto.ReportPhotoDto
import com.ecoruta.app.features.reports.data.remote.dto.formatPostgisPoint
import com.ecoruta.app.features.reports.data.remote.dto.parseIsoTimestampToMillis
import com.ecoruta.app.features.reports.data.remote.dto.parsePostgisPoint
import com.ecoruta.app.features.reports.data.remote.dto.toDomain
import com.ecoruta.app.features.reports.domain.model.AffectationType
import com.ecoruta.app.features.reports.domain.model.ReportStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportRepositoryDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Valida que ReportCreateDto y ReportPhotoCreateDto serialicen las claves snake_case que espera PostgREST
    @Test
    fun testReportCreateDtoSerialization() {
        val locationWkt = formatPostgisPoint(latitude = 4.0847, longitude = -76.1953)
        assertEquals("SRID=4326;POINT(-76.1953 4.0847)", locationWkt)

        val photoUrl1 = "http://127.0.0.1:54321/storage/v1/object/public/report-photos/citizen-uuid-123/1.jpg"
        val photoUrl2 = "http://127.0.0.1:54321/storage/v1/object/public/report-photos/citizen-uuid-123/2.jpg"

        val createDto = ReportCreateDto(
            citizenId = "citizen-uuid-123",
            type = AffectationType.CONTENEDOR_LLENO.typeCode,
            location = locationWkt,
            photoUrl = photoUrl1,
            pointId = "point-uuid-456",
        )

        val encoded = json.encodeToString(createDto)
        assertTrue(encoded.contains("\"citizen_id\":\"citizen-uuid-123\""))
        assertTrue(encoded.contains("\"type\":\"contenedor_lleno\""))
        assertTrue(encoded.contains("\"location\":\"SRID=4326;POINT(-76.1953 4.0847)\""))
        assertTrue(encoded.contains("\"photo_url\":\"$photoUrl1\""))
        assertTrue(encoded.contains("\"point_id\":\"point-uuid-456\""))
        assertTrue(encoded.contains("\"status\":\"pendiente\""))

        val photoDto = ReportPhotoCreateDto(
            reportId = "rep-uuid-1",
            photoUrl = photoUrl2,
        )
        val encodedPhoto = json.encodeToString(photoDto)
        assertTrue(encodedPhoto.contains("\"report_id\":\"rep-uuid-1\""))
        assertTrue(encodedPhoto.contains("\"photo_url\":\"$photoUrl2\""))
    }

    // Valida que parsePostgisPoint decodifique tanto formato WKT como el binario hexadecimal EWKB de PostGIS
    @Test
    fun testParsePostgisPointWktAndEwkbHex() {
        val (wktLat, wktLng) = parsePostgisPoint("SRID=4326;POINT(-76.1953 4.0847)")
        assertEquals(4.0847, wktLat, 0.0001)
        assertEquals(-76.1953, wktLng, 0.0001)

        // Cadena EWKB hexadecimal real generada por PostGIS para SRID=4326;POINT(-76.1953 4.0847)
        val ewkbHex = "0101000020E61000002A3A92CB7F0C53C082E2C798BB561040"
        val (hexLat, hexLng) = parsePostgisPoint(ewkbHex)
        assertEquals(4.0847, hexLat, 0.0001)
        assertEquals(-76.1953, hexLng, 0.0001)
    }

    // Valida que parseIsoTimestampToMillis convierta fechas ISO-8601 de PostgreSQL con microsegundos y zona horaria
    @Test
    fun testParseIsoTimestampToMillis() {
        val millisUtc = parseIsoTimestampToMillis("2026-11-06T21:45:00.123456+00:00")
        val millisZ = parseIsoTimestampToMillis("2026-11-06T21:45:00.123Z")
        val millisColombia = parseIsoTimestampToMillis("2026-11-06T16:45:00.123-05:00")

        assertTrue(millisUtc > 0L)
        assertEquals(millisUtc, millisZ)
        assertEquals(millisUtc, millisColombia)
    }

    // Valida la deserialización de ReportDto y su mapeo a CitizenReport unificando fotos sin duplicados
    @Test
    fun testReportDtoDeserializationAndToDomain() {
        val photo1 = "http://127.0.0.1:54321/storage/v1/object/public/report-photos/citizen-uuid-123/foto1.jpg"
        val photo2 = "http://127.0.0.1:54321/storage/v1/object/public/report-photos/citizen-uuid-123/foto2.jpg"

        val rawJson = """
            {
                "id": "rep-uuid-789",
                "citizen_id": "citizen-uuid-123",
                "point_id": null,
                "type": "botadero_clandestino",
                "photo_url": "$photo1",
                "location": "0101000020E61000002A3A92CB7F0C53C082E2C798BB561040",
                "status": "aprobado",
                "validated_by": "operator-uuid-1",
                "created_at": "2026-11-06T21:45:00.123456+00:00",
                "updated_at": "2026-11-06T22:00:00.000000+00:00"
            }
        """.trimIndent()

        val dto = json.decodeFromString<ReportDto>(rawJson)
        val extraPhotos = listOf(
            ReportPhotoDto(id = "p1", reportId = "rep-uuid-789", photoUrl = photo1),
            ReportPhotoDto(id = "p2", reportId = "rep-uuid-789", photoUrl = photo2),
        )

        val domain = dto.toDomain(
            photos = extraPhotos,
            address = "Calle 14 #8-20",
            area = "La Esperanza",
        )

        assertEquals("rep-uuid-789", domain.id)
        assertEquals(AffectationType.BOTADERO_CLANDESTINO, domain.type)
        assertEquals(ReportStatus.APROBADO, domain.status)
        assertEquals(4.0847, domain.latitude, 0.0001)
        assertEquals(-76.1953, domain.longitude, 0.0001)
        assertEquals(photo1, domain.photoUrl)
        assertEquals(listOf(photo1, photo2), domain.photoUrls)
        assertTrue(domain.createdAtMillis > 0L)
    }
}
