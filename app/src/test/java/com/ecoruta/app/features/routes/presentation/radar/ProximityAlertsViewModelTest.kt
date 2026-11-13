package com.ecoruta.app.features.routes.presentation.radar

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeNeighborhoodRouteRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.features.routes.presentation.map.NotificationKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Pruebas unitarias de ProximityAlertsViewModel: preferencias de radar, registro de token FCM,
// cálculo de distancia geodésica y disparo de alertas una sola vez por pasada del camión
@OptIn(ExperimentalCoroutinesApi::class)
class ProximityAlertsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeNeighborhoodRouteRepository
    private lateinit var viewModel: ProximityAlertsViewModel

    // Coordenadas base de prueba: Plaza de Bolívar de Tuluá (4.0847, -76.1953)
    private val citizenLat = 4.0847
    private val citizenLon = -76.1953

    @Before
    fun setUp() {
        fakeRepository = FakeNeighborhoodRouteRepository()
        viewModel = ProximityAlertsViewModel(fakeRepository)
        viewModel.setCitizenLocation(citizenLat, citizenLon)
    }

    @Test
    fun initialState_hasDefaultValues() {
        val state = viewModel.uiState.value
        assertEquals(RadarDefaults.RADIUS, state.radiusMeters)
        assertTrue(state.notificationsEnabled)
        assertFalse(state.isSaving)
        assertEquals(TokenRegistrationState.Idle, state.tokenRegistrationState)
        assertTrue(state.alerts.isEmpty())
        assertFalse(state.isCurrentlyInsidePass)
    }

    @Test
    fun onRadiusChange_clampsBetweenMinAndMax() {
        viewModel.onRadiusChange(50)
        assertEquals(RadarDefaults.MIN_RADIUS, viewModel.uiState.value.radiusMeters)

        viewModel.onRadiusChange(2000)
        assertEquals(RadarDefaults.MAX_RADIUS, viewModel.uiState.value.radiusMeters)

        viewModel.onRadiusChange(700)
        assertEquals(700, viewModel.uiState.value.radiusMeters)
    }

    @Test
    fun onNotificationsChange_updatesState() {
        viewModel.onNotificationsChange(false)
        assertFalse(viewModel.uiState.value.notificationsEnabled)

        viewModel.onNotificationsChange(true)
        assertTrue(viewModel.uiState.value.notificationsEnabled)
    }

    @Test
    fun onSave_onSuccess_persistsConfigAndEmitsSavedEvent() = runTest {
        val events = mutableListOf<ProximityAlertEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.setNeighborhood("barrio-esperanza", "La Esperanza")
        viewModel.onRadiusChange(600)
        viewModel.onNotificationsChange(true)
        viewModel.onSave()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        val savedConfig = fakeRepository.radarConfigs["barrio-esperanza"]
        assertEquals("barrio-esperanza", savedConfig?.neighborhoodId)
        assertEquals(600, savedConfig?.radiusMeters)
        assertTrue(savedConfig?.notificationsEnabled == true)

        val savedEvents = events.filterIsInstance<ProximityAlertEvent.Saved>()
        assertEquals(1, savedEvents.size)
        assertEquals(600, savedEvents.first().radiusMeters)
        collector.cancel()
    }

    @Test
    fun onSave_onRepositoryFailure_emitsShowNotice() = runTest {
        val events = mutableListOf<ProximityAlertEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fakeRepository.error = RuntimeException("Error al guardar radar")
        viewModel.onSave()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        val notices = events.filterIsInstance<ProximityAlertEvent.ShowNotice>()
        assertEquals(1, notices.size)
        assertEquals(R.string.radar_save_error, notices.first().messageRes)
        collector.cancel()
    }

    // --- Pruebas de Registro de Token FCM (Alerts token registration) ---

    @Test
    fun registerPushToken_onSuccess_updatesStateToSuccess() = runTest {
        viewModel.registerPushToken("fcm-test-token-abc123")
        advanceUntilIdle()

        val tokenState = viewModel.uiState.value.tokenRegistrationState
        assertTrue(tokenState is TokenRegistrationState.Success)
        assertEquals("fcm-test-token-abc123", (tokenState as TokenRegistrationState.Success).token)
        assertTrue(fakeRepository.registeredTokens.contains("fcm-test-token-abc123"))
    }

    @Test
    fun registerPushToken_withBlankToken_setsFailure() = runTest {
        viewModel.registerPushToken("   ")
        advanceUntilIdle()

        val tokenState = viewModel.uiState.value.tokenRegistrationState
        assertTrue(tokenState is TokenRegistrationState.Failure)
        assertTrue(fakeRepository.registeredTokens.isEmpty())
    }

    @Test
    fun registerPushToken_onRepositoryFailure_setsFailure() = runTest {
        fakeRepository.tokenRegistrationError = RuntimeException("FCM service unavailable")
        viewModel.registerPushToken("fcm-token-xyz")
        advanceUntilIdle()

        val tokenState = viewModel.uiState.value.tokenRegistrationState
        assertTrue(tokenState is TokenRegistrationState.Failure)
        val failure = tokenState as TokenRegistrationState.Failure
        assertEquals(R.string.radar_save_error, failure.messageRes)
    }

    // --- Pruebas de Lógica de Proximidad y Recorrido Simulado ---

    @Test
    fun proximityRadius_truckOutsideRadius_doesNotTriggerAlert() = runTest {
        val events = mutableListOf<ProximityAlertEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onRadiusChange(500)
        viewModel.onNotificationsChange(true)

        // Ubicación del camión a ~1.5 km de distancia
        val truckLatOutside = 4.0700
        val truckLonOutside = -76.1953

        viewModel.onTruckLocationUpdated("TRUCK-01", truckLatOutside, truckLonOutside)
        advanceUntilIdle()

        assertTrue(events.filterIsInstance<ProximityAlertEvent.AlertTriggered>().isEmpty())
        assertTrue(viewModel.uiState.value.alerts.isEmpty())
        assertFalse(viewModel.uiState.value.isCurrentlyInsidePass)
        collector.cancel()
    }

    @Test
    fun proximityRadius_truckEntersRadius_withNotificationsEnabled_triggersAlert() = runTest {
        val events = mutableListOf<ProximityAlertEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onRadiusChange(500)
        viewModel.onNotificationsChange(true)

        // Ubicación del camión a ~200 m (dentro del radio de 500 m)
        val truckLatInside = 4.0860
        val truckLonInside = -76.1953

        viewModel.onTruckLocationUpdated("TRUCK-01", truckLatInside, truckLonInside)
        advanceUntilIdle()

        val triggered = events.filterIsInstance<ProximityAlertEvent.AlertTriggered>()
        assertEquals(1, triggered.size)
        assertTrue(triggered.first().distanceMeters <= 500.0)

        val alerts = viewModel.uiState.value.alerts
        assertEquals(1, alerts.size)
        assertEquals(NotificationKind.PROXIMITY, alerts.first().kind)
        assertTrue(viewModel.uiState.value.isCurrentlyInsidePass)
        collector.cancel()
    }

    @Test
    fun proximityRadius_truckStaysInsideRadius_triggersAlertOnlyOncePerPass() = runTest {
        val events = mutableListOf<ProximityAlertEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onRadiusChange(500)
        viewModel.onNotificationsChange(true)

        // Paso 1: Camión ingresa a 400 m
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0870, -76.1953)
        advanceUntilIdle()
        assertEquals(1, events.filterIsInstance<ProximityAlertEvent.AlertTriggered>().size)

        // Paso 2: Camión se acerca a 200 m (sigue dentro de la misma pasada)
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0860, -76.1953)
        advanceUntilIdle()
        // No debe duplicarse la alerta
        assertEquals(1, events.filterIsInstance<ProximityAlertEvent.AlertTriggered>().size)

        // Paso 3: Camión pasa a 100 m (sigue dentro)
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0855, -76.1953)
        advanceUntilIdle()
        // Sigue habiendo solo 1 alerta emitida
        assertEquals(1, events.filterIsInstance<ProximityAlertEvent.AlertTriggered>().size)
        assertEquals(1, viewModel.uiState.value.alerts.size)
        assertTrue(viewModel.uiState.value.isCurrentlyInsidePass)

        collector.cancel()
    }

    @Test
    fun proximityRadius_truckLeavesAndReEntersRadius_triggersNewAlertOnSecondPass() = runTest {
        val events = mutableListOf<ProximityAlertEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onRadiusChange(500)
        viewModel.onNotificationsChange(true)

        // Primera pasada: entra al radio
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0860, -76.1953)
        advanceUntilIdle()
        assertEquals(1, events.filterIsInstance<ProximityAlertEvent.AlertTriggered>().size)
        assertTrue(viewModel.uiState.value.isCurrentlyInsidePass)

        // El camión se aleja y sale del radio (> 1.5 km)
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0700, -76.1953)
        advanceUntilIdle()
        // La pasada se reinicia
        assertFalse(viewModel.uiState.value.isCurrentlyInsidePass)
        assertEquals(1, events.filterIsInstance<ProximityAlertEvent.AlertTriggered>().size)

        // Segunda pasada: el camión regresa y vuelve a entrar al radio
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0855, -76.1953)
        advanceUntilIdle()
        // Ahora sí debe disparar la segunda alerta correspondiente a la nueva pasada
        assertEquals(2, events.filterIsInstance<ProximityAlertEvent.AlertTriggered>().size)
        assertEquals(2, viewModel.uiState.value.alerts.size)
        assertTrue(viewModel.uiState.value.isCurrentlyInsidePass)

        collector.cancel()
    }

    @Test
    fun proximityRadius_truckInsideRadius_withNotificationsDisabled_doesNotTriggerAlert() = runTest {
        val events = mutableListOf<ProximityAlertEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onRadiusChange(500)
        // Desactivamos notificaciones explícitamente
        viewModel.onNotificationsChange(false)

        // Camión entra al radio a 200 m
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0860, -76.1953)
        advanceUntilIdle()

        // Como las notificaciones están apagadas, no se emite ninguna alerta
        assertTrue(events.filterIsInstance<ProximityAlertEvent.AlertTriggered>().isEmpty())
        assertTrue(viewModel.uiState.value.alerts.isEmpty())
        assertTrue(viewModel.uiState.value.isCurrentlyInsidePass)

        collector.cancel()
    }

    @Test
    fun markAlertAsRead_updatesAlertState() {
        viewModel.onNotificationsChange(true)
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0860, -76.1953)

        val alert = viewModel.uiState.value.alerts.first()
        assertFalse(alert.read)

        viewModel.markAlertAsRead(alert.id)
        assertTrue(viewModel.uiState.value.alerts.first().read)
    }

    @Test
    fun clearAlerts_clearsList() {
        viewModel.onNotificationsChange(true)
        viewModel.onTruckLocationUpdated("TRUCK-01", 4.0860, -76.1953)
        assertFalse(viewModel.uiState.value.alerts.isEmpty())

        viewModel.clearAlerts()
        assertTrue(viewModel.uiState.value.alerts.isEmpty())
    }
}
