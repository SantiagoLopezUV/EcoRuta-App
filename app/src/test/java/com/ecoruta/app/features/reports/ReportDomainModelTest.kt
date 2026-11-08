package com.ecoruta.app.features.reports

// Pruebas unitarias de los modelos de dominio de reportes ciudadanos y FakeReportRepository
// Verifica conversión de tipos de afectación, estados de moderación, mapeo a UI y reglas de negocio

import com.ecoruta.app.core.testing.FakeReportRepository
import com.ecoruta.app.features.reports.domain.model.AffectationType
import com.ecoruta.app.features.reports.domain.model.CitizenReport
import com.ecoruta.app.features.reports.domain.model.ReportStatus
import com.ecoruta.app.features.reports.presentation.create.toUiReport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportDomainModelTest {

    // Verifica que AffectationType.fromCode convierta los códigos de base de datos y use CONTENEDOR_LLENO por defecto
    @Test
    fun testAffectationTypeFromCodeMapping() {
        assertEquals(AffectationType.CONTENEDOR_LLENO, AffectationType.fromCode("contenedor_lleno"))
        assertEquals(AffectationType.CONTENEDOR_LLENO, AffectationType.fromCode("FULL_CONTAINER"))
        assertEquals(AffectationType.BOTADERO_CLANDESTINO, AffectationType.fromCode("botadero_clandestino"))
        assertEquals(AffectationType.BOTADERO_CLANDESTINO, AffectationType.fromCode("illegal_dump"))
        assertEquals(AffectationType.CONTENEDOR_LLENO, AffectationType.fromCode(null))
        assertEquals(AffectationType.CONTENEDOR_LLENO, AffectationType.fromCode("desconocido"))
    }

    // Verifica que AffectationType.typeCode devuelva el código exacto exigido por reports_type_check en PostgreSQL
    @Test
    fun testAffectationTypeCodeConstants() {
        assertEquals("contenedor_lleno", AffectationType.CONTENEDOR_LLENO.typeCode)
        assertEquals("botadero_clandestino", AffectationType.BOTADERO_CLANDESTINO.typeCode)
    }

    // Verifica que ReportStatus.fromCode reconozca los cinco estados de moderación y su código en base de datos
    @Test
    fun testReportStatusFromCodeAndStatusCode() {
        assertEquals(ReportStatus.PENDIENTE, ReportStatus.fromCode("pendiente"))
        assertEquals(ReportStatus.APROBADO, ReportStatus.fromCode("aprobado"))
        assertEquals(ReportStatus.ATENDIDO, ReportStatus.fromCode("atendido"))
        assertEquals(ReportStatus.RECHAZADO, ReportStatus.fromCode("rechazado"))
        assertEquals(ReportStatus.DUPLICADO, ReportStatus.fromCode("duplicado"))
        assertEquals(ReportStatus.PENDIENTE, ReportStatus.fromCode(null))

        assertEquals("pendiente", ReportStatus.PENDIENTE.statusCode)
        assertEquals("aprobado", ReportStatus.APROBADO.statusCode)
        assertEquals("atendido", ReportStatus.ATENDIDO.statusCode)
        assertEquals("rechazado", ReportStatus.RECHAZADO.statusCode)
        assertEquals("duplicado", ReportStatus.DUPLICADO.statusCode)
    }

    // Verifica que CitizenReport.toUiReport conserve los atributos visuales y use coordenadas si la dirección está vacía
    @Test
    fun testCitizenReportToUiReportMapping() {
        val samplePhoto1 = "http://127.0.0.1:54321/storage/v1/object/public/report-photos/user-1/foto1.jpg"
        val samplePhoto2 = "http://127.0.0.1:54321/storage/v1/object/public/report-photos/user-1/foto2.jpg"

        val domainReport = CitizenReport(
            id = "rep-dup-1",
            citizenId = "user-1",
            type = AffectationType.BOTADERO_CLANDESTINO,
            status = ReportStatus.DUPLICADO,
            latitude = 4.0847,
            longitude = -76.1953,
            address = "",
            area = "La Esperanza",
            photoUrl = null,
            photoUrls = listOf(samplePhoto1, samplePhoto2),
            statusNote = "Ya fue reportado por otro vecino",
            originalReportId = "rep-orig-99",
            createdAtMillis = 1_793_977_200_000L,
        )

        val uiReport = domainReport.toUiReport()

        assertEquals("rep-dup-1", uiReport.id)
        assertEquals(AffectationType.BOTADERO_CLANDESTINO, uiReport.type)
        assertEquals(ReportStatus.DUPLICADO, uiReport.status)
        assertEquals("4.0847, -76.1953", uiReport.address)
        assertEquals("La Esperanza", uiReport.area)
        assertEquals(samplePhoto1, uiReport.photoUrl)
        assertEquals("Ya fue reportado por otro vecino", uiReport.statusNote)
        assertEquals("rep-orig-99", uiReport.originalReportId)
    }

    // Verifica que FakeReportRepository permita crear un reporte con hasta 3 fotos y consultarlo en el historial
    @Test
    fun testFakeReportRepositorySubmitAndFetch(): Unit = runBlocking {
        val repo = FakeReportRepository()
        val photos = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))

        val submitResult = repo.submitReport(
            type = AffectationType.BOTADERO_CLANDESTINO,
            latitude = 4.0900,
            longitude = -76.2000,
            photosBytes = photos,
            address = "Carrera 30 #20-15",
            area = "Centro",
            description = "Escombros y bolsas acumuladas",
        )

        assertTrue(submitResult.isSuccess)
        val created = submitResult.getOrThrow()
        assertEquals(AffectationType.BOTADERO_CLANDESTINO, created.type)
        assertEquals(ReportStatus.PENDIENTE, created.status)
        assertEquals(2, created.photoUrls.size)
        assertNotNull(created.photoUrl)

        val myReports = repo.getMyReports().getOrThrow()
        assertEquals(2, myReports.size)
        assertEquals(created.id, myReports.first().id)

        val detail = repo.getReportById(created.id).getOrThrow()
        assertEquals("Carrera 30 #20-15", detail.address)
    }

    // Verifica que FakeReportRepository rechace reportes sin fotografías o con más de 3 fotografías
    @Test
    fun testFakeReportRepositoryPhotoLimits(): Unit = runBlocking {
        val repo = FakeReportRepository()

        val emptyPhotosResult = repo.submitReport(
            type = AffectationType.CONTENEDOR_LLENO,
            latitude = 4.08,
            longitude = -76.19,
            photosBytes = emptyList(),
        )
        assertTrue(emptyPhotosResult.isFailure)

        val fourPhotosResult = repo.submitReport(
            type = AffectationType.CONTENEDOR_LLENO,
            latitude = 4.08,
            longitude = -76.19,
            photosBytes = listOf(byteArrayOf(1), byteArrayOf(2), byteArrayOf(3), byteArrayOf(4)),
        )
        assertTrue(fourPhotosResult.isFailure)
    }
}
