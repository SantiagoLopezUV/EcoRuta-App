package com.ecoruta.app.features.reports.data.repository

// Implementación concreta del repositorio de reportes
// Sube fotografías a Supabase Storage, inserta en Postgrest y consulta historial

import com.ecoruta.app.features.reports.data.remote.dto.ReportCreateDto
import com.ecoruta.app.features.reports.data.remote.dto.ReportDto
import com.ecoruta.app.features.reports.data.remote.dto.ReportPhotoCreateDto
import com.ecoruta.app.features.reports.data.remote.dto.ReportPhotoDto
import com.ecoruta.app.features.reports.data.remote.dto.formatPostgisPoint
import com.ecoruta.app.features.reports.data.remote.dto.toDomain
import com.ecoruta.app.features.reports.domain.model.AffectationType
import com.ecoruta.app.features.reports.domain.model.CitizenReport
import com.ecoruta.app.features.reports.domain.repository.ReportRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val storage: Storage,
) : ReportRepository {

    companion object {
        private const val REPORT_PHOTOS_BUCKET = "report-photos"
        private const val MAX_REPORT_PHOTOS = 3
    }

    // Sube las evidencias fotográficas a Storage e inserta el reporte ciudadano en PostgreSQL
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
    ): Result<CitizenReport> = runCatching {
        // Verifica que exista un ciudadano autenticado en la sesión actual
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para registrar el reporte")

        // Valida que se sugba 1 o 3 fotografías
        require(photosBytes.isNotEmpty()) {
            "Debe adjuntar al menos una fotografía de evidencia para el reporte"
        }
        require(photosBytes.size <= MAX_REPORT_PHOTOS) {
            "Un reporte no puede contener más de $MAX_REPORT_PHOTOS fotografías de evidencia"
        }

        val cleanExt = photoExtension.trim().trimStart('.').ifBlank { "jpg" }
        val bucket = storage.from(REPORT_PHOTOS_BUCKET)
        val baseTimestamp = System.currentTimeMillis()

        // Sube cada fotografía al bucket report-photos dentro de la carpeta del ciudadano y obtiene su URL pública
        val uploadedPhotoUrls = photosBytes.mapIndexed { index, bytes ->
            require(bytes.isNotEmpty()) { "La fotografía #${index + 1} está vacía" }
            val path = "$userId/${baseTimestamp}_${index + 1}.$cleanExt"
            bucket.upload(path, bytes) {
                upsert = true
            }
            bucket.publicUrl(path)
        }

        // Construye el DTO de creación con el punto geográfico SRID 4326 y la primera foto como portada
        val createDto = ReportCreateDto(
            citizenId = userId,
            type = type.typeCode,
            location = formatPostgisPoint(latitude = latitude, longitude = longitude),
            photoUrl = uploadedPhotoUrls.firstOrNull(),
            pointId = pointId?.takeIf { it.isNotBlank() },
            status = "pendiente",
        )

        // Inserta el reporte en public.reports y recupera la fila generada con su ID y fecha de creación
        val createdReportDto = postgrest.from("reports")
            .insert(createDto) {
                select()
            }
            .decodeSingle<ReportDto>()

        // Inserta las fotos en la tabla public.report_photos asociadas al reporte
        val photoCreateDtos = uploadedPhotoUrls.map { url ->
            ReportPhotoCreateDto(
                reportId = createdReportDto.id,
                photoUrl = url,
            )
        }
        val insertedPhotos = try {
            postgrest.from("report_photos")
                .insert(photoCreateDtos) {
                    select()
                }
                .decodeList<ReportPhotoDto>()
        } catch (_: Exception) {
            uploadedPhotoUrls.map { url ->
                ReportPhotoDto(reportId = createdReportDto.id, photoUrl = url)
            }
        }

        // Transforma la respuesta remota al modelo de dominio conservando la dirección, el barrio y las coordenadas
        createdReportDto.toDomain(
            photos = insertedPhotos,
            address = address,
            area = area,
            description = description,
        ).copy(
            latitude = latitude,
            longitude = longitude,
        )
    }

    // Consulta todos los reportes del ciudadano autenticado ordenados del más reciente al más antiguo
    override suspend fun getMyReports(): Result<List<CitizenReport>> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar el historial de reportes")

        // Obtiene los reportes propios filtrados por citizen_id desde public.reports
        val reportDtos = postgrest.from("reports")
            .select {
                filter { eq("citizen_id", userId) }
                order(column = "created_at", order = Order.DESCENDING)
            }
            .decodeList<ReportDto>()

        if (reportDtos.isEmpty()) return@runCatching emptyList()

        // Consulta las fotos asociadas en public.report_photos para todos los reportes encontrados
        val reportIds = reportDtos.map { it.id }
        val photosByReportId = try {
            postgrest.from("report_photos")
                .select {
                    filter { isIn("report_id", reportIds) }
                }
                .decodeList<ReportPhotoDto>()
                .groupBy { it.reportId }
        } catch (_: Exception) {
            emptyMap()
        }

        // Convierte cada DTO y su galería de fotos al modelo de dominio CitizenReport
        reportDtos.map { dto ->
            dto.toDomain(photos = photosByReportId[dto.id].orEmpty())
        }
    }

    // Consulta el detalle de un reporte ciudadano específico junto con sus fotografías de evidencia
    override suspend fun getReportById(reportId: String): Result<CitizenReport> = runCatching {
        require(reportId.isNotBlank()) { "El identificador del reporte no puede estar vacío" }

        // Busca el reporte por su uuid en public.reports
        val reportDto = postgrest.from("reports")
            .select {
                filter { eq("id", reportId) }
            }
            .decodeSingleOrNull<ReportDto>()
            ?: error("No se encontró el reporte solicitado")

        // Obtiene la galería de hasta tres fotografías asociadas en public.report_photos
        val reportPhotos = try {
            postgrest.from("report_photos")
                .select {
                    filter { eq("report_id", reportId) }
                }
                .decodeList<ReportPhotoDto>()
        } catch (_: Exception) {
            emptyList()
        }

        reportDto.toDomain(photos = reportPhotos)
    }
}
