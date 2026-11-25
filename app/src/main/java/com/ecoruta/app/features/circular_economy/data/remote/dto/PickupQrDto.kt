package com.ecoruta.app.features.circular_economy.data.remote.dto

// DTO serializable para la orden de recolección y su código QR de entrega
// Mapea el código dinámico y su vigencia desde public.recycler_pickups

import com.ecoruta.app.features.circular_economy.domain.model.DeliveryToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PickupQrDto(
    @SerialName("id") val id: String? = null,
    @SerialName("listing_id") val listingId: String? = null,
    @SerialName("recycler_id") val recyclerId: String? = null,
    @SerialName("qr_code") val qrCode: String,
    @SerialName("qr_expires_at") val qrExpiresAt: String,
    @SerialName("status") val status: String? = null,
)

// Convierte la respuesta de la orden de recolección al token de entrega de dominio
fun PickupQrDto.toDomain(): DeliveryToken
{
    val expiresEpoch = parseDateToMillis(qrExpiresAt) ?: (System.currentTimeMillis() + 5 * 60_000L)
    return DeliveryToken(
        token = qrCode,
        expiresAtMillis = expiresEpoch,
    )
}
