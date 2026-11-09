package com.ecoruta.app.features.bulky_waste.domain.model

// Modelo de dominio para gestores autorizados y canales de orientación de escombros RCD
// Presenta información de contacto, horarios y tipos de materiales que recibe cada gestor

import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType

data class RcdManagerOrientation(
    val id: String,
    val name: String,
    val phone: String?,
    val address: String?,
    val operatingHours: String?,
    val description: String?,
    val acceptedTypes: List<DebrisType> = emptyList(),
)
