package com.ecoruta.app.features.bulky_waste.data.remote.dto

// Objeto de transferencia de datos para la tabla bulky_waste_requests y authorized_rcd_managers
// Mapea entre las filas de PostgreSQL y los modelos de dominio de escombros y voluminosos

import com.ecoruta.app.features.bulky_waste.domain.model.BulkyWasteRequest
import com.ecoruta.app.features.bulky_waste.domain.model.RcdManagerOrientation
import com.ecoruta.app.features.bulky_waste.domain.model.debrisStatusFromDb
import com.ecoruta.app.features.bulky_waste.domain.model.debrisTypeFromDb
import com.ecoruta.app.features.reports.data.remote.dto.parsePostgisPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class BulkyWasteRequestDto(
    @SerialName("id") val id: String,
    @SerialName("citizen_id") val citizenId: String,
    @SerialName("item_type") val itemType: String,
    @SerialName("description") val description: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("scheduled_date") val scheduledDate: String? = null,
    @SerialName("status") val status: String = "pendiente",
    @SerialName("cost") val cost: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("company_id") val companyId: String? = null,
)

@Serializable
data class BulkyWasteCreateDto(
    @SerialName("citizen_id") val citizenId: String,
    @SerialName("item_type") val itemType: String,
    @SerialName("description") val description: String? = null,
    @SerialName("location") val location: String,
    @SerialName("scheduled_date") val scheduledDate: String? = null,
    @SerialName("status") val status: String = "pendiente",
)

@Serializable
data class AuthorizedRcdManagerDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("item_types") val itemTypes: List<String> = emptyList(),
    @SerialName("phone") val phone: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("operating_hours") val operatingHours: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

// Convierte el DTO de la base de datos al modelo de dominio puro
fun BulkyWasteRequestDto.toDomain(): BulkyWasteRequest {
    val (lat, lng) = parsePostgisPoint(location)
    val createdAtEpoch = parseDateToMillis(createdAt) ?: System.currentTimeMillis()
    val scheduledDateEpoch = parseDateToMillis(scheduledDate)

    return BulkyWasteRequest(
        id = id,
        citizenId = citizenId,
        type = debrisTypeFromDb(itemType),
        description = description.orEmpty(),
        latitude = lat,
        longitude = lng,
        address = "",
        scheduledDateMillis = scheduledDateEpoch,
        status = debrisStatusFromDb(status),
        quotedCop = cost?.toLong(),
        createdAtMillis = createdAtEpoch,
        companyId = companyId,
    )
}

fun AuthorizedRcdManagerDto.toDomain(): RcdManagerOrientation = RcdManagerOrientation(
    id = id,
    name = name,
    phone = phone,
    address = address,
    operatingHours = operatingHours,
    description = description,
    acceptedTypes = itemTypes.map { debrisTypeFromDb(it) }
)

private fun parseDateToMillis(rawDate: String?): Long? {
    if (rawDate.isNullOrBlank()) return null
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd",
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return sdf.parse(rawDate)?.time
        } catch (_: Exception) {}
    }
    return null
}
