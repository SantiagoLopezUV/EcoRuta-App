package com.ecoruta.app.features.profile.presentation

import com.ecoruta.app.core.testing.FakeProfileRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

// Pruebas de la carga de roles y del cambio de rol activo usando un repositorio falso en memoria
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeProfileRepository

    @Before
    fun setUp() {
        repository = FakeProfileRepository()
        // El usuario de prueba es ciudadano con el rol de comercio aprobado y el de reciclador pendiente
        repository.currentProfile = repository.currentProfile.copy(
            approvedRoles = setOf(UserRole.CIUDADANO, UserRole.COMERCIO),
            pendingRoles = setOf(UserRole.RECICLADOR),
        )
    }

    @Test
    fun loadRoles_showsActiveApprovedAndPendingRoles() = runTest {
        val viewModel = ProfileViewModel(repository)

        // Antes de que avance el dispatcher la pantalla muestra la carga
        assertEquals(ProfileUiState.Loading, viewModel.uiState.value)

        advanceUntilIdle()

        val profile = loaded(viewModel).profile
        assertEquals(UserRole.CIUDADANO, profile.role)
        assertEquals(setOf(UserRole.CIUDADANO, UserRole.COMERCIO), profile.approvedRoles)
        assertEquals(setOf(UserRole.RECICLADOR), profile.pendingRoles)
    }

    @Test
    fun loadRolesError_showsErrorAndRetryLoadsAgain() = runTest {
        repository.error = IOException("Sin conexión")
        val viewModel = ProfileViewModel(repository)
        advanceUntilIdle()
        assertEquals(ProfileUiState.Error, viewModel.uiState.value)

        repository.error = null
        viewModel.onRetry()
        assertEquals(ProfileUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(UserRole.CIUDADANO, loaded(viewModel).profile.role)
    }

    @Test
    fun switchToApprovedRole_updatesActiveRoleAndEmitsEvent() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)

        viewModel.onSwitchRole(UserRole.COMERCIO)
        advanceUntilIdle()

        val state = loaded(viewModel)
        assertEquals(UserRole.COMERCIO, state.profile.role)
        assertFalse(state.isSwitching)
        assertNull(state.switchErrorRes)
        assertEquals(listOf(UserRole.COMERCIO), repository.switchedRoles)
        assertEquals(listOf<ProfileEvent>(ProfileEvent.RoleSwitched(UserRole.COMERCIO)), events)
    }

    @Test
    fun switchRoleError_keepsCurrentRoleAndShowsError() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)
        repository.error = IOException("Sin conexión")

        viewModel.onSwitchRole(UserRole.COMERCIO)
        advanceUntilIdle()

        val state = loaded(viewModel)
        assertEquals(UserRole.CIUDADANO, state.profile.role)
        assertFalse(state.isSwitching)
        assertNotNull(state.switchErrorRes)
        assertTrue(events.isEmpty())
    }

    @Test
    fun switchRole_showsLoadingUntilRequestFinishes() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onSwitchRole(UserRole.COMERCIO)

        // La corrutina sigue en cola porque el dispatcher de prueba aún no avanza
        assertTrue(loaded(viewModel).isSwitching)

        advanceUntilIdle()

        assertFalse(loaded(viewModel).isSwitching)
    }

    @Test
    fun switchWhileSwitching_isIgnored() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onSwitchRole(UserRole.COMERCIO)
        viewModel.onSwitchRole(UserRole.COMERCIO)
        advanceUntilIdle()

        assertEquals(listOf(UserRole.COMERCIO), repository.switchedRoles)
    }

    @Test
    fun switchToNotApprovedRole_doesNotCallRepository() = runTest {
        val viewModel = loadedViewModel()
        val events = collectEvents(viewModel)

        // El reciclador está pendiente y el chofer nunca fue asignado
        viewModel.onSwitchRole(UserRole.RECICLADOR)
        viewModel.onSwitchRole(UserRole.CHOFER)
        advanceUntilIdle()

        val state = loaded(viewModel)
        assertEquals(UserRole.CIUDADANO, state.profile.role)
        assertFalse(state.isSwitching)
        assertTrue(repository.switchedRoles.isEmpty())
        assertTrue(events.isEmpty())
    }

    @Test
    fun switchToCurrentRole_doesNotCallRepository() = runTest {
        val viewModel = loadedViewModel()

        viewModel.onSwitchRole(UserRole.CIUDADANO)
        advanceUntilIdle()

        assertTrue(repository.switchedRoles.isEmpty())
    }

    @Test
    fun retrySwitchAfterError_clearsErrorAndSucceeds() = runTest {
        val viewModel = loadedViewModel()
        repository.error = IOException("Sin conexión")
        viewModel.onSwitchRole(UserRole.COMERCIO)
        advanceUntilIdle()
        assertNotNull(loaded(viewModel).switchErrorRes)

        repository.error = null
        viewModel.onSwitchRole(UserRole.COMERCIO)
        assertNull(loaded(viewModel).switchErrorRes)
        advanceUntilIdle()

        assertEquals(UserRole.COMERCIO, loaded(viewModel).profile.role)
    }

    // Crea el ViewModel y deja terminar la carga inicial de roles
    private fun TestScope.loadedViewModel(): ProfileViewModel {
        val viewModel = ProfileViewModel(repository)
        advanceUntilIdle()
        return viewModel
    }

    // Devuelve el estado cargado o hace fallar la prueba si la pantalla no tiene perfil
    private fun loaded(viewModel: ProfileViewModel): ProfileUiState.Loaded =
        viewModel.uiState.value as? ProfileUiState.Loaded
            ?: throw AssertionError("Se esperaba el perfil cargado y llegó ${viewModel.uiState.value}")

    // Guarda en una lista los avisos de una sola vez que emite el ViewModel durante la prueba
    private fun TestScope.collectEvents(viewModel: ProfileViewModel): List<ProfileEvent> {
        val events = mutableListOf<ProfileEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }
}
