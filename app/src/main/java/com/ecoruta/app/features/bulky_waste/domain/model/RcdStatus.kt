package com.ecoruta.app.features.bulky_waste.domain.model

// Modelos de dominio para estados y tipos del ciclo de retiro de escombros y voluminosos
// Mapea entre los códigos oficiales de la base de datos y la capa de presentación

import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisStatus
import com.ecoruta.app.features.bulky_waste.presentation.schedule.DebrisType

fun DebrisType.toDbCode(): String = when (this) {
    DebrisType.FURNITURE -> "muebles"
    DebrisType.MATTRESSES -> "colchones"
    DebrisType.RENOVATION_DEBRIS -> "escombros"
}

fun debrisTypeFromDb(code: String): DebrisType = when (code.lowercase().trim()) {
    "muebles", "furniture" -> DebrisType.FURNITURE
    "colchones", "colchon", "mattresses" -> DebrisType.MATTRESSES
    else -> DebrisType.RENOVATION_DEBRIS
}

fun DebrisStatus.toDbCode(): String = when (this) {
    DebrisStatus.PENDIENTE -> "pendiente"
    DebrisStatus.COTIZADO -> "cotizado"
    DebrisStatus.PROGRAMADO -> "programado"
    DebrisStatus.RECOLECTADO -> "recolectado"
    DebrisStatus.CANCELADO -> "cancelado"
}

fun debrisStatusFromDb(code: String): DebrisStatus = when (code.lowercase().trim()) {
    "pendiente" -> DebrisStatus.PENDIENTE
    "cotizado" -> DebrisStatus.COTIZADO
    "programado" -> DebrisStatus.PROGRAMADO
    "recolectado" -> DebrisStatus.RECOLECTADO
    "cancelado" -> DebrisStatus.CANCELADO
    else -> DebrisStatus.PENDIENTE
}
