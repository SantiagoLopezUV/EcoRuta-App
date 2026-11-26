package com.ecoruta.app.features.gamification.data.remote.dto

// DTO para el registro contable de transacciones de EcoPuntos
// Modela abonos por entrega o educación, y débitos por canje de cupones

import com.ecoruta.app.features.gamification.domain.model.PointsTransaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class EcoPointsTransactionDto(
    @SerialName("id") val id: String,
    @SerialName("reason_code") val description: String? = null,
    @SerialName("points") val delta: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

// Convierte el DTO de transacción al modelo de dominio de movimientos
fun EcoPointsTransactionDto.toDomain(): PointsTransaction = PointsTransaction(
    id = id,
    description = formatReasonCode(description),
    dateMillis = createdAt?.let { parseIsoDateToMillis(it) } ?: System.currentTimeMillis(),
    delta = delta,
)

//formateo de las variables para mostrar en la vista
private fun formatReasonCode(code: String?): String = when (code)
{
    "entrega_material" -> "Entrega de material reciclable"
    "capacitacion" -> "Capacitación ambiental completada"
    "redencion_cupon" -> "Redención de cupón en comercio aliado"
    else -> code?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Movimiento de EcoPuntos"
}

private fun parseIsoDateToMillis(isoDate: String): Long? = runCatching {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(isoDate.substringBefore("+").substringBefore("Z"))
        ?.time
}.getOrNull()
