package com.ecoruta.app.features.gamification.presentation.merchant_coupons

// ViewModel para la gestión de cupones del comercio aliado
// Valida el formulario de creación y edición; administra la lista con desactivación confirmada

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.core.navigation.NavigationDestination
import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantCoupon
import com.ecoruta.app.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CouponManagementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gamificationRepository: GamificationRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    // Null means create mode; non-null means edit mode
    private val editCouponId: String? = savedStateHandle[NavigationDestination.ARG_ID]

    private val _couponsState = MutableStateFlow(MerchantCouponsUiState())
    val couponsState: StateFlow<MerchantCouponsUiState> = _couponsState.asStateFlow()

    private val _formState = MutableStateFlow(MerchantCouponFormUiState())
    val formState: StateFlow<MerchantCouponFormUiState> = _formState.asStateFlow()

    init {
        loadCoupons()
        if (editCouponId != null) loadCouponForEdit(editCouponId)
    }

    // Carga la lista de cupones y el nombre del comercio desde el perfil en paralelo
    fun loadCoupons() {
        _couponsState.update { it.copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            val profileResult = profileRepository.getProfile()
            val couponsResult = gamificationRepository.getMerchantCoupons()

            if (couponsResult.isFailure) {
                _couponsState.update { it.copy(isLoading = false, hasError = true) }
                return@launch
            }

            _couponsState.update {
                it.copy(
                    isLoading = false,
                    storeName = profileResult.getOrNull()?.fullName.orEmpty(),
                    coupons = couponsResult.getOrThrow().map { coupon -> coupon.toPresentation() },
                )
            }
        }
    }

    // Pre-pobla el formulario con los datos del cupón a editar
    private fun loadCouponForEdit(id: String) {
        viewModelScope.launch {
            val profileResult = profileRepository.getProfile()
            val couponResult = gamificationRepository.getMerchantCoupon(id)
            if (couponResult.isFailure) {
                _formState.update { it.copy(isCouponMissing = true) }
                return@launch
            }
            val coupon = couponResult.getOrThrow()
            _formState.update {
                it.copy(
                    storeName = profileResult.getOrNull()?.fullName.orEmpty(),
                    title = coupon.title,
                    description = coupon.subtitle,
                    pointsCost = coupon.pointsCost.toString(),
                    quantity = coupon.availableQuantity.toString(),
                    validUntilMillis = coupon.validUntilMillis,
                    isActive = coupon.isActive,
                )
            }
        }
    }

    fun onTitleChange(value: String) = _formState.update { it.copy(title = value, invalidFields = it.invalidFields - MerchantCouponField.TITLE) }
    fun onDescriptionChange(value: String) = _formState.update { it.copy(description = value, invalidFields = it.invalidFields - MerchantCouponField.DESCRIPTION) }
    fun onPointsChange(value: String) = _formState.update { it.copy(pointsCost = value, invalidFields = it.invalidFields - MerchantCouponField.POINTS) }
    fun onQuantityChange(value: String) = _formState.update { it.copy(quantity = value, invalidFields = it.invalidFields - MerchantCouponField.QUANTITY) }
    fun onDateChange(millis: Long?) = _formState.update { it.copy(validUntilMillis = millis, invalidFields = it.invalidFields - MerchantCouponField.VALID_UNTIL) }
    fun onDatePickerToggle(open: Boolean) = _formState.update { it.copy(isDatePickerOpen = open) }
    fun onActiveChange(active: Boolean) = _formState.update { it.copy(isActive = active) }

    // Valida todos los campos y llama al repositorio para crear o actualizar el cupón
    fun onSubmit() {
        val form = _formState.value
        val invalid = buildSet {
            if (form.title.isBlank()) add(MerchantCouponField.TITLE)
            if (form.description.isBlank()) add(MerchantCouponField.DESCRIPTION)
            if (form.pointsCost.toIntOrNull()?.let { it > 0 } != true) add(MerchantCouponField.POINTS)
            if (form.quantity.toIntOrNull()?.let { it > 0 } != true) add(MerchantCouponField.QUANTITY)
            if (form.validUntilMillis == null || form.validUntilMillis < System.currentTimeMillis()) add(MerchantCouponField.VALID_UNTIL)
        }
        if (invalid.isNotEmpty()) {
            _formState.update { it.copy(invalidFields = invalid) }
            return
        }
        _formState.update { it.copy(isSaving = true, hasError = false) }
        viewModelScope.launch {
            val result = if (editCouponId == null) {
                gamificationRepository.createCoupon(
                    title = form.title.trim(),
                    pointsCost = form.pointsCost.toInt(),
                    quantityAvailable = form.quantity.toInt(),
                    validUntilMillis = form.validUntilMillis!!,
                    description = form.description.trim(),
                )
            } else {
                gamificationRepository.updateCoupon(
                    couponId = editCouponId,
                    title = form.title.trim(),
                    pointsCost = form.pointsCost.toInt(),
                    quantityAvailable = form.quantity.toInt(),
                    validUntilMillis = form.validUntilMillis!!,
                    description = form.description.trim(),
                    isActive = form.isActive,
                )
            }
            _formState.update { it.copy(isSaving = false, hasError = result.isFailure) }
        }
    }

    fun onDeactivateCoupon(coupon: MerchantCoupon) = _couponsState.update { it.copy(couponToDeactivate = coupon) }
    fun onDismissDeactivate() = _couponsState.update { it.copy(couponToDeactivate = null) }

    // Desactiva el cupón confirmado y refresca la lista
    fun onConfirmDeactivate() {
        val coupon = _couponsState.value.couponToDeactivate ?: return
        _couponsState.update { it.copy(couponToDeactivate = null) }
        viewModelScope.launch {
            gamificationRepository.deactivateCoupon(coupon.id)
            loadCoupons()
        }
    }

    private fun BenefitCoupon.toPresentation(): MerchantCoupon = MerchantCoupon(
        id = id,
        title = title,
        description = subtitle,
        pointsCost = pointsCost,
        availableQuantity = availableQuantity,
        validUntil = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(validUntilMillis)),
        isActive = isActive,
    )
}
