package com.ecoruta.app.features.bulky_waste.domain.model

// Modelo de dominio puro para solicitudes de retiro de escombros y voluminosos
// Captura tipología (ladrillo, muebles, colchones), coordenadas, estado y cotización

import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisRequest
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType

data class BulkyWasteRequest(
    val id: String,
    val citizenId: String,
    val type: DebrisType,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val scheduledDateMillis: Long? = null,
    val status: DebrisStatus = DebrisStatus.PENDIENTE,
    val quotedCop: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val companyId: String? = null,
    val driverName: String? = null,
    val statusNote: String? = null,
) {
    // Solo una solicitud pendiente se puede cancelar por el ciudadano
    val canCancel: Boolean
        get() = status == DebrisStatus.PENDIENTE

    // Convierte el modelo de dominio puro a la representación visual para la pantalla
    fun toUiRequest(): DebrisRequest = DebrisRequest(
        id = id,
        type = type,
        description = description,
        address = address.ifEmpty { "Lat: $latitude, Lng: $longitude" },
        status = status,
        createdAtMillis = createdAtMillis,
        preferredDateMillis = scheduledDateMillis ?: createdAtMillis,
        scheduledDateMillis = scheduledDateMillis,
        quotedCop = quotedCop,
        driverName = driverName,
        statusNote = statusNote,
    )
}
