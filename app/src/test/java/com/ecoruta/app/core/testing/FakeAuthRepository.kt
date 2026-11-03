package com.ecoruta.app.core.testing

import com.ecoruta.app.features.auth.domain.model.AuthSession
import com.ecoruta.app.features.auth.domain.model.BusinessRegistration
import com.ecoruta.app.features.auth.domain.model.CitizenRegistration
import com.ecoruta.app.features.auth.domain.model.RecyclerRegistration
import com.ecoruta.app.features.auth.domain.model.UserAccount
import com.ecoruta.app.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

// Repositorio de autenticación en memoria para pruebas unitarias sin red ni base de datos
// Guarda las cuentas en un mapa y expone la sesión como un flujo que las pruebas pueden observar
class FakeAuthRepository : AuthRepository {

    // Cuando tiene un valor, todas las operaciones que devuelven Result fallan con ese error
    var error: Throwable? = null

    // Cuenta que se usa al entrar o registrarse con Google
    var googleAccount: UserAccount = TestDataFactory.googleAccount()

    // Registros de las llamadas para que las pruebas comprueben qué se envió
    val passwordResetEmails = mutableListOf<String>()
    val recyclerRoleRequests = mutableListOf<String>()
    val businessRoleRequests = mutableListOf<String>()

    private val accounts = mutableMapOf<String, StoredAccount>()
    private val session = MutableStateFlow<AuthSession>(AuthSession.Unauthenticated)

    private data class StoredAccount(val user: UserAccount, val password: String)

    // Agrega una cuenta existente antes de la prueba sin iniciar sesión
    fun addAccount(user: UserAccount, password: String = TestDataFactory.DEFAULT_PASSWORD) {
        accounts[user.email] = StoredAccount(user, password)
    }

    // Cambia la sesión directamente para simular estados como la carga inicial
    fun setSession(value: AuthSession) {
        session.value = value
    }

    // Devuelve la clave guardada de una cuenta para verificar cambios de contraseña
    fun passwordOf(email: String): String? = accounts[email]?.password

    override suspend fun signUpWithEmail(email: String, password: String): Result<UserAccount> =
        result { register(email, password, fullName = null) }

    override suspend fun signInWithEmail(email: String, password: String): Result<UserAccount> =
        result {
            val stored = accounts[email]
            require(stored != null && stored.password == password) { "Credenciales inválidas" }
            startSession(stored.user)
        }

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String?): Result<UserAccount> =
        result { googleSession(idToken) }

    override suspend fun signUpWithGoogle(idToken: String, rawNonce: String?): Result<UserAccount> =
        result { googleSession(idToken) }

    override suspend fun signOut(): Result<Unit> =
        result { session.value = AuthSession.Unauthenticated }

    override suspend fun getCurrentUser(): UserAccount? =
        (session.value as? AuthSession.Authenticated)?.user

    override fun observeAuthState(): Flow<AuthSession> = session

    // Igual que el servicio real, responde con éxito aunque el correo no esté registrado
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
        result { passwordResetEmails += email }

    // Guarda la nueva clave y apaga la obligación de cambiarla
    override suspend fun updateFirstLoginPassword(newPassword: String): Result<Unit> =
        result {
            val user = requireUser()
            val updated = user.copy(mustChangePassword = false)
            accounts[user.email] = StoredAccount(updated, newPassword)
            session.value = AuthSession.Authenticated(updated)
        }

    override suspend fun signUpCitizen(registration: CitizenRegistration): Result<UserAccount> =
        result { register(registration.email, registration.password, registration.fullName) }

    override suspend fun signUpRecycler(registration: RecyclerRegistration): Result<UserAccount> =
        result { register(registration.email, registration.password, registration.fullName) }

    override suspend fun signUpBusiness(registration: BusinessRegistration): Result<UserAccount> =
        result { register(registration.email, registration.password, registration.fullName) }

    // Solo un usuario con sesión puede postularse a otro rol
    override suspend fun submitRecyclerRoleRequest(
        phone: String,
        associationName: String?,
        companyId: String?,
        idDocumentPdfUrl: String?
    ): Result<Unit> = result { recyclerRoleRequests += requireUser().id }

    override suspend fun submitBusinessRoleRequest(
        businessName: String,
        category: String,
        address: String,
        rutPdfUrl: String?
    ): Result<Unit> = result { businessRoleRequests += requireUser().id }

    // Aplica el error configurado o ejecuta la operación capturando sus fallos
    private inline fun <T> result(block: () -> T): Result<T> =
        error?.let { Result.failure(it) } ?: runCatching(block)

    // Crea la cuenta si el correo está libre y deja la sesión iniciada
    private fun register(email: String, password: String, fullName: String?): UserAccount {
        check(email !in accounts) { "El correo ya está registrado" }
        val user = UserAccount(id = "user-${accounts.size + 1}", email = email, fullName = fullName)
        accounts[email] = StoredAccount(user, password)
        return startSession(user)
    }

    private fun googleSession(idToken: String): UserAccount {
        require(idToken.isNotBlank()) { "Token de Google vacío" }
        return startSession(googleAccount)
    }

    private fun startSession(user: UserAccount): UserAccount {
        session.value = AuthSession.Authenticated(user)
        return user
    }

    private fun requireUser(): UserAccount =
        checkNotNull((session.value as? AuthSession.Authenticated)?.user) { "No hay sesión activa" }
}
