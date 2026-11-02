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
        // Deshabilita callbacks de ciclo de vida del proceso para compatibilidad con pruebas unitarias en la JVM
        enableLifecycleCallbacks = false

        //valores po defecto del sdk
        /*
        alwaysAutoRefresh = true        //mantiene la sesión activa, renueva el token JWT automáticamente
        autoLoadFromStorage = true      // persistencia entre los inicios de sesión, carga automáticamente la sesión guardada al abrir la app
        autoSaveToStorage = true        // Guarda la sesión de forma cifrada en el dispositivo
        enableLifecycleCallbacks = true // determina si se detiene la actualización automática al perder el foco y si se reanuda al volver a obtenerlo.
        */
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