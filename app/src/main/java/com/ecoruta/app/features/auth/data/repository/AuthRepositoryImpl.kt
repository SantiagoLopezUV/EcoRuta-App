package com.ecoruta.app.features.auth.data.repository


// Implementación concreta del repositorio de autenticación
// Conecta con el SDK de Supabase Auth para registro, login, tokens y logout

import com.ecoruta.app.features.auth.domain.model.UserAccount
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
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
        } ?:  // si supabase no devuelve el usuario, salta y busca si ya se guardo en la sesión local con auth.currentUserOrNull()
        auth.currentUserOrNull()
          ?: // Si la sesión local tambien es null, salta y ejecuta error, con una excepción para que runCatching atrape el fallo limpiamente
          error("No se pudo obtener la información de la cuenta tras el registro")

        // Transforma la respuesta del SDK al modelo de dominio
        UserAccount(
            id = userInfo.id,
            email = userInfo.email ?: email,
            fullName = null, // el nombre inicia nulo porque en este paso solo se capturan credenciales y el nombre se completa desde el perfil
            isEmailConfirmed = userInfo.emailConfirmedAt != null,
            mustChangePassword = false
        )
    }
}