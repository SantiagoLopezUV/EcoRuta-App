package com.ecoruta.app.features.education.data.repository

// Implementación concreta del repositorio de educación ambiental
// Consulta el catálogo de lecciones pedagógicas y ejecuta la función RPC de acreditación

import com.ecoruta.app.features.education.data.remote.dto.EducationalCompletionDto
import com.ecoruta.app.features.education.data.remote.dto.EducationalContentDto
import com.ecoruta.app.features.education.data.remote.dto.toDomain
import com.ecoruta.app.features.education.domain.model.EducationalLesson
import com.ecoruta.app.features.education.domain.repository.EducationRepository
import com.ecoruta.app.features.education.presentation.TopicReward
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class EducationRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
) : EducationRepository {

    // Consulta el catálogo de contenidos pedagógicos activos y cruza con las lecciones completadas por el usuario
    override suspend fun getLessons(): Result<List<EducationalLesson>> = runCatching {
        val contents = postgrest.from("educational_content")
            .select {
                filter { eq("is_active", true) }
            }
            .decodeList<EducationalContentDto>()

        val userId = auth.currentUserOrNull()?.id
        val completedIds = if (userId != null) {
            postgrest.from("educational_completions")
                .select {
                    filter { eq("citizen_id", userId) }
                }
                .decodeList<EducationalCompletionDto>()
                .map { it.contentId }
                .toSet()
        } else {
            emptySet()
        }

        contents.map { dto ->
            dto.toDomain(isCompleted = dto.id in completedIds)
        }
    }

    // Consulta una lección específica por su identificador
    override suspend fun getLessonById(lessonId: String): Result<EducationalLesson> = runCatching {
        val dto = postgrest.from("educational_content")
            .select {
                filter { eq("id", lessonId) }
            }
            .decodeSingle<EducationalContentDto>()

        val userId = auth.currentUserOrNull()?.id
        val isCompleted = if (userId != null) {
            val completions = postgrest.from("educational_completions")
                .select {
                    filter {
                        eq("citizen_id", userId)
                        eq("content_id", lessonId)
                    }
                }
                .decodeList<EducationalCompletionDto>()
            completions.isNotEmpty()
        } else {
            false
        }

        dto.toDomain(isCompleted = isCompleted)
    }

    // Ejecuta la función RPC complete_educational_lesson para registrar la finalización y acreditar EcoPuntos
    override suspend fun completeLesson(lessonId: String): Result<TopicReward> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para completar la lección educativa")

        val pointsEarned = postgrest.rpc(
            "complete_educational_lesson",
            buildJsonObject {
                put("p_content_id", lessonId)
            }
        ).decodeAs<Int>()

        TopicReward(pointsEarned = pointsEarned)
    }
}
