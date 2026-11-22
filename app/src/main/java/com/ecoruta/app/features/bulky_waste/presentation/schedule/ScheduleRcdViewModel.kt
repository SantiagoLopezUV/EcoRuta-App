package com.ecoruta.app.features.bulky_waste.presentation.schedule

// ViewModel para el agendamiento digital de escombros y voluminosos (RCD)
// Valida disponibilidad en calendario, tipo de residuo, descripción, dirección y gestiona cancelaciones

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.bulky_waste.domain.repository.BulkyWasteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// Eventos únicos de agendamiento y cancelación de escombros
sealed interface ScheduleRcdEvent {
    data class SubmitSuccess(val request: DebrisRequest) : ScheduleRcdEvent
    data class SubmitError(@param:StringRes val messageRes: Int) : ScheduleRcdEvent
    data class CancelSuccess(val requestId: String) : ScheduleRcdEvent
    data class CancelError(@param:StringRes val messageRes: Int) : ScheduleRcdEvent
}

@HiltViewModel
class ScheduleRcdViewModel @Inject constructor(
    private val bulkyWasteRepository: BulkyWasteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebrisRequestUiState())
    val uiState: StateFlow<DebrisRequestUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ScheduleRcdEvent>()
    val events: SharedFlow<ScheduleRcdEvent> = _events.asSharedFlow()

    // Coordenadas geográficas por defecto en el casco urbano de Tuluá (Valle del Cauca)
    private var latitude: Double = DEFAULT_TULUA_LAT
    private var longitude: Double = DEFAULT_TULUA_LNG

    init {
        initCalendarSlots()
    }

    // Inicializa el rango de fechas disponibles en el calendario (lunes a sábado, excluyendo domingos)
    fun initCalendarSlots() {
        val baseDay = DayMillis.startOfDay(System.currentTimeMillis())
        val firstSelectableDay = DayMillis.addDays(baseDay, 1)
        val lastSelectableDay = DayMillis.addDays(firstSelectableDay, DebrisRequestUiState.SELECTABLE_DAYS)
        val weekStart = DayMillis.mondayOf(firstSelectableDay)

        val available = mutableSetOf<Long>()
        val calendar = Calendar.getInstance()
        for (i in 0 until DebrisRequestUiState.SELECTABLE_DAYS) {
            val day = DayMillis.addDays(firstSelectableDay, i)
            calendar.timeInMillis = day
            if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                available.add(day)
            }
        }

        _uiState.update { current ->
            current.copy(
                firstSelectableDay = firstSelectableDay,
                lastSelectableDay = lastSelectableDay,
                weekStart = weekStart,
                availableDays = available,
                isLoadingDays = false,
                daysError = null,
            )
        }
    }

    // Actualiza la tipología de residuo seleccionada (muebles, colchones, escombros)
    fun onTypeSelected(type: DebrisType) {
        _uiState.update { it.copy(type = type, typeError = null) }
    }

    // Actualiza la descripción del residuo controlando longitud máxima
    fun onDescriptionChange(description: String) {
        val trimmed = if (description.length > DebrisRequestUiState.MAX_DESCRIPTION) {
            description.take(DebrisRequestUiState.MAX_DESCRIPTION)
        } else {
            description
        }
        _uiState.update { it.copy(description = trimmed, descriptionError = null) }
    }

    // Actualiza la dirección física de recogida
    fun onAddressChange(address: String) {
        _uiState.update { it.copy(address = address, addressError = null) }
    }

    // Actualiza las coordenadas geográficas del punto de retiro
    fun onLocationUpdated(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

    // Navega a la semana anterior en el selector de fechas
    fun onPreviousWeek() {
        if (_uiState.value.canGoToPreviousWeek) {
            _uiState.update { it.copy(weekStart = DayMillis.addDays(it.weekStart, -7)) }
        }
    }

    // Navega a la semana siguiente en el selector de fechas
    fun onNextWeek() {
        if (_uiState.value.canGoToNextWeek) {
            _uiState.update { it.copy(weekStart = DayMillis.addDays(it.weekStart, 7)) }
        }
    }

    // Selecciona un día para la recolección
    fun onDaySelected(dayMillis: Long) {
        if (dayMillis in _uiState.value.availableDays) {
            _uiState.update { it.copy(selectedDay = dayMillis, dateError = null) }
        }
    }

    // Reintenta la consulta de cupos disponibles
    fun onRetryDays() {
        initCalendarSlots()
    }

    // Valida el primer paso del asistente (tipo, descripción mínima y dirección)
    fun validateStep1(): Boolean {
        val state = _uiState.value
        var isValid = true

        val typeError = if (state.type == null) {
            isValid = false
            R.string.debris_type_required
        } else null

        val descriptionTrimmed = state.description.trim()
        val descriptionError = if (descriptionTrimmed.length < DebrisRequestUiState.MIN_DESCRIPTION) {
            isValid = false
            R.string.debris_description_required
        } else null

        val addressTrimmed = state.address.trim()
        val addressError = if (addressTrimmed.isEmpty()) {
            isValid = false
            R.string.debris_address_required
        } else null

        _uiState.update {
            it.copy(
                typeError = typeError,
                descriptionError = descriptionError,
                addressError = addressError,
            )
        }
        return isValid
    }

    // Valida la selección de fecha preferida con disponibilidad
    fun validateSchedule(): Boolean {
        val state = _uiState.value
        val isDateValid = state.selectedDay != null && state.selectedDay in state.availableDays
        val dateError = if (!isDateValid) R.string.debris_date_required else null

        _uiState.update { it.copy(dateError = dateError) }
        return isDateValid
    }

    // Envía la solicitud al backend o repositorio en memoria
    fun onSubmit(): Boolean {
        val step1Valid = validateStep1()
        val scheduleValid = validateSchedule()
        if (!step1Valid || !scheduleValid) {
            return false
        }

        val state = _uiState.value
        val type = state.type ?: return false
        val description = state.description.trim()
        val address = state.address.trim()
        val scheduledDateMillis = state.selectedDay

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            bulkyWasteRepository.createRequest(
                type = type,
                description = description,
                latitude = latitude,
                longitude = longitude,
                address = address,
                scheduledDateMillis = scheduledDateMillis,
            ).fold(
                onSuccess = { created ->
                    val uiRequest = created.toUiRequest().copy(address = address)
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitted = uiRequest,
                        )
                    }
                    _events.emit(ScheduleRcdEvent.SubmitSuccess(uiRequest))
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            dateError = R.string.debris_action_error,
                        )
                    }
                    _events.emit(ScheduleRcdEvent.SubmitError(R.string.debris_action_error))
                },
            )
        }
        return true
    }

    // Cancela una solicitud en estado pendiente
    fun cancelRequest(requestId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            bulkyWasteRepository.cancelRequest(requestId).fold(
                onSuccess = {
                    _events.emit(ScheduleRcdEvent.CancelSuccess(requestId))
                    onResult(true)
                },
                onFailure = {
                    _events.emit(ScheduleRcdEvent.CancelError(R.string.debris_action_error))
                    onResult(false)
                },
            )
        }
    }

    // Reinicia el formulario para un nuevo agendamiento
    fun resetForm() {
        _uiState.update {
            DebrisRequestUiState(
                firstSelectableDay = it.firstSelectableDay,
                lastSelectableDay = it.lastSelectableDay,
                weekStart = it.weekStart,
                availableDays = it.availableDays,
            )
        }
    }

    companion object {
        const val DEFAULT_TULUA_LAT = 4.0847
        const val DEFAULT_TULUA_LNG = -76.1953
    }
}

// Alias de compatibilidad arquitectónica para cumplir con la especificación de Linear ECO-1004
typealias BulkyRequestViewModel = ScheduleRcdViewModel
