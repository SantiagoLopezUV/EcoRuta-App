package com.ecoruta.app.features.auth.presentation.password_reset

// ViewModel para la recuperación de contraseña
// Gestiona el envío de correo OTP, validación de código y actualización de clave

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Avisos de una sola vez que el grafo de navegación consumirá para cambiar de pantalla cuando se conecte este ViewModel
sealed interface PasswordRecoveryEvent {
    data object CodeSent : PasswordRecoveryEvent
}

@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PasswordRecoveryUiState())
    val uiState: StateFlow<PasswordRecoveryUiState> = _uiState.asStateFlow()

    private val _events = Channel<PasswordRecoveryEvent>(Channel.BUFFERED)
    val events: Flow<PasswordRecoveryEvent> = _events.receiveAsFlow()

    private var countdownJob: Job? = null

    // Guarda el correo escrito y borra los errores que quedaron del intento anterior
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailErrorRes = null, requestFailed = false) }
    }

    // Valida el correo y pide al servidor el envío del código de recuperación
    fun onSubmit() {
        val current = _uiState.value
        if (current.isRequestingCode) return

        val email = current.email.trim()
        if (!EMAIL_REGEX.matches(email)) {
            _uiState.update { it.copy(emailErrorRes = R.string.login_error_email_invalid) }
            return
        }

        // La carga se marca antes de lanzar la corrutina para bloquear un segundo envío
        _uiState.update { it.copy(isRequestingCode = true, emailErrorRes = null, requestFailed = false) }
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
                .onSuccess {
                    _uiState.update { it.copy(isRequestingCode = false) }
                    startResendCountdown()
                    _events.send(PasswordRecoveryEvent.CodeSent)
                }
                .onFailure {
                    _uiState.update { it.copy(isRequestingCode = false, requestFailed = true) }
                }
        }
    }

    // Pide un código nuevo al mismo correo cuando el contador ya llegó a cero
    fun onResendClick() {
        val current = _uiState.value
        if (current.isRequestingCode || current.resendSecondsLeft > 0) return

        // La carga se marca antes de lanzar la corrutina para bloquear un segundo envío
        _uiState.update { it.copy(isRequestingCode = true, noticeRes = null) }
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(current.email.trim())
                .onSuccess {
                    // El código anterior deja de servir, así que se limpia el campo y sus errores
                    _uiState.update {
                        it.copy(isRequestingCode = false, code = "", codeErrorRes = null, isCodeExpired = false)
                    }
                    startResendCountdown()
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isRequestingCode = false, noticeRes = R.string.recovery_error_request_failed)
                    }
                }
        }
    }

    // Guarda solo los dígitos del código hasta su largo máximo y borra los errores anteriores
    fun onCodeChange(code: String) {
        val digits = code.filter { it.isDigit() }.take(RECOVERY_CODE_LENGTH)
        _uiState.update { it.copy(code = digits, codeErrorRes = null, noticeRes = null) }
    }

    // Valida que el código tenga seis dígitos y pide al servidor verificarlo con el correo del primer paso
    fun onSubmitCode() {
        val current = _uiState.value
        if (current.isVerifyingCode) return

        if (!CODE_REGEX.matches(current.code)) {
            _uiState.update { it.copy(codeErrorRes = R.string.recovery_error_code_incomplete) }
            return
        }

        // La carga se marca antes de lanzar la corrutina para bloquear un segundo envío
        _uiState.update { it.copy(isVerifyingCode = true, codeErrorRes = null, noticeRes = null) }
        viewModelScope.launch {
            authRepository.verifyPasswordResetCode(current.email.trim(), current.code)
                .onSuccess {
                    _uiState.update { it.copy(isVerifyingCode = false, isCodeVerified = true) }
                }
                .onFailure { error ->
                    // Solo el rechazo del código marca el campo y cualquier otro fallo muestra el aviso general
                    if (error.isInvalidCode()) {
                        _uiState.update {
                            it.copy(isVerifyingCode = false, codeErrorRes = R.string.recovery_error_code_incorrect)
                        }
                    } else {
                        _uiState.update {
                            it.copy(isVerifyingCode = false, noticeRes = R.string.recovery_error_request_failed)
                        }
                    }
                }
        }
    }

    // Guarda la nueva contraseña y borra su error anterior
    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordErrorRes = null, noticeRes = null) }
    }

    // Guarda la confirmación y borra su error anterior
    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordErrorRes = null, noticeRes = null) }
    }

    // Muestra u oculta el texto de cada campo de contraseña
    fun onToggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    // Valida la nueva contraseña y su confirmación y pide al servidor guardarla
    fun onSave() {
        val current = _uiState.value
        if (current.isSavingPassword) return

        val newPasswordError = if (isStrong(current.newPassword)) null else R.string.recovery_error_password_invalid
        val confirmPasswordError =
            if (current.confirmPassword == current.newPassword) null else R.string.recovery_error_password_mismatch
        if (newPasswordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(newPasswordErrorRes = newPasswordError, confirmPasswordErrorRes = confirmPasswordError)
            }
            return
        }

        // La carga se marca antes de lanzar la corrutina para bloquear un segundo envío
        _uiState.update {
            it.copy(isSavingPassword = true, newPasswordErrorRes = null, confirmPasswordErrorRes = null, noticeRes = null)
        }
        viewModelScope.launch {
            authRepository.updatePassword(current.newPassword)
                .onSuccess {
                    _uiState.update { it.copy(isSavingPassword = false, isPasswordUpdated = true) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isSavingPassword = false, noticeRes = R.string.recovery_error_request_failed)
                    }
                }
        }
    }

    // Reinicia la cuenta regresiva y resta un segundo cada vez hasta llegar a cero
    private fun startResendCountdown() {
        countdownJob?.cancel()
        _uiState.update { it.copy(resendSecondsLeft = RESEND_COOLDOWN_SECONDS) }
        countdownJob = viewModelScope.launch {
            while (_uiState.value.resendSecondsLeft > 0) {
                delay(ONE_SECOND_MILLIS)
                _uiState.update { it.copy(resendSecondsLeft = it.resendSecondsLeft - 1) }
            }
        }
    }

    // El servidor responde con el mismo código de error cuando el código es incorrecto o ya venció
    private fun Throwable.isInvalidCode(): Boolean =
        this is AuthRestException && errorCode == AuthErrorCode.OtpExpired

    // Aplica las mismas reglas que muestra la lista de requisitos del registro
    private fun isStrong(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH && password.any { it.isLetter() } && password.any { it.isDigit() }

    private companion object {
        // Exige texto antes y después de la arroba y un dominio con punto, sin espacios
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        val CODE_REGEX = Regex("^\\d{$RECOVERY_CODE_LENGTH}$")
        const val MIN_PASSWORD_LENGTH = 8
        const val RESEND_COOLDOWN_SECONDS = 60
        const val ONE_SECOND_MILLIS = 1_000L
    }
}
