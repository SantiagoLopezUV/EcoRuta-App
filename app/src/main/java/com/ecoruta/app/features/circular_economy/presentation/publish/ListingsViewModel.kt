package com.ecoruta.app.features.circular_economy.presentation.publish

// ViewModel para el inicio del módulo de reciclaje
// Carga el rol activo del usuario y sus publicaciones; vincula listados al negocio si el rol es COMERCIO

import android.net.Uri
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

    private val _isCancelling = MutableStateFlow(false)
    val isCancelling: StateFlow<Boolean> = _isCancelling.asStateFlow()

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
            val isBusiness = profile?.activeRole == UserRole.COMERCIO
            // Vincula las publicaciones al negocio si el usuario activo tiene rol de comercio
            val publications = listingsResult.getOrNull()
                ?.map { listing ->
                    listing.copy(
                        isBusinessListing = isBusiness,
                        businessId = if (isBusiness) profile?.id else null,
                    ).toPresentation()
                }
                ?: emptyList()

            _homeState.value = RecyclingHomeUiState.Loaded(
                ecoPoints = profile?.ecoPoints,
                publications = publications,
                activeRole = profile?.activeRole ?: UserRole.CIUDADANO,
            )
        }
    }

    fun onMaterialSelected(material: MaterialType) {
        _publishState.value = _publishState.value.copy(
            material = material,
            otherMaterial = if (material != MaterialType.OTRO) "" else _publishState.value.otherMaterial,
            otherMaterialError = null,
        )
    }

    fun onOtherMaterialChange(value: String) {
        _publishState.value = _publishState.value.copy(
            otherMaterial = value,
            otherMaterialError = null,
        )
    }

    fun onVolumeSelected(volume: MaterialVolume) {
        _publishState.value = _publishState.value.copy(
            volume = volume,
            volumeError = null,
        )
    }

    fun onAddressChange(value: String) {
        _publishState.value = _publishState.value.copy(
            address = value,
            addressError = null,
        )
    }

    fun onPhotoSelected(uri: Uri) {
        _publishState.value = _publishState.value.copy(photo = uri)
    }

    fun onPhotoRemoved() {
        _publishState.value = _publishState.value.copy(photo = null)
    }

    fun onLocationUpdated(latitude: Double, longitude: Double) {
        _publishState.value = _publishState.value.copy(
            latitude = latitude,
            longitude = longitude,
        )
    }

    // Valida y publica material reciclable en Supabase vinculando al negocio si el rol es COMERCIO
    fun publish(onSuccess: () -> Unit = {}) {
        val form = _publishState.value
        var hasError = false

        val otherError = if (form.material == MaterialType.OTRO && form.otherMaterial.isBlank()) {
            hasError = true
            R.string.recycling_other_required
        } else null

        val volumeError = if (form.volume == null) {
            hasError = true
            R.string.recycling_volume_required
        } else null

        val addressError = if (form.address.isBlank()) {
            hasError = true
            R.string.recycling_address_required
        } else null

        if (hasError) {
            _publishState.value = form.copy(
                otherMaterialError = otherError,
                volumeError = volumeError,
                addressError = addressError,
            )
            return
        }

        _publishState.value = form.copy(isPublishing = true, generalError = null)
        viewModelScope.launch {
            val profile = profileRepository.getProfile().getOrNull()
            val materialName = materialTypeToDb(form.material, form.otherMaterial)
            val volumeName = materialVolumeToDb(form.volume!!)
            val lat = form.latitude ?: 4.0847
            val lng = form.longitude ?: -76.1953

            val result = circularEconomyRepository.createListing(
                materialType = materialName,
                quantityCategory = volumeName,
                latitude = lat,
                longitude = lng,
                address = form.address.trim(),
                neighborhood = "Centro",
                photoUrl = form.photo?.toString(),
                otherMaterial = if (form.material == MaterialType.OTRO) form.otherMaterial.trim() else null,
            )

            if (result.isSuccess) {
                _publishState.value = PublishMaterialUiState()
                loadListings()
                onSuccess()
            } else {
                _publishState.value = _publishState.value.copy(
                    isPublishing = false,
                    generalError = R.string.recycling_publish_error,
                )
            }
        }
    }

    // Retorna una publicación específica por su ID consultando caché o la base de datos Supabase
    suspend fun getListingById(id: String): Result<RecyclingPublication> {
        val cached = (_homeState.value as? RecyclingHomeUiState.Loaded)?.publications?.find { it.id == id }
        if (cached != null) return Result.success(cached)
        return circularEconomyRepository.getListing(id).map { it.toPresentation() }
    }

    // Cancela una publicación propia en Supabase y recarga el listado de publicaciones
    fun cancelPublication(id: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        _isCancelling.value = true
        viewModelScope.launch {
            val result = circularEconomyRepository.cancelListing(id)
            _isCancelling.value = false
            if (result.isSuccess) {
                loadListings()
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Error al cancelar la publicación")
            }
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
        isBusinessListing = isBusinessListing,
        businessId = businessId,
        photoUrl = photoUrl,
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

    // Convierte el tipo de material seleccionado a su representación en español para Supabase
    private fun materialTypeToDb(material: MaterialType, otherMaterial: String): String = when (material) {
        MaterialType.PET -> "PET"
        MaterialType.CARTON -> "CARTON"
        MaterialType.VIDRIO -> "VIDRIO"
        MaterialType.OTRO -> otherMaterial.trim()
    }

    // Convierte el volumen estimado seleccionado a su categoría en español para Supabase
    private fun materialVolumeToDb(volume: MaterialVolume): String = when (volume) {
        MaterialVolume.BOLSA_PEQUENA -> "BOLSA_PEQUEÑA"
        MaterialVolume.BOLSA_GRANDE -> "BOLSA_GRANDE"
        MaterialVolume.VOLUMINOSO -> "VOLUMINOSO"
    }

    // Mapea el tipo de material proveniente de la base de datos al enum
    private fun materialTypeFrom(value: String): MaterialType = when (value.uppercase().trim()) {
        "PET" -> MaterialType.PET
        "CARTON", "CARTÓN", "CARDBOARD" -> MaterialType.CARTON
        "VIDRIO", "GLASS" -> MaterialType.VIDRIO
        else -> MaterialType.OTRO
    }

    // Mapea el volumen proveniente de la base de datos al enum
    private fun materialVolumeFrom(value: String): MaterialVolume {
        val normalized = value.uppercase().trim().replace(' ', '_')
        return when (normalized) {
            "BOLSA_PEQUEÑA", "BOLSA_PEQUENA", "SMALL_BAG" -> MaterialVolume.BOLSA_PEQUENA
            "BOLSA_GRANDE", "LARGE_BAG" -> MaterialVolume.BOLSA_GRANDE
            "VOLUMINOSO", "BULKY" -> MaterialVolume.VOLUMINOSO
            else -> MaterialVolume.BOLSA_PEQUENA
        }
    }

    // Mapea los estados en español de la base de datos Supabase al enum de presentación
    private fun publicationStatusFrom(value: String): PublicationStatus = when (value.uppercase().trim()) {
        "DISPONIBLE" -> PublicationStatus.DISPONIBLE
        "RESERVADO", "RESERVADA" -> PublicationStatus.RESERVADO
        "COMPLETADO", "COMPLETADA" -> PublicationStatus.COMPLETADO
        "CANCELADO", "CANCELADA" -> PublicationStatus.CANCELADO
        else -> PublicationStatus.DISPONIBLE
    }
}
