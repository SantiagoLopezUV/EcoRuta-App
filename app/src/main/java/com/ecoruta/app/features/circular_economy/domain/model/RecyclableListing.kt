package com.ecoruta.app.features.circular_economy.domain.model

// Modelo de dominio puro para publicaciones de reciclaje
// Información del material ofertado por el ciudadano, estado y reciclador asignado

data class RecyclableListing(
    val id: String,
    val code: String,
    val material: String,
    val otherMaterial: String? = null,
    val volume: String,
    val address: String,
    val status: String,
    val createdAtMillis: Long,
    val isBusinessListing: Boolean = false,
    val businessId: String? = null,
    val recyclerName: String? = null,
    val recyclerAffiliation: String? = null,
    val recyclerPhone: String? = null,
    val deliveredAtMillis: Long? = null,
    val pointsEarned: Int? = null,
    val balanceAfter: Int? = null,
)
