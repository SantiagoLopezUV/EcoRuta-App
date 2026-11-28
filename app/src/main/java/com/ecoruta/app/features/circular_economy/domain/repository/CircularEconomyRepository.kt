package com.ecoruta.app.features.circular_economy.domain.repository

// Contrato de dominio para operaciones de reciclaje y bolsa comunitaria
// Declaración abstracta para publicar, reclamar 1:1 y validar entrega presencial

import com.ecoruta.app.features.circular_economy.domain.model.DeliveryToken
import com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing

interface CircularEconomyRepository {

    // Registra una nueva publicación de material reciclable
    suspend fun createListing(
        materialType: String,
        quantityCategory: String,
        latitude: Double,
        longitude: Double,
        address: String = "",
        neighborhood: String = "",
        photoUrl: String? = null,
        otherMaterial: String? = null,
    ): Result<RecyclableListing>

    // Consulta las publicaciones de reciclaje del ciudadano o comercio autenticado
    suspend fun getMyListings(): Result<List<RecyclableListing>>

    // Retorna una publicación individual por su identificador
    suspend fun getListing(id: String): Result<RecyclableListing>

    // Cancela una publicación propia de material reciclable actualizando su estado a cancelado
    suspend fun cancelListing(id: String): Result<Unit>

    // Genera un token de entrega de un solo uso válido por 5 minutos para la publicación indicada
    suspend fun requestDeliveryCode(listingId: String): Result<DeliveryToken>
}
