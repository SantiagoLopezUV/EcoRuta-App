package com.ecoruta.app.features.auth.presentation.register

// ViewModel para el formulario de registro del reciclador independiente o vinculado a una empresa
// Guarda los campos, valida cada uno y avisa cuando el formulario queda listo para enviarse
// Por ahora no crea la cuenta ni maneja la cédula en PDF porque eso depende del repositorio y del selector de archivos

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

// Avisos de una sola vez que el grafo de navegación consumirá cuando se conecte este ViewModel
sealed interface RecyclerRegistrationEvent {
    data object FormValid : RecyclerRegistrationEvent
}

@HiltViewModel
class RecyclerRegistrationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(RecyclerRegistrationUiState())
    val uiState: StateFlow<RecyclerRegistrationUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecyclerRegistrationEvent>(Channel.BUFFERED)
    val events: Flow<RecyclerRegistrationEvent> = _events.receiveAsFlow()

    // Guarda el nombre escrito y borra su error anterior
    fun onFullNameChange(fullName: String) {
        _uiState.update { it.copy(fullName = fullName, fullNameErrorRes = null) }
    }

    // Guarda el correo escrito y borra su error anterior
    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailErrorRes = null) }
    }

    // Cambia el tipo de documento y borra el error porque el formato esperado cambia
    fun onDocumentTypeChange(documentType: DocumentType) {
        _uiState.update { it.copy(documentType = documentType, documentErrorRes = null) }
    }

    // Guarda el número de documento escrito y borra su error anterior
    fun onDocumentNumberChange(documentNumber: String) {
        _uiState.update { it.copy(documentNumber = documentNumber, documentErrorRes = null) }
    }

    // Guarda el celular escrito y borra su error anterior
    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneErrorRes = null) }
    }

    // Valida todos los campos a la vez y avisa si el formulario quedó listo
    fun onSubmit() {
        val current = _uiState.value
        if (current.isCreatingAccount) return

        val fullNameError = validateFullName(current.fullName)
        val emailError = validateEmail(current.email)
        val documentError = validateDocument(current.documentType, current.documentNumber)
        val phoneError = validatePhone(current.phone)

        _uiState.update {
            it.copy(
                fullNameErrorRes = fullNameError,
                emailErrorRes = emailError,
                documentErrorRes = documentError,
                phoneErrorRes = phoneError,
            )
        }

        val isValid = listOf(fullNameError, emailError, documentError, phoneError).all { it == null }
        if (isValid) {
            viewModelScope.launch { _events.send(RecyclerRegistrationEvent.FormValid) }
        }
    }

    private fun validateFullName(fullName: String): Int? =
        if (fullName.isBlank()) R.string.registration_error_name_required else null

    private fun validateEmail(email: String): Int? =
        if (EMAIL_REGEX.matches(email.trim())) null else R.string.login_error_email_invalid

    // El pasaporte admite letras y números, los demás documentos solo números
    private fun validateDocument(documentType: DocumentType, documentNumber: String): Int? {
        val number = documentNumber.trim()
        if (number.isEmpty()) return R.string.registration_error_field_required
        val regex = if (documentType == DocumentType.PASAPORTE) PASSPORT_REGEX else NUMERIC_DOCUMENT_REGEX
        return if (regex.matches(number)) null else R.string.registration_error_document_invalid
    }

    // Se ignoran los espacios para aceptar el celular escrito como 300 123 4567
    private fun validatePhone(phone: String): Int? {
        val digits = phone.replace(" ", "")
        if (digits.isEmpty()) return R.string.registration_error_field_required
        return if (MOBILE_REGEX.matches(digits)) null else R.string.registration_error_phone_invalid
    }

    private companion object {
        // Exige texto antes y después de la arroba y un dominio con punto, sin espacios
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        // Documentos numéricos entre 5 y 15 dígitos
        val NUMERIC_DOCUMENT_REGEX = Regex("^\\d{5,15}$")

        // Pasaporte con letras o números entre 5 y 15 caracteres
        val PASSPORT_REGEX = Regex("^[A-Za-z0-9]{5,15}$")

        // Celular colombiano de 10 dígitos que empieza por 3
        val MOBILE_REGEX = Regex("^3\\d{9}$")
    }
}
