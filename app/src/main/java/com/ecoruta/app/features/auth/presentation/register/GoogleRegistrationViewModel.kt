package com.ecoruta.app.features.auth.presentation.register

// ViewModel para completar el registro de ciudadano autenticado mediante Google OAuth
// Maneja el inicio de sesión federado con Google, valida los datos complementarios del perfil y emite eventos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.auth.domain.model.CitizenRegistration
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

sealed interface GoogleRegistrationEvent {
    data class GoogleAuthSuccess(val email: String, val fullName: String?) : GoogleRegistrationEvent
    data class ProfileCompleted(val email: String) : GoogleRegistrationEvent
    data class Error(val messageRes: Int) : GoogleRegistrationEvent
}

@HiltViewModel
class GoogleRegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CitizenRegistrationUiState())
    val uiState: StateFlow<CitizenRegistrationUiState> = _uiState.asStateFlow()

    private val _events = Channel<GoogleRegistrationEvent>(Channel.BUFFERED)
    val events: Flow<GoogleRegistrationEvent> = _events.receiveAsFlow()

    fun onGoogleSignIn(idToken: String, rawNonce: String? = null) {
        if (idToken.isBlank()) {
            viewModelScope.launch {
                _events.send(GoogleRegistrationEvent.Error(R.string.login_error_generic))
            }
            return
        }

        _uiState.update { it.copy(isCheckingEmail = true) }

        viewModelScope.launch {
            val result = authRepository.signUpWithGoogle(idToken, rawNonce)
            _uiState.update { it.copy(isCheckingEmail = false) }

            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            email = user.email,
                            fullName = user.fullName.orEmpty(),
                            fullNameErrorRes = null,
                            emailErrorRes = null,
                        )
                    }
                    _events.send(GoogleRegistrationEvent.GoogleAuthSuccess(user.email, user.fullName))
                },
                onFailure = {
                    _events.send(GoogleRegistrationEvent.Error(R.string.login_error_generic))
                },
            )
        }
    }

    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, fullNameErrorRes = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailErrorRes = null) }
    }

    fun onDocumentTypeChange(documentType: DocumentType) {
        _uiState.update { it.copy(documentType = documentType, documentErrorRes = null) }
    }

    fun onDocumentNumberChange(documentNumber: String) {
        _uiState.update { it.copy(documentNumber = documentNumber, documentErrorRes = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneErrorRes = null) }
    }

    fun onNeighborhoodChange(neighborhood: String) {
        _uiState.update { it.copy(neighborhood = neighborhood, neighborhoodErrorRes = null) }
    }

    fun onDataPolicyChange(accepted: Boolean) {
        _uiState.update { it.copy(acceptedDataPolicy = accepted) }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isCreatingAccount) return
        if (!current.acceptedDataPolicy) return

        val documentError = validateDocument(current.documentType, current.documentNumber)
        val phoneError = validatePhone(current.phone)
        val neighborhoodError = validateNeighborhood(current.neighborhood)

        _uiState.update {
            it.copy(
                documentErrorRes = documentError,
                phoneErrorRes = phoneError,
                neighborhoodErrorRes = neighborhoodError,
            )
        }

        val isValid = listOf(documentError, phoneError, neighborhoodError).all { it == null }
        if (!isValid) return

        _uiState.update { it.copy(isCreatingAccount = true) }

        viewModelScope.launch {
            val registration = CitizenRegistration(
                email = current.email.trim(),
                password = "GoogleAuth_${current.documentNumber.trim()}",
                fullName = current.fullName.trim(),
                documentNumber = current.documentNumber.trim(),
                phone = current.phone.replace(" ", "").trim(),
                neighborhood = current.neighborhood.trim(),
            )

            val result = authRepository.signUpCitizen(registration)
            _uiState.update { it.copy(isCreatingAccount = false) }

            result.fold(
                onSuccess = {
                    _events.send(GoogleRegistrationEvent.ProfileCompleted(registration.email))
                },
                onFailure = {
                    _events.send(GoogleRegistrationEvent.Error(R.string.login_error_generic))
                },
            )
        }
    }

    private fun validateDocument(documentType: DocumentType, documentNumber: String): Int? {
        val number = documentNumber.trim()
        if (number.isEmpty()) return R.string.registration_error_field_required
        val regex = if (documentType == DocumentType.PASAPORTE) PASSPORT_REGEX else NUMERIC_DOCUMENT_REGEX
        return if (regex.matches(number)) null else R.string.registration_error_document_invalid
    }

    private fun validatePhone(phone: String): Int? {
        val digits = phone.replace(" ", "")
        if (digits.isEmpty()) return R.string.registration_error_field_required
        return if (MOBILE_REGEX.matches(digits)) null else R.string.registration_error_phone_invalid
    }

    private fun validateNeighborhood(neighborhood: String): Int? =
        if (neighborhood.isBlank()) R.string.registration_error_field_required else null

    private companion object {
        val NUMERIC_DOCUMENT_REGEX = Regex("^\\d{5,15}$")
        val PASSPORT_REGEX = Regex("^[A-Za-z0-9]{5,15}$")
        val MOBILE_REGEX = Regex("^3\\d{9}$")
    }
}

