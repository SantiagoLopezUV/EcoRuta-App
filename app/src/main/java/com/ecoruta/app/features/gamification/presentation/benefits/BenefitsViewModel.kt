package com.ecoruta.app.features.gamification.presentation.benefits

// ViewModel para EcoPuntos: saldo disponible, historial de movimientos y catálogo de beneficios
// Expone balance, lista de movimientos y coordina el canje atómico de cupones por puntos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecoruta.app.R
import com.ecoruta.app.features.gamification.domain.model.PointsTransaction
import com.ecoruta.app.features.gamification.domain.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BenefitsViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) : ViewModel() {

    private val _benefitsState = MutableStateFlow(BenefitsUiState(isLoading = true))
    val benefitsState: StateFlow<BenefitsUiState> = _benefitsState.asStateFlow()

    private val _pointsHistoryState = MutableStateFlow<PointsHistoryUiState>(PointsHistoryUiState.Loading)
    val pointsHistoryState: StateFlow<PointsHistoryUiState> = _pointsHistoryState.asStateFlow()

    private val _redemptionState = MutableStateFlow(CouponRedemptionUiState(isLoading = false))
    val redemptionState: StateFlow<CouponRedemptionUiState> = _redemptionState.asStateFlow()

    init {
        loadBenefits()
        loadPointsHistory()
    }

    // Carga el saldo de EcoPuntos y el catálogo de cupones disponibles en paralelo
    fun loadBenefits() {
        _benefitsState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val balanceResult = gamificationRepository.getBalance()
            val couponsResult = gamificationRepository.getAvailableCoupons()

            val balance = balanceResult.getOrNull() ?: 0
            val domainCoupons = couponsResult.getOrNull()

            if (balanceResult.isFailure && couponsResult.isFailure) {
                _benefitsState.update {
                    it.copy(isLoading = false, error = R.string.benefits_load_error)
                }
                return@launch
            }

            // Mapea los cupones de dominio al modelo de presentación
            val uiCoupons = domainCoupons?.map { bc ->
                Coupon(
                    id = bc.id,
                    businessName = bc.businessName,
                    title = bc.title,
                    subtitle = bc.subtitle,
                    category = categoryFromString(bc.category),
                    pointsCost = bc.pointsCost,
                    availableQuantity = bc.availableQuantity,
                    validUntilMillis = bc.validUntilMillis,
                    conditions = bc.conditions,
                )
            } ?: emptyList()

            // Si el catálogo remoto aún no tiene registros en base de datos, presenta datos de muestra
            val (couponsToShow, isSample) = if (uiCoupons.isNotEmpty()) {
                uiCoupons to false
            } else {
                DefaultSampleCoupons to true
            }

            _benefitsState.update {
                it.copy(
                    isLoading = false,
                    balance = balance,
                    coupons = couponsToShow,
                    isSample = isSample,
                    error = null,
                )
            }
        }
    }

    // Carga el historial de movimientos de EcoPuntos (abonos y débitos) del ciudadano
    fun loadPointsHistory() {
        _pointsHistoryState.value = PointsHistoryUiState.Loading
        viewModelScope.launch {
            gamificationRepository.getPointsLedger().fold(
                onSuccess = { ledger ->
                    val uiMovements = ledger.movements.map { it.toUiMovement() }
                    _pointsHistoryState.value = PointsHistoryUiState.Loaded(
                        balance = ledger.balance,
                        movements = uiMovements,
                        isSample = false,
                    )
                },
                onFailure = {
                    _pointsHistoryState.value = PointsHistoryUiState.Error
                },
            )
        }
    }

    // Filtra cupones del catálogo por categoría
    fun onCategorySelected(category: CouponCategory?) {
        _benefitsState.update { it.copy(category = category) }
    }

    // Carga el detalle del cupón para el flujo de canje y verifica si el saldo es suficiente
    fun loadCouponForRedemption(couponId: String) {
        val coupon = _benefitsState.value.coupons.find { it.id == couponId }
        val balance = _benefitsState.value.balance
        _redemptionState.update {
            it.copy(
                isLoading = coupon == null && _benefitsState.value.isLoading,
                coupon = coupon,
                balance = balance,
                step = RedemptionStep.DETAIL,
                isRedeeming = false,
                receipt = null,
                error = null,
            )
        } //implementar el view model para los cupones
        if (coupon == null && !_benefitsState.value.isLoading) {
            viewModelScope.launch {
                val balanceResult = gamificationRepository.getBalance()
                val couponsResult = gamificationRepository.getAvailableCoupons()
                val loadedBalance = balanceResult.getOrNull()
                val domainCoupons = couponsResult.getOrNull()
                val uiCoupons = domainCoupons?.map { bc ->
                    Coupon(
                        id = bc.id,
                        businessName = bc.businessName,
                        title = bc.title,
                        subtitle = bc.subtitle,
                        category = categoryFromString(bc.category),
                        pointsCost = bc.pointsCost,
                        availableQuantity = bc.availableQuantity,
                        validUntilMillis = bc.validUntilMillis,
                        conditions = bc.conditions,
                    )
                } ?: emptyList()

                val (couponsToShow, isSample) = if (uiCoupons.isNotEmpty()) {
                    uiCoupons to false
                } else {
                    DefaultSampleCoupons to true
                }

                _benefitsState.update {
                    it.copy(
                        isLoading = false,
                        balance = loadedBalance,
                        coupons = couponsToShow,
                        isSample = isSample,
                        error = null,
                    )
                }
                val found = couponsToShow.find { it.id == couponId }
                _redemptionState.update {
                    it.copy(
                        isLoading = false,
                        coupon = found,
                        balance = loadedBalance,
                        error = if (found == null) R.string.benefits_load_error else null,
                    )
                }
            }
        }
    }

    // Avanza al paso de confirmación si el saldo es suficiente, o al de puntos insuficientes
    fun onRedeemRequested() {
        val state = _redemptionState.value
        val balance = state.balance ?: 0
        val cost = state.coupon?.pointsCost ?: 0
        val step = if (balance >= cost) RedemptionStep.CONFIRM else RedemptionStep.INSUFFICIENT
        _redemptionState.update { it.copy(step = step) }
    }

    // Confirma el canje atómico del cupón debitando los EcoPuntos en Supabase
    fun onConfirmRedeem() {
        val coupon = _redemptionState.value.coupon ?: return
        _redemptionState.update { it.copy(isRedeeming = true, error = null) }
        viewModelScope.launch {
            gamificationRepository.redeemCoupon(coupon.id).fold(
                onSuccess = { domainRedeemed ->
                    val newBalance = (_redemptionState.value.balance ?: 0) - coupon.pointsCost
                    val redeemedCoupon = RedeemedCoupon(
                        id = domainRedeemed.id,
                        coupon = coupon,
                        code = domainRedeemed.code,
                        redeemedAtMillis = domainRedeemed.redeemedAtMillis,
                        pointsSpent = coupon.pointsCost,
                    )
                    val receipt = RedemptionReceipt(
                        redeemedCoupon = redeemedCoupon,
                        balanceAfter = newBalance,
                    )
                    _redemptionState.update {
                        it.copy(
                            isRedeeming = false,
                            step = RedemptionStep.REDEEMED,
                            receipt = receipt,
                            redeemedWithoutBackend = false,
                        )
                    }
                    // Actualiza el saldo en la vista de beneficios
                    _benefitsState.update { it.copy(balance = newBalance) }
                },
                onFailure = {
                    _redemptionState.update {
                        it.copy(
                            isRedeeming = false,
                            error = R.string.benefits_load_error,
                        )
                    }
                },
            )
        }
    }

    // Vuelve al paso de detalle del cupón desde confirmación
    fun onBackToDetail() {
        _redemptionState.update { it.copy(step = RedemptionStep.DETAIL) }
    }

    private fun categoryFromString(value: String): CouponCategory = when (value.lowercase()) {
        "food", "alimentos", "comida" -> CouponCategory.FOOD
        "groceries", "abarrotes", "mercado" -> CouponCategory.GROCERIES
        "stationery", "papeleria", "papelería" -> CouponCategory.STATIONERY
        else -> CouponCategory.FOOD
    }

    private fun PointsTransaction.toUiMovement(): PointsMovement = PointsMovement(
        id = id,
        description = description,
        dateMillis = dateMillis,
        points = delta,
    )
}

