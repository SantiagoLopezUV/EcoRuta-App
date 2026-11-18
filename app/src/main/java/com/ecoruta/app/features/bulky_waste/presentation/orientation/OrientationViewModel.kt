package com.ecoruta.app.features.bulky_waste.presentation.orientation

// ViewModel de la guía de gestión autorizada de escombros y voluminosos
// Consulta los gestores autorizados y muestra solo los que reciben el tipo de residuo elegido

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.bulky_waste.domain.repository.BulkyWasteRepository
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OrientationViewModel @Inject constructor(
    private val bulkyWasteRepository: BulkyWasteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrientationUiState>(OrientationUiState.Loading)
    val uiState: StateFlow<OrientationUiState> = _uiState.asStateFlow()

    // Tipo de residuo con el que se filtran los gestores, si es nulo se muestran todos
    private var selectedType: DebrisType? = null
    private var hasLoaded = false
    private var isLoading = false

    // Recibe el tipo elegido en el paso anterior y carga los gestores la primera vez
    fun onTypeSelected(type: DebrisType?) {
        if (hasLoaded && type == selectedType) return
        selectedType = type
        load()
    }

    // Vuelve a consultar los gestores después de un error
    fun onRetry() {
        load()
    }

    private fun load() {
        if (isLoading) return
        isLoading = true
        _uiState.value = OrientationUiState.Loading

        viewModelScope.launch {
            bulkyWasteRepository.getAuthorizedManagers()
                .onSuccess { managers ->
                    val type = selectedType
                    val matching = if (type == null) managers else managers.filter { type in it.acceptedTypes }
                    hasLoaded = true
                    _uiState.value = if (matching.isEmpty()) {
                        OrientationUiState.Empty
                    } else {
                        OrientationUiState.Loaded(matching)
                    }
                }
                .onFailure {
                    _uiState.value = OrientationUiState.Error(R.string.debris_managers_error)
                }
            isLoading = false
        }
    }
}
