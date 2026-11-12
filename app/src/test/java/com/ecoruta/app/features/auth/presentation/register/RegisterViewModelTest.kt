package com.ecoruta.app.features.auth.presentation.register

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeAuthRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.core.testing.TestDataFactory
import java.net.UnknownHostException
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

// Pruebas del flujo de registro de ciudadano en dos pasos: validación de campos, eventos de
// navegación y llamada al repositorio de autenticación
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeAuthRepository()
        viewModel = RegisterViewModel(fakeRepository)
    }

    // Rellena los campos del paso 1 (nombre y correo) con datos válidos
    private fun fillStep1() {
        viewModel.onFullNameChange("Carlos Mendoza")
        viewModel.onEmailChange("carlos@correo.com")
    }

    // Rellena los campos del paso 2 (documento, teléfono, barrio y política) con datos válidos
    private fun fillStep2() {
        viewModel.onDocumentTypeChange(DocumentType.CITIZENSHIP_ID)
        viewModel.onDocumentNumberChange("1116234567")
        viewModel.onPhoneChange("3001234567")
        viewModel.onNeighborhoodChange("El Paraíso")
        viewModel.onDataPolicyChange(true)
    }

    @Test
    fun initialState_hasEmptyFieldsAndNoErrors() {
        val state = viewModel.uiState.value
        assertEquals(CitizenRegistrationUiState(), state)
        assertEquals(DocumentType.CITIZENSHIP_ID, state.documentType)
        assertFalse(state.isCreatingAccount)
        assertFalse(state.acceptedDataPolicy)
    }

    // --- Paso 1: onStep1Submit ---

    @Test
    fun step1_blankFullName_showsNameError() {
        viewModel.onEmailChange("carlos@correo.com")
        viewModel.onStep1Submit()

        assertEquals(R.string.registration_error_name_required, viewModel.uiState.value.fullNameErrorRes)
        assertNull(viewModel.uiState.value.emailErrorRes)
    }

    @Test
    fun step1_invalidEmail_showsEmailError() {
        viewModel.onFullNameChange("Carlos Mendoza")
        viewModel.onEmailChange("carlos@correo")
        viewModel.onStep1Submit()

        assertNull(viewModel.uiState.value.fullNameErrorRes)
        assertEquals(R.string.login_error_email_invalid, viewModel.uiState.value.emailErrorRes)
    }

    @Test
    fun step1_blankNameAndInvalidEmail_showsBothErrors() {
        viewModel.onStep1Submit()

        assertEquals(R.string.registration_error_name_required, viewModel.uiState.value.fullNameErrorRes)
        assertEquals(R.string.login_error_email_invalid, viewModel.uiState.value.emailErrorRes)
    }

    @Test
    fun step1_emailWithSpaces_isTrimmedAndAccepted() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onFullNameChange("Carlos Mendoza")
        viewModel.onEmailChange("  carlos@correo.com  ")
        viewModel.onStep1Submit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.emailErrorRes)
        assertEquals(listOf(RegistrationEvent.Step1Valid), events)
        collector.cancel()
    }

    @Test
    fun step1_validData_emitsStep1ValidEvent() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillStep1()
        viewModel.onStep1Submit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.fullNameErrorRes)
        assertNull(viewModel.uiState.value.emailErrorRes)
        assertEquals(listOf(RegistrationEvent.Step1Valid), events)
        collector.cancel()
    }

    @Test
    fun step1_invalidData_doesNotEmitStep1Valid() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onStep1Submit()
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        collector.cancel()
    }

    @Test
    fun editingFieldsAfterStep1Errors_clearsErrors() {
        viewModel.onStep1Submit()

        viewModel.onFullNameChange("C")
        viewModel.onEmailChange("c")

        assertNull(viewModel.uiState.value.fullNameErrorRes)
        assertNull(viewModel.uiState.value.emailErrorRes)
    }

    // --- Paso 2: onCreateAccount ---

    @Test
    fun createAccount_withoutAcceptingPolicy_showsPolicyErrorAndSkipsRepository() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillStep1()
        viewModel.onDocumentNumberChange("1116234567")
        viewModel.onPhoneChange("3001234567")
        viewModel.onNeighborhoodChange("El Paraíso")
        // La casilla de la política queda sin marcar
        viewModel.onCreateAccount()
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        assertFalse(viewModel.uiState.value.isCreatingAccount)
        assertEquals(R.string.registration_error_policy_required, viewModel.uiState.value.dataPolicyErrorRes)
        // Si el repositorio se hubiera llamado, el correo ya estaría ocupado y este registro fallaría
        val retry = fakeRepository.signUpCitizen(TestDataFactory.citizenRegistration(email = "carlos@correo.com"))
        assertTrue(retry.isSuccess)
        collector.cancel()
    }

    @Test
    fun acceptingPolicyAfterError_clearsPolicyError() {
        fillStep1()
        viewModel.onCreateAccount()

        viewModel.onDataPolicyChange(true)

        assertNull(viewModel.uiState.value.dataPolicyErrorRes)
    }

    @Test
    fun createAccount_withBlankDocument_showsDocumentError() {
        fillStep1()
        viewModel.onDataPolicyChange(true)
        viewModel.onPhoneChange("3001234567")
        viewModel.onNeighborhoodChange("El Paraíso")
        viewModel.onCreateAccount()

        assertEquals(R.string.registration_error_field_required, viewModel.uiState.value.documentErrorRes)
    }

    @Test
    fun createAccount_withInvalidDocument_showsDocumentInvalidError() {
        fillStep1()
        fillStep2()
        viewModel.onDocumentNumberChange("1234") // demasiado corto
        viewModel.onCreateAccount()

        assertEquals(R.string.registration_error_document_invalid, viewModel.uiState.value.documentErrorRes)
    }

    @Test
    fun createAccount_withBlankPhone_showsPhoneError() {
        fillStep1()
        viewModel.onDocumentNumberChange("1116234567")
        viewModel.onNeighborhoodChange("El Paraíso")
        viewModel.onDataPolicyChange(true)
        viewModel.onCreateAccount()

        assertEquals(R.string.registration_error_field_required, viewModel.uiState.value.phoneErrorRes)
    }

    @Test
    fun createAccount_withInvalidPhone_showsPhoneInvalidError() {
        fillStep1()
        fillStep2()
        viewModel.onPhoneChange("6022345678") // no empieza por 3
        viewModel.onCreateAccount()

        assertEquals(R.string.registration_error_phone_invalid, viewModel.uiState.value.phoneErrorRes)
    }

    @Test
    fun createAccount_withBlankNeighborhood_showsNeighborhoodError() {
        fillStep1()
        viewModel.onDocumentNumberChange("1116234567")
        viewModel.onPhoneChange("3001234567")
        viewModel.onDataPolicyChange(true)
        viewModel.onCreateAccount()

        assertEquals(R.string.registration_error_field_required, viewModel.uiState.value.neighborhoodErrorRes)
    }

    @Test
    fun createAccount_withPassportAndAlphanumericDoc_isAccepted() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillStep1()
        viewModel.onDocumentTypeChange(DocumentType.PASAPORTE)
        viewModel.onDocumentNumberChange("AB12345")
        viewModel.onPhoneChange("3001234567")
        viewModel.onNeighborhoodChange("El Paraíso")
        viewModel.onDataPolicyChange(true)
        viewModel.onCreateAccount()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.documentErrorRes)
        val created = events.filterIsInstance<RegistrationEvent.AccountCreated>()
        assertEquals(1, created.size)
        collector.cancel()
    }

    @Test
    fun createAccount_onSuccess_emitsAccountCreatedWithEmail() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillStep1()
        fillStep2()
        viewModel.onCreateAccount()
        advanceUntilIdle()

        val created = events.filterIsInstance<RegistrationEvent.AccountCreated>()
        assertEquals(1, created.size)
        assertEquals("carlos@correo.com", created.first().email)
        assertFalse(viewModel.uiState.value.isCreatingAccount)
        collector.cancel()
    }

    @Test
    fun createAccount_onFailure_emitsErrorEvent() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fakeRepository.error = RuntimeException("network error")
        fillStep1()
        fillStep2()
        viewModel.onCreateAccount()
        advanceUntilIdle()

        val errors = events.filterIsInstance<RegistrationEvent.Error>()
        assertEquals(1, errors.size)
        assertEquals(R.string.login_error_generic, errors.first().messageRes)
        assertEquals(R.string.login_error_generic, viewModel.uiState.value.submitErrorRes)
        assertFalse(viewModel.uiState.value.isCreatingAccount)
        collector.cancel()
    }

    @Test
    fun createAccount_withoutConnection_emitsConnectionError() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fakeRepository.error = UnknownHostException("Unable to resolve host 10.0.2.2")
        fillStep1()
        fillStep2()
        viewModel.onCreateAccount()
        advanceUntilIdle()

        val errors = events.filterIsInstance<RegistrationEvent.Error>()
        assertEquals(listOf(RegistrationEvent.Error(R.string.registration_error_no_connection)), errors)
        assertEquals(R.string.registration_error_no_connection, viewModel.uiState.value.submitErrorRes)
        assertFalse(viewModel.uiState.value.isCreatingAccount)
        collector.cancel()
    }

    @Test
    fun createAccount_withRegisteredEmail_emitsDuplicateError() = runTest {
        val events = mutableListOf<RegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fakeRepository.addAccount(TestDataFactory.userAccount(email = "carlos@correo.com"))
        fillStep1()
        fillStep2()
        viewModel.onCreateAccount()
        advanceUntilIdle()

        val errors = events.filterIsInstance<RegistrationEvent.Error>()
        assertEquals(listOf(RegistrationEvent.Error(R.string.registration_error_email_registered)), errors)
        assertTrue(events.none { it is RegistrationEvent.AccountCreated })
        collector.cancel()
    }

    @Test
    fun createAccount_retryAfterError_clearsSubmitError() = runTest {
        fakeRepository.error = UnknownHostException("sin red")
        fillStep1()
        fillStep2()
        viewModel.onCreateAccount()
        advanceUntilIdle()

        fakeRepository.error = null
        viewModel.onCreateAccount()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.submitErrorRes)
    }

    @Test
    fun editingFieldsAfterStep2Errors_clearsErrors() {
        fillStep1()
        viewModel.onDataPolicyChange(true)
        viewModel.onCreateAccount()

        viewModel.onDocumentTypeChange(DocumentType.FOREIGNER_ID)
        viewModel.onDocumentNumberChange("1")
        viewModel.onPhoneChange("3")
        viewModel.onNeighborhoodChange("B")

        val state = viewModel.uiState.value
        assertNull(state.documentErrorRes)
        assertNull(state.phoneErrorRes)
        assertNull(state.neighborhoodErrorRes)
    }

    // --- onResendEmail ---

    @Test
    fun resendEmail_withValidEmail_callsRepository() = runTest {
        fillStep1()
        viewModel.onResendEmail()
        advanceUntilIdle()

        assertTrue(fakeRepository.passwordResetEmails.contains("carlos@correo.com"))
        assertFalse(viewModel.uiState.value.isResendingEmail)
    }

    @Test
    fun resendEmail_withBlankEmail_doesNotCallRepository() = runTest {
        viewModel.onResendEmail()
        advanceUntilIdle()

        assertTrue(fakeRepository.passwordResetEmails.isEmpty())
    }
}

