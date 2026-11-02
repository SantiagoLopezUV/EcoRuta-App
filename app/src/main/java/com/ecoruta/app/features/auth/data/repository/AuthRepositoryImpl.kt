package com.ecoruta.app.features.auth.data.repository

// Implementación concreta del repositorio de autenticación
// Conecta con el SDK de Supabase Auth para registro, login, tokens y logout

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
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    //se trae dese NetworkModule
    private val auth: Auth, // llamamos a la funcion provideSupabaseAuth()
    private val postgrest: Postgrest  // llamado la funcion provideSupabasePostgrest()
) : AuthRepository
{
    override suspend fun signUpWithEmail(
        email: String,
        password: String
    ): Result<UserAccount> = runCatching {
        // Ejecuta el registro en Supabase Auth con credenciales de correo
        val userInfo = auth.signUpWith(Email)
        {
            this.email = email
            this.password = password
        } ?:
        auth.currentUserOrNull() // si supabase no devuelve el usuario, salta y busca si ya se guardo en la sesión local con auth.currentUserOrNull()
          ?:
        error("No se pudo obtener la información de la cuenta tras el registro") // Si la sesión local tambien es null, salta y ejecuta error, con una excepción para que runCatching atrape el fallo limpiamente

        // Transforma la respuesta del SDK al modelo de dominio
        UserAccount(
            id = userInfo.id,
            email = userInfo.email ?: email,
            fullName = null, // el nombre inicia nulo porque en este paso solo se capturan credenciales y el nombre se completa desde el perfil
            isEmailConfirmed = userInfo.emailConfirmedAt != null,
            mustChangePassword = false
        )
    }

    // Inicia sesión validando el correo y la contraseña contra el servidor de supabase
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
        // Confirma que el usuario haya quedado registrado en la sesión local
        val userInfo = auth.currentUserOrNull()
            ?: error("No se encontró la sesión del usuario tras iniciar sesión")
        // retorna el modelo de dominio con los datos básicos de la cuenta
        UserAccount(
            id = userInfo.id,
            email = userInfo.email ?: email,
            fullName = null, // inicia vacío y se completa despues al consultar la tabla de perfiles
            isEmailConfirmed = userInfo.emailConfirmedAt != null,
            mustChangePassword = false
        )
    }

    // Autentica al usuario mediante el token id de Google en Supabase
    override suspend fun signInWithGoogle(
        idToken: String,
        rawNonce: String?
    ): Result<UserAccount> = authenticateWithGoogle(idToken, rawNonce)

    // Registra al usuario asociado
    override suspend fun signUpWithGoogle(
        idToken: String,
        rawNonce: String?
    ): Result<UserAccount> = authenticateWithGoogle(idToken, rawNonce)

    // Ejecuta la autenticación o registro asociado con Google utilizando el SDK de Supabase
    private suspend fun authenticateWithGoogle(
        idToken: String,
        rawNonce: String?
    ): Result<UserAccount> = runCatching {
        auth.signInWith(IDToken)
        {
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
        // notifica a Supabase para invalidar la sesión y borrar las credenciales locales
        auth.signOut()
    }

    // Consulta en memoria el usuario actual sin necesidad de suscribirse a un flujo continuo
    override suspend fun getCurrentUser(): UserAccount? {
        // Si no hay una sesión activa en el dispositivo, retorna nulo para el modo visitante
        val userInfo = auth.currentUserOrNull() ?: return null
        // Construye el modelo con los datos del usuario que están en memoria
        return UserAccount(
            id = userInfo.id,
            email = userInfo.email.orEmpty(),
            fullName = null,
            isEmailConfirmed = userInfo.emailConfirmedAt != null,
            mustChangePassword = false
        )
    }

    // escucha en tiempo real los cambios de sesión para actualizar las pantallas y el grafo de rutas
    override fun observeAuthState(): Flow<AuthSession>
    {
        // Convierte el estado interno de Supabase en los tres estados limpios de la aplicación
        // transforma el flujo de estado de sesión del SDK al modelo sellado de dominio
        return auth.sessionStatus.map { status ->
            when (status)
            {
                // cuando hay un usuario logueado con sesión válida y vigente
                is SessionStatus.Authenticated ->
                {
                    val user = status.session.user
                    if (user != null)
                    {
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
                is SessionStatus.Initializing -> AuthSession.Loading // La app está leyendo el almacenamiento interno para ver si había sesión guardada
                is SessionStatus.NotAuthenticated -> AuthSession.Unauthenticated  // El usuario cerró sesión voluntariamente o nunca ha iniciado sesión
                is SessionStatus.RefreshFailure -> AuthSession.Unauthenticated // El token venció y no se pudo renovar por falta de conexión o porque se acabo
            }
        }
    }

    // Solicita a Supabase el envío del correo de recuperación de contraseña
    override suspend fun sendPasswordResetEmail(
        email: String
    ): Result<Unit>  = runCatching{
        // dispara la peticion al servidor con el correo
        auth.resetPasswordForEmail(email = email)
    }

    override suspend fun updateFirstLoginPassword(
        newPassword: String
    ): Result<Unit> = runCatching {
        // obtener el id del usuario en la sesion actual
        val userId = auth.currentUserOrNull()
            ?.id
            ?: error("No existe una sesión activa para actualizar la contraseña")
        auth.updateUser {
            password = newPassword
        }
        // actualiza el registro del perfil en la BD para desactivar la bandera
        postgrest.from("profiles").update(
            {
                set("must_change_password", false)
            }
        ) {
            filter {
                eq("id",userId)
            }
        }
    }

    // Función privada para el registro en Supabase Auth y la inserción del perfil
    // abarca el registro de todos los perfiles
    private suspend fun registerProfile(
        email: String,
        password: String,
        fullName: String,
        documentNumber: String,
        roleCode: String,
        insertRoleData: suspend (userId: String) -> Unit
    ): Result<UserAccount> = runCatching {
        //Registra credenciales en Supabase Auth reutilizando la función signUpWithEmail
        val account = signUpWithEmail(email, password).getOrThrow()

        // Inserta el registro base en la tabla profiles
        postgrest.from("profiles")
            .insert(
            ProfileDto(
                id = account.id,
                documentId = documentNumber,
                roleCode = roleCode,
                fullName = fullName,
                email = email,
                mustChangePassword = false
            )
        )
        //Inserta los datos complementarios de cada rol
        insertRoleData(account.id)
        //Retorna la cuenta autenticada con su nombre completo
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
        postgrest.from("citizens")
            .insert(
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
        postgrest.from("recyclers")
            .insert(
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
        postgrest.from("businesses")
            .insert(
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

        postgrest.from("recyclers")
            .insert(
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
        // insertar el comercio a la tabla
        postgrest.from("businesses")
            .insert(
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
