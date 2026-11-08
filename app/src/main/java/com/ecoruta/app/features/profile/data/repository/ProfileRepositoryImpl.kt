package com.ecoruta.app.features.profile.data.repository

// Implementación del repositorio de perfil y roles
// Consulta perfiles, ejecuta RPC switch_active_role y sube avatar a Storage

import com.ecoruta.app.features.profile.data.remote.dto.BusinessApplicationDto
import com.ecoruta.app.features.profile.data.remote.dto.CitizenDataDto
import com.ecoruta.app.features.profile.data.remote.dto.PendingRoleItemDto
import com.ecoruta.app.features.profile.data.remote.dto.ProfileDto
import com.ecoruta.app.features.profile.data.remote.dto.RecyclerApplicationDto
import com.ecoruta.app.features.profile.data.remote.dto.UserRoleItemDto
import com.ecoruta.app.features.profile.domain.model.Profile
import com.ecoruta.app.features.profile.domain.model.UserRole
import com.ecoruta.app.features.profile.domain.model.UserRolesSummary
import com.ecoruta.app.features.profile.domain.repository.ProfileRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val storage: Storage,
) : ProfileRepository {

    // Consulta los datos del perfil activo combinando la información base y complementaria del usuario
    override suspend fun getProfile(): Result<Profile> = runCatching {
        // Obtiene el identificador del usuario autenticado en la sesión actual
        val user = auth.currentUserOrNull()
            ?: error("No existe una sesión activa para consultar el perfil")

        // Consulta los datos de identidad registrados en la tabla profiles
        val profileDto = postgrest.from("profiles")
            .select {
                filter { eq("id", user.id) }
            }
            .decodeSingleOrNull<ProfileDto>()
            ?: error("No se encontró el registro de perfil en la base de datos")

        // Consulta los datos de ciudadano como barrio y balance de puntos si existen
        val citizenDto = try {
            postgrest.from("citizens")
                .select {
                    filter { eq("profile_id", user.id) }
                }
                .decodeSingleOrNull<CitizenDataDto>()
        } catch (_: Exception) {
            null
        }

        // Obtiene los roles aprobados y los que se encuentran pendientes de moderación
        val approvedRoles = getActiveRoles().getOrDefault(setOf(UserRole.CIUDADANO))
        val pendingRoles = getPendingRoles().getOrDefault(emptySet())

        // Determina el rol activo prefiriendo active_role y usando role_code como respaldo
        val rawActive = profileDto.activeRole ?: profileDto.roleCode ?: "citizen"
        val activeRole = UserRole.fromCode(rawActive)

        // Construye el modelo de dominio unificando la información de las tablas
        Profile(
            id = profileDto.id,
            fullName = profileDto.fullName ?: "Usuario",
            email = profileDto.email ?: user.email.orEmpty(),
            documentNumber = profileDto.documentId.orEmpty(),
            documentType = profileDto.documentType ?: "CC",
            activeRole = activeRole,
            approvedRoles = if (approvedRoles.isEmpty()) setOf(activeRole) else approvedRoles,
            pendingRoles = pendingRoles,
            // Asigna el barrio registrado o la ciudad sede por defecto para la visualización
            city = citizenDto?.neighborhood?.takeIf { it.isNotBlank() } ?: "Tuluá",
            ecoPoints = citizenDto?.ecopointsBalance ?: 0,
            photoUrl = profileDto.avatarUrl,
            phone = citizenDto?.phone ?: profileDto.phone,
        )
    }

    // Consulta todos los roles asignados y aprobados del usuario en la tabla user_roles
    override suspend fun getActiveRoles(): Result<Set<UserRole>> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar roles")

        // Obtiene la lista de roles asignados desde user_roles
        val userRoles = postgrest.from("user_roles")
            .select {
                filter { eq("profile_id", userId) }
            }
            .decodeList<UserRoleItemDto>()

        val result = userRoles.map { UserRole.fromCode(it.roleCode) }.toMutableSet()

        // Si la tabla user_roles no tiene registros aún, toma el rol principal registrado en profiles
        if (result.isEmpty()) {
            val profile = postgrest.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()
            val baseRole = profile?.activeRole ?: profile?.roleCode
            if (!baseRole.isNullOrBlank()) {
                result.add(UserRole.fromCode(baseRole))
            } else {
                result.add(UserRole.CIUDADANO)
            }
        }
        // Todo usuario registrado en el sistema cuenta con el rol base de ciudadano
        result.add(UserRole.CIUDADANO)
        result
    }

    // Consulta si el usuario tiene postulaciones pendientes de aprobación en comercios o recicladores
    override suspend fun getPendingRoles(): Result<Set<UserRole>> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para consultar solicitudes pendientes")

        val pending = mutableSetOf<UserRole>()

        // Verifica si tiene una postulación de comercio aliado en estado pendiente
        val pendingBusinesses = try {
            postgrest.from("businesses")
                .select {
                    filter {
                        eq("profile_id", userId)
                        eq("status", "pendiente")
                    }
                }
                .decodeList<PendingRoleItemDto>()
        } catch (_: Exception) {
            emptyList()
        }

        // Verifica si tiene una postulación de reciclador en estado pendiente
        val pendingRecyclers = try {
            postgrest.from("recyclers")
                .select {
                    filter {
                        eq("profile_id", userId)
                        eq("status", "pendiente")
                    }
                }
                .decodeList<PendingRoleItemDto>()
        } catch (_: Exception) {
            emptyList()
        }

        if (pendingBusinesses.isNotEmpty()) pending.add(UserRole.COMERCIO)
        if (pendingRecyclers.isNotEmpty()) pending.add(UserRole.RECICLADOR)

        pending
    }

    // Obtiene un resumen completo con el rol activo, los aprobados y los pendientes
    override suspend fun getUserRolesSummary(): Result<UserRolesSummary> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa")

        val profile = postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<ProfileDto>()

        val active = UserRole.fromCode(profile?.activeRole ?: profile?.roleCode ?: "citizen")
        val approved = getActiveRoles().getOrDefault(setOf(active))
        val pending = getPendingRoles().getOrDefault(emptySet())

        UserRolesSummary(
            activeRole = active,
            approvedRoles = approved,
            pendingRoles = pending,
        )
    }

    // Conmuta el rol activo en sesión ejecutando la función RPC en Supabase
    override suspend fun switchActiveRole(role: UserRole): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para conmutar de rol")

        // Busca el código textual que coincide con el rol solicitado en los roles del usuario
        val userRoles = postgrest.from("user_roles")
            .select { filter { eq("profile_id", userId) } }
            .decodeList<UserRoleItemDto>()

        val matchingRoleCode = userRoles.firstOrNull { UserRole.fromCode(it.roleCode) == role }?.roleCode
            ?: role.roleCode

        try {
            // Invoca la función almacenada switch_active_role en la base de datos
            postgrest.rpc(
                function = "switch_active_role",
                parameters = buildJsonObject {
                    put("p_role_code", matchingRoleCode)
                },
            )
        } catch (_: Exception) {
            // Sincronización directa en tabla profiles si la función RPC no está disponible
            postgrest.from("profiles").update(
                mapOf(
                    "active_role" to matchingRoleCode,
                    "role_code" to matchingRoleCode,
                ),
            ) {
                filter { eq("id", userId) }
            }
        }
    }

    // Inserta la solicitud de postulación en la tabla recyclers con estado inicial pendiente
    override suspend fun submitRecyclerRoleRequest(
        phone: String,
        associationName: String?,
        companyId: String?,
        idDocumentPdfUrl: String?,
    ): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para radicar la postulación")

        postgrest.from("recyclers").insert(
            RecyclerApplicationDto(
                profileId = userId,
                phone = phone,
                associationName = associationName,
                companyId = companyId,
                idDocumentPdfUrl = idDocumentPdfUrl,
                status = "pendiente",
            ),
        )
    }

    // Inserta los datos del establecimiento en la tabla businesses con estado inicial pendiente
    override suspend fun submitBusinessRoleRequest(
        businessName: String,
        category: String,
        address: String,
        rutPdfUrl: String?,
    ): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para radicar la postulación")

        postgrest.from("businesses").insert(
            BusinessApplicationDto(
                profileId = userId,
                businessName = businessName,
                category = category,
                address = address,
                rutPdfUrl = rutPdfUrl,
                status = "pendiente",
            ),
        )
    }

    // Sube el archivo de imagen al bucket de avatares en Supabase Storage y actualiza la URL en el perfil
    override suspend fun updateAvatar(
        imageBytes: ByteArray,
        extension: String,
    ): Result<String> = runCatching {
        val userId = auth.currentUserOrNull()?.id
            ?: error("No existe una sesión activa para actualizar el avatar")

        // Genera una ruta única para la imagen del usuario utilizando su identificador y la marca de tiempo
        val fileName = "${userId}_${System.currentTimeMillis()}.$extension"
        val path = "$userId/$fileName"

        val bucket = storage.from("avatars")
        bucket.upload(path, imageBytes) {
            upsert = true
        }

        // Obtiene la dirección pública para almacenar la referencia en el perfil
        val publicUrl = bucket.publicUrl(path)

        postgrest.from("profiles").update(
            mapOf("avatar_url" to publicUrl),
        ) {
            filter { eq("id", userId) }
        }

        publicUrl
    }
}
