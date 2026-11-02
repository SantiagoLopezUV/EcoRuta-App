package com.ecoruta.app.features.auth.domain.model

// Modelo de dominio para el registro de recicladores de oficio
// Agrupa credenciales, datos personales, asociación o vinculación y enlace al documento de soporte

data class RecyclerRegistration(
    val email: String,
    val password: String,
    val fullName: String,
    val documentNumber: String,
    val phone: String,
    val associationName: String? = null,
    val companyId: String? = null,
    val idDocumentPdfUrl: String? = null
)
