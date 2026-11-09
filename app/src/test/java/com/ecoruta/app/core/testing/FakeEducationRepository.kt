package com.ecoruta.app.core.testing

// Repositorio de educación ambiental en memoria para pruebas unitarias sin red ni base de datos
// Permite consultar lecciones pedagógicas simuladas y registrar completitud

import com.ecoruta.app.features.education.domain.model.EducationalLesson
import com.ecoruta.app.features.education.domain.repository.EducationRepository
import com.ecoruta.app.features.education.presentation.TopicIcon
import com.ecoruta.app.features.education.presentation.TopicReward

class FakeEducationRepository : EducationRepository {

    var error: Throwable? = null
    val completedLessons = mutableListOf<String>()

    val lessons = mutableListOf(
        EducationalLesson(
            id = "EDU-01",
            title = "Cómo separar tus residuos",
            summary = "Guía rápida del código de colores en Colombia",
            body = "Blanco, negro y verde...",
            contentType = "separacion_residuos",
            pointsReward = 20,
            icon = TopicIcon.LIST,
            isCompleted = false,
            opensSeparationGuide = true,
        ),
        EducationalLesson(
            id = "EDU-02",
            title = "Puntos Limpios en Tuluá",
            summary = "Contenedores públicos habilitados",
            body = "Ubicaciones estratégicas...",
            contentType = "puntos_limpios",
            pointsReward = 20,
            icon = TopicIcon.RECYCLING,
            isCompleted = false,
        )
    )

    override suspend fun getLessons(): Result<List<EducationalLesson>> = result {
        lessons.map { it.copy(isCompleted = it.id in completedLessons) }
    }

    override suspend fun getLessonById(lessonId: String): Result<EducationalLesson> = result {
        val found = lessons.firstOrNull { it.id == lessonId }
            ?: error("Lección no encontrada con id: $lessonId")
        found.copy(isCompleted = found.id in completedLessons)
    }

    override suspend fun completeLesson(lessonId: String): Result<TopicReward> = result {
        val lesson = lessons.firstOrNull { it.id == lessonId }
            ?: error("Lección no encontrada con id: $lessonId")
        if (lessonId in completedLessons) {
            TopicReward(pointsEarned = 0)
        } else {
            completedLessons += lessonId
            TopicReward(pointsEarned = lesson.pointsReward)
        }
    }

    private inline fun <T> result(block: () -> T): Result<T> =
        error?.let { Result.failure(it) } ?: runCatching(block)
}
