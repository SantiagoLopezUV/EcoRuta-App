package com.ecoruta.app.features.profile.domain.repository

// Contrato de dominio para gestión de perfiles y conmutación de roles
// Declaración abstracta de operaciones de lectura, cambio de rol y radicación de solicitudes

import com.ecoruta.app.features.profile.domain.model.Profile
import com.ecoruta.app.features.profile.domain.model.UserRole
import com.ecoruta.app.features.profile.domain.model.UserRolesSummary

interface ProfileRepository {
    // Consulta el perfil del usuario autenticado (datos personales, rol activo, roles aprobados y pendientes)
    suspend fun getProfile(): Result<Profile>

    // Consulta los roles asignados y aprobados del usuario autenticado desde user_roles
    suspend fun getActiveRoles(): Result<Set<UserRole>>

    // Consulta los roles con solicitud de postulación pendiente de moderación (comercios o recicladores)
    suspend fun getPendingRoles(): Result<Set<UserRole>>

    // Retorna el resumen de roles: activo, aprobados y pendientes
    suspend fun getUserRolesSummary(): Result<UserRolesSummary>

    // Conmuta el rol activo en sesión ejecutando el RPC 'switch_active_role' en Supabase
    suspend fun switchActiveRole(role: UserRole): Result<Unit>

    // Radica una nueva solicitud para postularse como reciclador de oficio
    suspend fun submitRecyclerRoleRequest(
        phone: String,
        associationName: String? = null,
        companyId: String? = null,
        idDocumentPdfUrl: String? = null,
    ): Result<Unit>

    // Radica una nueva solicitud para registrar un comercio aliado
    suspend fun submitBusinessRoleRequest(
        businessName: String,
        category: String,
        address: String,
        rutPdfUrl: String? = null,
    ): Result<Unit>

    // Sube una nueva fotografía de avatar a Supabase Storage y actualiza el perfil
    suspend fun updateAvatar(imageBytes: ByteArray, extension: String = "jpg"): Result<String>
}
