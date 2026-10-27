package com.ecoruta.app.features.reports.presentation.create

// Estado inmutable del formulario guiado de reporte
// Modela paso activo, lista de fotos seleccionadas, afectación, GPS y progreso

import android.net.Uri
import androidx.annotation.StringRes

enum class ReportType {
    CONTENEDOR_LLENO,
    BOTADERO_CLANDESTINO,
}

enum class ReportStatus {
    PENDIENTE,
    APROBADO,
    ATENDIDO,
    RECHAZADO,
    DUPLICADO,
}

data class Report(
    val id: String,
    val type: ReportType,
    val status: ReportStatus,
    val address: String,
    val area: String = "",
    val createdAtMillis: Long,
    val photoUrl: String? = null,
    val statusNote: String? = null,
    val originalReportId: String? = null,
)

// Estado visual compartido por los pasos del formulario de nuevo reporte
data class NewReportUiState(
    val photos: List<Uri> = emptyList(),
    val description: String = "",
    @param:StringRes val photoError: Int? = null,
    val type: ReportType? = null,
    @param:StringRes val typeError: Int? = null,
    val address: String = "",
    val area: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isEditingAddress: Boolean = false,
    val isLocating: Boolean = false,
    val isFetchingLocation: Boolean = false,
    @param:StringRes val locationError: Int? = null,
    val isSubmitting: Boolean = false,
    val sentWithoutBackend: Boolean = false,
) {
    val canAddPhoto: Boolean get() = photos.size < MAX_PHOTOS
    val hasGpsLocation: Boolean get() = latitude != null && longitude != null

    companion object {
        const val MAX_PHOTOS = 3
        const val MAX_DESCRIPTION = 300
    }
}

typealias CreateReportUiState = NewReportUiState
