package com.ecoruta.app.features.education.data.remote.dto

// Objeto de transferencia de datos para la tabla educational_content y educational_completions
// Transporta los contenidos didácticos de educación ambiental y su estado de completitud

import com.ecoruta.app.features.education.domain.model.EducationalLesson
import com.ecoruta.app.features.education.presentation.TopicIcon
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EducationalContentDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("points_reward") val pointsReward: Int = 20,
    @SerialName("summary") val summary: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("icon") val icon: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class EducationalCompletionDto(
    @SerialName("id") val id: String? = null,
    @SerialName("citizen_id") val citizenId: String,
    @SerialName("content_id") val contentId: String,
    @SerialName("completed_at") val completedAt: String? = null,
)

// Convierte el DTO remoto al modelo de dominio inmutable
fun EducationalContentDto.toDomain(isCompleted: Boolean = false): EducationalLesson {
    val topicIcon = when (icon?.lowercase()) {
        "recycling" -> TopicIcon.RECYCLING
        "debris" -> TopicIcon.DEBRIS
        "ecopoints" -> TopicIcon.ECOPOINTS
        else -> TopicIcon.LIST
    }
    return EducationalLesson(
        id = id,
        title = title,
        summary = summary ?: title,
        body = body.orEmpty(),
        contentType = contentType,
        pointsReward = pointsReward,
        icon = topicIcon,
        isCompleted = isCompleted,
        opensSeparationGuide = contentType == "separacion_residuos",
    )
}
