package com.ecoruta.app.features.bulky_waste.presentation.tracking

// Estado inmutable del seguimiento de escombros
// Contiene estado activo en línea de tiempo, valor en COP y fecha confirmada

import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisRequest

sealed interface MyDebrisRequestsUiState {
    data object Loading : MyDebrisRequestsUiState
    data class Loaded(val requests: List<DebrisRequest>, val isSample: Boolean = false) : MyDebrisRequestsUiState
    data object Error : MyDebrisRequestsUiState
}

sealed interface DebrisTrackingUiState {
    data object Loading : DebrisTrackingUiState
    data class Loaded(val request: DebrisRequest, val isSample: Boolean = false) : DebrisTrackingUiState
    data object NotFound : DebrisTrackingUiState
    data object Error : DebrisTrackingUiState
}

typealias RcdTrackingUiState = DebrisTrackingUiState
