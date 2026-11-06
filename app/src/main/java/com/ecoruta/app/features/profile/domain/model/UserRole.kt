package com.ecoruta.app.features.profile.domain.model

// Modelo de dominio para roles del sistema y estados de postulación
// Representa los roles canónicos del ecosistema EcoRuta

enum class UserRole {
    CIUDADANO,
    RECICLADOR,
    CHOFER,
    COMERCIO;

    // Objeto compañero para funciones estáticas asociadas al enum
    companion object {
        // Convierte el texto que devuelve la base de datos al rol correspondiente
        // Si el texto viene vacío o no coincide, asigna el rol de ciudadano por defecto
        fun fromCode(code: String?): UserRole = when (code?.trim()?.lowercase()) {
            "ciudadano", "citizen" -> CIUDADANO
            "reciclador", "recycler" -> RECICLADOR
            "chofer", "conductor", "driver", "operator" -> CHOFER
            "comercio", "business" -> COMERCIO
            else -> CIUDADANO
        }
    }

    // Código en texto que espera la base de datos para identificar cada rol
    val roleCode: String
        get() = when (this) {
            CIUDADANO -> "citizen"
            RECICLADOR -> "recycler"
            CHOFER -> "operator"
            COMERCIO -> "business"
        }
}
