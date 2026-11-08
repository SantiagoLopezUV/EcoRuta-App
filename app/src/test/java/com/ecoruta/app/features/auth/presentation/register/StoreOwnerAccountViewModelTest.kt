package com.ecoruta.app.features.auth.presentation.register

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.MainDispatcherRule
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

// Pruebas de los campos, la validación y el estado del paso de la cuenta del gerente del comercio
@OptIn(ExperimentalCoroutinesApi::class)
class StoreOwnerAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: StoreOwnerAccountViewModel

    @Before
    fun setUp() {
        viewModel = StoreOwnerAccountViewModel()
    }

    @Test
    fun initialState_hasEmptyFieldsAndNoErrors() {
        val state = viewModel.uiState.value
        assertEquals(StoreOwnerAccountUiState(), state)
        assertFalse(state.hasErrors)
    }

    @Test
    fun editingFields_updatesStateValues() {
        viewModel.onFullNameChange("Luis Pérez")
        viewModel.onEmailChange("luis@lapiz.com")

        val state = viewModel.uiState.value
        assertEquals("Luis Pérez", state.fullName)
        assertEquals("luis@lapiz.com", state.email)
    }

    @Test
    fun emptyForm_showsErrorsOnBothFields() {
        viewModel.onContinue()

        val state = viewModel.uiState.value
        assertEquals(R.string.store_registration_error_owner_name, state.fullNameErrorRes)
        assertEquals(R.string.store_registration_error_email, state.emailErrorRes)
        assertTrue(state.hasErrors)
    }

    @Test
    fun invalidEmail_showsEmailError() {
        viewModel.onFullNameChange("Luis Pérez")
        viewModel.onEmailChange("luis.lapiz.com")
        viewModel.onContinue()

        val state = viewModel.uiState.value
        assertNull(state.fullNameErrorRes)
        assertEquals(R.string.store_registration_error_email, state.emailErrorRes)
    }

    @Test
    fun editingFieldsAfterErrors_clearsEachError() {
        viewModel.onContinue()
        viewModel.onFullNameChange("L")
        viewModel.onEmailChange("l")

        assertFalse(viewModel.uiState.value.hasErrors)
    }

    @Test
    fun retry_clearsOfflineNotice() {
        viewModel.onRetry()

        assertFalse(viewModel.uiState.value.isOffline)
    }

    @Test
    fun validForm_emitsFormValidWithTrimmedEmail() = runTest {
        val events = mutableListOf<StoreOwnerAccountEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onFullNameChange("Luis Pérez")
        viewModel.onEmailChange("  luis@lapiz.com ")
        viewModel.onContinue()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasErrors)
        assertEquals(listOf(StoreOwnerAccountEvent.FormValid("luis@lapiz.com")), events)
        collector.cancel()
    }

    @Test
    fun invalidForm_doesNotEmitFormValid() = runTest {
        val events = mutableListOf<StoreOwnerAccountEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onContinue()
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        collector.cancel()
    }
}
