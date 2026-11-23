package com.ecoruta.app.features.circular_economy.data.repository

// Implementación concreta del repositorio de economía circular
// Gestiona la creación de publicaciones de reciclaje y consulta de entregas propias del usuario

import com.ecoruta.app.features.circular_economy.data.remote.dto.ListingCreateDto
import com.ecoruta.app.features.circular_economy.data.remote.dto.ListingDto
import com.ecoruta.app.features.circular_economy.data.remote.dto.toDomain
import com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing
import com.ecoruta.app.features.circular_economy.domain.repository.CircularEconomyRepository
import com.ecoruta.app.features.reports.data.remote.dto.formatPostgisPoint
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject

class CircularEconomyRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
) : CircularEconomyRepository
{
    // Registra una nueva publicación de material reciclable con coordenadas PostGIS
    override suspend fun createListing(
        materialType: String,
        quantityCategory: String,
        latitude: Double,
        longitude: Double,
        address: String,
        neighborhood: String,
        photoUrl: String?,
        otherMaterial: String?,
    ): Result<RecyclableListing> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para publicar material reciclable")

        val createDto = ListingCreateDto(
            ownerId = userId,
            materialType = materialType.trim(),
            quantityCategory = quantityCategory.trim(),
            location = formatPostgisPoint(latitude, longitude),
            status = "disponible",
            address = address.trim().ifEmpty { null },
            neighborhood = neighborhood.trim().ifEmpty { null },
            photoUrl = photoUrl?.trim()?.ifEmpty { null },
        )

        val created = postgrest.from("recyclable_material_listings")
            .insert(createDto) {
                select()
            }
            .decodeSingle<ListingDto>()

        created.toDomain().copy(
            address = address,
            otherMaterial = otherMaterial,
        )
    }

    // Consulta las publicaciones de reciclaje del usuario autenticado ordenadas cronológicamente
    override suspend fun getMyListings(): Result<List<RecyclableListing>> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar publicaciones")

        postgrest.from("recyclable_material_listings")
            .select {
                filter {
                    eq("owner_id", userId)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ListingDto>()
            .map { it.toDomain() }
    }
}