// Alias de compatibilidad arquitectónica para el issue ECO-1045
typealias EcoPointsViewModel = BenefitsViewModel

// Catálogo base de cupones de muestra mientras se aprovisionan datos en backend
val DefaultSampleCoupons = listOf(
    Coupon(
        id = "CUP-01",
        businessName = "Café del Río",
        title = "20 % de descuento en bebidas calientes",
        subtitle = "Válido de lunes a viernes",
        category = CouponCategory.FOOD,
        pointsCost = 450,
        availableQuantity = 18,
        validUntilMillis = 1_795_996_800_000L,
        conditions = listOf("Un cupón por compra", "Presenta el código QR en caja"),
    ),
    Coupon(
        id = "CUP-02",
        businessName = "Mercado Saludable Bio",
        title = "Bono de $ 15.000 en compras superiores a $ 60.000",
        subtitle = "Aplica en granos y frutos secos",
        category = CouponCategory.GROCERIES,
        pointsCost = 300,
        availableQuantity = 12,
        validUntilMillis = 1_795_996_800_000L,
        conditions = listOf("No acumulable con otras promociones"),
    ),
    Coupon(
        id = "CUP-03",
        businessName = "Papelería San Carlos",
        title = "15 % de descuento en cuadernos ecológicos",
        subtitle = "Válido todo el mes",
        category = CouponCategory.STATIONERY,
        pointsCost = 200,
        availableQuantity = 25,
        validUntilMillis = 1_795_996_800_000L,
        conditions = listOf("Presenta el código QR antes de pagar"),
    ),
)

