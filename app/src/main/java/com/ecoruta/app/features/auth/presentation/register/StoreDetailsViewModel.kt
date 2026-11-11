package com.ecoruta.app.features.auth.presentation.register

// ViewModel para el segundo paso del registro del comercio con los datos del establecimiento
// Guarda los campos, valida cada uno y avisa cuando la solicitud queda lista para enviarse
// Por ahora no envía la solicitud ni sube el RUT en PDF porque eso depende del repositorio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.profile.presentation.StoreCategory
import com.ecoruta.app.features.profile.presentation.StoreIdType
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
sealed interface StoreDetailsEvent {
    data object FormValid : StoreDetailsEvent
}

@HiltViewModel
class StoreDetailsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(StoreDetailsUiState())
    val uiState: StateFlow<StoreDetailsUiState> = _uiState.asStateFlow()

    private val _events = Channel<StoreDetailsEvent>(Channel.BUFFERED)
    val events: Flow<StoreDetailsEvent> = _events.receiveAsFlow()

    // Guarda el nombre del comercio y borra su error anterior
    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameErrorRes = null) }
    }

    // Cambia entre cédula y NIT y borra el error porque el formato esperado cambia
    fun onIdTypeChange(idType: StoreIdType) {
        _uiState.update { it.copy(idType = idType, idErrorRes = null) }
    }

    // Guarda el número de identificación y borra su error anterior
    fun onIdNumberChange(idNumber: String) {
        _uiState.update { it.copy(idNumber = idNumber, idErrorRes = null) }
    }

    // Guarda la categoría elegida y borra su error anterior
    fun onCategoryChange(category: StoreCategory) {
        _uiState.update { it.copy(category = category, categoryErrorRes = null) }
    }

    // Guarda la dirección y borra su error anterior
    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address, addressErrorRes = null) }
    }

    // Guarda el teléfono y borra su error anterior
    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneErrorRes = null) }
    }

    // Guarda el nombre del RUT en PDF que eligió el gerente y borra su error anterior
    fun onRutPicked(fileName: String) {
        _uiState.update { it.copy(rutFileName = fileName, rutErrorRes = null) }
    }

    // Quita el RUT adjunto para que el gerente pueda elegir otro
    fun onRemovePdf() {
        _uiState.update { it.copy(rutFileName = null) }
    }

    // Guarda si el gerente aceptó la política de tratamiento de datos
    fun onDataPolicyChange(accepted: Boolean) {
        _uiState.update { it.copy(acceptedDataPolicy = accepted) }
    }

    // Quita el aviso sin conexión para que el gerente vuelva al formulario
    fun onRetry() {
        _uiState.update { it.copy(isOffline = false) }
    }

    // Valida todos los campos a la vez y avisa si la solicitud quedó lista
    // Sin la política aceptada no hace nada, igual que el botón deshabilitado de la pantalla
    fun onSubmit() {
        val current = _uiState.value
        if (current.isSubmitting || !current.acceptedDataPolicy) return

        val nameError = if (current.name.isBlank()) R.string.store_application_error_name else null
        val idError = validateId(current.idType, current.idNumber)
        val categoryError = if (current.category == null) R.string.store_application_error_category else null
        val addressError = if (current.address.isBlank()) R.string.store_registration_error_address else null
        val phoneError = validatePhone(current.phone)
        // El RUT en PDF es obligatorio para enviar la solicitud a revisión
        val rutError = if (current.rutFileName == null) R.string.store_application_error_rut else null

        _uiState.update {
            it.copy(
                nameErrorRes = nameError,
                idErrorRes = idError,
                categoryErrorRes = categoryError,
                addressErrorRes = addressError,
                phoneErrorRes = phoneError,
                rutErrorRes = rutError,
            )
        }

        val isValid = listOf(nameError, idError, categoryError, addressError, phoneError, rutError).all { it == null }
        if (isValid) {
            viewModelScope.launch { _events.send(StoreDetailsEvent.FormValid) }
        }
    }

    // Se ignoran puntos y espacios para aceptar el número escrito como 900.123.456-7
    private fun validateId(idType: StoreIdType, idNumber: String): Int? {
        val number = idNumber.replace(".", "").replace(" ", "")
        if (number.isEmpty()) return R.string.store_registration_error_id
        return when (idType) {
            StoreIdType.NIT -> if (NIT_REGEX.matches(number)) null else R.string.store_application_error_nit
            StoreIdType.NATIONAL_ID -> if (NATIONAL_ID_REGEX.matches(number)) null else R.string.store_application_error_cedula
        }
    }

    // Se ignoran los espacios para aceptar el teléfono escrito como 300 123 4567
    private fun validatePhone(phone: String): Int? {
        val digits = phone.replace(" ", "")
        if (digits.isEmpty()) return R.string.store_registration_error_phone
        return if (PHONE_REGEX.matches(digits)) null else R.string.store_application_error_phone
    }

    private companion object {
        // Nueve dígitos y el dígito de verificación, con guion opcional
        val NIT_REGEX = Regex("^\\d{9}-?\\d$")

        // Cédula de 6 a 10 dígitos
        val NATIONAL_ID_REGEX = Regex("^\\d{6,10}$")

        // Celular o fijo de 10 dígitos
        val PHONE_REGEX = Regex("^\\d{10}$")
    }
}
