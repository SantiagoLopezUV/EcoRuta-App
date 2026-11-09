package com.ecoruta.app.features.education

// Pruebas unitarias de serialización DTO y mapeo a dominio para educación ambiental
// Valida compatibilidad con el esquema Supabase educational_content y educational_completions

import com.ecoruta.app.features.education.data.remote.dto.EducationalCompletionDto
import com.ecoruta.app.features.education.data.remote.dto.EducationalContentDto
import com.ecoruta.app.features.education.data.remote.dto.toDomain
import com.ecoruta.app.features.education.presentation.TopicIcon
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EducationRepositoryDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Valida deserialización de EducationalContentDto desde JSON snake_case de PostgREST
    @Test
    fun testEducationalContentDtoDeserialization() {
        val rawJson = """
            {
                "id": "e5c6a1b2-1111-4000-8000-000000000001",
                "title": "Cómo separar tus residuos en casa",
                "content_type": "separacion_residuos",
                "points_reward": 20,
                "summary": "Aprende el código de colores oficial en Colombia",
                "body": "Utiliza caneca blanca para aprovechables, negra para ordinarios y verde para orgánicos.",
                "icon": "recycling",
                "is_active": true,
                "created_at": "2026-11-08T10:00:00Z"
            }
        """.trimIndent()

        val dto = json.decodeFromString<EducationalContentDto>(rawJson)

        assertEquals("e5c6a1b2-1111-4000-8000-000000000001", dto.id)
        assertEquals("Cómo separar tus residuos en casa", dto.title)
        assertEquals("separacion_residuos", dto.contentType)
        assertEquals(20, dto.pointsReward)
        assertEquals("Aprende el código de colores oficial en Colombia", dto.summary)
        assertEquals("recycling", dto.icon)
        assertTrue(dto.isActive)
    }

    // Valida mapeo de iconos y reglas de dominio a partir del DTO remoto
    @Test
    fun testEducationalContentDtoToDomainMapping() {
        val recyclingDto = EducationalContentDto(
            id = "edu-1",
            title = "Reciclaje",
            contentType = "separacion_residuos",
            pointsReward = 20,
            summary = "Guía",
            body = "Detalle",
            icon = "recycling",
        )
        val recyclingDomain = recyclingDto.toDomain(isCompleted = true)
        assertEquals(TopicIcon.RECYCLING, recyclingDomain.icon)
        assertTrue(recyclingDomain.isCompleted)
        assertTrue(recyclingDomain.opensSeparationGuide)

        val debrisDto = EducationalContentDto(
            id = "edu-2",
            title = "Escombros y RCD",
            contentType = "escombros_rcd",
            pointsReward = 20,
            icon = "debris",
        )
        val debrisDomain = debrisDto.toDomain(isCompleted = false)
        assertEquals(TopicIcon.DEBRIS, debrisDomain.icon)
        assertFalse(debrisDomain.isCompleted)
        assertFalse(debrisDomain.opensSeparationGuide)

        val pointsDto = EducationalContentDto(
            id = "edu-3",
            title = "EcoPuntos y Beneficios",
            contentType = "ecopuntos",
            pointsReward = 20,
            icon = "ecopoints",
        )
        val pointsDomain = pointsDto.toDomain()
        assertEquals(TopicIcon.ECOPOINTS, pointsDomain.icon)

        val fallbackDto = EducationalContentDto(
            id = "edu-4",
            title = "General",
            contentType = "general",
            icon = "desconocido",
        )
        val fallbackDomain = fallbackDto.toDomain()
        assertEquals(TopicIcon.LIST, fallbackDomain.icon)
    }

    // Valida serialización de EducationalCompletionDto para auditoría de completitud
    @Test
    fun testEducationalCompletionDtoSerialization() {
        val dto = EducationalCompletionDto(
            citizenId = "citizen-uuid-123",
            contentId = "lesson-uuid-456",
        )

        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"citizen_id\":\"citizen-uuid-123\""))
        assertTrue(encoded.contains("\"content_id\":\"lesson-uuid-456\""))
    }
}
