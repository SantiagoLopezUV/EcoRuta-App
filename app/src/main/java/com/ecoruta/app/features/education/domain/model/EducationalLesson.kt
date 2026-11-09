package com.ecoruta.app.features.education.domain.model

// Modelo de dominio puro para lecciones de educación ambiental
// Modela identificador, título, contenido pedagógico y valor en EcoPuntos (+20)

import com.ecoruta.app.features.education.presentation.EducationTopic
import com.ecoruta.app.features.education.presentation.TopicIcon

data class EducationalLesson(
    val id: String,
    val title: String,
    val summary: String,
    val body: String = "",
    val contentType: String,
    val pointsReward: Int = 20,
    val icon: TopicIcon = TopicIcon.LIST,
    val isCompleted: Boolean = false,
    val opensSeparationGuide: Boolean = false,
) {
    // Convierte el modelo de dominio puro a la representación visual para la pantalla del catálogo
    fun toUiTopic(): EducationTopic = EducationTopic(
        id = id,
        title = title,
        summary = summary,
        rewardPoints = pointsReward,
        icon = icon,
        completed = isCompleted,
        opensSeparationGuide = opensSeparationGuide || contentType == "separacion_residuos",
    )
}
