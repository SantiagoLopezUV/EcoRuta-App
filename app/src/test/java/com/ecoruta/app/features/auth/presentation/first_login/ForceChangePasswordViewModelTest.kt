package com.ecoruta.app.features.auth.presentation.first_login

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeAuthRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.core.testing.TestDataFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

// Pruebas del cambio obligatorio de clave temporal usando un repositorio falso en memoria
@OptIn(ExperimentalCoroutinesApi::class)
class ForceChangePasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAuthRepository
    private lateinit var viewModel: ForceChangePasswordViewModel

    private val operator = TestDataFactory.firstLoginAccount()

    @Before
    fun setUp() {
        // El operador ya entró con su clave provisional y tiene la sesión activa
        repository = FakeAuthRepository()
        repository.addAccount(operator, password = TEMPORARY_PASSWORD)
        repository.setSession(TestDataFactory.authenticatedSession(operator))
        viewModel = ForceChangePasswordViewModel(repository)
    }

    @Test
    fun emptyCurrentPassword_showsErrorAndDoesNotSave() = runTest {
        fillForm(current = "")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.login_error_password_empty, state.currentErrorRes)
        assertFalse(state.isSaving)
        assertEquals(TEMPORARY_PASSWORD, repository.passwordOf(operator.email))
    }

    @Test
    fun weakNewPassword_showsErrorAndDoesNotSave() = runTest {
        // Cada caso incumple una regla distinta de longitud, letra o número
        listOf("Abc123", "12345678", "abcdefgh").forEach { weak ->
            fillForm(new = weak, confirm = weak)
            viewModel.onSave()
            advanceUntilIdle()

            assertEquals(R.string.recovery_error_password_invalid, viewModel.uiState.value.newErrorRes)
        }
        assertEquals(TEMPORARY_PASSWORD, repository.passwordOf(operator.email))
    }

    @Test
    fun newPasswordEqualToTemporary_showsErrorAndDoesNotSave() = runTest {
        fillForm(new = TEMPORARY_PASSWORD, confirm = TEMPORARY_PASSWORD)
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(R.string.change_password_error_same, viewModel.uiState.value.newErrorRes)
        assertEquals(TEMPORARY_PASSWORD, repository.passwordOf(operator.email))
    }

    @Test
    fun confirmationMismatch_showsErrorAndDoesNotSave() = runTest {
        fillForm(confirm = "OtraClave99")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.recovery_error_password_mismatch, state.confirmErrorRes)
        assertNull(state.newErrorRes)
        assertEquals(TEMPORARY_PASSWORD, repository.passwordOf(operator.email))
    }

    @Test
    fun emptyForm_showsAllErrorsAtOnce() = runTest {
        viewModel.onConfirmChange("x")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.login_error_password_empty, state.currentErrorRes)
        assertEquals(R.string.recovery_error_password_invalid, state.newErrorRes)
        assertEquals(R.string.recovery_error_password_mismatch, state.confirmErrorRes)
    }

    @Test
    fun editingFields_clearsTheirErrors() = runTest {
        viewModel.onConfirmChange("x")
        viewModel.onSave()

        viewModel.onCurrentChange(TEMPORARY_PASSWORD)
        viewModel.onNewChange(NEW_PASSWORD)
        viewModel.onConfirmChange(NEW_PASSWORD)

        val state = viewModel.uiState.value
        assertNull(state.currentErrorRes)
        assertNull(state.newErrorRes)
        assertNull(state.confirmErrorRes)
    }

    @Test
    fun toggles_switchPasswordVisibility() = runTest {
        viewModel.onToggleCurrentVisibility()
        viewModel.onToggleNewVisibility()
        viewModel.onToggleConfirmVisibility()

        val shown = viewModel.uiState.value
        assertTrue(shown.isCurrentVisible && shown.isNewVisible && shown.isConfirmVisible)

        viewModel.onToggleNewVisibility()

        assertFalse(viewModel.uiState.value.isNewVisible)
    }

    // Llena el formulario con datos válidos salvo el campo que la prueba quiera cambiar
    private fun fillForm(
        current: String = TEMPORARY_PASSWORD,
        new: String = NEW_PASSWORD,
        confirm: String = NEW_PASSWORD,
    ) {
        viewModel.onCurrentChange(current)
        viewModel.onNewChange(new)
        viewModel.onConfirmChange(confirm)
    }

    // Guarda en una lista los avisos de una sola vez que emite el ViewModel durante la prueba
    private fun TestScope.collectEvents(): List<ChangePasswordEvent> {
        val events = mutableListOf<ChangePasswordEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    private companion object {
        const val TEMPORARY_PASSWORD = "Temporal123"
        const val NEW_PASSWORD = "NuevaClave456"
    }
}
