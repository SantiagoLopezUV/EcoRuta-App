package com.ecoruta.app.features.education

// Pruebas unitarias para el modelo de dominio educativo y FakeEducationRepository
// Valida mapeo hacia EducationTopic de UI, acreditación de EcoPuntos e idempotencia

import com.ecoruta.app.core.testing.FakeEducationRepository
import com.ecoruta.app.features.education.domain.model.EducationalLesson
import com.ecoruta.app.features.education.presentation.TopicIcon
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationDomainModelTest {

    // Valida que EducationalLesson.toUiTopic mapee correctamente todos los campos a la vista
    @Test
    fun testEducationalLessonToUiTopicMapping() {
        val lesson = EducationalLesson(
            id = "EDU-01",
            title = "Separación en la fuente",
            summary = "Aprende el código de colores",
            body = "Cuerpo detallado...",
            contentType = "separacion_residuos",
            pointsReward = 20,
            icon = TopicIcon.LIST,
            isCompleted = true,
            opensSeparationGuide = true,
        )

        val uiTopic = lesson.toUiTopic()

        assertEquals("EDU-01", uiTopic.id)
        assertEquals("Separación en la fuente", uiTopic.title)
        assertEquals("Aprende el código de colores", uiTopic.summary)
        assertEquals(20, uiTopic.rewardPoints)
        assertEquals(TopicIcon.LIST, uiTopic.icon)
        assertTrue(uiTopic.completed)
        assertTrue(uiTopic.opensSeparationGuide)
    }

    // Valida que opensSeparationGuide sea verdadero si contentType es separacion_residuos aunque la bandera sea false
    @Test
    fun testEducationalLessonSeparationGuideContentTypeFlag() {
        val lesson = EducationalLesson(
            id = "EDU-99",
            title = "Código de Colores",
            summary = "Guía",
            contentType = "separacion_residuos",
            pointsReward = 20,
            opensSeparationGuide = false,
        )

        val uiTopic = lesson.toUiTopic()
        assertTrue(uiTopic.opensSeparationGuide)
    }

    // Valida que FakeEducationRepository liste las lecciones pedagógicas iniciales
    @Test
    fun testFakeEducationRepositoryGetLessons(): Unit = runBlocking {
        val repository = FakeEducationRepository()

        val result = repository.getLessons()
        assertTrue(result.isSuccess)

        val lessons = result.getOrThrow()
        assertTrue(lessons.size >= 2)
        assertFalse(lessons.first().isCompleted)
    }

    // Valida que completeLesson acredite +20 EcoPuntos en la primera llamada y 0 en llamadas repetidas
    @Test
    fun testFakeEducationRepositoryCompleteLessonIdempotency(): Unit = runBlocking {
        val repository = FakeEducationRepository()
        val lessonId = "EDU-01"

        // Primera completitud: debe ganar 20 EcoPuntos
        val firstCompletion = repository.completeLesson(lessonId)
        assertTrue(firstCompletion.isSuccess)
        assertEquals(20, firstCompletion.getOrThrow().pointsEarned)

        // Estado de completitud reflejado en getLessonById y getLessons
        val detailResult = repository.getLessonById(lessonId)
        assertTrue(detailResult.isSuccess)
        assertTrue(detailResult.getOrThrow().isCompleted)

        val listResult = repository.getLessons()
        assertTrue(listResult.getOrThrow().first { it.id == lessonId }.isCompleted)

        // Segunda completitud: debe retornar 0 puntos por regla de negocio única
        val secondCompletion = repository.completeLesson(lessonId)
        assertTrue(secondCompletion.isSuccess)
        assertEquals(0, secondCompletion.getOrThrow().pointsEarned)
    }

    // Valida que getLessonById devuelva error si el identificador no existe
    @Test
    fun testFakeEducationRepositoryLessonNotFound(): Unit = runBlocking {
        val repository = FakeEducationRepository()

        val result = repository.getLessonById("INEXISTENTE")
        assertTrue(result.isFailure)
    }

    // Valida manejo de errores simulados en FakeEducationRepository
    @Test
    fun testFakeEducationRepositorySimulatedError(): Unit = runBlocking {
        val repository = FakeEducationRepository()
        repository.error = IllegalStateException("Simulación de fallo de red")

        val result = repository.getLessons()
        assertTrue(result.isFailure)
        assertEquals("Simulación de fallo de red", result.exceptionOrNull()?.message)
    }
}
