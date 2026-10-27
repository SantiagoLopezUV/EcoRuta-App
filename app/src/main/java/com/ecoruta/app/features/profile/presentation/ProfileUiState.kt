package com.ecoruta.app.features.profile.presentation

// Estado inmutable de la pantalla de perfil
// Refleja autenticación, datos del usuario, roles activos, balance y estado de carga

import android.net.Uri
import androidx.annotation.StringRes
import com.ecoruta.app.core.ui.components.PickedDocument

// Rol de visualización de las pantallas de la aplicación móvil
enum class UserRole {
    CIUDADANO,
    RECICLADOR,
    CHOFER,
    COMERCIO,
}

// Datos visuales del perfil de usuario para renderizar el perfil y el cambio de rol
data class UserProfile(
    val fullName: String,
    val role: UserRole,
    val city: String,
    val email: String,
    val documentNumber: String = "",
    val ecoPoints: Int = 0,
    val photoUrl: String? = null,
    val approvedRoles: Set<UserRole> = setOf(role),
    val pendingRoles: Set<UserRole> = emptySet(),
)

enum class StoreCategory {
    TIENDA_DE_BARRIO,
    RESTAURANTE,
    CAFETERIA,
    MERCADO,
    PAPELERIA_LIBRERIA,
    OTRO,
}

enum class StoreIdType {
    CEDULA,
    NIT,
}

enum class RecyclerModality {
    INDEPENDIENTE,
    VINCULADO,
}

data class Company(
    val id: String,
    val name: String,
    val type: String,
)

// Estado visual del perfil de la sesión usado por el inicio ciudadano y la pantalla de perfil
sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Loaded(val profile: UserProfile, val localPhoto: Uri? = null) : ProfileUiState {
        val hasPhoto: Boolean get() = localPhoto != null || profile.photoUrl != null
    }

    data object Error : ProfileUiState
}

data class StoreApplicationUiState(
    val name: String = "",
    val address: String = "",
    val phone: String = "",
    val category: StoreCategory? = null,
    val idType: StoreIdType = StoreIdType.NIT,
    val idNumber: String = "",
    val rut: PickedDocument? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val addressError: Int? = null,
    @StringRes val phoneError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val idError: Int? = null,
    @StringRes val rutError: Int? = null,
    val isSubmitting: Boolean = false,
)

data class RecyclerApplicationUiState(
    val fullName: String = "",
    val documentNumber: String = "",
    val modality: RecyclerModality = RecyclerModality.INDEPENDIENTE,
    val phone: String = "",
    val idDocument: PickedDocument? = null,
    val company: Company? = null,
    @StringRes val phoneError: Int? = null,
    @StringRes val documentError: Int? = null,
    val query: String = "",
    val results: List<Company> = emptyList(),
    val isSearching: Boolean = false,
    @StringRes val searchError: Int? = null,
    val isSubmitting: Boolean = false,
    val submittedWithoutBackend: Boolean = false,
)
