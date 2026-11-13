package com.ecoruta.app.features.bulky_waste.presentation.tracking

// Pruebas unitarias para RcdTrackingViewModel (BulkyTrackingViewModel)
// Valida visualización de estados, cotización, actualización y cancelación de solicitudes RCD

import com.ecoruta.app.core.testing.FakeBulkyWasteRepository
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RcdTrackingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: FakeBulkyWasteRepository
    private lateinit var viewModel: RcdTrackingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBulkyWasteRepository()
        viewModel = RcdTrackingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Valida carga exitosa de las solicitudes del ciudadano con sus respectivos estados y costos
    @Test
    fun testLoadMyRequestsSuccess() = testScope.runTest {
        viewModel.loadMyRequests()
        advanceUntilIdle()

        val state = viewModel.myRequestsState.value
        assertTrue(state is MyDebrisRequestsUiState.Loaded)
        val loaded = state as MyDebrisRequestsUiState.Loaded
        assertFalse(loaded.isSample)
        assertEquals(2, loaded.requests.size)

        // req-101 es PENDIENTE sin cotizar
        val req101 = loaded.requests.first { it.id == "req-101" }
        assertEquals(DebrisStatus.PENDIENTE, req101.status)
        assertEquals(null, req101.quotedCop)

        // req-102 es COTIZADO con costo de 85.000 COP
        val req102 = loaded.requests.first { it.id == "req-102" }
        assertEquals(DebrisStatus.COTIZADO, req102.status)
        assertEquals(85_000L, req102.quotedCop)
    }

    // Valida manejo de errores en la lista de solicitudes cuando el repositorio falla
    @Test
    fun testLoadMyRequestsError() = testScope.runTest {
        repository.error = RuntimeException("Error simulado de conexión")
        viewModel.loadMyRequests()
        advanceUntilIdle()

        val state = viewModel.myRequestsState.value
        assertTrue(state is MyDebrisRequestsUiState.Error)
    }

    // Valida carga exitosa del detalle de una solicitud para la vista de seguimiento
    @Test
    fun testLoadRequestByIdSuccess() = testScope.runTest {
        viewModel.loadRequest("req-102")
        advanceUntilIdle()

        val state = viewModel.trackingState.value
        assertTrue(state is DebrisTrackingUiState.Loaded)
        val loaded = state as DebrisTrackingUiState.Loaded
        assertEquals("req-102", loaded.request.id)
        assertEquals(DebrisStatus.COTIZADO, loaded.request.status)
        assertEquals(85_000L, loaded.request.costCop)
    }

    // Valida estado de error cuando se consulta un identificador inexistente
    @Test
    fun testLoadRequestByIdNotFound() = testScope.runTest {
        viewModel.loadRequest("req-999")
        advanceUntilIdle()

        val state = viewModel.trackingState.value
        assertTrue(state is DebrisTrackingUiState.Error)
    }

    // Valida cancelación de solicitud pendiente desde el ViewModel de seguimiento
    @Test
    fun testCancelRequestFromTrackingViewModel() = testScope.runTest {
        var completed = false
        viewModel.cancelRequest("req-101") { completed = it }
        advanceUntilIdle()

        assertTrue(completed)
        assertFalse(viewModel.isWorking.value)

        // Comprueba que el estado en seguimiento refleje la cancelación
        val state = viewModel.trackingState.value
        assertTrue(state is DebrisTrackingUiState.Loaded)
        val loaded = state as DebrisTrackingUiState.Loaded
        assertEquals(DebrisStatus.CANCELADO, loaded.request.status)
    }
}
