package com.ecoruta.app.features.reports.presentation.list

import com.ecoruta.app.core.testing.FakeReportRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Pruebas unitarias de MyReportsViewModel: carga inicial de reportes y manejo de errores
@OptIn(ExperimentalCoroutinesApi::class)
class MyReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeReportRepository
    private lateinit var viewModel: MyReportsViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeReportRepository()
    }

    @Test
    fun initialLoad_loadsReportsFromRepository() = runTest {
        viewModel = MyReportsViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MyReportsUiState.Loaded)
        val loaded = state as MyReportsUiState.Loaded
        assertEquals(fakeRepository.reports.size, loaded.reports.size)
        assertEquals("rep-101", loaded.reports.first().id)
    }

    @Test
    fun initialLoad_onRepositoryFailure_setsErrorState() = runTest {
        fakeRepository.error = RuntimeException("Network error")
        viewModel = MyReportsViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MyReportsUiState.Error)
    }

    @Test
    fun refresh_reloadsReportsSuccessfully() = runTest {
        fakeRepository.error = RuntimeException("Temporary error")
        viewModel = MyReportsViewModel(fakeRepository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is MyReportsUiState.Error)

        // Clear error and refresh
        fakeRepository.error = null
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MyReportsUiState.Loaded)
        val loaded = state as MyReportsUiState.Loaded
        assertEquals(fakeRepository.reports.size, loaded.reports.size)
    }
}

