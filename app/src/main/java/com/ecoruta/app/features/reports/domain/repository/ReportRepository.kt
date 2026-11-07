package com.ecoruta.app.features.reports.domain.repository

// Contrato de dominio para operaciones de reportes de residuos
// Define operaciones abstractas para crear reporte y consultar historial propio

import com.ecoruta.app.features.reports.domain.model.AffectationType
import com.ecoruta.app.features.reports.domain.model.CitizenReport

interface ReportRepository {
    // Sube hasta tres fotografías de evidencia a Supabase Storage e inserta el reporte ciudadano
    suspend fun submitReport(
        type: AffectationType,
        latitude: Double,
        longitude: Double,
        photosBytes: List<ByteArray>,
        address: String = "",
        area: String = "",
        description: String = "",
        pointId: String? = null,
        photoExtension: String = "jpg"
    ): Result<CitizenReport>

    // Consulta el historial de reportes creados por el ciudadano autenticado ordenados por fecha
    suspend fun getMyReports(): Result<List<CitizenReport>>

    // Consulta el detalle de un reporte ciudadano específico por su identificador único
    suspend fun getReportById(reportId: String): Result<CitizenReport>
}
