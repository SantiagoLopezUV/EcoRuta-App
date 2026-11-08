package com.ecoruta.app.features.profile

import com.ecoruta.app.features.profile.data.remote.dto.BusinessApplicationDto
import com.ecoruta.app.features.profile.data.remote.dto.CitizenDataDto
import com.ecoruta.app.features.profile.data.remote.dto.PendingRoleItemDto
import com.ecoruta.app.features.profile.data.remote.dto.ProfileDto
import com.ecoruta.app.features.profile.data.remote.dto.RecyclerApplicationDto
import com.ecoruta.app.features.profile.data.remote.dto.UserRoleItemDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas de serialización de DTO para los perfiles y postulaciones a otros roles
class ProfileRepositoryDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Valida la serialización de ProfileDto y mapeo exacto de columnas de la tabla 'public.profiles'
    @Test
    fun testProfileDtoSerialization() {
        // Instancia el DTO con datos simulados que representan el registro de un perfil en PostgreSQL
        val dto = ProfileDto(
            id = "user-uuid-456",
            documentId = "1118555444",
            documentType = "CC",
            roleCode = "citizen",
            activeRole = "citizen",
            fullName = "Carlos Rivera",
            email = "carlos@ecoruta.co",
            avatarUrl = "https://ecoruta.co/avatars/carlos.jpg",
            phone = "3123456789",
            mustChangePassword = false,
        )

        // Convierte el objeto Kotlin a formato JSON mediante json.encodeToString
        val encoded = json.encodeToString(dto)

        // Comprueba que las claves generadas coincidan exactamente con las columnas de PostgreSQL en snake_case
        assertTrue(encoded.contains("\"document_id\":\"1118555444\""))
        assertTrue(encoded.contains("\"document_type\":\"CC\""))
        assertTrue(encoded.contains("\"role_code\":\"citizen\""))
        assertTrue(encoded.contains("\"active_role\":\"citizen\""))
        assertTrue(encoded.contains("\"full_name\":\"Carlos Rivera\""))
        assertTrue(encoded.contains("\"avatar_url\":\"https://ecoruta.co/avatars/carlos.jpg\""))

        // Comprueba que la deserialización inversa reconstruya el objeto original sin pérdida de atributos
        val decoded = json.decodeFromString<ProfileDto>(encoded)
        assertEquals(dto.id, decoded.id)
        assertEquals(dto.fullName, decoded.fullName)
    }

    // Valida la deserialización de la tabla 'public.citizens' con el balance de EcoPuntos
    @Test
    fun testCitizenDataDtoDeserialization() {
        // Simula la respuesta JSON que retorna Supabase al consultar la fila del ciudadano
        val rawJson = """
            {
                "profile_id": "citizen-uuid-789",
                "phone": "3001234567",
                "neighborhood": "Centro",
                "ecopoints_balance": 1500
            }
        """.trimIndent()

        // Decodifica la cadena JSON hacia el objeto Kotlin CitizenDataDto
        val decoded = json.decodeFromString<CitizenDataDto>(rawJson)
        // Comprueba que el identificador, el barrio y el saldo numérico se mapeen con exactitud
        assertEquals("citizen-uuid-789", decoded.profileId)
        assertEquals("Centro", decoded.neighborhood)
        assertEquals(1500, decoded.ecopointsBalance)
    }

    // Valida la deserialización de la tabla 'public.user_roles'
    @Test
    fun testUserRoleItemDtoDeserialization() {
        // Simula una fila de la tabla user_roles devuelta por la base de datos
        val rawJson = """
            {
                "id": "role-uuid-1",
                "profile_id": "user-uuid-1",
                "role_code": "recycler",
                "created_at": "2026-11-05T10:00:00Z"
            }
        """.trimIndent()

        // Decodifica el JSON hacia el DTO correspondiente
        val decoded = json.decodeFromString<UserRoleItemDto>(rawJson)
        // Verifica que el código del rol coincida con el valor esperado para su posterior mapeo al enum
        assertEquals("recycler", decoded.roleCode)
        assertEquals("user-uuid-1", decoded.profileId)
    }

    // Valida que la postulación de reciclador genere status 'pendiente' por defecto
    @Test
    fun testRecyclerApplicationDtoDefaults() {
        // Crea la postulación omitiendo el estado para comprobar que tome el valor por defecto
        val dto = RecyclerApplicationDto(
            profileId = "user-rec-1",
            phone = "3119876543",
            associationName = "Asociación ASOREC",
            companyId = "comp-123",
            idDocumentPdfUrl = "https://ecoruta.co/docs/id.pdf",
        )

        val encoded = json.encodeToString(dto)
        // Garantiza que la solicitud viaje con estado pendiente para requerir revisión administrativa
        assertTrue(encoded.contains("\"status\":\"pendiente\""))
        assertTrue(encoded.contains("\"association_name\":\"Asociación ASOREC\""))
        assertTrue(encoded.contains("\"company_id\":\"comp-123\""))
        assertTrue(encoded.contains("\"id_document_pdf_url\":\"https://ecoruta.co/docs/id.pdf\""))
    }

    // Valida que la postulación de comercio genere status 'pendiente' por defecto
    @Test
    fun testBusinessApplicationDtoDefaults() {
        // Crea los datos del establecimiento omitiendo el parámetro status
        val dto = BusinessApplicationDto(
            profileId = "user-biz-1",
            businessName = "Cafetería La 25",
            category = "Restaurante",
            address = "Carrera 26 # 25-14",
            rutPdfUrl = "https://ecoruta.co/docs/rut.pdf",
        )

        val encoded = json.encodeToString(dto)
        // Verifica que el comercio siempre se radique en estado pendiente de verificación
        assertTrue(encoded.contains("\"status\":\"pendiente\""))
        assertTrue(encoded.contains("\"business_name\":\"Cafetería La 25\""))
        assertTrue(encoded.contains("\"rut_pdf_url\":\"https://ecoruta.co/docs/rut.pdf\""))
    }

    // Valida la deserialización de roles pendientes de verificación
    @Test
    fun testPendingRoleItemDtoDeserialization() {
        // Simula la respuesta JSON con el estado de la solicitud en moderación
        val rawJson = """
            {
                "profile_id": "user-uuid-999",
                "status": "pendiente"
            }
        """.trimIndent()

        // Decodifica y verifica que se pueda consultar si la postulación sigue en trámite
        val decoded = json.decodeFromString<PendingRoleItemDto>(rawJson)
        assertEquals("user-uuid-999", decoded.profileId)
        assertEquals("pendiente", decoded.status)
    }
}
