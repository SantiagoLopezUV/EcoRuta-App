package com.ecoruta.app.features.circular_economy.presentation.publish

// Pruebas unitarias de ListingsViewModel: carga de publicaciones, rol activo y manejo de errores

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeCircularEconomyRepository
import com.ecoruta.app.core.testing.FakeProfileRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.features.profile.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeCircularEconomyRepository: FakeCircularEconomyRepository
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var viewModel: ListingsViewModel

    @Before
    fun setUp() {
        fakeCircularEconomyRepository = FakeCircularEconomyRepository()
        fakeProfileRepository = FakeProfileRepository()
    }

    @Test
    fun initialLoad_setsLoadingThenLoaded() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val state = viewModel.homeState.value
        assertTrue(state is RecyclingHomeUiState.Loaded)
    }

    @Test
    fun initialLoad_exposesPublicationsFromRepository() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(fakeCircularEconomyRepository.listings.size, loaded.publications.size)
        assertEquals("LST-01", loaded.publications.first().id)
    }

    @Test
    fun initialLoad_exposesCitizenRoleByDefault() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(UserRole.CIUDADANO, loaded.activeRole)
    }

    @Test
    fun initialLoad_exposesComercioRole_whenProfileHasBusinessRole() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(
            activeRole = UserRole.COMERCIO,
            approvedRoles = setOf(UserRole.CIUDADANO, UserRole.COMERCIO),
        )

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(UserRole.COMERCIO, loaded.activeRole)
    }

    @Test
    fun initialLoad_exposesEcoPointsFromProfile() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(ecoPoints = 1_250)

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(1_250, loaded.ecoPoints)
    }

    @Test
    fun initialLoad_bothFail_setsErrorState() = runTest {
        fakeCircularEconomyRepository.error = RuntimeException("Listings unavailable")
        fakeProfileRepository.error = RuntimeException("Profile unavailable")

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        assertTrue(viewModel.homeState.value is RecyclingHomeUiState.Error)
    }

    @Test
    fun initialLoad_onlyProfileFails_stillLoadsPublications() = runTest {
        fakeProfileRepository.error = RuntimeException("Profile unavailable")

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(fakeCircularEconomyRepository.listings.size, loaded.publications.size)
        assertNull(loaded.ecoPoints)
        assertEquals(UserRole.CIUDADANO, loaded.activeRole)
    }

    @Test
    fun initialLoad_onlyListingsFail_stillLoadsProfile() = runTest {
        fakeCircularEconomyRepository.error = RuntimeException("Listings unavailable")

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertTrue(loaded.publications.isEmpty())
        assertEquals(fakeProfileRepository.currentProfile.ecoPoints, loaded.ecoPoints)
    }

    @Test
    fun reload_refreshesStateAfterError() = runTest {
        fakeCircularEconomyRepository.error = RuntimeException("Temporary error")
        fakeProfileRepository.error = RuntimeException("Temporary error")

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()
        assertTrue(viewModel.homeState.value is RecyclingHomeUiState.Error)

        fakeCircularEconomyRepository.error = null
        fakeProfileRepository.error = null
        viewModel.loadListings()
        advanceUntilIdle()

        assertTrue(viewModel.homeState.value is RecyclingHomeUiState.Loaded)
    }

    @Test
    fun materialTypeMapping_pet_returnsCorrectType() = runTest {
        fakeCircularEconomyRepository.listings.apply {
            clear()
            add(
                com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                    id = "T-01", code = "EC-001", material = "pet",
                    volume = "SMALL_BAG", address = "Test", status = "DISPONIBLE",
                    createdAtMillis = 1_000L,
                ),
            )
        }

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(MaterialType.PET, loaded.publications.first().material)
    }

    @Test
    fun statusMapping_reservado_returnsCorrectStatus() = runTest {
        fakeCircularEconomyRepository.listings.apply {
            clear()
            add(
                com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                    id = "T-02", code = "EC-002", material = "VIDRIO",
                    volume = "VOLUMINOSO", address = "Test", status = "reservado",
                    createdAtMillis = 1_000L,
                ),
            )
        }

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(PublicationStatus.RESERVADO, loaded.publications.first().status)
    }

    @Test
    fun statusMapping_supabaseDbStatuses_mapCorrectly() = runTest {
        fakeCircularEconomyRepository.listings.apply {
            clear()
            add(
                com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                    id = "T-03", code = "EC-003", material = "PET",
                    volume = "BOLSA_PEQUEÑA", address = "Test", status = "reservado",
                    createdAtMillis = 1_000L,
                ),
            )
            add(
                com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                    id = "T-04", code = "EC-004", material = "CARTON",
                    volume = "BOLSA_GRANDE", address = "Test", status = "completado",
                    createdAtMillis = 2_000L,
                ),
            )
            add(
                com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                    id = "T-05", code = "EC-005", material = "VIDRIO",
                    volume = "VOLUMINOSO", address = "Test", status = "cancelado",
                    createdAtMillis = 3_000L,
                ),
            )
        }

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(PublicationStatus.RESERVADO, loaded.publications[0].status)
        assertEquals(PublicationStatus.COMPLETADO, loaded.publications[1].status)
        assertEquals(PublicationStatus.CANCELADO, loaded.publications[2].status)
    }

    @Test
    fun initialLoad_associatesPublicationsWithBusiness_whenRoleIsComercio() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(
            id = "biz-profile-999",
            activeRole = UserRole.COMERCIO,
            approvedRoles = setOf(UserRole.CIUDADANO, UserRole.COMERCIO),
        )

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertTrue(loaded.publications.isNotEmpty())
        assertTrue(loaded.publications.all { it.isBusinessListing })
        assertTrue(loaded.publications.all { it.businessId == "biz-profile-999" })
    }

    @Test
    fun publish_missingVolume_setsVolumeError() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        viewModel.onAddressChange("Calle 10 #5-20")
        var successCalled = false
        viewModel.publish(onSuccess = { successCalled = true })

        assertFalse(successCalled)
        assertEquals(R.string.recycling_volume_required, viewModel.publishState.value.volumeError)
    }

    @Test
    fun publish_missingAddress_setsAddressError() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        viewModel.onVolumeSelected(MaterialVolume.BOLSA_PEQUENA)
        var successCalled = false
        viewModel.publish(onSuccess = { successCalled = true })

        assertFalse(successCalled)
        assertEquals(R.string.recycling_address_required, viewModel.publishState.value.addressError)
    }

    @Test
    fun publish_otherMaterialBlank_setsOtherMaterialError() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        viewModel.onMaterialSelected(MaterialType.OTRO)
        viewModel.onVolumeSelected(MaterialVolume.BOLSA_PEQUENA)
        viewModel.onAddressChange("Calle 10 #5-20")
        var successCalled = false
        viewModel.publish(onSuccess = { successCalled = true })

        assertFalse(successCalled)
        assertEquals(R.string.recycling_other_required, viewModel.publishState.value.otherMaterialError)
    }

    @Test
    fun publish_validForm_createsListingAndReloads() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val initialCount = fakeCircularEconomyRepository.listings.size
        viewModel.onMaterialSelected(MaterialType.PET)
        viewModel.onVolumeSelected(MaterialVolume.BOLSA_GRANDE)
        viewModel.onAddressChange("Carrera 20 #15-30")

        var successCalled = false
        viewModel.publish(onSuccess = { successCalled = true })
        advanceUntilIdle()

        assertTrue(successCalled)
        assertEquals(initialCount + 1, fakeCircularEconomyRepository.listings.size)
        val created = fakeCircularEconomyRepository.listings.first()
        assertEquals("PET", created.material)
        assertEquals("BOLSA_GRANDE", created.volume)
        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(initialCount + 1, loaded.publications.size)
    }

    @Test
    fun publish_cardboardAndSmallBag_persistsSpanishValues() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        viewModel.onMaterialSelected(MaterialType.CARTON)
        viewModel.onVolumeSelected(MaterialVolume.BOLSA_PEQUENA)
        viewModel.onAddressChange("Calle 10 #12-30")

        var successCalled = false
        viewModel.publish(onSuccess = { successCalled = true })
        advanceUntilIdle()

        assertTrue(successCalled)
        val created = fakeCircularEconomyRepository.listings.first()
        assertEquals("CARTON", created.material)
        assertEquals("BOLSA_PEQUEÑA", created.volume)
        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(MaterialType.CARTON, loaded.publications.first().material)
        assertEquals(MaterialVolume.BOLSA_PEQUENA, loaded.publications.first().volume)
    }

    @Test
    fun materialAndVolumeMapping_supabaseSpanishValues_mapCorrectly() = runTest {
        fakeCircularEconomyRepository.listings.apply {
            clear()
            add(
                com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                    id = "T-10", code = "EC-010", material = "CARTÓN",
                    volume = "BOLSA_PEQUEÑA", address = "Test", status = "disponible",
                    createdAtMillis = 1_000L,
                ),
            )
            add(
                com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                    id = "T-11", code = "EC-011", material = "VIDRIO",
                    volume = "VOLUMINOSO", address = "Test", status = "disponible",
                    createdAtMillis = 2_000L,
                ),
            )
        }

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(MaterialType.CARTON, loaded.publications[0].material)
        assertEquals(MaterialVolume.BOLSA_PEQUENA, loaded.publications[0].volume)
        assertEquals(MaterialType.VIDRIO, loaded.publications[1].material)
        assertEquals(MaterialVolume.VOLUMINOSO, loaded.publications[1].volume)
    }

    @Test
    fun publish_repositoryError_setsGeneralError() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        fakeCircularEconomyRepository.error = RuntimeException("Fallo en base de datos")
        viewModel.onMaterialSelected(MaterialType.PET)
        viewModel.onVolumeSelected(MaterialVolume.BOLSA_GRANDE)
        viewModel.onAddressChange("Carrera 20 #15-30")

        var successCalled = false
        viewModel.publish(onSuccess = { successCalled = true })
        advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(R.string.recycling_publish_error, viewModel.publishState.value.generalError)
        assertFalse(viewModel.publishState.value.isPublishing)
    }

    @Test
    fun initialLoad_mapsPhotoUrlCorrectly() = runTest {
        fakeCircularEconomyRepository.listings.clear()
        fakeCircularEconomyRepository.listings.add(
            com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                id = "LST-PHOTO",
                code = "ECO-PHOTO",
                material = "PET",
                volume = "SMALL_BAG",
                address = "Calle 10",
                status = "disponible",
                createdAtMillis = 1_000L,
                photoUrl = "content://media/photos/99",
            )
        )

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals("content://media/photos/99", loaded.publications.first().photoUrl)
    }

    @Test
    fun getListingById_returnsCachedListing() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val result = viewModel.getListingById("LST-01")
        assertTrue(result.isSuccess)
        val pub = result.getOrNull()
        assertNotNull(pub)
        assertEquals("LST-01", pub?.id)
        assertEquals(PublicationStatus.RESERVADO, pub?.status)
    }

    @Test
    fun getListingById_fetchesFromRepository_whenNotCached() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        // Agregamos un listing al repositorio que no está en el listado cargado
        val extra = com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
            id = "LST-99", code = "ECO-99", material = "VIDRIO",
            volume = "VOLUMINOSO", address = "Calle 50 #10-20", status = "disponible",
            createdAtMillis = 5_000L, photoUrl = "https://example.com/photo.jpg",
        )
        fakeCircularEconomyRepository.listings.add(extra)

        val result = viewModel.getListingById("LST-99")
        assertTrue(result.isSuccess)
        val pub = result.getOrNull()
        assertNotNull(pub)
        assertEquals("LST-99", pub?.id)
        assertEquals(PublicationStatus.DISPONIBLE, pub?.status)
        assertEquals("https://example.com/photo.jpg", pub?.photoUrl)
    }

    @Test
    fun cancelPublication_success_updatesListingAndReloads() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        var successCalled = false
        viewModel.cancelPublication(
            id = "LST-03",
            onSuccess = { successCalled = true },
        )
        advanceUntilIdle()

        assertTrue(successCalled)
        assertFalse(viewModel.isCancelling.value)
        val listing = fakeCircularEconomyRepository.listings.first { it.id == "LST-03" }
        assertEquals("cancelado", listing.status)
        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        val pub = loaded.publications.first { it.id == "LST-03" }
        assertEquals(PublicationStatus.CANCELADO, pub.status)
    }

    @Test
    fun cancelPublication_error_callsOnError() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        fakeCircularEconomyRepository.error = RuntimeException("Error al cancelar en Supabase")
        var errorMsg: String? = null
        viewModel.cancelPublication(
            id = "LST-03",
            onError = { errorMsg = it },
        )
        advanceUntilIdle()

        assertNotNull(errorMsg)
        assertEquals("Error al cancelar en Supabase", errorMsg)
        assertFalse(viewModel.isCancelling.value)
    }
}
