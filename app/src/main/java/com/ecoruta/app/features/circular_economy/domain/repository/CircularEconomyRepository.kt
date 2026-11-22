package com.ecoruta.app.features.circular_economy.domain.repository

// Contrato de dominio para operaciones de reciclaje y bolsa comunitaria
// Declaración abstracta para publicar, reclamar 1:1 y validar entrega presencial

import com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing

interface CircularEconomyRepository {

    // Retorna las publicaciones del ciudadano o comercio autenticado
    suspend fun getMyListings(): Result<List<RecyclableListing>>

    // Publica una oferta de material reciclable; businessId no nulo vincula al comercio
    suspend fun publishListing(
        material: String,
        otherMaterial: String?,
        volume: String,
        address: String,
        businessId: String?,
    ): Result<RecyclableListing>
}
