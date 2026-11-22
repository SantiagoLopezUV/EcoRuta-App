package com.ecoruta.app.features.circular_economy.presentation.publish

// Pruebas unitarias de ListingsViewModel: rol activo, businessId, publicación por ciudadano y comercio

import com.ecoruta.app.core.testing.FakeCircularEconomyRepository
import com.ecoruta.app.core.testing.FakeProfileRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.features.profile.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    // --- Carga inicial ---

    @Test
    fun initialLoad_exposesPublicationsFromRepository() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(fakeCircularEconomyRepository.listings.size, loaded.publications.size)
    }

    @Test
    fun initialLoad_exposesCitizenRoleByDefault() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(UserRole.CIUDADANO, loaded.activeRole)
        assertNull(loaded.businessId)
    }

    @Test
    fun initialLoad_exposesComercioRoleAndBusinessId() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(
            activeRole = UserRole.COMERCIO,
            approvedRoles = setOf(UserRole.CIUDADANO, UserRole.COMERCIO),
            businessId = "biz-999",
        )
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertEquals(UserRole.COMERCIO, loaded.activeRole)
        assertEquals("biz-999", loaded.businessId)
    }

    @Test
    fun initialLoad_businessIdNullWhenCitizenRole() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(
            activeRole = UserRole.CIUDADANO,
            businessId = "biz-999",
        )
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        val loaded = viewModel.homeState.value as RecyclingHomeUiState.Loaded
        assertNull(loaded.businessId)
    }

    @Test
    fun initialLoad_bothFail_setsErrorState() = runTest {
        fakeCircularEconomyRepository.error = RuntimeException("Network error")
        fakeProfileRepository.error = RuntimeException("Network error")
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        assertTrue(viewModel.homeState.value is RecyclingHomeUiState.Error)
    }

    // --- Publicación ciudadano ---

    @Test
    fun citizenPublish_withValidForm_createsListingWithoutBusinessId() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()
        viewModel.onMaterialSelected(MaterialType.PET)
        viewModel.onVolumeSelected(MaterialVolume.SMALL_BAG)
        viewModel.onAddressChange("Calle 10 #5-20")

        viewModel.onPublish()
        advanceUntilIdle()

        assertEquals(1, fakeCircularEconomyRepository.publishedListings.size)
        assertNull(fakeCircularEconomyRepository.publishedListings.first().businessId)
    }

    @Test
    fun citizenPublish_withBlankAddress_setsAddressError() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()
        viewModel.onMaterialSelected(MaterialType.PET)
        viewModel.onVolumeSelected(MaterialVolume.SMALL_BAG)

        viewModel.onPublish()

        assertNotNull(viewModel.publishState.value.addressError)
        assertEquals(0, fakeCircularEconomyRepository.publishedListings.size)
    }

    @Test
    fun citizenPublish_withoutVolume_setsVolumeError() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()
        viewModel.onMaterialSelected(MaterialType.GLASS)
        viewModel.onAddressChange("Carrera 8 #12-00")

        viewModel.onPublish()

        assertNotNull(viewModel.publishState.value.volumeError)
    }

    @Test
    fun citizenPublish_otherMaterialBlank_setsOtherMaterialError() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()
        viewModel.onMaterialSelected(MaterialType.OTHER)
        viewModel.onVolumeSelected(MaterialVolume.LARGE_BAG)
        viewModel.onAddressChange("Calle 7 #3-10")

        viewModel.onPublish()

        assertNotNull(viewModel.publishState.value.otherMaterialError)
    }

    // --- Publicación comercio ---

    @Test
    fun businessPublish_attachesBusinessIdToListing() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(
            activeRole = UserRole.COMERCIO,
            approvedRoles = setOf(UserRole.CIUDADANO, UserRole.COMERCIO),
            businessId = "biz-999",
        )
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()
        viewModel.onMaterialSelected(MaterialType.CARDBOARD)
        viewModel.onVolumeSelected(MaterialVolume.BULKY)
        viewModel.onAddressChange("Av. 6N #25-40")

        viewModel.onPublish()
        advanceUntilIdle()

        val published = fakeCircularEconomyRepository.publishedListings.first()
        assertEquals("biz-999", published.businessId)
        assertTrue(published.isBusinessListing)
    }

    @Test
    fun businessPublish_publishFormPreloadedWithBusinessId() = runTest {
        fakeProfileRepository.currentProfile = fakeProfileRepository.currentProfile.copy(
            activeRole = UserRole.COMERCIO,
            approvedRoles = setOf(UserRole.CIUDADANO, UserRole.COMERCIO),
            businessId = "biz-888",
        )
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()

        assertEquals("biz-888", viewModel.publishState.value.businessId)
    }

    @Test
    fun publishError_doesNotAddToList() = runTest {
        viewModel = ListingsViewModel(fakeCircularEconomyRepository, fakeProfileRepository)
        advanceUntilIdle()
        fakeCircularEconomyRepository.error = RuntimeException("Publish failed")
        viewModel.onMaterialSelected(MaterialType.PET)
        viewModel.onVolumeSelected(MaterialVolume.SMALL_BAG)
        viewModel.onAddressChange("Calle 1 #1-1")

        viewModel.onPublish()
        advanceUntilIdle()

        assertEquals(0, fakeCircularEconomyRepository.publishedListings.size)
    }
}
