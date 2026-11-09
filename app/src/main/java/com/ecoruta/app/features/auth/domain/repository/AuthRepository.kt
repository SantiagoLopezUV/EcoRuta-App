package com.ecoruta.app.features.auth.domain.repository

// Contrato de dominio para operaciones de autenticación
// Define métodos abstractos de inicio de sesión, registro, recuperación y gestión de credenciales

import com.ecoruta.app.features.auth.domain.model.AuthSession
import com.ecoruta.app.features.auth.domain.model.BusinessRegistration
import com.ecoruta.app.features.auth.domain.model.CitizenRegistration
import com.ecoruta.app.features.auth.domain.model.RecyclerRegistration
import com.ecoruta.app.features.auth.domain.model.UserAccount
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    // Registra un nuevo ciudadano con correo y contraseña
    suspend fun signUpWithEmail(email: String, password: String): Result<UserAccount>

    // Inicio de sesión de un usuario registrado con correo y contraseña
    suspend fun signInWithEmail(email: String, password: String): Result<UserAccount>

    // Autentica un usuario mediante Google OAuth con el ID Token obtenido del dispositivo
    suspend fun signInWithGoogle(idToken: String, rawNonce: String? = null): Result<UserAccount>

    // Registra a un nuevo usuario mediante Google OAuth utilizando el ID Token del proveedor
    suspend fun signUpWithGoogle(idToken: String, rawNonce: String? = null): Result<UserAccount>

    // Cierra la sesión activa y elimina las credenciales del dispositivo
    suspend fun signOut(): Result<Unit>

    // Obtiene el usuario autenticado actual o nulo si está en modo visitante
    suspend fun getCurrentUser(): UserAccount?

    // Emite los cambios en el estado de autenticación
    fun observeAuthState(): Flow<AuthSession>

    // Envía un correo electrónico con el enlace para restablecer la contraseña
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    // Verifica el código OTP de 6 dígitos enviado por correo para la recuperación de contraseña
    suspend fun verifyPasswordResetCode(email: String, code: String): Result<Unit>

    // Actualiza la contraseña del usuario en el servidor tras la verificación del código de recuperación
    suspend fun updatePassword(newPassword: String): Result<Unit>

    // Actualiza la contraseña en el primer inicio de sesión y desactiva la bandera
    suspend fun updateFirstLoginPassword(newPassword: String): Result<Unit>

    // Envía un enlace mágico o código de verificación al correo para registro o acceso sin contraseña inicial
    suspend fun signUpWithEmailLink(email: String): Result<Unit>

    // Sube un documento de soporte (cédula de reciclador o RUT de comercio) al almacenamiento institucional
    suspend fun uploadRegistrationDocument(fileName: String, bytes: ByteArray): Result<String>

    // Registro de ciudadano creando cuenta en autenticación, perfil y entidad ciudadana
    suspend fun signUpCitizen(registration: CitizenRegistration): Result<UserAccount>

    // Registra a un reciclador de oficio con perfil y estado inicial pendiente de aprobación
    suspend fun signUpRecycler(registration: RecyclerRegistration): Result<UserAccount>

    // Completa o actualiza los datos del reciclador en estado pendiente (modalidad, empresa y soporte)
    suspend fun completeRecyclerRegistration(
        phone: String? = null,
        associationName: String? = null,
        companyId: String? = null,
        idDocumentPdfUrl: String? = null
    ): Result<Unit>

    // Registra a un comercio aliado con perfil y estado inicial pendiente de verificación
    suspend fun signUpBusiness(registration: BusinessRegistration): Result<UserAccount>

    // Registra la postulación al rol de reciclador para un usuario autenticado con estado pendiente
    suspend fun submitRecyclerRoleRequest(
        phone: String,
        associationName: String? = null,
        companyId: String? = null,
        idDocumentPdfUrl: String? = null
    ): Result<Unit>

    // Registra la postulación al rol de comercio aliado para un usuario autenticado con estado pendiente
    suspend fun submitBusinessRoleRequest(
        businessName: String,
        category: String,
        address: String,
        rutPdfUrl: String? = null
    ): Result<Unit>
}
