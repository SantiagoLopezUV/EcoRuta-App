package com.ecoruta.app.features.reports.presentation.list

// Estado inmutable del historial de reportes
// Contiene colección de reportes, filtro activo, estado de carga y posibles errores

import com.ecoruta.app.features.reports.presentation.create.Report

sealed interface MyReportsUiState {
    data object Loading : MyReportsUiState
    data class Loaded(val reports: List<Report>, val isSample: Boolean = false) : MyReportsUiState
    data object Error : MyReportsUiState
}

sealed interface ReportDetailUiState {
    data object Loading : ReportDetailUiState
    data class Loaded(val report: Report, val isSample: Boolean = false) : ReportDetailUiState
    data object NotFound : ReportDetailUiState
    data object Error : ReportDetailUiState
}
