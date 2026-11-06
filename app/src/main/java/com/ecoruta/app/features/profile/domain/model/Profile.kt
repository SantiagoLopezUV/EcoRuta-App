package com.ecoruta.app.features.profile.domain.model

// Modelo de dominio limpio para el perfil del usuario
// Información del ciudadano u operador, avatar, roles activos y balance de EcoPuntos

data class Profile(
    val id: String,
    val fullName: String,
    val email: String,
    val documentNumber: String = "",
    val documentType: String = "CC",
    val activeRole: UserRole = UserRole.CIUDADANO,
    val approvedRoles: Set<UserRole> = setOf(activeRole),
    val pendingRoles: Set<UserRole> = emptySet(),
    val city: String = "Tuluá",
    val ecoPoints: Int = 0,
    val photoUrl: String? = null,
    val phone: String? = null,
)

// Resumen ligero de roles de un usuario para operaciones de conmutación
data class UserRolesSummary(
    val activeRole: UserRole,
    val approvedRoles: Set<UserRole> = setOf(activeRole),
    val pendingRoles: Set<UserRole> = emptySet(),
)
