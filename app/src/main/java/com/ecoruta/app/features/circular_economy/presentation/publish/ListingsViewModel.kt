package com.ecoruta.app.features.circular_economy.presentation.publish

// ViewModel para el inicio del módulo de reciclaje
// Carga el rol activo del usuario y sus publicaciones; vincula listados al negocio si el rol es COMERCIO

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing
import com.ecoruta.app.features.circular_economy.domain.repository.CircularEconomyRepository
import com.ecoruta.app.features.profile.domain.model.UserRole
import com.ecoruta.app.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListingsViewModel @Inject constructor(
    private val circularEconomyRepository: CircularEconomyRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _homeState = MutableStateFlow<RecyclingHomeUiState>(RecyclingHomeUiState.Loading)
    val homeState: StateFlow<RecyclingHomeUiState> = _homeState.asStateFlow()

    init {
        loadListings()
    }

    // Carga el perfil y las publicaciones en paralelo; expone el rol activo en el estado resultante
    fun loadListings() {
        _homeState.value = RecyclingHomeUiState.Loading
        viewModelScope.launch {
            val profileResult = profileRepository.getProfile()
            val listingsResult = circularEconomyRepository.getMyListings()

            if (profileResult.isFailure && listingsResult.isFailure) {
                _homeState.value = RecyclingHomeUiState.Error
                return@launch
            }

            val profile = profileResult.getOrNull()
            val publications = listingsResult.getOrNull()
                ?.map { it.toPresentation() }
                ?: emptyList()

            _homeState.value = RecyclingHomeUiState.Loaded(
                ecoPoints = profile?.ecoPoints,
                publications = publications,
                activeRole = profile?.activeRole ?: UserRole.CIUDADANO,
            )
        }
    }

    private fun RecyclableListing.toPresentation(): RecyclingPublication = RecyclingPublication(
        id = id,
        code = code,
        material = materialTypeFrom(material),
        otherMaterial = otherMaterial,
        volume = materialVolumeFrom(volume),
        address = address,
        status = publicationStatusFrom(status),
        createdAtMillis = createdAtMillis,
        recycler = recyclerName?.let {
            AssignedRecycler(
                name = it,
                affiliation = recyclerAffiliation.orEmpty(),
                phone = recyclerPhone,
            )
        },
        deliveredAtMillis = deliveredAtMillis,
        pointsEarned = pointsEarned,
        balanceAfter = balanceAfter,
    )

    private fun materialTypeFrom(value: String): MaterialType = when (value.uppercase()) {
        "PET" -> MaterialType.PET
        "CARDBOARD", "CARTON", "CARTÓN" -> MaterialType.CARDBOARD
        "GLASS", "VIDRIO" -> MaterialType.GLASS
        else -> MaterialType.OTHER
    }

    private fun materialVolumeFrom(value: String): MaterialVolume = when (value.uppercase()) {
        "SMALL_BAG", "BOLSA_PEQUEÑA", "BOLSA_PEQUENA" -> MaterialVolume.SMALL_BAG
        "LARGE_BAG", "BOLSA_GRANDE" -> MaterialVolume.LARGE_BAG
        "BULKY", "VOLUMINOSO" -> MaterialVolume.BULKY
        else -> MaterialVolume.SMALL_BAG
    }

    private fun publicationStatusFrom(value: String): PublicationStatus = when (value.uppercase()) {
        "DISPONIBLE", "AVAILABLE" -> PublicationStatus.DISPONIBLE
        "EN_CAMINO", "ON_THE_WAY" -> PublicationStatus.EN_CAMINO
        "ENTREGADA", "DELIVERED" -> PublicationStatus.ENTREGADA
        "CANCELADA", "CANCELLED" -> PublicationStatus.CANCELADA
        else -> PublicationStatus.DISPONIBLE
    }
}
