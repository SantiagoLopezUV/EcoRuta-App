package com.ecoruta.app.features.circular_economy.presentation.scanner

// Estado inmutable del escáner QR y validación presencial
// Modela estado de cámara, resultado de decodificación, validación GPS y acreditación

import com.ecoruta.app.features.circular_economy.presentation.deliveries_map.RecyclerDelivery

// Categorías de volumen que el reciclador puede confirmar al recibir el material
enum class DeliveredVolume {
    SMALL_BAG,
    LARGE_BAG,
    SACK,
}

// Motivos por los que una entrega escaneada no se puede completar
enum class ScanFailureReason {
    EXPIRED_CODE,
    USED_CODE,
    INVALID_LOCATION,
}

// Resultado de la verificación del código QR y la ubicación del reciclador
sealed interface ScanResult {
    data object Completed : ScanResult
    data class Failed(val reasons: List<ScanFailureReason> = ScanFailureReason.entries) : ScanResult
}

// Estado visual del flujo de escaneo: entrega en verificación, volumen elegido y resultado
data class RecyclerScannerUiState(
    val delivery: RecyclerDelivery? = null,
    // Contenido del código QR leído por la cámara, pendiente de validar en la capa de lógica
    val scannedCode: String? = null,
    val selectedVolume: DeliveredVolume = DeliveredVolume.LARGE_BAG,
    val result: ScanResult = ScanResult.Completed,
)
