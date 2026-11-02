package com.ecoruta.app.features.auth.domain.model

// Modelo de dominio para el registro de comercios aliados
// Contiene credenciales del titular, nombre comercial, categoría, dirección y RUT de soporte

data class BusinessRegistration(
    val email: String,
    val password: String,
    val fullName: String,
    val documentNumber: String,
    val businessName: String,
    val category: String,
    val address: String,
    val rutPdfUrl: String? = null
)
