package com.ecoruta.app.features.auth.presentation.password_reset

import com.ecoruta.app.core.testing.FakeAuthRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.core.testing.TestDataFactory
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

// Pruebas del paso del correo en la recuperación de contraseña usando un repositorio falso en memoria
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: PasswordResetViewModel

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        viewModel = PasswordResetViewModel(repository)
    }

    @Test
    fun emptyEmail_showsErrorAndDoesNotCallRepository() = runTest {
        viewModel.onEmailChange("")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.emailErrorRes)
        assertFalse(state.isRequestingCode)
        assertTrue(repository.passwordResetEmails.isEmpty())
    }

    @Test
    fun invalidEmail_showsErrorAndDoesNotCallRepository() = runTest {
        viewModel.onEmailChange("ana@")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.emailErrorRes)
        assertFalse(state.isRequestingCode)
        assertTrue(repository.passwordResetEmails.isEmpty())
    }

    @Test
    fun emailChange_clearsPreviousErrors() = runTest {
        // Provoca primero el error de formato y luego el fallo del servidor
        viewModel.onEmailChange("ana@")
        viewModel.onSubmit()
        repository.error = IOException("Sin conexión")
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.requestFailed)

        viewModel.onEmailChange("ana@correo.co")

        val state = viewModel.uiState.value
        assertNull(state.emailErrorRes)
        assertFalse(state.requestFailed)
    }

    @Test
    fun validEmail_sendsResetEmailAndEmitsCodeSent() = runTest {
        repository.addAccount(TestDataFactory.userAccount(email = "ana@correo.com"))
        val events = collectEvents()

        viewModel.onEmailChange(" ana@correo.com ")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("ana@correo.com"), repository.passwordResetEmails)
        assertNull(state.emailErrorRes)
        assertFalse(state.requestFailed)
        assertFalse(state.isRequestingCode)
        assertEquals(listOf(PasswordRecoveryEvent.CodeSent), events)
    }

    @Test
    fun unregisteredEmail_behavesLikeSuccess() = runTest {
        // El repositorio falso no tiene cuentas, igual que un correo que nadie registró
        val events = collectEvents()

        viewModel.onEmailChange("nadie@correo.com")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("nadie@correo.com"), repository.passwordResetEmails)
        assertNull(state.emailErrorRes)
        assertFalse(state.requestFailed)
        assertEquals(listOf(PasswordRecoveryEvent.CodeSent), events)
    }

    @Test
    fun networkError_setsRequestFailedAndKeepsEmail() = runTest {
        repository.error = IOException("Sin conexión")
        val events = collectEvents()

        viewModel.onEmailChange("ana@correo.com")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.requestFailed)
        assertFalse(state.isRequestingCode)
        assertEquals("ana@correo.com", state.email)
        assertTrue(events.isEmpty())
    }

    @Test
    fun submit_showsLoadingUntilRequestFinishes() = runTest {
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onSubmit()

        // La corrutina sigue en cola porque el dispatcher de prueba aún no avanza
        assertTrue(viewModel.uiState.value.isRequestingCode)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRequestingCode)
    }

    @Test
    fun submitWhileLoading_isIgnored() = runTest {
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onSubmit()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(listOf("ana@correo.com"), repository.passwordResetEmails)
    }

    @Test
    fun retryAfterError_succeeds() = runTest {
        repository.error = IOException("Sin conexión")
        val events = collectEvents()
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.requestFailed)

        repository.error = null
        viewModel.onSubmit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.requestFailed)
        assertEquals(listOf("ana@correo.com"), repository.passwordResetEmails)
        assertEquals(listOf(PasswordRecoveryEvent.CodeSent), events)
    }

    // Guarda en una lista los avisos de una sola vez que emite el ViewModel durante la prueba
    private fun TestScope.collectEvents(): List<PasswordRecoveryEvent> {
        val events = mutableListOf<PasswordRecoveryEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }
}
