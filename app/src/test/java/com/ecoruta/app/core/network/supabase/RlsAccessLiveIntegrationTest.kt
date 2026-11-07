package com.ecoruta.app.core.network.supabase

import com.ecoruta.app.BuildConfig
import com.ecoruta.app.features.auth.data.repository.AuthRepositoryImpl
import com.ecoruta.app.features.auth.domain.model.CitizenRegistration
import com.ecoruta.app.features.auth.domain.model.RecyclerRegistration
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
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test
    fun citizen_cannotReadAnotherUsersProfile() = runBlocking {
        val citizenA = registerCitizen()
        val citizenB = registerCitizen()

        val seenByA = citizenA.client.postgrest.from("profiles")
            .select { filter { eq("id", citizenB.id) } }.decodeList<JsonObject>()
        val seenByB = citizenB.client.postgrest.from("profiles")
            .select { filter { eq("id", citizenB.id) } }.decodeList<JsonObject>()

        assertTrue("A no debe ver el perfil de B", seenByA.isEmpty())
        assertEquals("B sí ve su propio perfil", 1, seenByB.size)
    }

    @Test
    fun citizen_cannotReadAnotherUsersRoles() = runBlocking {
        val citizenA = registerCitizen()
        val citizenB = registerCitizen()

        val seenByA = citizenA.client.postgrest.from("user_roles")
            .select { filter { eq("profile_id", citizenB.id) } }.decodeList<JsonObject>()
        val seenByB = citizenB.client.postgrest.from("user_roles")
            .select { filter { eq("profile_id", citizenB.id) } }.decodeList<JsonObject>()

        assertTrue("A no debe ver los roles de B", seenByA.isEmpty())
        assertTrue("B sí ve sus propios roles", seenByB.isNotEmpty())
    }

    @Test
    fun getMyRoles_returnsOnlyTheCallersRoles() = runBlocking {
        val citizen = registerCitizen()
        // El otro usuario es reciclador para que sus roles sean distintos a los del ciudadano
        val recycler = registerRecycler()

        val citizenRoles = myRoles(citizen.client)
        val recyclerRoles = myRoles(recycler.client)

        assertEquals(mapOf("citizen" to true), citizenRoles)
        assertEquals(setOf("citizen", "recycler"), recyclerRoles.keys)
        assertEquals("El rol activo del reciclador es el de su registro", true, recyclerRoles["recycler"])
    }

    @Test
    fun switchActiveRole_rejectsRoleNotAssigned() = runBlocking {
        val citizen = registerCitizen()

        val result = runCatching {
            citizen.client.postgrest.rpc("switch_active_role", buildJsonObject { put("p_role_code", "business") })
        }

        assertTrue("El cambio a un rol no asignado debe fallar en el servidor", result.exceptionOrNull() is RestException)
        assertEquals("El rol activo no cambia", mapOf("citizen" to true), myRoles(citizen.client))
    }

    @Test
    fun citizen_cannotReadOrModifyAnotherCitizensEcoPoints() = runBlocking {
        val citizenA = registerCitizen()
        val citizenB = registerCitizen()

        val seenByA = citizenA.client.postgrest.from("citizens")
            .select { filter { eq("profile_id", citizenB.id) } }.decodeList<JsonObject>()
        val updatedByA = citizenA.client.postgrest.from("citizens")
            .update({ set("ecopoints_balance", 999) }) {
                select()
                filter { eq("profile_id", citizenB.id) }
            }.decodeList<JsonObject>()
        val balanceOfB = citizenB.client.postgrest.from("citizens")
            .select { filter { eq("profile_id", citizenB.id) } }.decodeSingle<JsonObject>()
            .getValue("ecopoints_balance").jsonPrimitive.int

        assertTrue("A no debe ver la fila de EcoPuntos de B", seenByA.isEmpty())
        assertTrue("La actualización de A no debe tocar ninguna fila", updatedByA.isEmpty())
        assertEquals("El saldo de B sigue en cero", 0, balanceOfB)
    }

    // Registra un ciudadano nuevo con su propio cliente y deja la sesión iniciada
    private suspend fun registerCitizen(): TestUser {
        val client = newClient()
        val account = AuthRepositoryImpl(client.auth, client.postgrest).signUpCitizen(
            CitizenRegistration(
                email = uniqueEmail(),
                password = TEST_PASSWORD,
                fullName = "Ciudadano Prueba RLS",
                documentNumber = uniqueDocument(),
                phone = "3150000000",
                neighborhood = "Centro",
            ),
        ).getOrThrow()
        return TestUser(client, account.id)
    }

    // Registra un reciclador independiente nuevo, que queda pendiente de aprobación
    private suspend fun registerRecycler(): TestUser {
        val client = newClient()
        val account = AuthRepositoryImpl(client.auth, client.postgrest).signUpRecycler(
            RecyclerRegistration(
                email = uniqueEmail(),
                password = TEST_PASSWORD,
                fullName = "Reciclador Prueba RLS",
                documentNumber = uniqueDocument(),
                phone = "3150000001",
                associationName = null,
                companyId = null,
                idDocumentPdfUrl = null,
            ),
        ).getOrThrow()
        return TestUser(client, account.id)
    }

    // Devuelve los roles del usuario de la sesión con la marca de cuál está activo
    private suspend fun myRoles(client: SupabaseClient): Map<String, Boolean> =
        client.postgrest.rpc("get_my_roles").decodeList<JsonObject>().associate { row ->
            row.getValue("role_code").jsonPrimitive.content to row.getValue("is_active").jsonPrimitive.boolean
        }

    // El identificador aleatorio evita choques de correo y documento entre corridas
    private fun uniqueEmail(): String = "rls-${UUID.randomUUID()}@example.com"

    private fun uniqueDocument(): String = (1_000_000_000L + Random.nextLong(9_000_000_000L)).toString()

    private data class TestUser(val client: SupabaseClient, val id: String)

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
        const val TEST_PASSWORD = "ClavePruebaRls123*"
        val LOCAL_HOSTS = setOf("127.0.0.1", "localhost", "10.0.2.2")
        val PROTECTED_TABLES = listOf("profiles", "user_roles", "citizens", "reports", "bulky_waste_requests")
    }
}
