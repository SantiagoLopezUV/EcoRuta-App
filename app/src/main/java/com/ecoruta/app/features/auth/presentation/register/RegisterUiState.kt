package com.ecoruta.app.features.auth.presentation.register

// Estado inmutable de la pantalla de registro
// Almacena valores de los campos del formulario, mensajes de error y estado de envío

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ecoruta.app.R

// Tipos de documento de identidad disponibles en el formulario de registro
enum class DocumentType(val code: String) {
    CITIZENSHIP_ID("CC"),
    TARJETA_IDENTIDAD("TI"),
    FOREIGNER_ID("CE"),
    PERMISO_PROTECCION_TEMPORAL("PPT"),
    PASAPORTE("PAS"),
}

// Modalidades de participación que el usuario puede elegir al iniciar el registro
enum class RegistrationType(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
) {
    CITIZEN(R.string.registration_type_citizen, R.string.registration_type_citizen_description, R.drawable.ic_reg_leaf),
    INDEPENDENT_RECYCLER(R.string.registration_type_independent, R.string.registration_type_independent_description, R.drawable.ic_reg_recycle),
    COMPANY_RECYCLER(R.string.registration_type_company_recycler, R.string.registration_type_company_recycler_description, R.drawable.ic_reg_recycle),
    STORE(R.string.registration_type_store, R.string.registration_type_store_description, R.drawable.ic_store),
}

// Estado compartido entre los pasos del formulario de registro para el ciudadano
data class CitizenRegistrationUiState(
    // Datos correspondientes al primer paso de creación de cuenta
    val fullName: String = "",
    val email: String = "",
    @StringRes val fullNameErrorRes: Int? = null,
    @StringRes val emailErrorRes: Int? = null,
    val isCheckingEmail: Boolean = false,
    // Datos correspondientes al segundo paso de información personal y ubicación
    val documentType: DocumentType = DocumentType.CITIZENSHIP_ID,
    val documentNumber: String = "",
    val phone: String = "",
    val neighborhood: String = "",
    val acceptedDataPolicy: Boolean = false,
    @StringRes val documentErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    @StringRes val neighborhoodErrorRes: Int? = null,
    val isCreatingAccount: Boolean = false,
    // Estado de envío en la pantalla de confirmación por correo
    val isResendingEmail: Boolean = false,
)

typealias RegisterUiState = CitizenRegistrationUiState
