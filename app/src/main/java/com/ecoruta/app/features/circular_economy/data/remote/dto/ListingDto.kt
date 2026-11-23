package com.ecoruta.app.features.circular_economy.data.remote.dto

// DTO serializable para la publicación de material reciclable
// Modela tipo de material (PET, cartón, vidrio), escala de volumen, foto y coordenadas PostGIS

import com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing
import com.ecoruta.app.features.reports.data.remote.dto.parsePostgisPoint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class ListingDto(
    @SerialName("id") val id: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("material_type") val materialType: String,
    @SerialName("quantity_category") val quantityCategory: String,
    @SerialName("location") val location: String? = null,
    @SerialName("status") val status: String = "disponible",
    @SerialName("address") val address: String? = null,
    @SerialName("neighborhood") val neighborhood: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("cancelled_at") val cancelledAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ListingCreateDto(
    @SerialName("owner_id") val ownerId: String,
    @SerialName("material_type") val materialType: String,
    @SerialName("quantity_category") val quantityCategory: String,
    @SerialName("location") val location: String,
    @SerialName("status") val status: String = "disponible",
    @SerialName("address") val address: String? = null,
    @SerialName("neighborhood") val neighborhood: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
)

// Convierte el DTO de PostgreSQL al modelo de dominio puro de economía circular
fun ListingDto.toDomain(): RecyclableListing {
    val (lat, lng) = parsePostgisPoint(location)
    val createdAtEpoch = parseDateToMillis(createdAt) ?: System.currentTimeMillis()

    return RecyclableListing(
        id = id,
        code = "ECO-REC-${id.takeLast(4).uppercase()}",
        material = materialType,
        otherMaterial = null,
        volume = quantityCategory,
        address = address.orEmpty(),
        status = status,
        createdAtMillis = createdAtEpoch,
        isBusinessListing = false,
        businessId = null,
        recyclerName = null,
        recyclerAffiliation = null,
        recyclerPhone = null,
        deliveredAtMillis = null,
        pointsEarned = null,
        balanceAfter = null,
        latitude = lat,
        longitude = lng,
        neighborhood = neighborhood,
        photoUrl = photoUrl,
        ownerId = ownerId,
    )
}

// Parsea fechas ISO 8601 a milisegundos de época de forma segura
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
