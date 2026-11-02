package com.ecoruta.app.features.auth.domain.model

// Modelo de dominio para la captura de datos en el registro del ciudadano
// Incluye credenciales básicas, documento de identidad, número telefónico y barrio

data class CitizenRegistration(
    val email: String,
    val password: String,
    val fullName: String,
    val documentNumber: String,
    val phone: String,
    val neighborhood: String
)
