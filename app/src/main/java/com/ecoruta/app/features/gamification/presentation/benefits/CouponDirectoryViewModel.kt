package com.ecoruta.app.features.gamification.presentation.benefits

// ViewModel para el catálogo de cupones del ciudadano
// Carga cupones y balance, aplica filtros de categoría y coordina el flujo de redención

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.NavigationDestination
import com.ecoruta.app.features.gamification.domain.model.BenefitCoupon
import com.ecoruta.app.features.gamification.domain.model.RedeemedCouponModel
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository
import com.ecoruta.app.features.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CouponDirectoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val gamificationRepository: GamificationRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    // Null for catalog screen; non-null for redemption flow
    private val couponId: String? = savedStateHandle[NavigationDestination.ARG_ID]

    private val _catalogState = MutableStateFlow(BenefitsUiState())
    val catalogState: StateFlow<BenefitsUiState> = _catalogState.asStateFlow()

    private val _redemptionState = MutableStateFlow(CouponRedemptionUiState())
    val redemptionState: StateFlow<CouponRedemptionUiState> = _redemptionState.asStateFlow()

    init {
        loadCatalog()
        couponId?.let { loadCouponForRedemption(it) }
    }

    // Carga el catálogo de cupones activos y el balance del usuario en paralelo
    fun loadCatalog() {
        _catalogState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val profileResult = profileRepository.getProfile()
            val couponsResult = gamificationRepository.getCoupons()

            if (couponsResult.isFailure) {
                _catalogState.update { it.copy(isLoading = false, error = R.string.benefits_load_error) }
                return@launch
            }

            _catalogState.update {
                it.copy(
                    isLoading = false,
                    balance = profileResult.getOrNull()?.ecoPoints,
                    coupons = couponsResult.getOrThrow().map { model -> model.toPresentation() },
                )
            }
        }
    }

    // Aplica o quita el filtro de categoría; el catálogo ya filtrado se expone en visibleCoupons
    fun onCategorySelected(category: CouponCategory?) {
        _catalogState.update { it.copy(category = category) }
    }

    // Carga el cupón a canjear y el balance del usuario para mostrar el detalle de redención
    private fun loadCouponForRedemption(id: String) {
        _redemptionState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val profileResult = profileRepository.getProfile()
            val couponResult = gamificationRepository.getCoupon(id)

            if (couponResult.isFailure) {
                _redemptionState.update { it.copy(isLoading = false, error = R.string.benefits_load_error) }
                return@launch
            }

            val coupon = couponResult.getOrThrow()
            val balance = profileResult.getOrNull()?.ecoPoints
            _redemptionState.update {
                it.copy(
                    isLoading = false,
                    coupon = coupon.toPresentation(),
                    balance = balance,
                    step = if (balance != null && balance < coupon.pointsCost) RedemptionStep.INSUFFICIENT else RedemptionStep.DETAIL,
                )
            }
        }
    }

    // Avanza de DETAIL a CONFIRM para que el usuario confirme el canje
    fun onConfirmRedemption() {
        _redemptionState.update { it.copy(step = RedemptionStep.CONFIRM) }
    }

    // Vuelve de CONFIRM a DETAIL si el usuario cancela
    fun onCancelRedemption() {
        _redemptionState.update { it.copy(step = RedemptionStep.DETAIL) }
    }

    // Ejecuta el canje y actualiza el estado con el recibo o el error resultante
    fun onRedeemConfirmed() {
        val id = couponId ?: return
        _redemptionState.update { it.copy(isRedeeming = true) }
        viewModelScope.launch {
            val result = gamificationRepository.redeemCoupon(id)
            if (result.isFailure) {
                _redemptionState.update { it.copy(isRedeeming = false, error = R.string.coupon_redeem_error) }
                return@launch
            }
            val redeemed = result.getOrThrow()
            _redemptionState.update { state ->
                val coupon = state.coupon ?: return@update state
                state.copy(
                    isRedeeming = false,
                    step = RedemptionStep.REDEEMED,
                    receipt = RedemptionReceipt(
                        redeemedCoupon = RedeemedCoupon(
                            id = redeemed.id,
                            coupon = coupon,
                            code = redeemed.code,
                            redeemedAtMillis = redeemed.redeemedAtMillis,
                            pointsSpent = redeemed.pointsCost,
                        ),
                        balanceAfter = redeemed.balanceAfter,
                    ),
                )
            }
        }
    }

    private fun BenefitCoupon.toPresentation(): Coupon = Coupon(
        id = id,
        businessName = storeName,
        title = title,
        subtitle = description,
        category = categoryFrom(category),
        pointsCost = pointsCost,
        availableQuantity = availableQuantity,
        validUntilMillis = validUntilMillis,
        conditions = conditions,
    )

    private fun categoryFrom(value: String): CouponCategory = when (value.uppercase()) {
        "FOOD", "COMIDA", "RESTAURANTE" -> CouponCategory.FOOD
        "GROCERIES", "SUPERMERCADO", "TIENDA" -> CouponCategory.GROCERIES
        "STATIONERY", "PAPELERIA", "PAPELERÍA" -> CouponCategory.STATIONERY
        else -> CouponCategory.FOOD
    }
}
