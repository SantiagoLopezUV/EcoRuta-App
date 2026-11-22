package com.ecoruta.app.features.circular_economy.presentation.publish

// Pruebas unitarias de ListingsViewModel: carga de publicaciones, rol activo y manejo de errores

import com.ecoruta.app.core.testing.FakeCircularEconomyRepository
import com.ecoruta.app.core.testing.FakeProfileRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.features.profile.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun statusMapping_enCamino_returnsCorrectStatus() = runTest {
        fakeCircularEconomyRepository.listings.apply {
            clear()
            add(
                com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing(
                    id = "T-02", code = "EC-002", material = "GLASS",
                    volume = "BULKY", address = "Test", status = "en_camino",
                    createdAtMillis = 1_000L,
                ),
            )
        }

        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(PublicationStatus.EN_CAMINO, loaded.publications.first().status)
    }
}
