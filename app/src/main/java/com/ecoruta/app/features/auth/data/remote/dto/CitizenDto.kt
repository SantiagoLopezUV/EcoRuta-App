package com.ecoruta.app.features.auth.data.remote.dto

// Objeto de transferencia de datos para la tabla citizens en PostgreSQL
// Transporta los campos complementarios del perfil ciudadano

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CitizenDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("neighborhood") val neighborhood: String? = null
)
