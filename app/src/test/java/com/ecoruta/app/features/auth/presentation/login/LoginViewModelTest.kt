package com.ecoruta.app.features.auth.presentation.login

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeAuthRepository
import com.ecoruta.app.core.testing.FakeProfileRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.core.testing.TestDataFactory
import com.ecoruta.app.features.auth.domain.model.UserAccount
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import com.ecoruta.app.features.profile.domain.model.UserRole
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Pruebas del inicio de sesión con correo y contraseña usando repositorios falsos en memoria
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var countingRepository: CountingAuthRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var viewModel: LoginViewModel

    // Cuenta los intentos de inicio de sesión para comprobar que no se envían dos a la vez
    private class CountingAuthRepository(
        private val delegate: FakeAuthRepository,
    ) : AuthRepository by delegate {
        var signInCalls = 0

        override suspend fun signInWithEmail(email: String, password: String): Result<UserAccount> {
            signInCalls++
            return delegate.signInWithEmail(email, password)
        }
    }

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        authRepository.addAccount(TestDataFactory.userAccount())
        countingRepository = CountingAuthRepository(authRepository)
        profileRepository = FakeProfileRepository()
        viewModel = LoginViewModel(countingRepository, profileRepository)
    }

    private fun fillValidCredentials() {
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onPasswordChange(TestDataFactory.DEFAULT_PASSWORD)
    }

    @Test
    fun initialState_hasEmptyFieldsAndNoFlags() {
        assertEquals(LoginUiState(), viewModel.uiState.value)
    }

    @Test
    fun fieldEdits_updateEmailPasswordAndVisibility() {
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onPasswordChange("secreta")
        viewModel.onTogglePasswordVisibility()

        val state = viewModel.uiState.value
        assertEquals("ana@correo.com", state.email)
        assertEquals("secreta", state.password)
        assertTrue(state.isPasswordVisible)

        viewModel.onTogglePasswordVisibility()
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun emptyFields_showBothErrorsAndDoNotCallRepository() = runTest {
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.login_error_email_invalid, state.emailErrorRes)
        assertEquals(R.string.login_error_password_empty, state.passwordErrorRes)
        assertFalse(state.isSubmitting)
        assertEquals(0, countingRepository.signInCalls)
    }

    @Test
    fun invalidEmail_showsEmailErrorOnly() = runTest {
        viewModel.onEmailChange("ana@")
        viewModel.onPasswordChange(TestDataFactory.DEFAULT_PASSWORD)
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.login_error_email_invalid, state.emailErrorRes)
        assertNull(state.passwordErrorRes)
        assertEquals(0, countingRepository.signInCalls)
    }

    @Test
    fun emptyPassword_showsPasswordErrorOnly() = runTest {
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.emailErrorRes)
        assertEquals(R.string.login_error_password_empty, state.passwordErrorRes)
        assertEquals(0, countingRepository.signInCalls)
    }

    @Test
    fun fieldEdits_clearValidationErrors() = runTest {
        viewModel.onSubmit()
        advanceUntilIdle()

        fillValidCredentials()

        val state = viewModel.uiState.value
        assertNull(state.emailErrorRes)
        assertNull(state.passwordErrorRes)
    }

    @Test
    fun validCredentials_setSignedInRoleFromProfile() = runTest {
        profileRepository.currentProfile = profileRepository.currentProfile.copy(
            activeRole = UserRole.RECICLADOR,
            approvedRoles = setOf(UserRole.RECICLADOR),
        )
        viewModel.onEmailChange("  ${TestDataFactory.DEFAULT_EMAIL}  ")
        viewModel.onPasswordChange(TestDataFactory.DEFAULT_PASSWORD)
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UserRole.RECICLADOR, state.signedInRole)
        assertNull(state.passwordChangeRole)
        assertFalse(state.isSubmitting)
        assertFalse(state.showInvalidCredentials)
        assertNull(state.noticeRes)
    }

    @Test
    fun temporaryPassword_setsPasswordChangeRole() = runTest {
        val account = TestDataFactory.firstLoginAccount()
        authRepository.addAccount(account)
        profileRepository.currentProfile = profileRepository.currentProfile.copy(
            activeRole = UserRole.CHOFER,
            approvedRoles = setOf(UserRole.CHOFER),
        )
        viewModel.onEmailChange(account.email)
        viewModel.onPasswordChange(TestDataFactory.DEFAULT_PASSWORD)
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UserRole.CHOFER, state.passwordChangeRole)
        assertNull(state.signedInRole)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun wrongPassword_showsInvalidCredentials() = runTest {
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onPasswordChange("ClaveEquivocada")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showInvalidCredentials)
        assertNull(state.signedInRole)
        assertNull(state.noticeRes)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun editAfterInvalidCredentials_clearsWarning() = runTest {
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onPasswordChange("ClaveEquivocada")
        viewModel.onSubmit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showInvalidCredentials)

        viewModel.onPasswordChange(TestDataFactory.DEFAULT_PASSWORD)

        assertFalse(viewModel.uiState.value.showInvalidCredentials)
    }

    @Test
    fun networkError_showsGenericNoticeAndKeepsFields() = runTest {
        authRepository.error = IOException("Sin conexión")
        fillValidCredentials()
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.login_error_generic, state.noticeRes)
        assertFalse(state.showInvalidCredentials)
        assertFalse(state.isSubmitting)
        assertEquals(TestDataFactory.DEFAULT_EMAIL, state.email)
        assertEquals(TestDataFactory.DEFAULT_PASSWORD, state.password)
    }

    @Test
    fun profileError_showsGenericNoticeWithoutRole() = runTest {
        profileRepository.error = IOException("Sin conexión")
        fillValidCredentials()
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.login_error_generic, state.noticeRes)
        assertNull(state.signedInRole)
        assertNull(state.passwordChangeRole)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun submit_showsLoadingUntilRequestFinishes() = runTest {
        fillValidCredentials()
        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value.isSubmitting)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun submitWhileLoading_isIgnored() = runTest {
        fillValidCredentials()
        viewModel.onSubmit()
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, countingRepository.signInCalls)
        assertEquals(UserRole.CIUDADANO, viewModel.uiState.value.signedInRole)
    }

    @Test
    fun retryAfterNetworkError_succeeds() = runTest {
        authRepository.error = IOException("Sin conexión")
        fillValidCredentials()
        viewModel.onSubmit()
        advanceUntilIdle()
        assertEquals(R.string.login_error_generic, viewModel.uiState.value.noticeRes)

        authRepository.error = null
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(UserRole.CIUDADANO, state.signedInRole)
        assertNull(state.noticeRes)
        assertEquals(2, countingRepository.signInCalls)
    }
}
