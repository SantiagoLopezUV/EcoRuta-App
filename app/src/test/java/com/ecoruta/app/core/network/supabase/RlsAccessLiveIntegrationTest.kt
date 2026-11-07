package com.ecoruta.app.core.network.supabase

import com.ecoruta.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.engine.cio.CIO
import java.net.URI
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

// Pruebas de integración en vivo de las políticas RLS y del acceso entre roles contra la base LOCAL de Supabase
// Solo corren cuando SUPABASE_ENV es LOCAL y la URL apunta a la máquina local; en cualquier otro caso se saltan solas
// Cada prueba registra sus propios usuarios con correos únicos porque el seed no trae usuarios
// Al terminar se cierra la sesión y se borra la fila propia de citizens cuando las políticas lo permiten
// Lo que no se puede borrar con la clave anónima queda en la base local hasta el siguiente supabase db reset
// Eso incluye los usuarios de auth.users, sus filas de profiles y user_roles, y los reportes y solicitudes de escombros
// La fila de citizens tampoco se borra si ya tiene reportes o solicitudes de escombros que la referencian
class RlsAccessLiveIntegrationTest {

    // Clientes abiertos durante la prueba para cerrarlos y limpiarlos al final
    private val openClients = mutableListOf<SupabaseClient>()

    @Before
    fun requireLocalDatabase() {
        assumeTrue("Pruebas RLS omitidas porque SUPABASE_ENV no es LOCAL", BuildConfig.SUPABASE_ENV == "LOCAL")
        val host = runCatching { URI(BuildConfig.SUPABASE_URL).host }.getOrNull()
        assumeTrue("Pruebas RLS omitidas porque la URL no es local", host in LOCAL_HOSTS)
    }

    @After
    fun cleanUp() = runBlocking {
        openClients.forEach { client ->
            // Cada paso puede fallar sin afectar a los demás porque la limpieza es de mejor esfuerzo
            val userId = client.auth.currentUserOrNull()?.id
            if (userId != null) {
                runCatching { client.postgrest.from("citizens").delete { filter { eq("profile_id", userId) } } }
                runCatching { client.auth.signOut() }
            }
            runCatching { client.close() }
        }
        openClients.clear()
    }

    @Test
    fun withoutSession_protectedTablesReturnNoRows() = runBlocking {
        val anonymous = newClient()

        PROTECTED_TABLES.forEach { table ->
            val rows = anonymous.postgrest.from(table).select().decodeList<JsonObject>()
            assertTrue("La tabla $table no debe devolver filas sin sesión", rows.isEmpty())
        }
    }

    @Test
    fun withoutSession_roleFunctionsRejectTheCall() = runBlocking {
        val anonymous = newClient()

        val myRoles = runCatching { anonymous.postgrest.rpc("get_my_roles") }
        val switchRole = runCatching {
            anonymous.postgrest.rpc("switch_active_role", buildJsonObject { put("p_role_code", "citizen") })
        }

        // El rechazo debe venir del servidor y no de un fallo de red
        assertTrue("get_my_roles debe fallar sin sesión", myRoles.exceptionOrNull() is RestException)
        assertTrue("switch_active_role debe fallar sin sesión", switchRole.exceptionOrNull() is RestException)
    }

    // Crea un cliente propio con la sesión en memoria para no compartirla con otras pruebas
    private fun newClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        httpEngine = CIO.create()
        install(Auth) {
            sessionManager = MemorySessionManager()
            codeVerifierCache = MemoryCodeVerifierCache()
            enableLifecycleCallbacks = false
            alwaysAutoRefresh = false
        }
        install(Postgrest)
    }.also { openClients.add(it) }

    private companion object {
        val LOCAL_HOSTS = setOf("127.0.0.1", "localhost", "10.0.2.2")
        val PROTECTED_TABLES = listOf("profiles", "user_roles", "citizens", "reports", "bulky_waste_requests")
    }
}
