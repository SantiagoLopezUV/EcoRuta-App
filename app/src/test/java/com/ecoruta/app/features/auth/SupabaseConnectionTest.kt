package com.ecoruta.app.features.auth

import com.ecoruta.app.BuildConfig
import com.ecoruta.app.core.network.supabase.supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Prueba de verificación de conectividad con el backend de Supabase
// Comprueba que las credenciales de BuildConfig alcancen exitosamente la base de datos

class SupabaseConnectionTest {

    // Verifica que las credenciales de URL y Anon Key estén cargadas correctamente desde local.properties
    @Test
    fun verifySupabaseCredentialsConfigured() {
        assertNotNull(BuildConfig.SUPABASE_URL)
        assertTrue(BuildConfig.SUPABASE_URL.isNotEmpty())
        assertNotNull(BuildConfig.SUPABASE_ANON_KEY)
        assertTrue(BuildConfig.SUPABASE_ANON_KEY.isNotEmpty())
    }

    // Consulta la tabla roles vía PostgREST para validar comunicación en vivo con PostgreSQL
    @Test
    fun verifyDatabaseConnection() = runBlocking {
        // Ejecuta una petición select a la base de datos mediante el cliente configurado
        val result = supabase.postgrest.from("roles").select()

        // Imprime en consola la respuesta
        println("CONEXION EXITOSA A SUPABASE")
        println("Respuesta del servidor: ${result.data}")
        println("Encabezados HTTP: ${result.headers}")

        // Valida que la respuesta no sea nula
        assertNotNull(result)
        assertNotNull(result.data)
    }
}
