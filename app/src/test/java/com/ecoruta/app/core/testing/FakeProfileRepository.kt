package com.ecoruta.app.core.testing

import com.ecoruta.app.features.profile.domain.model.Profile
import com.ecoruta.app.features.profile.domain.model.UserRole
import com.ecoruta.app.features.profile.domain.model.UserRolesSummary
import com.ecoruta.app.features.profile.domain.repository.ProfileRepository

// Repositorio de perfil y roles en memoria para pruebas unitarias sin red ni dependencias externas
class FakeProfileRepository : ProfileRepository {

    // Variable para forzar errores simulados y validar cómo reacciona la interfaz ante fallos de conexión
    var error: Throwable? = null

    // Perfil mutable en memoria que simula la información guardada en la base de datos
    var currentProfile: Profile = Profile(
        id = "test-user-id",
        fullName = "Alejandro Test",
        email = "alejandro@ecoruta.co",
        documentNumber = "1118567890",
        documentType = "CC",
        activeRole = UserRole.CIUDADANO,
        approvedRoles = setOf(UserRole.CIUDADANO),
        pendingRoles = emptySet(),
        city = "Tuluá",
        ecoPoints = 250,
        photoUrl = null,
        phone = "3151234567",
    )

    // Listas en memoria para registrar las llamadas realizadas y verificar los argumentos enviados en las pruebas
    val switchedRoles = mutableListOf<UserRole>()
    val recyclerRequests = mutableListOf<RecyclerRequestRecord>()
    val businessRequests = mutableListOf<BusinessRequestRecord>()
    val uploadedAvatars = mutableListOf<ByteArray>()

    // Registro estructurado para guardar las solicitudes de reciclador recibidas
    data class RecyclerRequestRecord(
        val phone: String,
        val associationName: String?,
        val companyId: String?,
        val idDocumentPdfUrl: String?,
    )

    // Registro estructurado para guardar las solicitudes de comercios aliados recibidas
    data class BusinessRequestRecord(
        val businessName: String,
        val category: String,
        val address: String,
        val rutPdfUrl: String?,
    )

    // Envuelve la ejecución en un Result retornando el fallo simulado si la variable de error tiene un valor
    private inline fun <T> result(block: () -> T): Result<T> =
        error?.let { Result.failure(it) } ?: runCatching { block() }

    override suspend fun getProfile(): Result<Profile> = result { currentProfile }

    override suspend fun getActiveRoles(): Result<Set<UserRole>> = result {
        currentProfile.approvedRoles
    }

    override suspend fun getPendingRoles(): Result<Set<UserRole>> = result {
        currentProfile.pendingRoles
    }

    override suspend fun getUserRolesSummary(): Result<UserRolesSummary> = result {
        UserRolesSummary(
            activeRole = currentProfile.activeRole,
            approvedRoles = currentProfile.approvedRoles,
            pendingRoles = currentProfile.pendingRoles,
        )
    }

    // Cambia el rol activo verificando previamente que el usuario tenga ese rol aprobado
    override suspend fun switchActiveRole(role: UserRole): Result<Unit> = result {
        require(role in currentProfile.approvedRoles) {
            "El usuario no tiene asignado el rol ${role.name}"
        }
        switchedRoles.add(role)
        currentProfile = currentProfile.copy(activeRole = role)
    }

    // Registra la postulación de reciclador y la agrega a los roles pendientes
    override suspend fun submitRecyclerRoleRequest(
        phone: String,
        associationName: String?,
        companyId: String?,
        idDocumentPdfUrl: String?,
    ): Result<Unit> = result {
        recyclerRequests.add(
            RecyclerRequestRecord(
                phone = phone,
                associationName = associationName,
                companyId = companyId,
                idDocumentPdfUrl = idDocumentPdfUrl,
            ),
        )
        currentProfile = currentProfile.copy(
            pendingRoles = currentProfile.pendingRoles + UserRole.RECICLADOR,
        )
    }

    // Registra la postulación de comercio aliado y la agrega a los roles pendientes
    override suspend fun submitBusinessRoleRequest(
        businessName: String,
        category: String,
        address: String,
        rutPdfUrl: String?,
    ): Result<Unit> = result {
        businessRequests.add(
            BusinessRequestRecord(
                businessName = businessName,
                category = category,
                address = address,
                rutPdfUrl = rutPdfUrl,
            ),
        )
        currentProfile = currentProfile.copy(
            pendingRoles = currentProfile.pendingRoles + UserRole.COMERCIO,
        )
    }

    // Guarda los bytes del avatar simulado y actualiza la dirección de la foto en el perfil
    override suspend fun updateAvatar(
        imageBytes: ByteArray,
        extension: String,
    ): Result<String> = result {
        uploadedAvatars.add(imageBytes)
        val generatedUrl = "https://supabase.co/storage/v1/object/public/avatars/${currentProfile.id}/avatar.$extension"
        currentProfile = currentProfile.copy(photoUrl = generatedUrl)
        generatedUrl
    }
}
