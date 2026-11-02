package com.ecoruta.app.features.auth.data.repository

// Implementación concreta del repositorio de autenticación
// Conecta con el SDK de Supabase Auth para registro, login, tokens y logout

import com.ecoruta.app.features.auth.domain.model.AuthSession
import com.ecoruta.app.features.auth.domain.model.UserAccount
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth //se trae dese NetworkModule
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

}