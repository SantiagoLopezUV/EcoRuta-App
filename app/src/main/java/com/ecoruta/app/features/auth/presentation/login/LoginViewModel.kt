package com.ecoruta.app.features.auth.presentation.login

// ViewModel para la pantalla de inicio de sesión
// Procesa validaciones de credenciales, emite estado y maneja llamadas al repositorio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import com.ecoruta.app.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.exception.AuthRestException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Guarda el correo escrito y borra los errores que quedaron del intento anterior
    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(email = email, emailErrorRes = null, showInvalidCredentials = false, noticeRes = null)
        }
    }

    // Guarda la contraseña escrita y borra los errores que quedaron del intento anterior
    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, passwordErrorRes = null, showInvalidCredentials = false, noticeRes = null)
        }
    }

    // Muestra u oculta el texto de la contraseña
    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    // Valida los campos, inicia sesión y consulta el rol activo para decidir la ruta
    fun onSubmit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val email = current.email.trim()
        val password = current.password
        val emailError = if (EMAIL_REGEX.matches(email)) null else R.string.login_error_email_invalid
        val passwordError = if (password.isEmpty()) R.string.login_error_password_empty else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailErrorRes = emailError, passwordErrorRes = passwordError) }
            return
        }

        // La carga se marca antes de lanzar la corrutina para bloquear un segundo envío
        _uiState.update {
            it.copy(
                isSubmitting = true,
                emailErrorRes = null,
                passwordErrorRes = null,
                showInvalidCredentials = false,
                noticeRes = null,
            )
        }
        viewModelScope.launch {
            authRepository.signInWithEmail(email, password)
                .onSuccess { account ->
                    // El rol sale del perfil porque la cuenta de autenticación no lo trae
                    profileRepository.getUserRolesSummary()
                        .onSuccess { summary ->
                            val role = summary.activeRole
                            _uiState.update {
                                if (account.mustChangePassword) {
                                    it.copy(isSubmitting = false, passwordChangeRole = role)
                                } else {
                                    it.copy(isSubmitting = false, signedInRole = role)
                                }
                            }
                        }
                        .onFailure {
                            _uiState.update { it.copy(isSubmitting = false, noticeRes = R.string.login_error_generic) }
                        }
                }
                .onFailure { error ->
                    // Solo la clave o el correo equivocados marcan las credenciales y cualquier otro fallo muestra el aviso general
                    if (error.isInvalidCredentials()) {
                        _uiState.update { it.copy(isSubmitting = false, showInvalidCredentials = true) }
                    } else {
                        _uiState.update { it.copy(isSubmitting = false, noticeRes = R.string.login_error_generic) }
                    }
                }
        }
    }

    // Limpia las banderas de navegación cuando la pantalla ya las atendió para no repetir el salto al volver
    fun onNavigationHandled() {
        _uiState.update { it.copy(signedInRole = null, passwordChangeRole = null, webOnlyAccess = false) }
    }

    // Reconoce el rechazo del servidor por correo o contraseña incorrectos
    // Se compara el código crudo porque esta versión del cliente no trae ese código en su lista conocida
    private fun Throwable.isInvalidCredentials(): Boolean =
        this is AuthRestException && error == INVALID_CREDENTIALS_CODE

    private companion object {
        const val INVALID_CREDENTIALS_CODE = "invalid_credentials"

        // Exige texto antes y después de la arroba y un dominio con punto, sin espacios
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
