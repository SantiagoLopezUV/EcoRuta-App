package com.ecoruta.app.features.profile.presentation

// ViewModel para la pantalla de perfil y conmutador multirol
// Administra datos personales, cambio de rol activo, cierre de sesión y subida de foto
// Por ahora solo cubre la carga de roles y el cambio de rol activo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.profile.domain.repository.ProfileRepository
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

// Avisos de una sola vez que el grafo de navegación consumirá para llevar al inicio del rol nuevo
sealed interface ProfileEvent {
    data class RoleSwitched(val role: UserRole) : ProfileEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events: Flow<ProfileEvent> = _events.receiveAsFlow()

    init {
        loadRoles()
    }

    // Vuelve a pedir el perfil con sus roles después de un error de carga
    fun onRetry() {
        loadRoles()
    }

    // Cambia el rol activo solo si está aprobado, no es el actual y no hay otro cambio en curso
    fun onSwitchRole(role: UserRole) {
        val current = _uiState.value as? ProfileUiState.Loaded ?: return
        if (current.isSwitching) return
        if (role == current.profile.role) return
        if (role !in current.profile.approvedRoles) return

        // La carga se marca antes de lanzar la corrutina para bloquear un segundo cambio
        _uiState.update { current.copy(isSwitching = true, switchErrorRes = null) }
        viewModelScope.launch {
            profileRepository.switchActiveRole(role)
                .onSuccess {
                    updateLoaded { it.copy(profile = it.profile.copy(role = role), isSwitching = false) }
                    _events.send(ProfileEvent.RoleSwitched(role))
                }
                .onFailure {
                    updateLoaded { it.copy(isSwitching = false, switchErrorRes = R.string.recovery_error_request_failed) }
                }
        }
    }

    // Consulta el perfil con el rol activo y los roles aprobados y pendientes
    private fun loadRoles() {
        if ((_uiState.value as? ProfileUiState.Loaded)?.isSwitching == true) return
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            profileRepository.getProfile()
                .onSuccess { profile -> _uiState.value = ProfileUiState.Loaded(profile.toUserProfile()) }
                .onFailure { _uiState.value = ProfileUiState.Error }
        }
    }

    // Aplica el cambio solo cuando el estado ya tiene el perfil cargado
    private fun updateLoaded(transform: (ProfileUiState.Loaded) -> ProfileUiState.Loaded) {
        _uiState.update { state -> if (state is ProfileUiState.Loaded) transform(state) else state }
    }
}
