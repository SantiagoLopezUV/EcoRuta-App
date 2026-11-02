package com.ecoruta.app.features.auth.domain.model

// Modelo de dominio puro para datos de la cuenta de usuario
// Identificador, correo electrónico, metadatos y estado de confirmación

// Representa al usuario autenticado sin depender del sdk de Supabase
// El rol, el avatar y los puntos se manejan en el módulo de perfil
data class UserAccount(
    val id: String,                            // Identificador único de la cuenta, es el mismo que usa la tabla de perfiles
    val email: String,                         // Correo con el que se registró el usuario
    val fullName: String? = null,              // Nombre completo, puede venir vacío cuando el registro llega desde Google
    val isEmailConfirmed: Boolean = false,     // Indica si el usuario ya confirmó su correo electrónico
    val mustChangePassword: Boolean = false    // Obliga a cambiar la clave temporal la primera vez que el usuario inicia sesión
)

