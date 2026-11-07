package com.ecoruta.app.features.reports.domain.model

// Modelo de dominio puro para un reporte de afectación ambiental
// Modela evidencias fotográficas, dirección física, punto GPS y estado de moderación

enum class ReportStatus {
    PENDIENTE,
    APROBADO,
    ATENDIDO,
    RECHAZADO,
    DUPLICADO;

    // Objeto compañero para mapear el estado de moderación recibido desde la base de datos
    companion object {
        // Convierte el código de estado devuelto por Supabase al estado de dominio correspondiente
        fun fromCode(code: String?): ReportStatus = when (code?.trim()?.lowercase()) {
            "pendiente", "pending" -> PENDIENTE
            "aprobado", "approved" -> APROBADO
            "atendido", "attended" -> ATENDIDO
            "rechazado", "rejected" -> RECHAZADO
            "duplicado", "duplicate" -> DUPLICADO
            else -> PENDIENTE
        }
    }

    // Código en texto que almacena la columna status de la tabla reports en PostgreSQL
    val statusCode: String
        get() = when (this) {
            PENDIENTE -> "pendiente"
            APROBADO -> "aprobado"
            ATENDIDO -> "atendido"
            RECHAZADO -> "rechazado"
            DUPLICADO -> "duplicado"
        }
}

// Datos inmutables de un reporte ciudadano registrado en el sistema
data class CitizenReport(
    val id: String,
    val citizenId: String,
    val pointId: String? = null,
    val type: AffectationType,
    val status: ReportStatus = ReportStatus.PENDIENTE,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val area: String = "",
    val description: String = "",
    val photoUrl: String? = null, // es la imagen de portada que usa la tarjeta de listado en el estado del reporte del cuidadano
    val photoUrls: List<String> = emptyList(), //Permite adjuntar hasta 3 fotos en el reporte
    val statusNote: String? = null,
    val originalReportId: String? = null, //guarda el id del reporte original cuando el estado es DUPLICADO. cuando es null, en la interfaz no se muestra el boton de ver reporte orginal no se muestra
    val validatedBy: String? = null,
    val createdAt: String = "",
    val createdAtMillis: Long = 0L, //formatea la fecha que viene de supabase como formato ISO
    val updatedAt: String = "",
)
