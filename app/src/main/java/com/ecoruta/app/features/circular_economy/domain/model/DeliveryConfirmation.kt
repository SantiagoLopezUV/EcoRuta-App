package com.ecoruta.app.features.circular_economy.domain.model

// Modelo de dominio para el resultado de validación de entrega
// Estado de acreditación de EcoPuntos, vigencia de QR (< 5 min) y geocerca (< 100 m)

// Token de entrega de un solo uso válido por 5 minutos para confirmar presencialmente
data class DeliveryToken(
    val token: String,
    val expiresAtMillis: Long,
)
