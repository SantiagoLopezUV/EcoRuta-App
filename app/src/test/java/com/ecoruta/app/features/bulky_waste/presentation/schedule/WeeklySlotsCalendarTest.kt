package com.ecoruta.app.features.bulky_waste.presentation.schedule

// Pruebas unitarias del calendario semanal de cupos que vive en ScheduleRcdViewModel
// Cubre semana con cupos, semana sin cupos, cambio de semana, fecha elegida, error de consulta y reintento

import com.ecoruta.app.core.testing.FakeBulkyWasteRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class WeeklySlotsCalendarTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var repository: FakeBulkyWasteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBulkyWasteRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Devuelve los siete días de la semana que empieza en el lunes indicado
    private fun weekDays(weekStart: Long): List<Long> = (0 until 7).map { DayMillis.addDays(weekStart, it) }

    private fun isSunday(dayMillis: Long): Boolean =
        Calendar.getInstance().apply { timeInMillis = dayMillis }.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY

    // Valida que una semana con capacidad muestre de lunes a sábado como disponibles y el domingo sin cupo
    @Test
    fun testWeekWithAvailableSlots() = testScope.runTest {
        val viewModel = ScheduleRcdViewModel(repository)
        assertTrue(viewModel.uiState.value.isLoadingDays)

        advanceUntilIdle()
        viewModel.onNextWeek()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingDays)
        assertFalse(state.daysAreSample)
        assertNull(state.daysError)
        weekDays(state.weekStart).forEach { day ->
            assertEquals(!isSunday(day), day in state.availableDays)
        }
    }

    // Valida que una semana con todos los cupos ocupados no ofrezca ningún día mientras las demás sí
    @Test
    fun testWeekWithoutSlots() = testScope.runTest {
        repository.weeklyCapacities = mapOf(1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 0)
        val firstSelectableDay = DayMillis.addDays(DayMillis.startOfDay(System.currentTimeMillis()), 1)
        val fullWeekStart = DayMillis.addDays(DayMillis.mondayOf(firstSelectableDay), 7)

        // Ocupa el único cupo de cada día de la segunda semana
        weekDays(fullWeekStart).filterNot(::isSunday).forEach { day ->
            repository.createRequest(
                type = DebrisType.FURNITURE,
                description = "Cupo ocupado para la prueba",
                latitude = 4.0847,
                longitude = -76.1953,
                scheduledDateMillis = day,
            )
        }

        val viewModel = ScheduleRcdViewModel(repository)
        advanceUntilIdle()
        viewModel.onNextWeek()

        val state = viewModel.uiState.value
        assertEquals(fullWeekStart, state.weekStart)
        assertFalse(state.daysAreSample)
        assertTrue(weekDays(state.weekStart).none { it in state.availableDays })
        assertTrue(weekDays(DayMillis.addDays(fullWeekStart, 7)).any { it in state.availableDays })
    }

    // Valida que avanzar y retroceder mueva la semana visible dentro del rango permitido
    @Test
    fun testWeekNavigationMovesWithinRange() = testScope.runTest {
        val viewModel = ScheduleRcdViewModel(repository)
        advanceUntilIdle()
        val initialWeekStart = viewModel.uiState.value.weekStart

        // En la primera semana no se puede retroceder
        assertFalse(viewModel.uiState.value.canGoToPreviousWeek)
        viewModel.onPreviousWeek()
        assertEquals(initialWeekStart, viewModel.uiState.value.weekStart)

        viewModel.onNextWeek()
        assertEquals(DayMillis.addDays(initialWeekStart, 7), viewModel.uiState.value.weekStart)

        viewModel.onPreviousWeek()
        assertEquals(initialWeekStart, viewModel.uiState.value.weekStart)

        // Al llegar al final del rango ya no se puede avanzar
        while (viewModel.uiState.value.canGoToNextWeek) viewModel.onNextWeek()
        val lastWeekStart = viewModel.uiState.value.weekStart
        viewModel.onNextWeek()
        assertEquals(lastWeekStart, viewModel.uiState.value.weekStart)
    }

    // Comportamiento actual: cambiar de semana no vuelve a consultar el repositorio
    // Los días de cada semana salen de la ventana de 28 días cargada al abrir el calendario
    @Test
    fun testWeekChangeUsesLoadedWindowWithoutNewQuery() = testScope.runTest {
        val viewModel = ScheduleRcdViewModel(repository)
        advanceUntilIdle()
        val loadedDays = viewModel.uiState.value.availableDays

        // Si el repositorio se volviera a consultar ya no habría días disponibles
        repository.weeklyCapacities = mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0, 7 to 0)

        viewModel.onNextWeek()
        advanceUntilIdle()
        assertEquals(loadedDays, viewModel.uiState.value.availableDays)

        viewModel.onPreviousWeek()
        advanceUntilIdle()
        assertEquals(loadedDays, viewModel.uiState.value.availableDays)
    }

    // Valida que la fecha elegida se conserve al cambiar de semana y al pasar por otros pasos del formulario
    @Test
    fun testSelectedDayIsKeptAcrossWeeksAndSteps() = testScope.runTest {
        val viewModel = ScheduleRcdViewModel(repository)
        advanceUntilIdle()

        viewModel.onNextWeek()
        val chosenDay = weekDays(viewModel.uiState.value.weekStart).first { it in viewModel.uiState.value.availableDays }
        viewModel.onDaySelected(chosenDay)

        viewModel.onPreviousWeek()
        assertEquals(chosenDay, viewModel.uiState.value.selectedDay)

        viewModel.onNextWeek()
        viewModel.onNextWeek()
        assertEquals(chosenDay, viewModel.uiState.value.selectedDay)

        // Los cambios del paso uno no borran la fecha elegida
        viewModel.onTypeSelected(DebrisType.MATTRESSES)
        viewModel.onAddressChange("Calle 26 #12-15")
        viewModel.validateStep1()
        assertEquals(chosenDay, viewModel.uiState.value.selectedDay)
    }

    // Valida que un día sin cupo no reemplace la fecha ya elegida
    @Test
    fun testUnavailableDayDoesNotReplaceSelection() = testScope.runTest {
        val viewModel = ScheduleRcdViewModel(repository)
        advanceUntilIdle()
        viewModel.onNextWeek()

        val days = weekDays(viewModel.uiState.value.weekStart)
        val chosenDay = days.first { it in viewModel.uiState.value.availableDays }
        val sunday = days.first(::isSunday)
        viewModel.onDaySelected(chosenDay)
        viewModel.onDaySelected(sunday)

        assertEquals(chosenDay, viewModel.uiState.value.selectedDay)
    }

    // Comportamiento esperado: mostrar daysError. Pendiente de corrección en ScheduleRcdViewModel (Alejo).
    // Hoy el fallo de la consulta se oculta y se muestran fechas de ejemplo
    @Test
    fun testQueryErrorShowsSampleDays() = testScope.runTest {
        repository.error = RuntimeException("Error simulado de red en Supabase")

        val viewModel = ScheduleRcdViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingDays)
        assertTrue(state.daysAreSample)
        assertNull(state.daysError)
        assertTrue(state.availableDays.isNotEmpty())
    }

    // Valida que reintentar tras un fallo cargue los cupos reales y quite el aviso de fechas de ejemplo
    @Test
    fun testRetryAfterFailureLoadsRealSlots() = testScope.runTest {
        repository.error = RuntimeException("Error simulado de red en Supabase")
        val viewModel = ScheduleRcdViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.daysAreSample)

        repository.error = null
        viewModel.onRetryDays()
        assertTrue(viewModel.uiState.value.isLoadingDays)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        val expected = repository.getAvailablePickupDates(
            startDateMillis = state.firstSelectableDay,
            daysCount = DebrisRequestUiState.SELECTABLE_DAYS,
        ).getOrThrow()
        assertFalse(state.isLoadingDays)
        assertFalse(state.daysAreSample)
        assertNull(state.daysError)
        assertEquals(expected, state.availableDays)
    }

    // Pendiente: la prueba de dos solicitudes simultáneas por el último cupo depende de ECO-1017
    // Esa tarjeta agrega la función atómica de Santiago en el backend que reserva el cupo del día
    // Su prueba real será de integración contra la base local y no una prueba unitaria con el fake
}
