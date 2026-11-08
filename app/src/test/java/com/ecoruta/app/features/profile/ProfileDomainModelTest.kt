package com.ecoruta.app.features.profile

import com.ecoruta.app.core.testing.FakeProfileRepository
import com.ecoruta.app.features.profile.domain.model.Profile
import com.ecoruta.app.features.profile.domain.model.UserRole
import com.ecoruta.app.features.profile.presentation.toUserProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas para la capa de dominio y repositorio de profiles
class ProfileDomainModelTest {

    // Valida que el mapeo de códigos textuales desde Supabase hacia el enum UserRole sea insensible a mayúsculas
    @Test
    fun testUserRoleFromCodeMapping() {
        // Verifica que cadenas en ingles o mayúsculas se conviertan al rol de ciudadano
        assertEquals(UserRole.CIUDADANO, UserRole.fromCode("citizen"))
        assertEquals(UserRole.CIUDADANO, UserRole.fromCode("CIUDADANO"))
        // Comprueba que valores nulos o desconocidos no rompan la app y asignen ciudadano por defecto
        assertEquals(UserRole.CIUDADANO, UserRole.fromCode(null))
        assertEquals(UserRole.CIUDADANO, UserRole.fromCode("unknown_role"))

        // Verifica las variantes de texto para el rol de reciclador
        assertEquals(UserRole.RECICLADOR, UserRole.fromCode("recycler"))
        assertEquals(UserRole.RECICLADOR, UserRole.fromCode("reciclador"))

        // Verifica las distintas denominaciones operativas para el rol de chofer
        assertEquals(UserRole.CHOFER, UserRole.fromCode("operator"))
        assertEquals(UserRole.CHOFER, UserRole.fromCode("chofer"))
        assertEquals(UserRole.CHOFER, UserRole.fromCode("driver"))

        // Verifica las opciones de texto para el rol de comercio aliado
        assertEquals(UserRole.COMERCIO, UserRole.fromCode("business"))
        assertEquals(UserRole.COMERCIO, UserRole.fromCode("comercio"))
    }

    // Valida que el código devuelto para el backend corresponda al esperado por PostgREST
    @Test
    fun testUserRoleCodeConstants() {
        // Comprueba que cada enum retorne exactamente la cadena en minúsculas requerida por la base de datos
        assertEquals("citizen", UserRole.CIUDADANO.roleCode)
        assertEquals("recycler", UserRole.RECICLADOR.roleCode)
        assertEquals("operator", UserRole.CHOFER.roleCode)
        assertEquals("business", UserRole.COMERCIO.roleCode)
    }

    // Valida la conversión del modelo de dominio hacia el UserProfile de la capa de presentación
    @Test
    fun testDomainProfileToPresentationUserProfile() {
        // Crea una instancia del modelo puro de dominio con datos de prueba completos
        val domainProfile = Profile(
            id = "user-abc-123",
            fullName = "Laura Gómez",
            email = "laura@ecoruta.co",
            documentNumber = "1118999888",
            activeRole = UserRole.CIUDADANO,
            approvedRoles = setOf(UserRole.CIUDADANO, UserRole.RECICLADOR),
            pendingRoles = setOf(UserRole.COMERCIO),
            city = "Tuluá - Las Delicias",
            ecoPoints = 850,
            photoUrl = "https://ecoruta.co/avatars/user.jpg",
            phone = "3159998877",
        )

        // Ejecuta la función de extensión que traduce el perfil a la estructura usada por la vista
        val presentationProfile = domainProfile.toUserProfile()

        // Comprueba que los datos básicos de identidad coincidan
        assertEquals("Laura Gómez", presentationProfile.fullName)
        assertEquals(UserRole.CIUDADANO, presentationProfile.role)
        assertEquals("Tuluá - Las Delicias", presentationProfile.city)
        assertEquals(850, presentationProfile.ecoPoints)
        // Comprueba que la lista de roles aprobados y pendientes no pierda elementos en la conversión
        assertTrue(presentationProfile.approvedRoles.contains(UserRole.RECICLADOR))
        assertTrue(presentationProfile.pendingRoles.contains(UserRole.COMERCIO))
    }

    // Valida la conmutación de rol activo y el registro de solicitudes en el repositorio
    @Test
    fun testFakeProfileRepositoryOperations() = runBlocking {
        // Instancia el repositorio falso en memoria para simular el comportamiento sin red
        val repo = FakeProfileRepository()

        //Perfil inicial
        // Comprueba que el usuario inicie con el rol base de ciudadano
        val initialProfile = repo.getProfile().getOrThrow()
        assertEquals(UserRole.CIUDADANO, initialProfile.activeRole)

        //Cambiar a un rol no aprobado debe fallar
        // Intenta cambiar a un rol no asignado y valida que el resultado sea de fallo
        val switchFail = repo.switchActiveRole(UserRole.RECICLADOR)
        assertTrue(switchFail.isFailure)

        //Radicar postulación a Reciclador
        // Envía la solicitud y comprueba que se registre y quede en la lista de pendientes
        val submitRecycler = repo.submitRecyclerRoleRequest(
            phone = "3151112233",
            associationName = "Asociación Valle Verde",
        )
        assertTrue(submitRecycler.isSuccess)
        assertEquals(1, repo.recyclerRequests.size)
        assertTrue(repo.getPendingRoles().getOrThrow().contains(UserRole.RECICLADOR))

        //Radicar postulación a Comercio
        // Envía los datos del negocio y valida que se agregue como solicitud pendiente
        val submitBusiness = repo.submitBusinessRoleRequest(
            businessName = "Tienda La Esperanza",
            category = "Minimercado",
            address = "Calle 26 # 20-10",
        )
        assertTrue(submitBusiness.isSuccess)
        assertEquals(1, repo.businessRequests.size)
        assertTrue(repo.getPendingRoles().getOrThrow().contains(UserRole.COMERCIO))

        //Simular aprobación administrativa de rol reciclador
        // Actualiza el estado simulando que un administrador aprobó la postulación
        repo.currentProfile = repo.currentProfile.copy(
            approvedRoles = setOf(UserRole.CIUDADANO, UserRole.RECICLADOR),
            pendingRoles = emptySet(),
        )

        // Ahora el cambio de rol debe ser exitoso y reflejarse como rol activo
        val switchSuccess = repo.switchActiveRole(UserRole.RECICLADOR)
        assertTrue(switchSuccess.isSuccess)
        assertEquals(UserRole.RECICLADOR, repo.getProfile().getOrThrow().activeRole)
        assertEquals(listOf(UserRole.RECICLADOR), repo.switchedRoles)

        //Actualización de avatar
        // Simula la subida de una imagen en bytes y comprueba que se actualice la dirección del avatar
        val avatarBytes = byteArrayOf(1, 2, 3, 4, 5)
        val updateAvatarResult = repo.updateAvatar(avatarBytes, "png")
        assertTrue(updateAvatarResult.isSuccess)
        assertEquals(1, repo.uploadedAvatars.size)
        assertTrue(repo.getProfile().getOrThrow().photoUrl?.contains("avatar.png") == true)
    }
}
