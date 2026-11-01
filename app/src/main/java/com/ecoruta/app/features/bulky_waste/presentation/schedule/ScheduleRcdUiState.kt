package com.ecoruta.app.features.bulky_waste.presentation.schedule

// Estado inmutable de la solicitud de escombros
// Modela residuo elegido, dirección, fecha seleccionada, estado y errores

import androidx.annotation.StringRes
import java.util.Calendar
import java.util.Locale

enum class DebrisType {
    FURNITURE,
    MATTRESSES,
    RENOVATION_DEBRIS,
}

enum class DebrisStatus {
    PENDIENTE,
    COTIZADO,
    PROGRAMADO,
    RECOLECTADO,
    CANCELADO,
}

data class DebrisRequest(
    val id: String?,
    val type: DebrisType,
    val description: String,
    val address: String,
    val status: DebrisStatus,
    val createdAtMillis: Long,
    val preferredDateMillis: Long,
    val scheduledDateMillis: Long? = null,
    val quotedCop: Long? = null,
    val costCop: Long? = quotedCop,
    val driverName: String? = null,
    val statusNote: String? = null,
)

// Funciones auxiliares para calcular el inicio del día y desplazamientos semanales en el calendario
object DayMillis {
    private val Colombia: Locale = Locale.forLanguageTag("es-CO")

    fun startOfDay(millis: Long): Long = calendarAt(millis).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addDays(dayStartMillis: Long, days: Int): Long =
        calendarAt(dayStartMillis).apply { add(Calendar.DAY_OF_MONTH, days) }.timeInMillis

    fun mondayOf(dayStartMillis: Long): Long = calendarAt(dayStartMillis).apply {
        val offset = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_MONTH, -offset)
    }.timeInMillis

    fun isSameMonth(firstMillis: Long, secondMillis: Long): Boolean {
        val a = calendarAt(firstMillis)
        val b = calendarAt(secondMillis)
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
    }

    private fun calendarAt(millis: Long): Calendar = Calendar.getInstance(Colombia).apply { timeInMillis = millis }
}

data class DebrisRequestUiState(
    val type: DebrisType? = null,
    @param:StringRes val typeError: Int? = null,
    val description: String = "",
    @param:StringRes val descriptionError: Int? = null,
    val address: String = "",
    @param:StringRes val addressError: Int? = null,
    val firstSelectableDay: Long = 0L,
    val lastSelectableDay: Long = 0L,
    val weekStart: Long = 0L,
    val availableDays: Set<Long> = emptySet(),
    val isLoadingDays: Boolean = false,
    @param:StringRes val daysError: Int? = null,
    val daysAreSample: Boolean = false,
    val selectedDay: Long? = null,
    @param:StringRes val dateError: Int? = null,
    val isSubmitting: Boolean = false,
    val submitted: DebrisRequest? = null,
    val sentWithoutBackend: Boolean = false,
) {
    val canGoToPreviousWeek: Boolean get() = weekStart > DayMillis.mondayOf(firstSelectableDay)
    val canGoToNextWeek: Boolean get() = DayMillis.addDays(weekStart, 7) <= lastSelectableDay

    companion object {
        const val MIN_DESCRIPTION = 10
        const val MAX_DESCRIPTION = 280
        const val SELECTABLE_DAYS = 28
    }
}

typealias ScheduleRcdUiState = DebrisRequestUiState
