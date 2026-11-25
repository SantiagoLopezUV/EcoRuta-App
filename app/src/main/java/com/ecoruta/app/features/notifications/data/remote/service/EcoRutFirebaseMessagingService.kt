package com.ecoruta.app.features.notifications.data.remote.service

import android.util.Log
import com.ecoruta.app.features.notifications.domain.repository.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

class EcoRutFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

        serviceScope.launch {
            fcmTokenRepository.registerToken(
                token = token,
                deviceModel = deviceModel
            ).onFailure { error ->
                Log.w(
                    "EcoRutaFCM",
                    "No se pudo registrar el token FCM: ${error.message}"
                )
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}