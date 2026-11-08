package com.ecoruta.app.features.reports.domain.model

// Modelo de dominio para tipos exclusivos de afectación
// Restringido estrictamente a: Contenedor lleno o Botadero clandestino

enum class AffectationType {
    CONTENEDOR_LLENO,
    BOTADERO_CLANDESTINO;

    // Objeto compañero para funciones estáticas de conversión del tipo de afectacion
    companion object {
        // Convierte el valor en texto devuelto por la base de datos al tipo de

        fun fromCode(code: String?): AffectationType = when (code?.trim()?.lowercase()) {
            "contenedor_lleno", "full_container" -> CONTENEDOR_LLENO
            "botadero_clandestino", "illegal_dump" -> BOTADERO_CLANDESTINO
            else -> CONTENEDOR_LLENO
        }
    }

    // Código en texto que almacena la columna type de la tabla reports en PostgreSQL
    val typeCode: String
        get() = when (this) {
            CONTENEDOR_LLENO -> "contenedor_lleno"
            BOTADERO_CLANDESTINO -> "botadero_clandestino"
        }
}
