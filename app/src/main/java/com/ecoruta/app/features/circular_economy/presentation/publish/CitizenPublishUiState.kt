package com.ecoruta.app.features.circular_economy.presentation.publish

// Estado inmutable de la publicación de reciclaje
// Modela campos del formulario, código QR generado, tiempo restante y errores

import android.net.Uri
import androidx.annotation.StringRes

enum class MaterialType {
    PET,
    CARDBOARD,
    GLASS,
    OTHER,
}

enum class MaterialVolume {
    SMALL_BAG,
    LARGE_BAG,
    BULKY,
}

enum class PublicationStatus {
    DISPONIBLE,
    EN_CAMINO,
    ENTREGADA,
    CANCELADA,
}

data class AssignedRecycler(
    val name: String,
    val affiliation: String,
    val phone: String? = null,
)

data class RecyclingPublication(
    val id: String,
    val code: String,
    val material: MaterialType,
    val otherMaterial: String? = null,
    val volume: MaterialVolume,
    val address: String,
    val area: String = "Barrio La Merced",
    val status: PublicationStatus,
    val createdAtMillis: Long,
    val recycler: AssignedRecycler? = null,
    val deliveredAtMillis: Long? = null,
    val pointsEarned: Int? = null,
    val balanceAfter: Int? = null,
)

data class DeliveryCode(
    val token: String,
    val expiresAtMillis: Long,
) {
    companion object {
        const val VALIDITY_MS = 5 * 60_000L
    }
}

sealed interface RecyclingHomeUiState {
    data object Loading : RecyclingHomeUiState
    data class Loaded(
        val ecoPoints: Int?,
        val publications: List<RecyclingPublication>,
        val isSample: Boolean = false,
    ) : RecyclingHomeUiState
    data object Error : RecyclingHomeUiState
}

data class PublishMaterialUiState(
    val material: MaterialType = MaterialType.PET,
    val otherMaterial: String = "",
    @StringRes val otherMaterialError: Int? = null,
    val volume: MaterialVolume? = null,
    @StringRes val volumeError: Int? = null,
    val address: String = "",
    @StringRes val addressError: Int? = null,
    val photo: Uri? = null,
    val isPublishing: Boolean = false,
)

typealias CitizenPublishUiState = PublishMaterialUiState

sealed interface PublicationUiState {
    data object Loading : PublicationUiState
    data class Loaded(val publication: RecyclingPublication, val isSample: Boolean = false) : PublicationUiState
    data object NotFound : PublicationUiState
    data object Error : PublicationUiState
}

data class DeliveryCodeUiState(
    val isLoading: Boolean = true,
    val publication: RecyclingPublication? = null,
    val code: DeliveryCode? = null,
    val isSample: Boolean = false,
    @StringRes val error: Int? = null,
)
