package com.ecoruta.app.features.bulky_waste.presentation.schedule

// Pruebas unitarias para BulkyRequestViewModel (ScheduleRcdViewModel) y validación de políticas RLS
// Cubre validación de formulario, agendamiento, cancelación y aislamiento de datos entre ciudadanos

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeBulkyWasteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BulkyRequestViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: FakeBulkyWasteRepository
    private lateinit var viewModel: ScheduleRcdViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBulkyWasteRepository()
        viewModel = ScheduleRcdViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Valida que el estado inicial tenga el calendario inicializado y campos vacíos
    @Test
    fun testInitialStateHasCalendarInitialized() = testScope.runTest {
        val state = viewModel.uiState.value
        assertNull(state.type)
        assertEquals("", state.description)
        assertEquals("", state.address)
        assertNull(state.selectedDay)
        assertTrue(state.availableDays.isNotEmpty())
        assertTrue(state.firstSelectableDay > 0L)
        assertTrue(state.lastSelectableDay > state.firstSelectableDay)
    }

    // Valida errores de validación en el paso 1 cuando los campos están incompletos
    @Test
    fun testValidateStep1WithMissingFields() = testScope.runTest {
        val isValid = viewModel.validateStep1()
        assertFalse(isValid)

        val state = viewModel.uiState.value
        assertEquals(R.string.debris_type_required, state.typeError)
        assertEquals(R.string.debris_description_required, state.descriptionError)
        assertEquals(R.string.debris_address_required, state.addressError)
    }

    // Valida que una descripción inferior a 10 caracteres sea rechazada
    @Test
    fun testValidateStep1WithShortDescription() = testScope.runTest {
        viewModel.onTypeSelected(DebrisType.FURNITURE)
        viewModel.onAddressChange("Calle 25 #18-40")
        viewModel.onDescriptionChange("Mueble") // Solo 6 caracteres

        val isValid = viewModel.validateStep1()
        assertFalse(isValid)

        val state = viewModel.uiState.value
        assertNull(state.typeError)
        assertNull(state.addressError)
        assertEquals(R.string.debris_description_required, state.descriptionError)
    }

    // Valida que cuando todos los campos del paso 1 son correctos, la validación pase
    @Test
    fun testValidateStep1Success() = testScope.runTest {
        viewModel.onTypeSelected(DebrisType.FURNITURE)
        viewModel.onAddressChange("Calle 25 #18-40, Barrio Victoria")
        viewModel.onDescriptionChange("Sofá cama de tres puestos en buen estado")

        val isValid = viewModel.validateStep1()
        assertTrue(isValid)

        val state = viewModel.uiState.value
        assertNull(state.typeError)
        assertNull(state.descriptionError)
        assertNull(state.addressError)
    }

    // Valida que la fecha seleccionada deba pertenecer a los días disponibles
    @Test
    fun testValidateScheduleRequiresAvailableDay() = testScope.runTest {
        assertFalse(viewModel.validateSchedule())
        assertEquals(R.string.debris_date_required, viewModel.uiState.value.dateError)

        val validDay = viewModel.uiState.value.availableDays.first()
        viewModel.onDaySelected(validDay)

        assertTrue(viewModel.validateSchedule())
        assertNull(viewModel.uiState.value.dateError)
    }

    // Valida el envío exitoso de la solicitud y emisión del evento SubmitSuccess
    @Test
    fun testSubmitRequestSuccess() = testScope.runTest {
        viewModel.onTypeSelected(DebrisType.RENOVATION_DEBRIS)
        viewModel.onDescriptionChange("6 bultos de escombros de remodelación de cocina")
        viewModel.onAddressChange("Carrera 30 #22-10, La Esperanza")
        val availableDay = viewModel.uiState.value.availableDays.first()
        viewModel.onDaySelected(availableDay)

        val events = mutableListOf<ScheduleRcdEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        val submitted = viewModel.onSubmit()
        assertTrue(submitted)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNotNull(state.submitted)
        assertEquals(DebrisType.RENOVATION_DEBRIS, state.submitted?.type)
        assertEquals(DebrisStatus.PENDIENTE, state.submitted?.status)
        assertEquals("Carrera 30 #22-10, La Esperanza", state.submitted?.address)

        assertTrue(events.any { it is ScheduleRcdEvent.SubmitSuccess })
        job.cancel()
    }

    // Valida manejo de errores cuando el repositorio falla en el envío
    @Test
    fun testSubmitRequestFailure() = testScope.runTest {
        repository.error = RuntimeException("Error simulado de red en Supabase")

        viewModel.onTypeSelected(DebrisType.MATTRESSES)
        viewModel.onDescriptionChange("Colchón doble y basecama en madera")
        viewModel.onAddressChange("Calle 26 #12-15")
        val availableDay = viewModel.uiState.value.availableDays.first()
        viewModel.onDaySelected(availableDay)

        val events = mutableListOf<ScheduleRcdEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSubmitting)
        assertNull(state.submitted)
        assertEquals(R.string.debris_action_error, state.dateError)
        assertTrue(events.any { it is ScheduleRcdEvent.SubmitError })
        job.cancel()
    }

    // Valida cancelación exitosa de una solicitud en estado pendiente
    @Test
    fun testCancelPendingRequestSuccess() = testScope.runTest {
        // req-101 en FakeBulkyWasteRepository es PENDIENTE
        val events = mutableListOf<ScheduleRcdEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        var completed = false
        viewModel.cancelRequest("req-101") { completed = it }
        advanceUntilIdle()

        assertTrue(completed)
        assertTrue(events.any { it is ScheduleRcdEvent.CancelSuccess && it.requestId == "req-101" })

        val updated = repository.getRequestById("req-101").getOrThrow()
        assertEquals(DebrisStatus.CANCELADO, updated.status)
        job.cancel()
    }

    // Valida que no se pueda cancelar una solicitud que ya fue cotizada o procesada
    @Test
    fun testCancelNonPendingRequestFails() = testScope.runTest {
        // req-102 en FakeBulkyWasteRepository está en estado COTIZADO
        val events = mutableListOf<ScheduleRcdEvent>()
        val job = launch { viewModel.events.collect { events.add(it) } }

        var completed = true
        viewModel.cancelRequest("req-102") { completed = it }
        advanceUntilIdle()

        assertFalse(completed)
        assertTrue(events.any { it is ScheduleRcdEvent.CancelError })
        job.cancel()
    }

    // Valida política RLS: un ciudadano no puede ver ni cancelar solicitudes radicadas por otro ciudadano
    @Test
    fun testCitizenCannotSeeOrCancelAnotherCitizensRequests_RlsPolicy() = testScope.runTest {
        // Ciudadano 1 crea una solicitud
        repository.currentUserId = "citizen-001"
        val createResult = repository.createRequest(
            type = DebrisType.FURNITURE,
            description = "Mueble de sala de 3 puestos",
            latitude = 4.0847,
            longitude = -76.1953,
            address = "Calle 25 #14-10",
            scheduledDateMillis = System.currentTimeMillis() + 86400000L,
        )
        assertTrue(createResult.isSuccess)
        val citizen1RequestId = createResult.getOrThrow().id

        // Ciudadano 1 consulta sus solicitudes y verifica que la suya está presente
        val citizen1List = repository.getMyRequests().getOrThrow()
        assertTrue(citizen1List.any { it.id == citizen1RequestId })

        // Se autentica el Ciudadano 2
        repository.currentUserId = "citizen-002"

        // El Ciudadano 2 NO debe ver en su listado la solicitud del Ciudadano 1 (Aislamiento RLS)
        val citizen2List = repository.getMyRequests().getOrThrow()
        assertFalse(citizen2List.any { it.id == citizen1RequestId })

        // El Ciudadano 2 intenta cancelar la solicitud del Ciudadano 1 y es RECHAZADO por la política RLS
        val cancelAttempt = repository.cancelRequest(citizen1RequestId)
        assertTrue(cancelAttempt.isFailure)

        // Verificamos que la solicitud del Ciudadano 1 permanezca intacta en estado PENDIENTE
        val requestStillPending = repository.getRequestById(citizen1RequestId).getOrThrow()
        assertEquals(DebrisStatus.PENDIENTE, requestStillPending.status)

        // El Ciudadano 1 se vuelve a conectar y puede cancelar su propia solicitud
        repository.currentUserId = "citizen-001"
        val authorizedCancel = repository.cancelRequest(citizen1RequestId)
        assertTrue(authorizedCancel.isSuccess)

        val requestNowCancelled = repository.getRequestById(citizen1RequestId).getOrThrow()
        assertEquals(DebrisStatus.CANCELADO, requestNowCancelled.status)
    }

    // Valida navegación de semanas en el calendario de agendamiento
    @Test
    fun testCalendarWeekNavigation() = testScope.runTest {
        val initialWeekStart = viewModel.uiState.value.weekStart
        if (viewModel.uiState.value.canGoToNextWeek) {
            viewModel.onNextWeek()
            assertEquals(DayMillis.addDays(initialWeekStart, 7), viewModel.uiState.value.weekStart)

            viewModel.onPreviousWeek()
            assertEquals(initialWeekStart, viewModel.uiState.value.weekStart)
        }
    }
}
