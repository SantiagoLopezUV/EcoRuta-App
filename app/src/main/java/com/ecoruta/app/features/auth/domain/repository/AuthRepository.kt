package com.ecoruta.app.features.auth.domain.repository

import com.ecoruta.app.features.auth.domain.model.AuthSession
import com.ecoruta.app.features.auth.domain.model.UserAccount
import kotlinx.coroutines.flow.Flow

// Contrato de dominio para operaciones de autenticación
// Define métodos abstractos de inicio de sesión, registro y gestión de credenciales

// Retorna Result<Unit> para encapsular éxito o fallo sin transferir datos, ya que es una acción de actualización
// unit representa la ausencia de un valor de retorno (void)
//

interface AuthRepository {
    // Registra un nuevo ciudadano con correo y contraseña
    suspend fun signUpWithEmail(email: String, password: String): Result<UserAccount>
    // Inicio de sesión de un usuario registrado con correo y contraseña
    suspend fun signInWithEmail(email: String, password: String): Result<UserAccount>
    // Cierra la sesión activa y elimina las credenciales del dispositivo

    suspend fun signOut(): Result<Unit>

    // Obtiene el usuario autenticado actual o nulo si está en modo visitante
    suspend fun getCurrentUser(): UserAccount?
    // Emite los cambios en el estado de autenticación
    fun observeAuthState(): Flow<AuthSession>
    // envía un correo electronico con el enlace para reestablecer la contraseña
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    // actualiza la contraseña en el primer inicio de sesion y desacativa la bandera
    suspend fun updateFirstLoginPassword(newPassword: String): Result<Unit>

}