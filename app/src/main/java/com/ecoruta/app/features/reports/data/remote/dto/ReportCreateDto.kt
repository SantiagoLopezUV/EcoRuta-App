package com.ecoruta.app.features.reports.data.remote.dto

// DTO para la creación de nuevos reportes comunitarios
// Transporta fotos cargadas a Storage, tipo de afectación y coordenadas GPS

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Estructura de inserción para crear un reporte ciudadano en la tabla public.reports
@Serializable
data class ReportCreateDto(
    @SerialName("citizen_id") val citizenId: String,
    @SerialName("type") val type: String,
    @SerialName("location") val location: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("point_id") val pointId: String? = null,
    @SerialName("status") val status: String = "pendiente",
)

// Estructura para registrar cada foto de evidencia en public.report_photos
@Serializable
data class ReportPhotoCreateDto(
    @SerialName("report_id") val reportId: String,
    @SerialName("photo_url") val photoUrl: String,
)

// Construye la representación textual EWKT que espera PostGIS (SRID 4326: longitud primero, latitud después)
fun formatPostgisPoint(latitude: Double, longitude: Double): String =
    "SRID=4326;POINT($longitude $latitude)"
