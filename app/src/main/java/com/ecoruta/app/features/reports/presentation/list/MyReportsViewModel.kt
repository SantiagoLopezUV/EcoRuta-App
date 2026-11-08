package com.ecoruta.app.features.reports.presentation.list

// ViewModel para la consulta del historial de reportes comunitarios
// Obtiene reportes del ciudadano desde el repositorio y mapea los modelos a la interfaz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.features.reports.domain.repository.ReportRepository
import com.ecoruta.app.features.reports.presentation.create.toUiReport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MyReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyReportsUiState>(MyReportsUiState.Loading)
    val uiState: StateFlow<MyReportsUiState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        _uiState.value = MyReportsUiState.Loading
        viewModelScope.launch {
            reportRepository.getMyReports()
                .onSuccess { domainReports ->
                    val uiReports = domainReports.map { it.toUiReport() }
                    _uiState.value = MyReportsUiState.Loaded(reports = uiReports, isSample = false)
                }
                .onFailure {
                    _uiState.value = MyReportsUiState.Error
                }
        }
    }

    fun refresh() = loadReports()
}
