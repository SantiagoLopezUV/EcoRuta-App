package com.ecoruta.app.features.auth.data.remote.dto

// Objeto de transferencia de datos para la tabla recyclers en PostgreSQL
// Transporta los datos de contacto y estado inicial pendiente de aprobación

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecyclerDto(
    @SerialName("profile_id") val profileId: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("association_name") val associationName: String? = null,
    @SerialName("company_id") val companyId: String? = null,
    @SerialName("id_document_pdf_url") val idDocumentPdfUrl: String? = null,
    @SerialName("status") val status: String = "pendiente"
)
