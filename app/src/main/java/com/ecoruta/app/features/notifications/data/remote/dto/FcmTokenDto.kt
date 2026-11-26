package com.ecoruta.app.features.notifications.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenDto (
    @SerialName("profile_id")
    val profileId: String,

    @SerialName("token")
    val token: String,

    @SerialName("device_model")
    val deviceModel: String
)