package com.ecoruta.app.features.auth

import com.ecoruta.app.core.testing.FakeAuthRepository
import com.ecoruta.app.core.testing.TestDataFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Pruebas unitarias para las extensiones del repositorio de autenticación en FakeAuthRepository
// Valida verificación de código OTP, cambio de clave recuperada, enlace de correo y subida de documentos
class AuthExtensionsRepositoryTest {

    private lateinit var repository: FakeAuthRepository
    private val testUser = TestDataFactory.firstLoginAccount()

    @Before
    fun setUp() {
        repository = FakeAuthRepository()
        repository.addAccount(testUser, password = "OldPassword123*")
    }

    @Test
    fun verifyPasswordResetCode_validCode_verifiesEmail() = runBlocking {
        repository.expectedResetCode = "123456"

        val result = repository.verifyPasswordResetCode(testUser.email, "123456")

        assertTrue("La verificación debe ser exitosa con el código esperado", result.isSuccess)
        assertEquals(testUser.email, repository.verifiedResetEmail)
    }

    @Test
    fun verifyPasswordResetCode_invalidCode_returnsFailure() = runBlocking {
        repository.expectedResetCode = "123456"

        val result = repository.verifyPasswordResetCode(testUser.email, "999999")

        assertTrue("La verificación debe fallar si el código no coincide", result.isFailure)
    }

    @Test
    fun updatePassword_afterVerification_updatesStoredPassword() = runBlocking {
        repository.verifiedResetEmail = testUser.email

        val result = repository.updatePassword("NewSecurePassword123*")

        assertTrue("La actualización debe ser exitosa", result.isSuccess)
        assertEquals("NewSecurePassword123*", repository.passwordOf(testUser.email))
    }

    @Test
    fun updatePassword_withoutSessionOrVerification_fails() = runBlocking {
        repository.verifiedResetEmail = null

        val result = repository.updatePassword("NewSecurePassword123*")

        assertTrue("La actualización debe fallar sin sesión ni correo verificado", result.isFailure)
    }

    @Test
    fun signUpWithEmailLink_registersEmailRequest() = runBlocking {
        val email = "nuevo.usuario@example.com"

        val result = repository.signUpWithEmailLink(email)

        assertTrue(result.isSuccess)
        assertEquals(listOf(email), repository.emailLinkRequests)
    }

    @Test
    fun uploadRegistrationDocument_storesBytesAndReturnsUrl() = runBlocking {
        val bytes = "Fake PDF Content".toByteArray()
        val fileName = "rut_comercio.pdf"

        val result = repository.uploadRegistrationDocument(fileName, bytes)

        assertTrue(result.isSuccess)
        assertEquals(1, repository.uploadedDocuments.size)
        assertEquals(fileName, repository.uploadedDocuments.first().first)
        assertTrue(result.getOrThrow().contains("institutional-docs"))
        assertTrue(result.getOrThrow().contains(fileName))
    }

    @Test
    fun signInWithEmail_profileReadFails_returnsFailure() = runBlocking {
        repository.profileReadError = IllegalStateException("No se pudo leer el perfil")

        val result = repository.signInWithEmail(testUser.email, "OldPassword123*")

        assertTrue("El inicio de sesión debe fallar si no se puede leer el perfil", result.isFailure)
    }

    @Test
    fun signInWithEmail_profileReadable_returnsAccountWithTemporaryPasswordFlag() = runBlocking {
        val result = repository.signInWithEmail(testUser.email, "OldPassword123*")

        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrThrow())
        assertTrue(result.getOrThrow().mustChangePassword)
    }

    @Test
    fun completeRecyclerRegistration_recordsCallWhenUserAuthenticated() = runBlocking {
        repository.setSession(TestDataFactory.authenticatedSession(testUser))

        val result = repository.completeRecyclerRegistration(
            phone = "3151234567",
            associationName = "Asociación Norte",
            companyId = "company-1",
            idDocumentPdfUrl = "http://storage/cedula.pdf"
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(testUser.id), repository.completedRecyclerRegistrations)
    }
}
