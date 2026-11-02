package com.ecoruta.app.features.auth.data.remote.dto

// Objeto de transferencia de datos para la tabla profiles en PostgreSQL
// Transporta los atributos de identidad base del usuario registrado

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("document_id") val documentId: String? = null,
    @SerialName("role_code") val roleCode: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("email") val email: String,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false
)
