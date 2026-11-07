package com.ecoruta.app.features.reports.data.remote.dto

// DTO serializable para la recepción de reportes desde Supabase
// Mapea identificadores, coordenadas PostGIS, fotos, estados y fechas

import com.ecoruta.app.features.reports.domain.model.AffectationType
import com.ecoruta.app.features.reports.domain.model.CitizenReport
import com.ecoruta.app.features.reports.domain.model.ReportStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

//Fila de evidencia fotográfica leída desde la tabla public.report_photos
@Serializable
data class ReportPhotoDto(
    @SerialName("id") val id: String = "",
    @SerialName("report_id") val reportId: String = "",
    @SerialName("photo_url") val photoUrl: String,
    @SerialName("created_at") val createdAt: String = "",
)

// Representa un reporte ciudadano leído desde la tabla public.reports en Supabase
@Serializable
data class ReportDto(
    @SerialName("id") val id: String,
    @SerialName("citizen_id") val citizenId: String,
    @SerialName("point_id") val pointId: String? = null,
    @SerialName("type") val type: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("status") val status: String = "pendiente",
    @SerialName("validated_by") val validatedBy: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

// Convierte el DTO remoto y su lista de fotos asociadas al modelo puro de dominio CitizenReport
fun ReportDto.toDomain(
    photos: List<ReportPhotoDto> = emptyList(),
    address: String = "",
    area: String = "",
    description: String = "",
    statusNote: String? = null,
    originalReportId: String? = null,
): CitizenReport {
    val (lat, lng) = parsePostgisPoint(location)
    val allPhotos = buildList {
        if (!photoUrl.isNullOrBlank()) add(photoUrl)
        photos.forEach { photo ->
            if (photo.photoUrl.isNotBlank() && photo.photoUrl !in this) {
                add(photo.photoUrl)
            }
        }
    }
    val primaryPhoto = photoUrl?.takeIf { it.isNotBlank() } ?: allPhotos.firstOrNull()

    return CitizenReport(
        id = id,
        citizenId = citizenId,
        pointId = pointId,
        type = AffectationType.fromCode(type),
        status = ReportStatus.fromCode(status),
        latitude = lat,
        longitude = lng,
        address = address,
        area = area,
        description = description,
        photoUrl = primaryPhoto,
        photoUrls = allPhotos,
        statusNote = statusNote,
        originalReportId = originalReportId,
        validatedBy = validatedBy,
        createdAt = createdAt,
        createdAtMillis = parseIsoTimestampToMillis(createdAt),
        updatedAt = updatedAt,
    )
}

// Saca la latitud, longitud desde un texto WKT/EWKT ("POINT(lng lat)") o hexadecimal EWKB de PostGIS
fun parsePostgisPoint(rawLocation: String?): Pair<Double, Double> {
    if (rawLocation.isNullOrBlank()) return 0.0 to 0.0
    val trimmed = rawLocation.trim()

    // Formato WKT o EWKT: POINT(-76.1953 4.0847 o SRID=4326;POINT(-76.1953 4.0847
    val pointRegex = Regex("""POINT\s*\(\s*([+-]?\d+(?:\.\d+)?)\s+([+-]?\d+(?:\.\d+)?)\s*\)""", RegexOption.IGNORE_CASE)
    pointRegex.find(trimmed)?.let { match ->
        val lng = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val lat = match.groupValues[2].toDoubleOrNull() ?: 0.0
        return lat to lng
    }

    // Formato EWKB hexadecimal devuelto por PostgREST para geography(Point, 4326)
    if (trimmed.length >= 42 && trimmed.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
        return runCatching {
            val bytes = ByteArray(trimmed.length / 2) { i ->
                trimmed.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
            val buffer = ByteBuffer.wrap(bytes)
            val byteOrder = if (bytes[0].toInt() == 1) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN
            buffer.order(byteOrder)
            val typeInt = buffer.getInt(1)
            val hasSrid = (typeInt and 0x20000000) != 0
            val coordOffset = if (hasSrid) 9 else 5
            val lng = buffer.getDouble(coordOffset)
            val lat = buffer.getDouble(coordOffset + 8)
            lat to lng
        }.getOrDefault(0.0 to 0.0)
    }

    return 0.0 to 0.0
}

// convierte una fecha de Postgresql a milisegundos Epoch para las vistas en compose
fun parseIsoTimestampToMillis(isoString: String?): Long {
    if (isoString.isNullOrBlank()) return 0L
    val regex = Regex("""^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})(?:\.(\d+))?(?:Z|([+-]\d{2}):?(\d{2})?)?$""")
    val match = regex.find(isoString.trim()) ?: return 0L

    val datePart = match.groupValues[1]
    val timePart = match.groupValues[2]
    val millisPart = match.groupValues[3].padEnd(3, '0').take(3)
    val offsetHours = match.groupValues[4].toIntOrNull() ?: 0
    val offsetMinutes = match.groupValues[5].toIntOrNull() ?: 0

    return runCatching {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcMillis = formatter.parse("${datePart}T${timePart}.${millisPart}")?.time ?: 0L
        val sign = if (offsetHours < 0) -1 else 1
        val offsetMillis = (offsetHours * 3_600_000L) + (sign * offsetMinutes * 60_000L)
        utcMillis - offsetMillis
    }.getOrDefault(0L)
}
