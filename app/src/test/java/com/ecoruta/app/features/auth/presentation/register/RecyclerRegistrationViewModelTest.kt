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

// Pruebas de los campos, la validación y el estado del formulario de registro del reciclador
@OptIn(ExperimentalCoroutinesApi::class)
class RecyclerRegistrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: RecyclerRegistrationViewModel

    @Before
    fun setUp() {
        viewModel = RecyclerRegistrationViewModel()
    }

    // Llena el formulario con datos válidos para que cada prueba cambie solo el campo que revisa
    private fun fillValidForm() {
        viewModel.onFullNameChange("Ana Gómez")
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onDocumentTypeChange(DocumentType.CITIZENSHIP_ID)
        viewModel.onDocumentNumberChange("1116234567")
        viewModel.onPhoneChange("300 123 4567")
    }

    @Test
    fun initialState_hasEmptyFieldsAndNoErrors() {
        val state = viewModel.uiState.value
        assertEquals(RecyclerRegistrationUiState(), state)
        assertEquals(DocumentType.CITIZENSHIP_ID, state.documentType)
        assertFalse(state.isCreatingAccount)
    }

    @Test
    fun editingFields_updatesStateValues() {
        fillValidForm()
        viewModel.onDocumentTypeChange(DocumentType.PASAPORTE)

        val state = viewModel.uiState.value
        assertEquals("Ana Gómez", state.fullName)
        assertEquals("ana@correo.com", state.email)
        assertEquals(DocumentType.PASAPORTE, state.documentType)
        assertEquals("1116234567", state.documentNumber)
        assertEquals("300 123 4567", state.phone)
    }

    @Test
    fun emptyForm_showsRequiredErrorsOnEveryField() {
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.registration_error_name_required, state.fullNameErrorRes)
        assertEquals(R.string.login_error_email_invalid, state.emailErrorRes)
        assertEquals(R.string.registration_error_field_required, state.documentErrorRes)
        assertEquals(R.string.registration_error_field_required, state.phoneErrorRes)
    }

    @Test
    fun blankFullName_showsNameError() {
        fillValidForm()
        viewModel.onFullNameChange("   ")
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_name_required, viewModel.uiState.value.fullNameErrorRes)
    }

    @Test
    fun invalidEmail_showsEmailError() {
        fillValidForm()
        viewModel.onEmailChange("ana@correo")
        viewModel.onSubmit()

        assertEquals(R.string.login_error_email_invalid, viewModel.uiState.value.emailErrorRes)
    }

    @Test
    fun emailWithSpaces_isTrimmedAndAccepted() {
        fillValidForm()
        viewModel.onEmailChange("  ana@correo.com  ")
        viewModel.onSubmit()

        assertNull(viewModel.uiState.value.emailErrorRes)
    }

    @Test
    fun citizenshipIdWithLetters_showsDocumentInvalidError() {
        fillValidForm()
        viewModel.onDocumentNumberChange("11162ABC")
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_document_invalid, viewModel.uiState.value.documentErrorRes)
    }

    @Test
    fun citizenshipIdTooShort_showsDocumentInvalidError() {
        fillValidForm()
        viewModel.onDocumentNumberChange("1234")
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_document_invalid, viewModel.uiState.value.documentErrorRes)
    }

    @Test
    fun passportWithLetters_isAccepted() {
        fillValidForm()
        viewModel.onDocumentTypeChange(DocumentType.PASAPORTE)
        viewModel.onDocumentNumberChange("AB123456")
        viewModel.onSubmit()

        assertNull(viewModel.uiState.value.documentErrorRes)
    }

    @Test
    fun phoneNotStartingWithThree_showsPhoneInvalidError() {
        fillValidForm()
        viewModel.onPhoneChange("6022345678")
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_phone_invalid, viewModel.uiState.value.phoneErrorRes)
    }

    @Test
    fun phoneWithNineDigits_showsPhoneInvalidError() {
        fillValidForm()
        viewModel.onPhoneChange("300123456")
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_phone_invalid, viewModel.uiState.value.phoneErrorRes)
    }

    @Test
    fun editingFieldsAfterErrors_clearsEachError() {
        viewModel.onSubmit()

        viewModel.onFullNameChange("A")
        viewModel.onEmailChange("a")
        viewModel.onPhoneChange("3")
        viewModel.onDocumentNumberChange("1")

        val state = viewModel.uiState.value
        assertNull(state.fullNameErrorRes)
        assertNull(state.emailErrorRes)
        assertNull(state.phoneErrorRes)
        assertNull(state.documentErrorRes)
    }

    @Test
    fun changingDocumentType_clearsDocumentError() {
        viewModel.onSubmit()
        viewModel.onDocumentTypeChange(DocumentType.FOREIGNER_ID)

        assertNull(viewModel.uiState.value.documentErrorRes)
    }

    @Test
    fun validForm_hasNoErrorsAndEmitsFormValid() = runTest {
        val events = mutableListOf<RecyclerRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillValidForm()
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.fullNameErrorRes)
        assertNull(state.emailErrorRes)
        assertNull(state.documentErrorRes)
        assertNull(state.phoneErrorRes)
        assertEquals(listOf(RecyclerRegistrationEvent.FormValid), events)
        collector.cancel()
    }

    @Test
    fun invalidForm_doesNotEmitFormValid() = runTest {
        val events = mutableListOf<RecyclerRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        collector.cancel()
    }
}
