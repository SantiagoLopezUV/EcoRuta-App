package com.ecoruta.app.features.auth.presentation.register

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeAuthRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.core.testing.TestDataFactory
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

// Pruebas unitarias para GoogleRegistrationViewModel: autenticación federada con Google,
// validación de campos complementarios del perfil y emisión de eventos
@OptIn(ExperimentalCoroutinesApi::class)
class GoogleRegistrationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: GoogleRegistrationViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeAuthRepository()
        viewModel = GoogleRegistrationViewModel(fakeRepository)
    }

    private fun fillProfileFields() {
        viewModel.onFullNameChange("Carlos Mendoza")
        viewModel.onEmailChange("carlos@gmail.com")
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

    // --- onGoogleSignIn ---

    @Test
    fun onGoogleSignIn_withValidToken_updatesEmailAndNameAndEmitsSuccess() = runTest {
        val events = mutableListOf<GoogleRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        val testUser = TestDataFactory.googleAccount().copy(email = "test@google.com", fullName = "Usuario Google")
        fakeRepository.googleAccount = testUser

        viewModel.onGoogleSignIn("valid_token_123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("test@google.com", state.email)
        assertEquals("Usuario Google", state.fullName)
        assertFalse(state.isCheckingEmail)

        val success = events.filterIsInstance<GoogleRegistrationEvent.GoogleAuthSuccess>()
        assertEquals(1, success.size)
        assertEquals("test@google.com", success.first().email)
        collector.cancel()
    }

    @Test
    fun onGoogleSignIn_withBlankToken_emitsError() = runTest {
        val events = mutableListOf<GoogleRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        viewModel.onGoogleSignIn("   ")
        advanceUntilIdle()

        val errors = events.filterIsInstance<GoogleRegistrationEvent.Error>()
        assertEquals(1, errors.size)
        assertEquals(R.string.login_error_generic, errors.first().messageRes)
        collector.cancel()
    }

    @Test
    fun onGoogleSignIn_onRepositoryFailure_emitsError() = runTest {
        val events = mutableListOf<GoogleRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fakeRepository.error = RuntimeException("auth error")
        viewModel.onGoogleSignIn("token_123")
        advanceUntilIdle()

        val errors = events.filterIsInstance<GoogleRegistrationEvent.Error>()
        assertEquals(1, errors.size)
        assertEquals(R.string.login_error_generic, errors.first().messageRes)
        assertFalse(viewModel.uiState.value.isCheckingEmail)
        collector.cancel()
    }

    // --- onSubmit ---

    @Test
    fun onSubmit_withoutAcceptingPolicy_doesNothing() = runTest {
        val events = mutableListOf<GoogleRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillProfileFields()
        viewModel.onDataPolicyChange(false)
        viewModel.onSubmit()
        advanceUntilIdle()

        assertTrue(events.isEmpty())
        assertFalse(viewModel.uiState.value.isCreatingAccount)
        collector.cancel()
    }

    @Test
    fun onSubmit_withBlankDocument_showsDocumentError() {
        fillProfileFields()
        viewModel.onDocumentNumberChange("  ")
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_field_required, viewModel.uiState.value.documentErrorRes)
    }

    @Test
    fun onSubmit_withInvalidDocument_showsDocumentInvalidError() {
        fillProfileFields()
        viewModel.onDocumentNumberChange("123") // demasiado corto
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_document_invalid, viewModel.uiState.value.documentErrorRes)
    }

    @Test
    fun onSubmit_withBlankPhone_showsPhoneError() {
        fillProfileFields()
        viewModel.onPhoneChange("  ")
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_field_required, viewModel.uiState.value.phoneErrorRes)
    }

    @Test
    fun onSubmit_withInvalidPhone_showsPhoneInvalidError() {
        fillProfileFields()
        viewModel.onPhoneChange("6022345678") // no empieza por 3
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_phone_invalid, viewModel.uiState.value.phoneErrorRes)
    }

    @Test
    fun onSubmit_withBlankNeighborhood_showsNeighborhoodError() {
        fillProfileFields()
        viewModel.onNeighborhoodChange("  ")
        viewModel.onSubmit()

        assertEquals(R.string.registration_error_field_required, viewModel.uiState.value.neighborhoodErrorRes)
    }

    @Test
    fun onSubmit_withPassportAndAlphanumericDoc_isAccepted() = runTest {
        val events = mutableListOf<GoogleRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillProfileFields()
        viewModel.onDocumentTypeChange(DocumentType.PASAPORTE)
        viewModel.onDocumentNumberChange("PASS12345")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.documentErrorRes)
        val completed = events.filterIsInstance<GoogleRegistrationEvent.ProfileCompleted>()
        assertEquals(1, completed.size)
        collector.cancel()
    }

    @Test
    fun onSubmit_onSuccess_emitsProfileCompletedWithEmail() = runTest {
        val events = mutableListOf<GoogleRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fillProfileFields()
        viewModel.onSubmit()
        advanceUntilIdle()

        val completed = events.filterIsInstance<GoogleRegistrationEvent.ProfileCompleted>()
        assertEquals(1, completed.size)
        assertEquals("carlos@gmail.com", completed.first().email)
        assertFalse(viewModel.uiState.value.isCreatingAccount)
        collector.cancel()
    }

    @Test
    fun onSubmit_onFailure_emitsError() = runTest {
        val events = mutableListOf<GoogleRegistrationEvent>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.toList(events) }

        fakeRepository.error = RuntimeException("network error")
        fillProfileFields()
        viewModel.onSubmit()
        advanceUntilIdle()

        val errors = events.filterIsInstance<GoogleRegistrationEvent.Error>()
        assertEquals(1, errors.size)
        assertEquals(R.string.login_error_generic, errors.first().messageRes)
        assertFalse(viewModel.uiState.value.isCreatingAccount)
        collector.cancel()
    }

    @Test
    fun editingFieldsAfterErrors_clearsErrors() {
        fillProfileFields()
        viewModel.onDocumentNumberChange("")
        viewModel.onPhoneChange("")
        viewModel.onNeighborhoodChange("")
        viewModel.onSubmit()

        viewModel.onDocumentNumberChange("1")
        viewModel.onPhoneChange("3")
        viewModel.onNeighborhoodChange("B")

        val state = viewModel.uiState.value
        assertNull(state.documentErrorRes)
        assertNull(state.phoneErrorRes)
        assertNull(state.neighborhoodErrorRes)
    }
}
