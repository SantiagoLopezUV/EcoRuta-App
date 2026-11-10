package com.ecoruta.app.features.auth.presentation.password_reset

import com.ecoruta.app.R
import com.ecoruta.app.core.testing.FakeAuthRepository
import com.ecoruta.app.core.testing.MainDispatcherRule
import com.ecoruta.app.core.testing.TestDataFactory
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import io.github.jan.supabase.auth.exception.AuthRestException
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Pruebas de los pasos de correo, código y nueva clave en la recuperación de contraseña usando un repositorio falso en memoria
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAuthRepository
    private lateinit var countingRepository: CountingAuthRepository
    private lateinit var viewModel: PasswordResetViewModel

    // Cuenta las verificaciones de código y los guardados de clave para comprobar que no se envían dos a la vez
    private class CountingAuthRepository(
        private val delegate: FakeAuthRepository,
    ) : AuthRepository by delegate {
        var verifyCalls = 0
        var updateCalls = 0

        override suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit> {
            verifyCalls++
            return delegate.verifyPasswordResetCode(email, code)
        }

        override suspend fun updatePassword(newPassword: String): Result<Unit> {
            updateCalls++
            return delegate.updatePassword(newPassword)
        }
    }

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        countingRepository = CountingAuthRepository(repository)
        viewModel = PasswordResetViewModel(countingRepository)
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

    @Test
    fun codeChange_keepsOnlySixDigits() {
        viewModel.onCodeChange("12a34 5678")

        assertEquals("123456", viewModel.uiState.value.code)
    }

    @Test
    fun incompleteCode_showsErrorAndDoesNotCallRepository() = runTest {
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onCodeChange("1234")
        viewModel.onSubmitCode()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.recovery_error_code_incomplete, state.codeErrorRes)
        assertFalse(state.isVerifyingCode)
        assertFalse(state.isCodeVerified)
        assertEquals(0, countingRepository.verifyCalls)
    }

    @Test
    fun validCode_verifiesWithEmailAndMarksCodeVerified() = runTest {
        repository.expectedResetCode = "482910"
        viewModel.onEmailChange(" ana@correo.com ")
        viewModel.onCodeChange("482910")
        viewModel.onSubmitCode()

        // La corrutina sigue en cola porque el dispatcher de prueba aún no avanza
        assertTrue(viewModel.uiState.value.isVerifyingCode)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isCodeVerified)
        assertFalse(state.isVerifyingCode)
        assertNull(state.codeErrorRes)
        assertNull(state.noticeRes)
        assertEquals("ana@correo.com", repository.verifiedResetEmail)
    }

    @Test
    fun invalidCode_showsIncorrectCodeError() = runTest {
        // El servidor responde con este código de error cuando el código no es válido o ya venció
        repository.error = AuthRestException("otp_expired", "Token has expired or is invalid", 403)
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onCodeChange("000000")
        viewModel.onSubmitCode()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.recovery_error_code_incorrect, state.codeErrorRes)
        assertNull(state.noticeRes)
        assertFalse(state.isCodeVerified)
        assertFalse(state.isVerifyingCode)
    }

    @Test
    fun verifyCodeNetworkError_showsGeneralNoticeInsteadOfCodeError() = runTest {
        repository.error = IOException("Sin conexión")
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onCodeChange("482910")
        viewModel.onSubmitCode()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.recovery_error_request_failed, state.noticeRes)
        assertNull(state.codeErrorRes)
        assertFalse(state.isCodeVerified)
    }

    @Test
    fun verifyCodeOtherServerError_showsGeneralNotice() = runTest {
        repository.error = AuthRestException("over_request_rate_limit", "Too many requests", 429)
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onCodeChange("482910")
        viewModel.onSubmitCode()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.recovery_error_request_failed, state.noticeRes)
        assertNull(state.codeErrorRes)
    }

    @Test
    fun codeChange_clearsCodeErrorAndNotice() = runTest {
        repository.error = AuthRestException("otp_expired", "Token has expired or is invalid", 403)
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onCodeChange("000000")
        viewModel.onSubmitCode()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.codeErrorRes)

        viewModel.onCodeChange("00000")

        val state = viewModel.uiState.value
        assertNull(state.codeErrorRes)
        assertNull(state.noticeRes)
    }

    @Test
    fun submitCodeWhileVerifying_isIgnored() = runTest {
        viewModel.onEmailChange(TestDataFactory.DEFAULT_EMAIL)
        viewModel.onCodeChange("482910")
        viewModel.onSubmitCode()
        viewModel.onSubmitCode()
        advanceUntilIdle()

        assertEquals(1, countingRepository.verifyCalls)
    }

    @Test
    fun passwordFieldEdits_updateValuesAndVisibility() {
        viewModel.onNewPasswordChange("EcoRuta2026")
        viewModel.onConfirmPasswordChange("EcoRuta2026")
        viewModel.onToggleNewPasswordVisibility()
        viewModel.onToggleConfirmPasswordVisibility()

        val state = viewModel.uiState.value
        assertEquals("EcoRuta2026", state.newPassword)
        assertEquals("EcoRuta2026", state.confirmPassword)
        assertTrue(state.isNewPasswordVisible)
        assertTrue(state.isConfirmPasswordVisible)
    }

    @Test
    fun weakPassword_showsInvalidPasswordError() = runTest {
        viewModel.onNewPasswordChange("ecoruta")
        viewModel.onConfirmPasswordChange("ecoruta")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.recovery_error_password_invalid, state.newPasswordErrorRes)
        assertNull(state.confirmPasswordErrorRes)
        assertFalse(state.isSavingPassword)
        assertEquals(0, countingRepository.updateCalls)
    }

    @Test
    fun passwordWithoutLetter_showsInvalidPasswordError() = runTest {
        viewModel.onNewPasswordChange("12345678")
        viewModel.onConfirmPasswordChange("12345678")
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(R.string.recovery_error_password_invalid, viewModel.uiState.value.newPasswordErrorRes)
        assertEquals(0, countingRepository.updateCalls)
    }

    @Test
    fun mismatchedPasswords_showsMismatchError() = runTest {
        viewModel.onNewPasswordChange("EcoRuta2026")
        viewModel.onConfirmPasswordChange("EcoRuta2027")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.newPasswordErrorRes)
        assertEquals(R.string.recovery_error_password_mismatch, state.confirmPasswordErrorRes)
        assertFalse(state.isPasswordUpdated)
        assertEquals(0, countingRepository.updateCalls)
    }

    @Test
    fun passwordChange_clearsItsError() = runTest {
        viewModel.onNewPasswordChange("ecoruta")
        viewModel.onConfirmPasswordChange("otra")
        viewModel.onSave()
        advanceUntilIdle()

        viewModel.onNewPasswordChange("EcoRuta2026")
        viewModel.onConfirmPasswordChange("EcoRuta2026")

        val state = viewModel.uiState.value
        assertNull(state.newPasswordErrorRes)
        assertNull(state.confirmPasswordErrorRes)
    }

    @Test
    fun fullFlow_updatesPasswordAndMarksSuccess() = runTest {
        repository.addAccount(TestDataFactory.userAccount(email = "ana@correo.com"))
        repository.expectedResetCode = "482910"

        viewModel.onEmailChange("ana@correo.com")
        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onCodeChange("482910")
        viewModel.onSubmitCode()
        advanceUntilIdle()
        viewModel.onNewPasswordChange("EcoRuta2026")
        viewModel.onConfirmPasswordChange("EcoRuta2026")
        viewModel.onSave()

        // La corrutina sigue en cola porque el dispatcher de prueba aún no avanza
        assertTrue(viewModel.uiState.value.isSavingPassword)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isPasswordUpdated)
        assertFalse(state.isSavingPassword)
        assertNull(state.noticeRes)
        assertEquals("EcoRuta2026", repository.passwordOf("ana@correo.com"))
    }

    @Test
    fun updatePasswordFailure_showsGeneralNoticeAndKeepsFields() = runTest {
        repository.error = IOException("Sin conexión")
        viewModel.onNewPasswordChange("EcoRuta2026")
        viewModel.onConfirmPasswordChange("EcoRuta2026")
        viewModel.onSave()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.recovery_error_request_failed, state.noticeRes)
        assertFalse(state.isPasswordUpdated)
        assertFalse(state.isSavingPassword)
        assertEquals("EcoRuta2026", state.newPassword)
    }

    @Test
    fun saveWhileSaving_isIgnored() = runTest {
        viewModel.onNewPasswordChange("EcoRuta2026")
        viewModel.onConfirmPasswordChange("EcoRuta2026")
        viewModel.onSave()
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(1, countingRepository.updateCalls)
    }

    @Test
    fun codeSent_startsResendCountdownAndCountsDownToZero() = runTest {
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onSubmit()
        runCurrent()

        assertEquals(60, viewModel.uiState.value.resendSecondsLeft)

        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(59, viewModel.uiState.value.resendSecondsLeft)

        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.resendSecondsLeft)
    }

    @Test
    fun resendWhileCountdownRunning_isIgnored() = runTest {
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onSubmit()
        runCurrent()

        viewModel.onResendClick()
        runCurrent()

        assertEquals(listOf("ana@correo.com"), repository.passwordResetEmails)
        assertFalse(viewModel.uiState.value.isRequestingCode)
    }

    @Test
    fun resendAfterCountdown_sendsToSameEmailAndRestartsCountdown() = runTest {
        repository.error = AuthRestException("otp_expired", "Token has expired or is invalid", 403)
        viewModel.onEmailChange(" ana@correo.com ")
        viewModel.onSubmit()
        advanceUntilIdle()
        viewModel.onCodeChange("000000")
        viewModel.onSubmitCode()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.codeErrorRes)

        repository.error = null
        viewModel.onResendClick()

        // La corrutina sigue en cola porque el dispatcher de prueba aún no avanza
        assertTrue(viewModel.uiState.value.isRequestingCode)

        runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf("ana@correo.com"), repository.passwordResetEmails)
        assertFalse(state.isRequestingCode)
        assertEquals(60, state.resendSecondsLeft)
        assertEquals("", state.code)
        assertNull(state.codeErrorRes)
        assertNull(state.noticeRes)
    }

    @Test
    fun resendWhileRequesting_isIgnored() = runTest {
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onResendClick()
        viewModel.onResendClick()
        advanceUntilIdle()

        assertEquals(listOf("ana@correo.com"), repository.passwordResetEmails)
    }

    @Test
    fun resendFailure_showsGeneralNoticeAndKeepsCounterAtZero() = runTest {
        repository.error = IOException("Sin conexión")
        viewModel.onEmailChange("ana@correo.com")
        viewModel.onResendClick()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.recovery_error_request_failed, state.noticeRes)
        assertFalse(state.isRequestingCode)
        assertEquals(0, state.resendSecondsLeft)
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
