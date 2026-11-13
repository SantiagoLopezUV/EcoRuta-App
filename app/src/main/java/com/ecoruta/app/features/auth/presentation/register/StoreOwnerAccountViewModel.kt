package com.ecoruta.app.features.auth.presentation.register

// ViewModel para el primer paso del registro del comercio con los datos de la cuenta del gerente
// Guarda el nombre y el correo, los valida y avisa cuando se puede continuar
// Por ahora no crea la cuenta porque eso depende del repositorio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
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

// Avisos de una sola vez que consume el grafo de navegación
sealed interface StoreOwnerAccountEvent {
    // Lleva el correo limpio porque la pantalla de confirmación lo recibe como argumento
    data class FormValid(val email: String) : StoreOwnerAccountEvent
}

@HiltViewModel
class StoreOwnerAccountViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StoreOwnerAccountUiState())
    val uiState: StateFlow<StoreOwnerAccountUiState> = _uiState.asStateFlow()

    private val _events = Channel<StoreOwnerAccountEvent>(Channel.BUFFERED)
    val events: Flow<StoreOwnerAccountEvent> = _events.receiveAsFlow()

    // Guarda el nombre del gerente y borra su error anterior
    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, fullNameErrorRes = null) }
    }

    // Guarda el correo del gerente y borra su error anterior
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailErrorRes = null) }
    }

    // Quita el aviso sin conexión para que el gerente vuelva al formulario
    fun onRetry() {
        _uiState.update { it.copy(isOffline = false) }
    }

    // Valida ambos campos a la vez y avisa si se puede pasar a la confirmación del correo
    fun onContinue() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val email = current.email.trim()
        val fullNameError = if (current.fullName.isBlank()) R.string.store_registration_error_owner_name else null
        val emailError = if (EMAIL_REGEX.matches(email)) null else R.string.store_registration_error_email

        _uiState.update { it.copy(fullNameErrorRes = fullNameError, emailErrorRes = emailError) }

        if (fullNameError == null && emailError == null) {
            viewModelScope.launch { _events.send(StoreOwnerAccountEvent.FormValid(email)) }
        }
    }

    private companion object {
        // Exige texto antes y después de la arroba y un dominio con punto, sin espacios
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
