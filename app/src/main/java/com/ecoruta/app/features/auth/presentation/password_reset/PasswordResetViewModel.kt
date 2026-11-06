package com.ecoruta.app.features.auth.presentation.password_reset

// ViewModel para la recuperación de contraseña
// Gestiona el envío de correo OTP, validación de código y actualización de clave
// Por ahora solo cubre el paso del correo porque el repositorio aún no verifica el código ni guarda la clave nueva

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
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
                    _events.send(PasswordRecoveryEvent.CodeSent)
                }
                .onFailure {
                    _uiState.update { it.copy(isRequestingCode = false, requestFailed = true) }
                }
        }
    }

    private companion object {
        // Exige texto antes y después de la arroba y un dominio con punto, sin espacios
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
