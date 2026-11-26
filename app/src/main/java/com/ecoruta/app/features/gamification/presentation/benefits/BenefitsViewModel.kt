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

    private val _myCouponsState = MutableStateFlow(MyCouponsUiState(isLoading = false, coupons = emptyList(), isSample = false))
    val myCouponsState: StateFlow<MyCouponsUiState> = _myCouponsState.asStateFlow()

    private val _myCouponDetailState = MutableStateFlow<MyCouponUiState>(MyCouponUiState.Loading)
    val myCouponDetailState: StateFlow<MyCouponUiState> = _myCouponDetailState.asStateFlow()

    private var currentRedemptionCouponId: String? = null

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

            // Catálogo real de cupones desde base de datos sin datos simulados
            _benefitsState.update {
                it.copy(
                    isLoading = false,
                    balance = balance,
                    coupons = uiCoupons,
                    isSample = false,
                    error = null,
                )
            }

            currentRedemptionCouponId?.let { targetId ->
                val found = uiCoupons.find { it.id == targetId }
                _redemptionState.update {
                    it.copy(
                        isLoading = false,
                        coupon = found,
                        balance = balance,
                        isSample = false,
                        error = if (found == null) R.string.benefits_load_error else null,
                    )
                }
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
        currentRedemptionCouponId = couponId
        val existing = _benefitsState.value.coupons.find { it.id == couponId }
        val currentBalance = _benefitsState.value.balance

        if (existing != null && currentBalance != null) {
            _redemptionState.update {
                it.copy(
                    isLoading = false,//se pone false para que cuando se cargue el cupon ya este en memoria se active
                    coupon = existing,
                    balance = currentBalance,
                    step = RedemptionStep.DETAIL,
                    isRedeeming = false,
                    receipt = null,
                    error = null,
                )
            }
            return
        }

        _redemptionState.update {
            it.copy(
                isLoading = true,
                coupon = existing,
                balance = currentBalance,
                step = RedemptionStep.DETAIL,
                isRedeeming = false,
                receipt = null,
                error = null,
            )
        }

        viewModelScope.launch {
            val balanceResult = gamificationRepository.getBalance()
            val couponsResult = gamificationRepository.getAvailableCoupons()
            val loadedBalance = balanceResult.getOrNull() ?: 0
            val domainCoupons = couponsResult.getOrNull().orEmpty()
            val uiCoupons = domainCoupons.map { bc ->
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
            }

            _benefitsState.update {
                it.copy(
                    isLoading = false,
                    balance = loadedBalance,
                    coupons = uiCoupons,
                    isSample = false,
                    error = null,
                )
            }

            val found = uiCoupons.find { it.id == couponId }
            _redemptionState.update {
                it.copy(
                    isLoading = false,
                    coupon = found,
                    balance = loadedBalance,
                    isSample = false,
                    error = if (found == null) R.string.benefits_load_error else null,
                )
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
                    val resolvedBusiness = domainRedeemed.businessName.takeIf { it.isNotBlank() && it != "Comercio Aliado" } ?: coupon.businessName
                    val redeemedCoupon = RedeemedCoupon(
                        id = domainRedeemed.id,
                        coupon = coupon.copy(businessName = resolvedBusiness),
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
                    // Agrega el cupón redimido a la billetera en memoria
                    _myCouponsState.update { state ->
                        state.copy(coupons = listOf(redeemedCoupon) + state.coupons.filterNot { it.id == redeemedCoupon.id })
                    }
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

    // Carga los cupones canjeados del ciudadano desde Supabase sin datos simulados
    fun loadMyCoupons() {
        _myCouponsState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            gamificationRepository.getMyCoupons().fold(
                onSuccess = { redeemedList ->
                    val availableCatalog = _benefitsState.value.coupons
                    val uiRedeemed = redeemedList.map { rbc ->
                        val matchingCoupon = availableCatalog.find { it.id == rbc.couponId }
                        val resolvedBusinessName = rbc.businessName.takeIf { it.isNotBlank() && it != "Comercio Aliado" }
                            ?: matchingCoupon?.businessName?.takeIf { it.isNotBlank() && it != "Comercio Aliado" }
                            ?: rbc.businessName.ifBlank { "Comercio Aliado" }
                        val resolvedTitle = rbc.title.takeIf { it.isNotBlank() && it != "Cupón Canjeado" }
                            ?: matchingCoupon?.title?.takeIf { it.isNotBlank() }
                            ?: rbc.title.ifBlank { "Cupón Canjeado" }

                        RedeemedCoupon(
                            id = rbc.id,
                            coupon = matchingCoupon?.copy(
                                businessName = resolvedBusinessName,
                                title = resolvedTitle,
                                validUntilMillis = rbc.validUntilMillis,
                                pointsCost = rbc.pointsSpent,
                            ) ?: Coupon(
                                id = rbc.couponId,
                                businessName = resolvedBusinessName,
                                title = resolvedTitle,
                                subtitle = matchingCoupon?.subtitle ?: resolvedTitle,
                                category = matchingCoupon?.category ?: CouponCategory.FOOD,
                                pointsCost = rbc.pointsSpent,
                                availableQuantity = matchingCoupon?.availableQuantity ?: 0,
                                validUntilMillis = rbc.validUntilMillis,
                                conditions = matchingCoupon?.conditions ?: emptyList(),
                            ),
                            code = rbc.code,
                            redeemedAtMillis = rbc.redeemedAtMillis,
                            pointsSpent = rbc.pointsSpent,
                        )
                    }
                    _myCouponsState.update {
                        it.copy(
                            isLoading = false,
                            coupons = uiRedeemed,
                            isSample = false,
                            error = null,
                        )
                    }
                },
                onFailure = {
                    _myCouponsState.update {
                        it.copy(
                            isLoading = false,
                            coupons = emptyList(),
                            isSample = false,
                            error = null,
                        )
                    }
                },
            )
        }
    }

    // Filtra los cupones canjeados por categoría en la billetera
    fun onMyCouponsCategorySelected(category: CouponCategory?) {
        _myCouponsState.update { it.copy(category = category) }
    }

    // Alterna la expansión de la tarjeta del cupón canjeado
    fun onToggleMyCouponExpanded(id: String) {
        _myCouponsState.update {
            it.copy(expandedId = if (it.expandedId == id) null else id)
        }
    }

    // Carga el detalle del cupón canjeado con su código QR
    fun loadMyCouponDetail(redeemedId: String) {
        _myCouponDetailState.value = MyCouponUiState.Loading
        val existing = _myCouponsState.value.coupons.find { it.id == redeemedId }
        if (existing != null && existing.coupon.validUntilMillis > 0L && existing.coupon.businessName.isNotBlank() && existing.coupon.businessName != "Comercio Aliado") {
            _myCouponDetailState.value = MyCouponUiState.Loaded(coupon = existing, isSample = false)
            return
        }
        viewModelScope.launch {
            gamificationRepository.getMyCouponById(redeemedId).fold(
                onSuccess = { rbc ->
                    val availableCatalog = _benefitsState.value.coupons
                    val matchingCoupon = availableCatalog.find { it.id == rbc.couponId }
                    val resolvedBusinessName = rbc.businessName.takeIf { it.isNotBlank() && it != "Comercio Aliado" }
                        ?: matchingCoupon?.businessName?.takeIf { it.isNotBlank() && it != "Comercio Aliado" }
                        ?: rbc.businessName.ifBlank { "Comercio Aliado" }
                    val resolvedTitle = rbc.title.takeIf { it.isNotBlank() && it != "Cupón Canjeado" }
                        ?: matchingCoupon?.title?.takeIf { it.isNotBlank() }
                        ?: rbc.title.ifBlank { "Cupón Canjeado" }

                    val uiCoupon = RedeemedCoupon(
                        id = rbc.id,
                        coupon = matchingCoupon?.copy(
                            businessName = resolvedBusinessName,
                            title = resolvedTitle,
                            validUntilMillis = rbc.validUntilMillis,
                            pointsCost = rbc.pointsSpent,
                        ) ?: Coupon(
                            id = rbc.couponId,
                            businessName = resolvedBusinessName,
                            title = resolvedTitle,
                            subtitle = matchingCoupon?.subtitle ?: resolvedTitle,
                            category = matchingCoupon?.category ?: CouponCategory.FOOD,
                            pointsCost = rbc.pointsSpent,
                            availableQuantity = matchingCoupon?.availableQuantity ?: 0,
                            validUntilMillis = rbc.validUntilMillis,
                            conditions = matchingCoupon?.conditions ?: emptyList(),
                        ),
                        code = rbc.code,
                        redeemedAtMillis = rbc.redeemedAtMillis,
                        pointsSpent = rbc.pointsSpent,
                    )
                    _myCouponDetailState.value = MyCouponUiState.Loaded(coupon = uiCoupon, isSample = false)
                },
                onFailure = {
                    _myCouponDetailState.value = MyCouponUiState.NotFound
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

