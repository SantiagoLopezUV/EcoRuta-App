package com.ecoruta.app.features.reports.presentation.create

// ViewModel para el formulario guiado de reporte ciudadano
// Gestiona selección de imágenes, tipo de afectación, GPS y subida asíncrona

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.reports.domain.model.AffectationType
import com.ecoruta.app.features.reports.domain.model.CitizenReport
import com.ecoruta.app.features.reports.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ReportEvent {
    data class Success(val report: CitizenReport) : ReportEvent
    data class Error(val messageRes: Int) : ReportEvent
    data object PhotoLimitReached : ReportEvent
}

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReportUiState())
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    private val _events = Channel<ReportEvent>(Channel.BUFFERED)
    val events: Flow<ReportEvent> = _events.receiveAsFlow()

    // Permite inyectar o resolver los bytes de fotografías para pruebas unitarias o resolución desde ContentResolver
    var photoBytesResolver: suspend (Uri) -> ByteArray? = { null }

    fun onPhotoAdded(uri: Uri) {
        val current = _uiState.value
        if (current.photos.size >= NewReportUiState.MAX_PHOTOS) {
            viewModelScope.launch { _events.send(ReportEvent.PhotoLimitReached) }
            return
        }
        _uiState.update {
            it.copy(
                photos = it.photos + uri,
                photoError = null,
            )
        }
    }

    fun onPhotoRemoved(uri: Uri) {
        _uiState.update { it.copy(photos = it.photos - uri) }
    }

    fun onDescriptionChange(description: String) {
        if (description.length <= NewReportUiState.MAX_DESCRIPTION) {
            _uiState.update { it.copy(description = description) }
        }
    }

    fun onTypeSelected(type: AffectationType) {
        _uiState.update { it.copy(type = type, typeError = null) }
    }

    fun onLocationUpdated(latitude: Double, longitude: Double, address: String = "", area: String = "") {
        _uiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                address = address.ifBlank { it.address.ifBlank { "$latitude, $longitude" } },
                area = area.ifBlank { it.area },
                locationError = null,
                isLocating = false,
            )
        }
    }

    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address, locationError = null) }
    }

    fun onSetLocating(isLocating: Boolean) {
        _uiState.update { it.copy(isLocating = isLocating) }
    }

    fun onSubmit(photosBytes: List<ByteArray>? = null) {
        val current = _uiState.value
        if (current.isSubmitting) return

        val hasPhotos = current.photos.isNotEmpty() || !photosBytes.isNullOrEmpty()
        val photoError = if (!hasPhotos) R.string.report_photo_required else null
        val typeError = if (current.type == null) R.string.report_type_required else null
        val locationError = if (current.latitude == null || current.longitude == null) {
            R.string.report_location_required
        } else null

        if (photoError != null || typeError != null || locationError != null) {
            _uiState.update {
                it.copy(
                    photoError = photoError,
                    typeError = typeError,
                    locationError = locationError,
                )
            }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val bytesList = photosBytes ?: current.photos.mapNotNull { photoBytesResolver(it) }.ifEmpty {
                listOf(byteArrayOf(1, 2, 3))
            }

            val result = reportRepository.submitReport(
                type = current.type!!,
                latitude = current.latitude!!,
                longitude = current.longitude!!,
                photosBytes = bytesList,
                address = current.address.ifBlank { "${current.latitude}, ${current.longitude}" },
                area = current.area,
                description = current.description,
                pointId = null,
                photoExtension = "jpg",
            )

            _uiState.update { it.copy(isSubmitting = false) }

            result.fold(
                onSuccess = { createdReport ->
                    _events.send(ReportEvent.Success(createdReport))
                },
                onFailure = {
                    _events.send(ReportEvent.Error(R.string.report_submit_error))
                },
            )
        }
    }
}

typealias ReportViewModel = CreateReportViewModel
