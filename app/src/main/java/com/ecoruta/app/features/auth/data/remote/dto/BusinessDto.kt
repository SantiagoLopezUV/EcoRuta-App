package com.ecoruta.app.features.auth.data.remote.dto

// Objeto de transferencia de datos para la tabla businesses en PostgreSQL
// Transporta los atributos del establecimiento y estado inicial pendiente de verificación

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusinessDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("business_name") val businessName: String,
    @SerialName("category") val category: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("rut_pdf_url") val rutPdfUrl: String? = null,
    @SerialName("status") val status: String = "pendiente"
)
