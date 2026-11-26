package com.ecoruta.app.features.gamification.domain.model

// Modelo de dominio para el balance contable de EcoPuntos
// Saldo actual disponible, movimientos históricos y resumen de abonos y débitos

data class EcoPointsLedger(
    // Saldo total disponible del ciudadano en EcoPuntos
    val balance: Int,
    // Historial de movimientos ordenado del más reciente al más antiguo
    val movements: List<PointsTransaction>,
) {
    // Suma neta de abonos (entregas, educación, bonificaciones)
    val totalCredits: Int get() = movements.filter { it.delta > 0 }.sumOf { it.delta }

    // Suma neta de débitos (canjes de cupones)
    val totalDebits: Int get() = movements.filter { it.delta < 0 }.sumOf { it.delta }
}

data class PointsTransaction(
    val id: String,
    // Descripción legible en español de la operación (ej: "Entrega de material reciclable")
    val description: String,
    val dateMillis: Long,
    // Delta positivo = abono; negativo = débito
    val delta: Int,
)
