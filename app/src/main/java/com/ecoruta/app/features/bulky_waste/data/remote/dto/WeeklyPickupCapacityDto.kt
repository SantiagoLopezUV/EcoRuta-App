package com.ecoruta.app.features.bulky_waste.data.remote.dto

// DTO para la tabla weekly_pickup_capacities en Supabase
// Modela la capacidad operativa máxima de recolección de escombros por día de la semana

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyPickupCapacityDto(
    @SerialName("id") val id: String? = null,
    @SerialName("company_id") val companyId: String? = null,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("max_slots") val maxSlots: Int = 10,
)
