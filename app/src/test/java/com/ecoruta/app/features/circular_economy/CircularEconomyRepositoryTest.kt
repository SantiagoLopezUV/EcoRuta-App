package com.ecoruta.app.features.circular_economy

// Pruebas unitarias para las operaciones del repositorio de economía circular y mapeo DTO
// Valida inserción de ofertas, consulta de listados y conversión de coordenadas PostGIS

import com.ecoruta.app.core.testing.FakeCircularEconomyRepository
import com.ecoruta.app.features.circular_economy.data.remote.dto.ListingDto
import com.ecoruta.app.features.circular_economy.data.remote.dto.toDomain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CircularEconomyRepositoryTest
{
    private lateinit var repository: FakeCircularEconomyRepository

    @Before
    fun setUp() {
        repository = FakeCircularEconomyRepository()
    }

    @Test
    fun createListing_success_addsToListAndReturnsListing() = runTest {
        val initialCount = repository.listings.size
        val result = repository.createListing(
            materialType = "PET",
            quantityCategory = "LARGE_BAG",
            latitude = 4.0847,
            longitude = -76.1953,
            address = "Calle 25 #12-30",
            neighborhood = "Centro",
            photoUrl = "https://storage.supabase.co/listings/test.jpg",
        )

        assertTrue("La inserción debe completarse exitosamente", result.isSuccess)
        val listing = result.getOrNull()
        assertNotNull(listing)
        assertEquals("PET", listing?.material)
        assertEquals("LARGE_BAG", listing?.volume)
        assertEquals("Calle 25 #12-30", listing?.address)
        assertEquals(4.0847, listing?.latitude ?: 0.0, 0.0001)
        assertEquals(-76.1953, listing?.longitude ?: 0.0, 0.0001)
        assertEquals(initialCount + 1, repository.listings.size)
    }

    @Test
    fun createListing_withError_returnsFailure() = runTest {
        repository.error = RuntimeException("Error simulado de red o base de datos")

        val result = repository.createListing(
            materialType = "CARDBOARD",
            quantityCategory = "SMALL_BAG",
            latitude = 4.08,
            longitude = -76.19,
        )

        assertTrue("Debe retornar fallo cuando ocurre un error en el repositorio", result.isFailure)
        assertEquals("Error simulado de red o base de datos", result.exceptionOrNull()?.message)
    }

    @Test
    fun getMyListings_success_returnsAllUserListings() = runTest {
        val result = repository.getMyListings()

        assertTrue("La consulta debe ser exitosa", result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertEquals(3, list?.size)
    }

    @Test
    fun getMyListings_withError_returnsFailure() = runTest {
        repository.error = RuntimeException("No se pudieron cargar las publicaciones")

        val result = repository.getMyListings()

        assertTrue("Debe retornar fallo al ocurrir un error", result.isFailure)
        assertEquals("No se pudieron cargar las publicaciones", result.exceptionOrNull()?.message)
    }

    @Test
    fun listingDto_toDomain_mapsFieldsCorrectly() {
        val dto = ListingDto(
            id = "test-uuid-1234",
            ownerId = "owner-uuid-5678",
            materialType = "GLASS",
            quantityCategory = "BULKY",
            location = "SRID=4326;POINT(-76.1953 4.0847)",
            status = "disponible",
            address = "Carrera 40 #10-15",
            neighborhood = "Victoria",
            photoUrl = "https://example.com/glass.jpg",
            createdAt = "2026-11-20T10:30:00Z",
        )

        val domain = dto.toDomain()

        assertEquals("test-uuid-1234", domain.id)
        assertEquals("ECO-REC-1234", domain.code)
        assertEquals("GLASS", domain.material)
        assertEquals("BULKY", domain.volume)
        assertEquals("Carrera 40 #10-15", domain.address)
        assertEquals("disponible", domain.status)
        assertEquals(4.0847, domain.latitude ?: 0.0, 0.0001)
        assertEquals(-76.1953, domain.longitude ?: 0.0, 0.0001)
        assertEquals("Victoria", domain.neighborhood)
        assertEquals("https://example.com/glass.jpg", domain.photoUrl)
        assertEquals("owner-uuid-5678", domain.ownerId)
    }
}
