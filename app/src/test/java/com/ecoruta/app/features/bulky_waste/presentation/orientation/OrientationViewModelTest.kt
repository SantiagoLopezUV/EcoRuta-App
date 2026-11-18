package com.ecoruta.app.features.bulky_waste.presentation.orientation

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeBulkyWasteRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Pruebas de la carga y el filtro de gestores autorizados en la guía de escombros
@OptIn(ExperimentalCoroutinesApi::class)
class OrientationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeBulkyWasteRepository
    private lateinit var viewModel: OrientationViewModel

    @Before
    fun setUp() {
        repository = FakeBulkyWasteRepository()
        viewModel = OrientationViewModel(repository)
    }

    @Test
    fun initialState_isLoading() {
        assertEquals(OrientationUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun renovationDebris_loadsBothManagersThatAcceptIt() = runTest {
        viewModel.onTypeSelected(DebrisType.RENOVATION_DEBRIS)
        advanceUntilIdle()

        val state = viewModel.uiState.value as OrientationUiState.Loaded
        assertEquals(listOf("rcd-01", "rcd-02"), state.managers.map { it.id })
    }

    @Test
    fun mattresses_keepsOnlyManagersThatAcceptThem() = runTest {
        viewModel.onTypeSelected(DebrisType.MATTRESSES)
        advanceUntilIdle()

        val state = viewModel.uiState.value as OrientationUiState.Loaded
        assertEquals(listOf("rcd-01"), state.managers.map { it.id })
    }

    @Test
    fun noTypeSelected_showsAllManagers() = runTest {
        viewModel.onTypeSelected(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value as OrientationUiState.Loaded
        assertEquals(2, state.managers.size)
    }

    @Test
    fun noManagerAcceptsType_showsEmpty() = runTest {
        repository.managers.removeAll { DebrisType.FURNITURE in it.acceptedTypes }

        viewModel.onTypeSelected(DebrisType.FURNITURE)
        advanceUntilIdle()

        assertEquals(OrientationUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun emptyDirectory_showsEmpty() = runTest {
        repository.managers.clear()

        viewModel.onTypeSelected(DebrisType.RENOVATION_DEBRIS)
        advanceUntilIdle()

        assertEquals(OrientationUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun repositoryFailure_showsError() = runTest {
        repository.error = IOException("sin red")

        viewModel.onTypeSelected(DebrisType.RENOVATION_DEBRIS)
        advanceUntilIdle()

        assertEquals(OrientationUiState.Error(R.string.debris_managers_error), viewModel.uiState.value)
    }

    @Test
    fun retryAfterFailure_loadsManagers() = runTest {
        repository.error = IOException("sin red")
        viewModel.onTypeSelected(DebrisType.RENOVATION_DEBRIS)
        advanceUntilIdle()

        repository.error = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is OrientationUiState.Loaded)
    }

    @Test
    fun sameTypeAgain_doesNotReload() = runTest {
        viewModel.onTypeSelected(DebrisType.RENOVATION_DEBRIS)
        advanceUntilIdle()
        repository.error = IOException("no debería consultarse")

        viewModel.onTypeSelected(DebrisType.RENOVATION_DEBRIS)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is OrientationUiState.Loaded)
    }
}
