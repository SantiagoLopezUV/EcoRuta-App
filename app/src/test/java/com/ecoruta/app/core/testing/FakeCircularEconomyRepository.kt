package com.ecoruta.app.core.testing

// Repositorio de economía circular en memoria para pruebas unitarias sin red ni base de datos

import com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing
import com.ecoruta.app.features.circular_economy.domain.repository.CircularEconomyRepository

class FakeCircularEconomyRepository : CircularEconomyRepository {

    var error: Throwable? = null

    val listings = mutableListOf(
        RecyclableListing(
            id = "LST-01",
            code = "ECO-REC-3041",
            material = "PET",
            volume = "LARGE_BAG",
            address = "Cra. 22E #6-40",
            status = "EN_CAMINO",
            createdAtMillis = 1_760_100_000_000L,
            recyclerName = "Carlos Gómez",
            recyclerAffiliation = "Asorecicladores del Oriente",
            recyclerPhone = "3112345678",
        ),
        RecyclableListing(
            id = "LST-02",
            code = "ECO-REC-2980",
            material = "CARDBOARD",
            volume = "SMALL_BAG",
            address = "Calle 14 #8-20",
            status = "ENTREGADA",
            createdAtMillis = 1_759_900_000_000L,
            deliveredAtMillis = 1_759_950_000_000L,
            pointsEarned = 80,
            balanceAfter = 1_330,
        ),
        RecyclableListing(
            id = "LST-03",
            code = "ECO-BIZ-0012",
            material = "GLASS",
            volume = "BULKY",
            address = "Carrera 15 #25-10",
            status = "DISPONIBLE",
            createdAtMillis = 1_760_050_000_000L,
            isBusinessListing = true,
            businessId = "biz-001",
        ),
    )

    val publishedListings = mutableListOf<RecyclableListing>()

    override suspend fun getMyListings(): Result<List<RecyclableListing>> =
        error?.let { Result.failure(it) } ?: Result.success(listings.toList())

    override suspend fun publishListing(
        material: String,
        otherMaterial: String?,
        volume: String,
        address: String,
        businessId: String?,
    ): Result<RecyclableListing> {
        if (error != null) return Result.failure(error!!)
        val listing = RecyclableListing(
            id = "LST-NEW-${publishedListings.size + 1}",
            code = "ECO-${System.currentTimeMillis()}",
            material = material,
            otherMaterial = otherMaterial,
            volume = volume,
            address = address,
            status = "DISPONIBLE",
            createdAtMillis = System.currentTimeMillis(),
            isBusinessListing = businessId != null,
            businessId = businessId,
        )
        publishedListings.add(listing)
        listings.add(listing)
        return Result.success(listing)
    }
}
