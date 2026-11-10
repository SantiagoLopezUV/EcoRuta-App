package com.ecoruta.app.features.auth.presentation.first_login

// ViewModel para el cambio obligatorio de clave temporal
// Valida coincidencia de nueva contraseña, actualiza en servidor y limpia bandera
// La clave provisional solo se exige escrita porque el repositorio no la recibe para verificarla

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

@HiltViewModel
class ForceChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForceChangePasswordUiState())
    val uiState: StateFlow<ForceChangePasswordUiState> = _uiState.asStateFlow()

    private val _events = Channel<ChangePasswordEvent>(Channel.BUFFERED)
    val events: Flow<ChangePasswordEvent> = _events.receiveAsFlow()

    // Guarda la clave provisional escrita y borra su error anterior
    fun onCurrentChange(value: String) {
        _uiState.update { it.copy(currentPassword = value, currentErrorRes = null) }
    }

    // Guarda la clave nueva y borra su error anterior
    fun onNewChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newErrorRes = null) }
    }

    // Guarda la confirmación y borra su error anterior
    fun onConfirmChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmErrorRes = null) }
    }

    // Muestra u oculta el texto de cada campo de contraseña
    fun onToggleCurrentVisibility() {
        _uiState.update { it.copy(isCurrentVisible = !it.isCurrentVisible) }
    }

    fun onToggleNewVisibility() {
        _uiState.update { it.copy(isNewVisible = !it.isNewVisible) }
    }

    fun onToggleConfirmVisibility() {
        _uiState.update { it.copy(isConfirmVisible = !it.isConfirmVisible) }
    }

    // Valida los tres campos y pide al servidor guardar la clave nueva y apagar la obligación de cambiarla
    fun onSave() {
        val current = _uiState.value
        if (current.isSaving) return

        val currentErrorRes = if (current.currentPassword.isBlank()) R.string.login_error_password_empty else null
        val newErrorRes = when {
            !isStrong(current.newPassword) -> R.string.recovery_error_password_invalid
            current.newPassword == current.currentPassword -> R.string.change_password_error_same
            else -> null
        }
        val confirmErrorRes = if (current.confirmPassword != current.newPassword) R.string.recovery_error_password_mismatch else null

        if (currentErrorRes != null || newErrorRes != null || confirmErrorRes != null) {
            _uiState.update {
                it.copy(currentErrorRes = currentErrorRes, newErrorRes = newErrorRes, confirmErrorRes = confirmErrorRes)
            }
            return
        }

        // La carga se marca antes de lanzar la corrutina para bloquear un segundo envío
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            authRepository.updateFirstLoginPassword(current.newPassword)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(ChangePasswordEvent.Done)
                }
                .onFailure {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(ChangePasswordEvent.ShowNotice(R.string.recovery_error_request_failed))
                }
        }
    }

    // Aplica las mismas reglas que muestra la lista de requisitos de la pantalla
    private fun isStrong(password: String): Boolean =
        password.length >= MIN_PASSWORD_LENGTH && password.any { it.isLetter() } && password.any { it.isDigit() }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}

typealias OperatorActivationViewModel = ForceChangePasswordViewModel

