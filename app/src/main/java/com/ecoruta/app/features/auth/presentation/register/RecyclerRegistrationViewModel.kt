package com.ecoruta.app.features.auth.presentation.register

// ViewModel para el formulario de registro del reciclador independiente o vinculado a una empresa
// Guarda los campos, valida cada uno y avisa cuando el formulario queda listo para enviarse
// Por ahora no crea la cuenta ni sube la cédula en PDF porque eso depende del repositorio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.core.ui.components.PickedDocument
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
sealed interface RecyclerRegistrationEvent {
    data object FormValid : RecyclerRegistrationEvent

    // La cédula está adjunta y el registro con Google puede seguir
    data object GoogleSignUpAllowed : RecyclerRegistrationEvent
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

    // Guarda la cédula en PDF que eligió el usuario y borra su error anterior
    fun onIdDocumentPicked(document: PickedDocument) {
        _uiState.update { it.copy(idDocument = document, idDocumentErrorRes = null) }
    }

    // Quita la cédula adjunta para que el usuario pueda elegir otra
    fun onIdDocumentRemoved() {
        _uiState.update { it.copy(idDocument = null) }
    }

    // Valida todos los campos a la vez y avisa si el formulario quedó listo
    fun onSubmit() {
        val current = _uiState.value
        if (current.isCreatingAccount) return

        val errors = validateRecyclerForm(
            fullName = current.fullName,
            email = current.email,
            documentType = current.documentType,
            documentNumber = current.documentNumber,
            hasIdDocument = current.idDocument != null,
            phone = current.phone,
        )

        _uiState.update {
            it.copy(
                fullNameErrorRes = errors.fullNameErrorRes,
                emailErrorRes = errors.emailErrorRes,
                documentErrorRes = errors.documentErrorRes,
                idDocumentErrorRes = errors.idDocumentErrorRes,
                phoneErrorRes = errors.phoneErrorRes,
            )
        }

        if (errors.isValid) {
            viewModelScope.launch { _events.send(RecyclerRegistrationEvent.FormValid) }
        }
    }

    // Antes de registrarse con Google solo exige la cédula, porque Google aporta el nombre y el correo
    fun onGoogleClick() {
        val current = _uiState.value
        if (current.isCreatingAccount) return

        val idDocumentError = validateIdDocument(current.idDocument != null)
        _uiState.update { it.copy(idDocumentErrorRes = idDocumentError) }

        if (idDocumentError == null) {
            viewModelScope.launch { _events.send(RecyclerRegistrationEvent.GoogleSignUpAllowed) }
        }
    }
}

// Errores de cada campo del formulario de reciclador, vacíos cuando el campo es válido
internal data class RecyclerFormErrors(
    val fullNameErrorRes: Int?,
    val emailErrorRes: Int?,
    val documentErrorRes: Int?,
    val idDocumentErrorRes: Int?,
    val phoneErrorRes: Int?,
) {
    val isValid: Boolean
        get() = listOf(fullNameErrorRes, emailErrorRes, documentErrorRes, idDocumentErrorRes, phoneErrorRes)
            .all { it == null }
}

// Regla de validación del formulario sin tipos de Android para poder probarla en la JVM
// Recibe solo si hay cédula adjunta porque el archivo elegido depende de Android
internal fun validateRecyclerForm(
    fullName: String,
    email: String,
    documentType: DocumentType,
    documentNumber: String,
    hasIdDocument: Boolean,
    phone: String,
): RecyclerFormErrors = RecyclerFormErrors(
    fullNameErrorRes = validateFullName(fullName),
    emailErrorRes = validateEmail(email),
    documentErrorRes = validateDocument(documentType, documentNumber),
    idDocumentErrorRes = validateIdDocument(hasIdDocument),
    phoneErrorRes = validatePhone(phone),
)

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

// La cédula en PDF es obligatoria en los dos registros de reciclador
internal fun validateIdDocument(hasIdDocument: Boolean): Int? =
    if (hasIdDocument) null else R.string.recycler_registration_error_pdf

// Se ignoran los espacios para aceptar el celular escrito como 300 123 4567
private fun validatePhone(phone: String): Int? {
    val digits = phone.replace(" ", "")
    if (digits.isEmpty()) return R.string.registration_error_field_required
    return if (MOBILE_REGEX.matches(digits)) null else R.string.registration_error_phone_invalid
}

// Exige texto antes y después de la arroba y un dominio con punto, sin espacios
private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

// Documentos numéricos entre 5 y 15 dígitos
private val NUMERIC_DOCUMENT_REGEX = Regex("^\\d{5,15}$")

// Pasaporte con letras o números entre 5 y 15 caracteres
private val PASSPORT_REGEX = Regex("^[A-Za-z0-9]{5,15}$")

// Celular colombiano de 10 dígitos que empieza por 3
private val MOBILE_REGEX = Regex("^3\\d{9}$")
