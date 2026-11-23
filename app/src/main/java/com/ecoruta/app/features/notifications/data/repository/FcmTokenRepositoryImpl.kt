package com.ecoruta.app.features.notifications.data.repository

import com.ecoruta.app.features.notifications.data.remote.dto.FcmTokenDto
import com.ecoruta.app.features.notifications.domain.repository.FcmTokenRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Inject

class FcmTokenRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
): FcmTokenRepository{
    override suspend fun registerToken(
        token: String,
        deviceModel: String
    ): Result<Unit> = runCatching {
        require(token.isNotBlank()) {
            "El token FCM no puede estar vacío"
        }

        val user = auth.currentUserOrNull()
            ?: error("No existe una sesión activa para registrar el dispositivo")

        val tokenDto = FcmTokenDto(
            profileId = user.id,
            token = token,
            deviceModel = deviceModel
        )

        postgrest.from("user_fcm_tokens").upsert(
            tokenDto,
            onConflict = "profile_id,token"
        )
    }

    override suspend fun removeToken(
        token: String
    ): Result<Unit> = runCatching {

        val user = auth.currentUserOrNull()
            ?: error("No existe una sesión activa para eliminar el dispositivo")

        require(token.isNotBlank()) {
            "El token FCM no puede estar vacío"
        }

        postgrest.from("user_fcm_tokens").delete {
            filter {
                eq("profile_id", user.id)
                eq("token", token)
            }
        }
    }
}