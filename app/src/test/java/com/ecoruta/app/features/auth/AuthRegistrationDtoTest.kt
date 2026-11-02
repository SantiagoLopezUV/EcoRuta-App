package com.ecoruta.app.features.auth

import com.ecoruta.app.features.auth.data.remote.dto.BusinessDto
import com.ecoruta.app.features.auth.data.remote.dto.CitizenDto
import com.ecoruta.app.features.auth.data.remote.dto.ProfileDto
import com.ecoruta.app.features.auth.data.remote.dto.RecyclerDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas unitarias de serialización para DTOs del flujo de registro y estado pendiente
// Valida que los nombres serializados coincidan exactamente con el esquema de Supabase

class AuthRegistrationDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true //forzar que se incluya status:pendiente en el JSON enviado
    }

    // Valida que ProfileDto serialice las claves requeridas por la tabla profiles
    @Test
    fun testProfileDtoSerialization() {
        // crea el objeto en memoria
        val dto = ProfileDto(
            id = "user-123",
            documentId = "1085324910",
            roleCode = "recycler",
            fullName = "Juan Reciclador",
            email = "juan@ecoruta.co",
            mustChangePassword = false
        )
        // simula la conversion a JSON
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"role_code\":\"recycler\""))
        assertTrue(encoded.contains("\"document_id\":\"1085324910\""))
        assertTrue(encoded.contains("\"full_name\":\"Juan Reciclador\""))
    }

    // Valida que RecyclerDto serialice por defecto el estado pendiente
    @Test
    fun testRecyclerDtoPendingStatus() {
        val dto = RecyclerDto(
            profileId = "user-123",
            phone = "3154829103",
            associationName = "Asociación Tulueña de Reciclaje"
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"status\":\"pendiente\""))
        assertTrue(encoded.contains("\"association_name\":\"Asociación Tulueña de Reciclaje\""))
    }

    // Valida que BusinessDto serialice por defecto el estado pendiente
    @Test
    fun testBusinessDtoPendingStatus() {
        val dto = BusinessDto(
            profileId = "user-456",
            businessName = "Tienda La Esperanza",
            category = "Tienda de barrio",
            address = "Calle 25 # 20-30"
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"status\":\"pendiente\""))
        assertTrue(encoded.contains("\"business_name\":\"Tienda La Esperanza\""))
    }

    // Valida que CitizenDto serialice los atributos de contacto y barrio
    @Test
    fun testCitizenDtoSerialization() {
        val dto = CitizenDto(
            profileId = "user-789",
            phone = "3123456789",
            neighborhood = "progresar"
        )
        val encoded = json.encodeToString(dto)
        assertTrue(encoded.contains("\"neighborhood\":\"Torobajo\""))
        assertTrue(encoded.contains("\"phone\":\"3123456789\""))
    }
}
