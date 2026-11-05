package com.ecoruta.app.features.auth.presentation.register

// Estados inmutables de las pantallas de registro del comercio aliado
// Guardan los campos de cada paso, sus mensajes de error y el estado de envío

import androidx.annotation.StringRes
import com.ecoruta.app.features.profile.presentation.StoreCategory
import com.ecoruta.app.features.profile.presentation.StoreIdType

// Estado del primer paso con los datos de la cuenta del gerente del comercio
data class StoreOwnerAccountUiState(
    val fullName: String = "",
    val email: String = "",
    @StringRes val fullNameErrorRes: Int? = null,
    @StringRes val emailErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
) {
    val hasErrors: Boolean get() = fullNameErrorRes != null || emailErrorRes != null
}

// Estado del segundo paso con los datos del establecimiento y la aceptación de la política de datos
data class StoreDetailsUiState(
    val name: String = "",
    val idType: StoreIdType = StoreIdType.NIT,
    val idNumber: String = "",
    val category: StoreCategory? = null,
    val address: String = "",
    val phone: String = "",
    val acceptedDataPolicy: Boolean = false,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val idErrorRes: Int? = null,
    @StringRes val categoryErrorRes: Int? = null,
    @StringRes val addressErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false,
) {
    val hasErrors: Boolean
        get() = listOf(nameErrorRes, idErrorRes, categoryErrorRes, addressErrorRes, phoneErrorRes).any { it != null }
}

// Estado de la solicitud del comercio después de enviarla a revisión
sealed interface StoreRequestStatusUiState {
    data object Loading : StoreRequestStatusUiState

    data class InReview(val storeName: String? = null) : StoreRequestStatusUiState

    data class Approved(val storeName: String? = null) : StoreRequestStatusUiState

    data class Rejected(val reason: String? = null) : StoreRequestStatusUiState

    data object Offline : StoreRequestStatusUiState

    data object Error : StoreRequestStatusUiState
}
