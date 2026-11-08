package com.ecoruta.app.features.profile.data.remote.dto

// DTO serializable para la recepción de datos de perfil desde Supabase
// Mapea campos de nombres, celular, barrio, rol activo y saldo de EcoPuntos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("document_id") val documentId: String? = null,
    @SerialName("document_type") val documentType: String? = "CC",
    @SerialName("role_code") val roleCode: String? = null,
    @SerialName("active_role") val activeRole: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("must_change_password") val mustChangePassword: Boolean = false,
)

// Datos complementarios del ciudadano almacenados en la tabla citizens
@Serializable
data class CitizenDataDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("neighborhood") val neighborhood: String? = null,
    @SerialName("ecopoints_balance") val ecopointsBalance: Int = 0,
)

// Registro de rol asignado al usuario en la tabla user_roles
@Serializable
data class UserRoleItemDto(
    @SerialName("id") val id: String? = null,
    @SerialName("profile_id") val profileId: String? = null,
    @SerialName("role_code") val roleCode: String,
    @SerialName("created_at") val createdAt: String? = null,
)
