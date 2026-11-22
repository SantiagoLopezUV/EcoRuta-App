package com.ecoruta.app.features.circular_economy.presentation.publish

// Pruebas unitarias de DeliveryQrViewModel: carga de token QR, renovación automática y errores

import androidx.lifecycle.SavedStateHandle
import com.ecoruta.app.core.navigation.NavigationDestination
import com.ecoruta.app.core.testing.FakeCircularEconomyRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.features.circular_economy.domain.model.DeliveryToken
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
class DeliveryQrViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeCircularEconomyRepository
    private lateinit var savedState: SavedStateHandle

    @Before
    fun setUp() {
        fakeRepository = FakeCircularEconomyRepository()
        savedState = SavedStateHandle(mapOf(NavigationDestination.ARG_ID to "LST-01"))
    }

    @Test
    fun initialLoad_exposesTokenFromRepository() = runTest {
        val viewModel = DeliveryQrViewModel(savedState, fakeRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.code)
        assertEquals("ECO-TEST-0001", state.code?.token)
    }

    @Test
    fun initialLoad_exposesPublicationInfo() = runTest {
        val viewModel = DeliveryQrViewModel(savedState, fakeRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.publication)
        assertEquals("LST-01", state.publication?.id)
    }

    @Test
    fun initialLoad_recyclerNameExposedInPublication() = runTest {
        val viewModel = DeliveryQrViewModel(savedState, fakeRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Carlos Gómez", state.publication?.recycler?.name)
    }

    @Test
    fun initialLoad_onDeliveryCodeFailure_setsError() = runTest {
        fakeRepository.deliveryError = RuntimeException("Token service unavailable")

        val viewModel = DeliveryQrViewModel(savedState, fakeRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.code)
        assertNotNull(state.error)
    }

    @Test
    fun initialLoad_onListingFailure_stillLoadsToken() = runTest {
        fakeRepository.error = RuntimeException("Listing not found")

        val viewModel = DeliveryQrViewModel(savedState, fakeRepository)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.code)
        assertNull(state.publication)
    }

    @Test
    fun reload_afterError_clearsErrorAndLoadsToken() = runTest {
        fakeRepository.deliveryError = RuntimeException("Temporary error")
        val viewModel = DeliveryQrViewModel(savedState, fakeRepository)
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.error)

        fakeRepository.deliveryError = null
        viewModel.loadQrCode()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.error)
        assertNotNull(state.code)
    }

    @Test
    fun autoRefresh_requestsNewToken_whenExpired() = runTest {
        fakeRepository.nextToken = DeliveryToken(token = "ECO-FIRST", expiresAtMillis = 1L)
        val viewModel = DeliveryQrViewModel(savedState, fakeRepository)
        advanceUntilIdle()

        // After expiry, the ViewModel requests a new token; simulate a fresh one
        fakeRepository.nextToken = DeliveryToken(token = "ECO-RENEWED", expiresAtMillis = Long.MAX_VALUE)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("ECO-RENEWED", state.code?.token)
    }

    @Test
    fun state_isNotLoading_afterSuccessfulLoad() = runTest {
        val viewModel = DeliveryQrViewModel(savedState, fakeRepository)
        advanceUntilIdle()

        assertTrue(!viewModel.state.value.isLoading)
    }
}
