package com.ecoruta.app.features.notifications.domain.repository

interface FcmTokenRepository {
    suspend fun registerToken(
        token: String,
        deviceModel: String
    ): Result<Unit>

    // Retirar el registro de ese dispositivo cuando cierre sesión o no se necesite.
    suspend fun removeToken(
        token: String
    ): Result<Unit>

}