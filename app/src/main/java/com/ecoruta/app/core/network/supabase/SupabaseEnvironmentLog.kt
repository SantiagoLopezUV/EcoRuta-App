package com.ecoruta.app.core.network.supabase

// Arma la línea de diagnóstico que dice a qué servidor de Supabase apunta la app
// Solo incluye el entorno y el host para no exponer la clave ni la ruta completa

import java.net.URI

fun supabaseEnvironmentLogLine(environment: String, url: String): String {
    val host = runCatching { URI(url.trim()).host }.getOrNull()
    return "Supabase env=${environment.ifBlank { "?" }} host=${host ?: "?"}"
}
