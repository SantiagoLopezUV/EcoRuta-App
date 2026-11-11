package com.ecoruta.app.core.network.supabase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// Pruebas de la línea de Logcat que identifica el servidor de Supabase activo
class SupabaseEnvironmentLogTest {

    @Test
    fun localUrl_showsEnvironmentAndHostOnly() {
        val line = supabaseEnvironmentLogLine("LOCAL", "http://10.0.2.2:54321")

        assertEquals("Supabase env=LOCAL host=10.0.2.2", line)
    }

    @Test
    fun cloudUrlWithPathAndQuery_dropsEverythingButHost() {
        val line = supabaseEnvironmentLogLine("CLOUD", "https://abc123.supabase.co/rest/v1?apikey=secreto")

        assertEquals("Supabase env=CLOUD host=abc123.supabase.co", line)
        assertFalse(line.contains("secreto"))
    }

    @Test
    fun emptyValues_showPlaceholders() {
        assertEquals("Supabase env=? host=?", supabaseEnvironmentLogLine("", ""))
    }

    @Test
    fun malformedUrl_doesNotLeakIt() {
        val line = supabaseEnvironmentLogLine("PHONE", "no es una url")

        assertEquals("Supabase env=PHONE host=?", line)
    }
}
