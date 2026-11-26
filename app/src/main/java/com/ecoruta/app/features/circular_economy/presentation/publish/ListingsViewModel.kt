package com.ecoruta.app.features.circular_economy.presentation.publish

// ViewModel para el inicio del módulo de reciclaje
// Carga el rol activo y el businessId del comercio; adjunta el negocio a publicaciones en modo COMERCIO

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
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

    private val _publishState = MutableStateFlow(PublishMaterialUiState())
    val publishState: StateFlow<PublishMaterialUiState> = _publishState.asStateFlow()

    init { loadListings() }

    // Carga perfil y publicaciones en paralelo; expone rol activo y businessId en el estado
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

            val businessId = if (profile?.activeRole == UserRole.COMERCIO) profile.businessId else null
            _homeState.value = RecyclingHomeUiState.Loaded(
                ecoPoints = profile?.ecoPoints,
                publications = publications,
                activeRole = profile?.activeRole ?: UserRole.CIUDADANO,
                businessId = businessId,
            )
            // Pre-carga el businessId en el formulario de publicación si el rol es COMERCIO
            _publishState.value = PublishMaterialUiState(businessId = businessId)
        }
    }

    fun onMaterialSelected(material: MaterialType) = _publishState.value.let {
        _publishState.value = it.copy(material = material, otherMaterial = "")
    }

    fun onOtherMaterialChange(value: String) = _publishState.value.let {
        _publishState.value = it.copy(otherMaterial = value, otherMaterialError = null)
    }

    fun onVolumeSelected(volume: MaterialVolume) = _publishState.value.let {
        _publishState.value = it.copy(volume = volume, volumeError = null)
    }

    fun onAddressChange(value: String) = _publishState.value.let {
        _publishState.value = it.copy(address = value, addressError = null)
    }

    fun onLocationChange(latitude: Double, longitude: Double) = _publishState.value.let {
        _publishState.value = it.copy(latitude = latitude, longitude = longitude, locationError = null)
    }

    // Valida el formulario y llama al repositorio; el businessId se adjunta si el rol es COMERCIO
    fun onPublish() {
        val form = _publishState.value
        var hasError = false

        if (form.material == MaterialType.OTHER && form.otherMaterial.isBlank()) {
            _publishState.value = form.copy(otherMaterialError = R.string.recycling_other_required)
            hasError = true
        }
        if (form.volume == null) {
            _publishState.value = _publishState.value.copy(volumeError = R.string.recycling_volume_required)
            hasError = true
        }
        if (form.address.isBlank()) {
            _publishState.value = _publishState.value.copy(addressError = R.string.recycling_address_required)
            hasError = true
        }
        if (form.latitude == null || form.longitude == null) {
            _publishState.value = _publishState.value.copy(locationError = R.string.recycling_location_required)
            hasError = true
        }
        if (hasError) return

        _publishState.value = _publishState.value.copy(isPublishing = true)
        viewModelScope.launch {
            val result = circularEconomyRepository.createListing(
                materialType = if (form.material == MaterialType.OTHER) form.otherMaterial else form.material.name,
                quantityCategory = form.volume!!.name,
                latitude = form.latitude!!,
                longitude = form.longitude!!,
                address = form.address,
                otherMaterial = if (form.material == MaterialType.OTHER) form.otherMaterial else null,
            )
            _publishState.value = _publishState.value.copy(isPublishing = false)
            if (result.isSuccess) {
                _publishState.value = PublishMaterialUiState(businessId = form.businessId)
                loadListings()
            }
        }
    }

    private fun RecyclableListing.toPresentation(): RecyclingPublication = RecyclingPublication(
        id = id, code = code,
        material = materialTypeFrom(material),
        otherMaterial = otherMaterial,
        volume = materialVolumeFrom(volume),
        address = address,
        status = publicationStatusFrom(status),
        createdAtMillis = createdAtMillis,
        recycler = recyclerName?.let {
            AssignedRecycler(it, recyclerAffiliation.orEmpty(), recyclerPhone)
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
