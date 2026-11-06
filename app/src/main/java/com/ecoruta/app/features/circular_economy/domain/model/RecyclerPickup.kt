package com.ecoruta.app.features.circular_economy.domain.model

// Modelo de dominio puro para una recogida reservada por el reciclador
// Une la recogida con la entrega del ciudadano, su volumen y su estado actual

data class RecyclerPickup(
    val id: String,
    val listingId: String,
    val materialType: String,
    val quantityCategory: String,
    val confirmedVolumeCategory: String? = null,
    val status: PickupStatus,
    val listingStatus: ListingStatus,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    // Solo una recogida pendiente se puede cancelar
    val canCancel: Boolean
        get() = status == PickupStatus.PENDIENTE
}
