package com.ecoruta.app.features.education.domain.repository

// Contrato de dominio para catálogo pedagógico y separación de residuos
// Declaración abstracta de consulta de lecciones y registro de finalización

import com.ecoruta.app.features.education.domain.model.EducationalLesson
import com.ecoruta.app.features.education.presentation.TopicReward

interface EducationRepository {

    // Consulta el catálogo de lecciones pedagógicas disponibles y el estado de completitud del usuario
    suspend fun getLessons(): Result<List<EducationalLesson>>

    // Consulta el detalle de una lección pedagógica por su identificador
    suspend fun getLessonById(lessonId: String): Result<EducationalLesson>

    // Registra la finalización de la lección acreditando los EcoPuntos al saldo del ciudadano
    suspend fun completeLesson(lessonId: String): Result<TopicReward>
}
