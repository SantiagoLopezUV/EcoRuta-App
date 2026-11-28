package com.ecoruta.app.core.network.supabase

import com.ecoruta.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SettingsCodeVerifierCache
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.cio.CIO
import kotlin.time.Duration.Companion.seconds

// Proveedor e inicializador del cliente Supabase
// Configura plugins de Auth, Postgrest, Storage y Realtime con motor Ktor


// Detecta de forma segura si la ejecución es en un dispositivo Android o en la JVM de pruebas unitarias
private val isAndroidRuntime: Boolean = runCatching {
    val vmName = System.getProperty("java.vm.name").orEmpty()
    vmName.contains("Dalvik", ignoreCase = true)
}.getOrDefault(false)

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    //definir el motor de red asíncrono que utilizará el SDK de Supabase para comunicarse por internet
    // CIO - Coroutine-based I/O
    httpEngine = CIO.create()

    install(Auth){              // administra el token JWT, permite no volver a iniciar sesión¿
        flowType = FlowType.PKCE        // Flujo de autenticación, da seguridad contra ataques de intercepción al JWT
        // sheme y host para que android sepa que app debe abrir el correo de recuperación o el callback de google
        scheme = "ecoruta"
        host = "auth"

        // Usa SharedPreferences en Android y memoria volátil en pruebas unitarias de la JVM
        sessionManager = try {
            SettingsSessionManager()
        } catch (e: Throwable) {
            MemorySessionManager()
        }
        codeVerifierCache = try {
            SettingsCodeVerifierCache()
        } catch (e: Throwable) {
            MemoryCodeVerifierCache()
        }

        // Mantiene la sesión activa y renueva automáticamente el token antes de vencer
        alwaysAutoRefresh = true // true para que el SDK renueve los token JWT en segundo plano antes de vencerse
        autoLoadFromStorage = true
        autoSaveToStorage = true

        // Habilita los callbacks de ciclo de vida en Android para renovar el token al volver a la app
        enableLifecycleCallbacks = isAndroidRuntime
    }
    install(Postgrest){ // base de datos PostgreSQL
        // Valores predeterminados
        /*
        defaultSchema = "public" // esquema estandar de supabase
        propertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
        timeout = 30.seconds // tiempo límete para consultas HTTP
        */
    }
    install(Storage){ // almacena archivos multimedia
        //valores predeterminados
        // transferTimeout = 120.seconds // tiempo de espera para bajar y subir archivos
    }
    install(Realtime){ // comunicación en vivo con websockets para los gps y radares en vivo
        reconnectDelay = 5.seconds // reconexión para GPS
        //Valores predeterminados
        /*
        disconnectOnSessionLoss = true // Indica si se debe desconectar el websocket cuando se pierde la sesión
        heartbeatInterval = 15.seconds // cierra el socket automaticamente si se pierde la sesión
         */
    }
    install(Functions) //implementación de funciones edge
}