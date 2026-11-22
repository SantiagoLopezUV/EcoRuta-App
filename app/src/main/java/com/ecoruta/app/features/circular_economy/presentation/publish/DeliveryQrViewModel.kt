package com.ecoruta.app.features.circular_economy.presentation.publish

// ViewModel para el código QR de entrega
// Solicita el token al backend y lo refresca automáticamente al expirar

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.NavigationDestination
import com.ecoruta.app.features.circular_economy.domain.model.DeliveryToken
import com.ecoruta.app.features.circular_economy.domain.model.RecyclableListing
import com.ecoruta.app.features.circular_economy.domain.repository.CircularEconomyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryQrViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CircularEconomyRepository,
) : ViewModel() {

    private val listingId: String = checkNotNull(savedStateHandle[NavigationDestination.ARG_ID])

    private val _state = MutableStateFlow(DeliveryCodeUiState())
    val state: StateFlow<DeliveryCodeUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    init {
        loadQrCode()
    }

    // Carga la publicación y el token de entrega; programa la renovación automática al expirar
    fun loadQrCode() {
        refreshJob?.cancel()
        _state.value = DeliveryCodeUiState(isLoading = true)
        viewModelScope.launch {
            val listingResult = repository.getListing(listingId)
            val tokenResult = repository.requestDeliveryCode(listingId)

            if (tokenResult.isFailure) {
                _state.value = DeliveryCodeUiState(
                    isLoading = false,
                    publication = listingResult.getOrNull()?.toPresentation(),
                    error = R.string.recycling_code_error,
                )
                return@launch
            }

            val token = tokenResult.getOrThrow()
            _state.value = DeliveryCodeUiState(
                isLoading = false,
                publication = listingResult.getOrNull()?.toPresentation(),
                code = token.toPresentation(),
            )
            scheduleRefresh(token)
        }
    }

    // Espera hasta que el token expire y lanza una nueva solicitud automáticamente
    private fun scheduleRefresh(token: DeliveryToken) {
        refreshJob = viewModelScope.launch {
            val delayMs = (token.expiresAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
            delay(delayMs)
            loadQrCode()
        }
    }

    private fun RecyclableListing.toPresentation(): RecyclingPublication = RecyclingPublication(
        id = id,
        code = code,
        material = when (material.uppercase()) {
            "PET" -> MaterialType.PET
            "CARDBOARD", "CARTON", "CARTÓN" -> MaterialType.CARDBOARD
            "GLASS", "VIDRIO" -> MaterialType.GLASS
            else -> MaterialType.OTHER
        },
        otherMaterial = otherMaterial,
        volume = when (volume.uppercase()) {
            "LARGE_BAG", "BOLSA_GRANDE" -> MaterialVolume.LARGE_BAG
            "BULKY", "VOLUMINOSO" -> MaterialVolume.BULKY
            else -> MaterialVolume.SMALL_BAG
        },
        address = address,
        status = when (status.uppercase()) {
            "EN_CAMINO", "ON_THE_WAY" -> PublicationStatus.EN_CAMINO
            "ENTREGADA", "DELIVERED" -> PublicationStatus.ENTREGADA
            "CANCELADA", "CANCELLED" -> PublicationStatus.CANCELADA
            else -> PublicationStatus.DISPONIBLE
        },
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

    private fun DeliveryToken.toPresentation(): DeliveryCode =
        DeliveryCode(token = token, expiresAtMillis = expiresAtMillis)
}
