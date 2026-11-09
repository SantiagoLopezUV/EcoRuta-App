package com.ecoruta.app.features.auth.data.repository

// Implementación concreta del repositorio de autenticación
// Conecta con el SDK de Supabase Auth, Storage y PostgREST para sesiones, perfiles y credenciales

import com.ecoruta.app.features.auth.data.remote.dto.BusinessDto
import com.ecoruta.app.features.auth.data.remote.dto.CitizenDto
import com.ecoruta.app.features.auth.data.remote.dto.ProfileDto
import com.ecoruta.app.features.auth.data.remote.dto.RecyclerDto
import com.ecoruta.app.features.auth.domain.model.AuthSession
import com.ecoruta.app.features.auth.domain.model.BusinessRegistration
import com.ecoruta.app.features.auth.domain.model.CitizenRegistration
import com.ecoruta.app.features.auth.domain.model.RecyclerRegistration
import com.ecoruta.app.features.auth.domain.model.UserAccount
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val storage: Storage? = null
) : AuthRepository {

    // Registra las credenciales en Supabase Auth y retorna la cuenta inicial creada
    override suspend fun signUpWithEmail(
        email: String,
        password: String
    ): Result<UserAccount> = runCatching {
        val userInfo = auth.signUpWith(Email) {
            this.email = email
            this.password = password
        } ?: auth.currentUserOrNull()
          ?: error("No se pudo obtener la información de la cuenta tras el registro")

        UserAccount(
            id = userInfo.id,
            email = userInfo.email ?: email,
            fullName = null,
            isEmailConfirmed = userInfo.emailConfirmedAt != null,
            mustChangePassword = false
        )
    }

    // Inicia sesión validando credenciales y consultando la bandera de contraseña temporal del perfil
    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<UserAccount> = runCatching {
        // auntentica las credenciales del usuario con supabase auth
        auth.signInWith(Email)
        {
            this.email = email
            this.password = password
        }
        val userInfo = auth.currentUserOrNull()
            ?: error("No se encontró la sesión del usuario tras iniciar sesión")

        // Consulta en la tabla profiles la bandera de cambio obligatorio de clave y el nombre
        val profile = runCatching {
            postgrest.from("profiles")
                .select { filter { eq("id", userInfo.id) } }
                .decodeSingleOrNull<ProfileDto>()
        }.getOrNull()

        UserAccount(
            id = userInfo.id,
            email = userInfo.email ?: email,
            fullName = profile?.fullName,
            isEmailConfirmed = userInfo.emailConfirmedAt != null,
            mustChangePassword = profile?.mustChangePassword ?: false
        )
    }

    // Autentica al usuario mediante el token id de Google en Supabase
    override suspend fun signInWithGoogle(
        idToken: String,
        rawNonce: String?
    ): Result<UserAccount> = authenticateWithGoogle(idToken, rawNonce)

    // Registra al usuario asociado mediante credenciales de Google
    override suspend fun signUpWithGoogle(
        idToken: String,
        rawNonce: String?
    ): Result<UserAccount> = authenticateWithGoogle(idToken, rawNonce)

    // Ejecuta la autenticación o registro asociado con Google utilizando el SDK de Supabase
    private suspend fun authenticateWithGoogle(
        idToken: String,
        rawNonce: String?
    ): Result<UserAccount> = runCatching {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
            this.nonce = rawNonce
        }
        val userInfo = auth.currentUserOrNull()
            ?: error("No se encontró la sesión del usuario tras autenticar con Google")
        val fullName = userInfo.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
            ?: userInfo.userMetadata?.get("name")?.jsonPrimitive?.contentOrNull
        UserAccount(
            id = userInfo.id,
            email = userInfo.email.orEmpty(),
            fullName = fullName,
            isEmailConfirmed = userInfo.emailConfirmedAt != null,
            mustChangePassword = false
        )
    }

    // Cierra la sesión activa en el servidor y limpia los tokens guardados en el teléfono
    override suspend fun signOut(): Result<Unit> = runCatching {
        auth.signOut()
    }

    // Consulta en memoria el usuario actual y su perfil para determinar si está en modo visitante
    override suspend fun getCurrentUser(): UserAccount? {
        val userInfo = auth.currentUserOrNull() ?: return null
        val profile = runCatching {
            postgrest.from("profiles")
                .select { filter { eq("id", userInfo.id) } }
                .decodeSingleOrNull<ProfileDto>()
        }.getOrNull()
        return UserAccount(
            id = userInfo.id,
            email = userInfo.email.orEmpty(),
            fullName = profile?.fullName,
            isEmailConfirmed = userInfo.emailConfirmedAt != null,
            mustChangePassword = profile?.mustChangePassword ?: false
        )
    }

    // Escucha en tiempo real los cambios de sesión para actualizar las pantallas y el grafo de rutas
    override fun observeAuthState(): Flow<AuthSession> {
        return auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    if (user != null) {
                        AuthSession.Authenticated(
                            user = UserAccount(
                                id = user.id,
                                email = user.email.orEmpty(),
                                fullName = null,
                                isEmailConfirmed = user.emailConfirmedAt != null,
                                mustChangePassword = false
                            )
                        )
                    } else {
                        AuthSession.Loading
                    }
                }
                is SessionStatus.Initializing -> AuthSession.Loading
                is SessionStatus.NotAuthenticated -> AuthSession.Unauthenticated
                is SessionStatus.RefreshFailure -> AuthSession.Unauthenticated
            }
        }
    }

    // Solicita a Supabase el envío del correo de recuperación de contraseña con código OTP
    override suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit> = runCatching {
        auth.resetPasswordForEmail(email = email)
    }

    // Verifica el código OTP de 6 dígitos enviado por correo para la recuperación de contraseña
    override suspend fun verifyPasswordResetCode(
        email: String,
        code: String
    ): Result<Unit> = runCatching {
        auth.verifyEmailOtp(
            type = OtpType.Email.RECOVERY,
            email = email,
            token = code
        )
    }

    // Actualiza la contraseña del usuario en el servidor tras la verificación del código de recuperación
    override suspend fun updatePassword(
        newPassword: String
    ): Result<Unit> = runCatching {
        auth.updateUser {
            password = newPassword
        }
    }

    // Actualiza la contraseña temporal en el primer inicio de sesión y desactiva la bandera en perfiles
    override suspend fun updateFirstLoginPassword(
        newPassword: String
    ): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para actualizar la contraseña")
        auth.updateUser {
            password = newPassword
        }
        postgrest.from("profiles").update(
            mapOf("must_change_password" to false)
        ) {
            filter {
                eq("id", userId)
            }
        }
    }

    // Envía un enlace mágico o código de verificación al correo para registro o acceso sin contraseña inicial
    override suspend fun signUpWithEmailLink(
        email: String
    ): Result<Unit> = runCatching {
        auth.signInWith(OTP) {
            this.email = email
            this.createUser = true
        }
    }

    // Sube un documento de soporte (cédula de reciclador o RUT de comercio) al almacenamiento institucional
    override suspend fun uploadRegistrationDocument(
        fileName: String,
        bytes: ByteArray
    ): Result<String> = runCatching {
        val s = storage ?: error("El servicio de almacenamiento no está configurado")
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para subir el documento de registro")
        val safeName = fileName.replace(" ", "_")
        val path = "$userId/$safeName"
        val bucket = s.from("institutional-docs")
        bucket.upload(path, bytes) {
            upsert = true
        }
        bucket.publicUrl(path)
    }

    // Función privada para el registro en Supabase Auth y la inserción del perfil base
    private suspend fun registerProfile(
        email: String,
        password: String,
        fullName: String,
        documentNumber: String,
        roleCode: String,
        insertRoleData: suspend (userId: String) -> Unit
    ): Result<UserAccount> = runCatching {
        val account = signUpWithEmail(email, password).getOrThrow()

        postgrest.from("profiles").insert(
            ProfileDto(
                id = account.id,
                documentId = documentNumber,
                roleCode = roleCode,
                fullName = fullName,
                email = email,
                mustChangePassword = false
            )
        )
        insertRoleData(account.id)
        account.copy(fullName = fullName)
    }

    // Registro de ciudadano creando cuenta en Supabase Auth, perfil y fila en tabla citizens
    override suspend fun signUpCitizen(
        registration: CitizenRegistration
    ): Result<UserAccount> = registerProfile(
        email = registration.email,
        password = registration.password,
        fullName = registration.fullName,
        documentNumber = registration.documentNumber,
        roleCode = "citizen"
    ) { userId ->
        postgrest.from("citizens").insert(
            CitizenDto(
                profileId = userId,
                phone = registration.phone,
                neighborhood = registration.neighborhood
            )
        )
    }

    // Registra a un reciclador de oficio con perfil y estado inicial pendiente de aprobación
    override suspend fun signUpRecycler(
        registration: RecyclerRegistration
    ): Result<UserAccount> = registerProfile(
        email = registration.email,
        password = registration.password,
        fullName = registration.fullName,
        documentNumber = registration.documentNumber,
        roleCode = "recycler"
    ) { userId ->
        postgrest.from("recyclers").insert(
            RecyclerDto(
                profileId = userId,
                phone = registration.phone,
                associationName = registration.associationName,
                companyId = registration.companyId,
                idDocumentPdfUrl = registration.idDocumentPdfUrl,
                status = "pendiente"
            )
        )
    }

    // Completa o actualiza los datos del reciclador en estado pendiente, modalidad, empresa y soporte
    override suspend fun completeRecyclerRegistration(
        phone: String?,
        associationName: String?,
        companyId: String?,
        idDocumentPdfUrl: String?
    ): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para completar el registro de reciclador")

        postgrest.from("profiles").update(
            mapOf("role_code" to "recycler")
        ) {
            filter { eq("id", userId) }
        }

        postgrest.from("recyclers").upsert(
            RecyclerDto(
                profileId = userId,
                phone = phone,
                associationName = associationName,
                companyId = companyId,
                idDocumentPdfUrl = idDocumentPdfUrl,
                status = "pendiente"
            )
        )
    }

    // Registra a un comercio aliado con perfil y estado inicial pendiente de verificación
    override suspend fun signUpBusiness(
        registration: BusinessRegistration
    ): Result<UserAccount> = registerProfile(
        email = registration.email,
        password = registration.password,
        fullName = registration.fullName,
        documentNumber = registration.documentNumber,
        roleCode = "business"
    ) { userId ->
        postgrest.from("businesses").insert(
            BusinessDto(
                profileId = userId,
                businessName = registration.businessName,
                category = registration.category,
                address = registration.address,
                rutPdfUrl = registration.rutPdfUrl,
                status = "pendiente"
            )
        )
    }

    // Registra la postulación al rol de reciclador para un usuario autenticado con estado pendiente
    override suspend fun submitRecyclerRoleRequest(
        phone: String,
        associationName: String?,
        companyId: String?,
        idDocumentPdfUrl: String?
    ): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para radicar la postulación")

        postgrest.from("recyclers").upsert(
            RecyclerDto(
                profileId = userId,
                phone = phone,
                associationName = associationName,
                companyId = companyId,
                idDocumentPdfUrl = idDocumentPdfUrl,
                status = "pendiente"
            )
        )
    }

    // Registra la postulación al rol de comercio aliado para un usuario autenticado con estado pendiente
    override suspend fun submitBusinessRoleRequest(
        businessName: String,
        category: String,
        address: String,
        rutPdfUrl: String?
    ): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para radicar la postulación")

        postgrest.from("profiles").update(
            mapOf("role_code" to "business")
        ) {
            filter { eq("id", userId) }
        }

        postgrest.from("businesses").upsert(
            BusinessDto(
                profileId = userId,
                businessName = businessName,
                category = category,
                address = address,
                rutPdfUrl = rutPdfUrl,
                status = "pendiente"
            )
        )
    }
}
