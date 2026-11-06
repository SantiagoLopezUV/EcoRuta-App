package com.ecoruta.app.features.profile.data.remote.dto

// DTO serializable para el envío de postulación a nuevos roles
// Transporta documentos (RUT, cédula PDF) y datos para rol reciclador o comercio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Datos para radicar la postulación al rol de reciclador en la tabla recyclers
@Serializable
data class RecyclerApplicationDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("association_name") val associationName: String? = null,
    @SerialName("company_id") val companyId: String? = null,
    @SerialName("id_document_pdf_url") val idDocumentPdfUrl: String? = null,
    @SerialName("status") val status: String = "pendiente",
)

// Datos para radicar la postulación de un comercio aliado en la tabla businesses
@Serializable
data class BusinessApplicationDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("business_name") val businessName: String,
    @SerialName("category") val category: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("rut_pdf_url") val rutPdfUrl: String? = null,
    @SerialName("status") val status: String = "pendiente",
)

// Registro simplificado para verificar si un usuario tiene una postulación en estado pendiente
@Serializable
data class PendingRoleItemDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("status") val status: String,
)
