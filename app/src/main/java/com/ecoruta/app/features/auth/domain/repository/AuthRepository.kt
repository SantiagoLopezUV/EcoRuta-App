package com.ecoruta.app.features.auth.domain.repository

import com.ecoruta.app.features.auth.domain.model.UserAccount

// Contrato de dominio para operaciones de autenticación
// Define métodos abstractos de inicio de sesión, registro y gestión de credenciales

interface AuthRepository {
    suspend fun signUpWithEmail(email: String, password: String): Result<UserAccount>
}