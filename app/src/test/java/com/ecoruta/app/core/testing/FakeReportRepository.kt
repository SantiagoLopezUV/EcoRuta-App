package com.ecoruta.app.core.testing

// Repositorio de reportes ciudadanos en memoria para pruebas unitarias
// Simula carga de fotografías a Storage, creación de reportes y consulta de historial sin red

import com.ecoruta.app.features.reports.domain.model.AffectationType
import com.ecoruta.app.features.reports.domain.model.CitizenReport
import com.ecoruta.app.features.reports.domain.model.ReportStatus
import com.ecoruta.app.features.reports.domain.repository.ReportRepository

class FakeReportRepository : ReportRepository {

    // Permite forzar un fallo simulado para probar el manejo de errores en ViewModels
    var error: Throwable? = null

    // Lista mutable en memoria con los reportes registrados del ciudadano de prueba
    val reports = mutableListOf(
        CitizenReport(
            id = "rep-101",
            citizenId = "test-citizen-id",
            type = AffectationType.CONTENEDOR_LLENO,
            status = ReportStatus.APROBADO,
            latitude = 4.0847,
            longitude = -76.1953,
            address = "Calle 25 #14-10",
            area = "Centro",
            description = "Contenedor desbordado frente al parque",
            photoUrl = "http://127.0.0.1:54321/storage/v1/object/public/report-photos/test-citizen-id/foto1.jpg",
            photoUrls = listOf("http://127.0.0.1:54321/storage/v1/object/public/report-photos/test-citizen-id/foto1.jpg"),
            createdAt = "2026-11-06T15:00:00Z",
            createdAtMillis = 1_793_977_200_000L,
        ),
    )

    // Registra las fotografías recibidas en cada llamada a submitReport para aserciones en tests
    val uploadedPhotoBatches = mutableListOf<List<ByteArray>>()

    private inline fun <T> result(block: () -> T): Result<T> =
        error?.let { Result.failure(it) } ?: runCatching { block() }

    // Simula la subida de hasta tres evidencias fotográficas y la inserción del reporte en estado pendiente
    override suspend fun submitReport(
        type: AffectationType,
        latitude: Double,
        longitude: Double,
        photosBytes: List<ByteArray>,
        address: String,
        area: String,
        description: String,
        pointId: String?,
        photoExtension: String,
    ): Result<CitizenReport> = result {
        require(photosBytes.isNotEmpty()) {
            "Debe adjuntar al menos una fotografía de evidencia para el reporte"
        }
        require(photosBytes.size <= 3) {
            "Un reporte no puede contener más de 3 fotografías de evidencia"
        }
        photosBytes.forEachIndexed { index, bytes ->
            require(bytes.isNotEmpty()) { "La fotografía #${index + 1} está vacía" }
        }

        uploadedPhotoBatches.add(photosBytes)

        val newId = "rep-${reports.size + 101}"
        val cleanExt = photoExtension.trim().trimStart('.').ifBlank { "jpg" }
        val generatedUrls = photosBytes.indices.map { index ->
            "http://127.0.0.1:54321/storage/v1/object/public/report-photos/test-citizen-id/${newId}_${index + 1}.$cleanExt"
        }

        val created = CitizenReport(
            id = newId,
            citizenId = "test-citizen-id",
            pointId = pointId,
            type = type,
            status = ReportStatus.PENDIENTE,
            latitude = latitude,
            longitude = longitude,
            address = address,
            area = area,
            description = description,
            photoUrl = generatedUrls.firstOrNull(),
            photoUrls = generatedUrls,
            createdAt = "2026-11-06T20:00:00Z",
            createdAtMillis = 1_793_995_200_000L + reports.size * 1_000L,
        )

        reports.add(0, created)
        created
    }

    // Devuelve los reportes almacenados en memoria ordenados por fecha de creación descendente
    override suspend fun getMyReports(): Result<List<CitizenReport>> = result {
        reports.sortedByDescending { it.createdAtMillis }
    }

    // Busca un reporte en memoria por su identificador único o lanza error si no existe
    override suspend fun getReportById(reportId: String): Result<CitizenReport> = result {
        require(reportId.isNotBlank()) { "El identificador del reporte no puede estar vacío" }
        reports.find { it.id == reportId }
            ?: error("No se encontró el reporte solicitado")
    }
}
