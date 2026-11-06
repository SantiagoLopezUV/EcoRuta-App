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

// PRUEBAS UNITARIAS DE SERIALIZACIÓN (DATA LAYER / DTOs)
// ¿Cómo funcionan estas pruebas?
// Validan la capa de serialización (kotlinx.serialization) sin necesidad de conectarse a internet.
// Su objetivo es garantizar que los nombres de las propiedades en Kotlin (camelCase) se traduzcan
// exactamente a los nombres de las columnas en PostgreSQL (snake_case) esperados por Supabase PostgREST.
// Si un nombre de columna no coincide exactamente (@SerialName), Supabase rechazaría la inserción HTTP.

class AuthRegistrationDtoTest {

    // Configuración del motor JSON:
    // ignoreUnknownKeys: evita fallos si el servidor responde con columnas adicionales.
    // encodeDefaults = true: OBLIGA a incluir campos con valores por defecto (como status = "pendiente")
    //  en el JSON final que se envía en la petición POST a la base de datos.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // testProfileDtoSerialization()
    // ¿Cómo funciona?
    // Instancia un ProfileDto con datos de prueba simulados en memoria RAM.
    // Convierte el objeto Kotlin a una cadena de texto en formato JSON mediante json.encodeToString().
    // Comprueba mediante aserciones (assertTrue) que las claves generadas coincidan con el esquema de la tabla 'public.profiles': "role_code", "document_id" y "full_name"

    @Test
    fun testProfileDtoSerialization() {
        val dto = ProfileDto(
            id = "user-123",
            documentId = "1085324910",
            roleCode = "recycler",
            fullName = "Juan Reciclador",
            email = "juan@ecoruta.co",
            mustChangePassword = false
        )

        val encoded = json.encodeToString(dto)

        assertTrue(encoded.contains("\"role_code\":\"recycler\""))
        assertTrue(encoded.contains("\"document_id\":\"1085324910\""))
        assertTrue(encoded.contains("\"full_name\":\"Juan Reciclador\""))
    }

    // testRecyclerDtoPendingStatus()
    // ¿Cómo funciona?
    // Instancia un RecyclerDto omitiendo el parámetro 'status' para que tome su valor por defecto ("pendiente").
    // Serializa a JSON y comprueba que la clave "status":"pendiente" esté presente en la carga útil.
    // Esto garantiza que ningún reciclador pueda registrarse con privilegios automáticos sin pasar por la revisión administrativa de EcoRuta.

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

    // testBusinessDtoPendingStatus()
    // ¿Cómo funciona?
    // Instancia un BusinessDto para comercios aliados con atributos comerciales (razón social, dirección).
    // Serializa a JSON y valida que el estado inicial asignado sea obligatoriamente "pendiente".
    // Verifica que la clave "business_name" coincida con la columna esperada en la tabla 'public.businesses'.

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

    // testCitizenDtoSerialization()
    // ¿Cómo funciona?
    // Instancia un CitizenDto que representa la información complementaria de un usuario residencial.
    // Serializa a JSON y valida que las claves "phone" y "neighborhood" se mapeen con exactitud.
    // Garantiza que la información de contacto y barrio quede lista para su persistencia en 'public.citizens'.

    @Test
    fun testCitizenDtoSerialization() {
        val dto = CitizenDto(
            profileId = "user-789",
            phone = "3123456789",
            neighborhood = "progresar"
        )

        val encoded = json.encodeToString(dto)

        assertTrue(encoded.contains("\"neighborhood\":\"progresar\""))
        assertTrue(encoded.contains("\"phone\":\"3123456789\""))
    }
}
