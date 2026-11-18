package com.ecoruta.app.features.bulky_waste.presentation.orientation

// Estado inmutable de la orientación de gestión autorizada de escombros y voluminosos
// Representa la carga de los gestores, la lista filtrada por tipo, el caso sin gestores y el error

import androidx.annotation.StringRes
import com.ecoruta.app.features.bulky_waste.domain.model.RcdManagerOrientation

sealed interface OrientationUiState {
    data object Loading : OrientationUiState

    // Gestores que reciben el tipo de residuo elegido por el ciudadano
    data class Loaded(val managers: List<RcdManagerOrientation>) : OrientationUiState

    // No hay gestores activos que reciban el tipo de residuo elegido
    data object Empty : OrientationUiState

    data class Error(@StringRes val messageRes: Int) : OrientationUiState
}
