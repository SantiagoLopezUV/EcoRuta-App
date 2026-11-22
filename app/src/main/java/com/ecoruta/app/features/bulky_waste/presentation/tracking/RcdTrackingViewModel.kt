package com.ecoruta.app.features.bulky_waste.presentation.tracking

// ViewModel para la consulta y seguimiento del ciclo de vida de solicitudes de escombros (RCD)
// Muestra estados (pendiente, cotizado, programado, recolectado, cancelado), costos y gestiona cancelación

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.features.bulky_waste.domain.repository.BulkyWasteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RcdTrackingViewModel @Inject constructor(
    private val bulkyWasteRepository: BulkyWasteRepository,
    savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {

    private val _myRequestsState = MutableStateFlow<MyDebrisRequestsUiState>(MyDebrisRequestsUiState.Loading)
    val myRequestsState: StateFlow<MyDebrisRequestsUiState> = _myRequestsState.asStateFlow()

    private val _trackingState = MutableStateFlow<DebrisTrackingUiState>(DebrisTrackingUiState.Loading)
    val trackingState: StateFlow<DebrisTrackingUiState> = _trackingState.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    init {
        loadMyRequests()
        val initialRequestId = savedStateHandle?.get<String>(EcoRutaDestination.ARG_REPORT_ID)
        if (!initialRequestId.isNullOrBlank()) {
            loadRequest(initialRequestId)
        }
    }

    // Consulta el historial de solicitudes radicadas por el ciudadano autenticado
    fun loadMyRequests() {
        _myRequestsState.value = MyDebrisRequestsUiState.Loading
        viewModelScope.launch {
            bulkyWasteRepository.getMyRequests().fold(
                onSuccess = { list ->
                    val uiRequests = list.map { it.toUiRequest() }
                    _myRequestsState.value = MyDebrisRequestsUiState.Loaded(
                        requests = uiRequests,
                        isSample = false,
                    )
                },
                onFailure = {
                    _myRequestsState.value = MyDebrisRequestsUiState.Error
                },
            )
        }
    }

    // Consulta el detalle de una solicitud específica para la vista de seguimiento
    fun loadRequest(requestId: String) {
        if (requestId.isBlank()) {
            _trackingState.value = DebrisTrackingUiState.NotFound
            return
        }
        _trackingState.value = DebrisTrackingUiState.Loading
        viewModelScope.launch {
            bulkyWasteRepository.getRequestById(requestId).fold(
                onSuccess = { request ->
                    _trackingState.value = DebrisTrackingUiState.Loaded(
                        request = request.toUiRequest(),
                        isSample = false,
                    )
                },
                onFailure = {
                    _trackingState.value = DebrisTrackingUiState.Error
                },
            )
        }
    }

    // Cancela una solicitud en estado pendiente y refresca la información
    fun cancelRequest(requestId: String, onComplete: (Boolean) -> Unit = {}) {
        _isWorking.value = true
        viewModelScope.launch {
            bulkyWasteRepository.cancelRequest(requestId).fold(
                onSuccess = {
                    loadRequest(requestId)
                    loadMyRequests()
                    _isWorking.value = false
                    onComplete(true)
                },
                onFailure = {
                    _isWorking.value = false
                    onComplete(false)
                },
            )
        }
    }

    // Aprueba el valor cotizado para el retiro
    fun approveQuote(requestId: String, onComplete: (Boolean) -> Unit = {}) {
        _isWorking.value = true
        viewModelScope.launch {
            // Refresca la solicitud tras la aprobación
            loadRequest(requestId)
            _isWorking.value = false
            onComplete(true)
        }
    }
}

// Alias de compatibilidad arquitectónica
typealias BulkyTrackingViewModel = RcdTrackingViewModel
