package com.ecoruta.app.features.profile.presentation

// Estado inmutable de la edición del perfil del ciudadano
// Guarda los datos editables, el correo de solo lectura, el barrio, el radio del radar, los errores y el avance del guardado

import androidx.annotation.StringRes
import com.ecoruta.app.features.routes.presentation.map.Neighborhood
import com.ecoruta.app.features.routes.presentation.radar.RadarDefaults

// Datos personales del ciudadano junto con su barrio y el radio de aviso del radar
data class CitizenEditProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val neighborhood: Neighborhood? = null,
    val neighborhoods: List<Neighborhood> = emptyList(),
    val radarRadiusMeters: Int = RadarDefaults.RADIUS,
    @StringRes val fullNameErrorRes: Int? = null,
    @StringRes val phoneErrorRes: Int? = null,
    @StringRes val neighborhoodErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    // El botón de guardar se deshabilita mientras algún campo tenga un error pendiente
    val hasErrors: Boolean
        get() = listOf(fullNameErrorRes, phoneErrorRes, neighborhoodErrorRes).any { it != null }

    // Radios disponibles para el radar desde el mínimo hasta el máximo permitido
    val radarRadiusOptions: List<Int>
        get() = (RadarDefaults.MIN_RADIUS..RadarDefaults.MAX_RADIUS step RadarDefaults.STEP).toList()
}

// Acciones de la edición del perfil del ciudadano
data class CitizenEditProfileActions(
    val onFullNameChange: (String) -> Unit = {},
    val onPhoneChange: (String) -> Unit = {},
    val onNeighborhoodChange: (Neighborhood) -> Unit = {},
    val onRadarRadiusChange: (Int) -> Unit = {},
    val onSave: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onBack: () -> Unit = {},
)
