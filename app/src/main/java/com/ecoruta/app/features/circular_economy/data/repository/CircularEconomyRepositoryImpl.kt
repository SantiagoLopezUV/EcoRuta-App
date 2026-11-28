package com.ecoruta.app.features.circular_economy.data.repository

// Implementación concreta del repositorio de economía circular
// Gestiona la creación de publicaciones de reciclaje, consulta de entregas y emisión de códigos QR

import com.ecoruta.app.features.circular_economy.data.remote.dto.ListingCreateDto
import com.ecoruta.app.features.circular_economy.data.remote.dto.ListingDto
import com.ecoruta.app.features.circular_economy.data.remote.dto.PickupQrDto
import com.ecoruta.app.features.circular_economy.data.remote.dto.toDomain
import com.ecoruta.app.features.circular_economy.domain.model.DeliveryToken
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
) : CircularEconomyRepository {

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

    // Retorna una publicación individual por su identificador único
    override suspend fun getListing(id: String): Result<RecyclableListing> = runCatching {
        postgrest.from("recyclable_material_listings")
            .select {
                filter {
                    eq("id", id)
                }
            }
            .decodeSingle<ListingDto>()
            .toDomain()
    }

    // Cancela una publicación propia de material reciclable en Supabase
    override suspend fun cancelListing(id: String): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para cancelar la publicación")

        postgrest.from("recyclable_material_listings")
            .update(mapOf("status" to "cancelado")) {
                filter {
                    eq("id", id)
                    eq("owner_id", userId)
                }
            }
        Unit
    }

    // Obtiene el token de entrega dinámico asignado a la recolección de la publicación
    override suspend fun requestDeliveryCode(listingId: String): Result<DeliveryToken> = runCatching {
        val pickup = postgrest.from("recycler_pickups")
            .select {
                filter {
                    eq("listing_id", listingId)
                    isIn("status", listOf("pendiente", "en_camino"))
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<PickupQrDto>()
            .firstOrNull() ?: error("No se encontró una orden de recolección activa para la publicación solicitada")

        pickup.toDomain()
    }
}
