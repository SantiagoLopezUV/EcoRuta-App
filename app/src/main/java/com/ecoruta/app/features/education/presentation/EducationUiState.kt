package com.ecoruta.app.features.education.presentation

// Estado inmutable del catálogo de educación ambiental
// Colección de lecciones disponibles, lección seleccionada, carga y posibles errores

import androidx.annotation.StringRes
import java.text.Normalizer

enum class TopicIcon {
    LISTA,
    RECICLAJE,
    ESCOMBROS,
    ECOPUNTOS,
}

data class EducationTopic(
    val id: String,
    val title: String,
    val summary: String,
    val rewardPoints: Int,
    val icon: TopicIcon,
    val completed: Boolean = false,
    val favorite: Boolean = false,
    val opensSeparationGuide: Boolean = false,
)

data class TopicReward(
    val pointsEarned: Int,
)

data class EducationUiState(
    val isLoading: Boolean = true,
    val topics: List<EducationTopic> = emptyList(),
    val query: String = "",
    val isSample: Boolean = false,
    @param:StringRes val error: Int? = null,
) {
    val visibleTopics: List<EducationTopic>
        get() = if (query.isBlank()) {
            topics
        } else {
            val normalizedQuery = normalizeText(query.trim())
            topics.filter {
                normalizeText(it.title).contains(normalizedQuery) ||
                    normalizeText(it.summary).contains(normalizedQuery)
            }
        }
}

private fun normalizeText(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

data class SeparationGuideUiState(
    val favorite: Boolean = false,
    val completed: Boolean = false,
    val rewardPoints: Int = 50,
    val isCompleting: Boolean = false,
    val reward: TopicReward? = null,
    val completedWithoutBackend: Boolean = false,
)
