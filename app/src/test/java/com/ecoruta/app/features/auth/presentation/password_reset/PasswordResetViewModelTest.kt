package com.ecoruta.app.features.auth.presentation.password_reset

import com.ecoruta.app.core.testing.FakeAuthRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
}
