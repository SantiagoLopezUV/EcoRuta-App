package com.ecoruta.app.features.gamification.presentation.navigation

// Subgrafo de navegación para beneficios comerciales, EcoPuntos y billetera de cupones
// Conecta el catálogo de beneficios, flujo de canje, historial de puntos y mis cupones

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.navigateToCitizenTab
import com.ecoruta.app.core.navigation.navigateToPublicTab
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsActions
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsScreen
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsUiState
import com.ecoruta.app.features.gamification.presentation.benefits.Coupon
import com.ecoruta.app.features.gamification.presentation.benefits.CouponCategory
import com.ecoruta.app.features.gamification.presentation.benefits.CouponRedemptionUiState
import com.ecoruta.app.features.gamification.presentation.benefits.GuestBenefitsScreen
import com.ecoruta.app.features.gamification.presentation.benefits.MyCouponUiState
import com.ecoruta.app.features.gamification.presentation.benefits.MyCouponsUiState
import com.ecoruta.app.features.gamification.presentation.benefits.PointsHistoryScreen
import com.ecoruta.app.features.gamification.presentation.benefits.PointsHistoryUiState
import com.ecoruta.app.features.gamification.presentation.benefits.PointsMovement
import com.ecoruta.app.features.gamification.presentation.benefits.RedeemedCoupon
import com.ecoruta.app.features.gamification.presentation.benefits.RedemptionReceipt
import com.ecoruta.app.features.gamification.presentation.benefits.RedemptionStep
import com.ecoruta.app.features.gamification.presentation.my_coupons.CouponRedemptionActions
import com.ecoruta.app.features.gamification.presentation.my_coupons.CouponRedemptionScreen
import com.ecoruta.app.features.gamification.presentation.my_coupons.MyCouponDetailScreen
import com.ecoruta.app.features.gamification.presentation.my_coupons.MyCouponsActions
import com.ecoruta.app.features.gamification.presentation.my_coupons.MyCouponsScreen

private val SampleCoupons = listOf(
    Coupon(
        id = "CUP-01",
        businessName = "Café del Río",
        title = "20 % de descuento en bebidas calientes",
        subtitle = "Válido de lunes a viernes",
        category = CouponCategory.GASTRONOMIA,
        pointsCost = 450,
        availableQuantity = 18,
        validUntilMillis = 1_795_996_800_000L,
        conditions = listOf("Un cupón por compra", "Presenta el código QR en caja"),
    ),
)

private val SampleRedeemedCoupons = listOf(
    RedeemedCoupon(
        id = "RED-01",
        coupon = SampleCoupons.first(),
        code = "ECO-CAFE-8492",
        redeemedAtMillis = 1_760_100_000_000L,
        pointsSpent = 450,
    ),
)

// Registra las rutas del catálogo de beneficios, canje de cupones e historial de puntos
fun NavGraphBuilder.gamificationGraph(navController: NavHostController) {
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }
    fun backToBenefits() {
        if (!navController.popBackStack(EcoRutaDestination.BENEFITS, inclusive = false)) {
            navController.navigateToCitizenTab(CitizenTab.BENEFITS)
        }
    }
    val tabClick: (CitizenTab) -> Unit = { tab -> navController.navigateToCitizenTab(tab) }

    composable(EcoRutaDestination.BENEFITS) {
        var state by remember {
            mutableStateOf(
                BenefitsUiState(
                    isLoading = false,
                    balance = 1_250,
                    coupons = SampleCoupons,
                    isSample = true,
                ),
            )
        }
        BenefitsScreen(
            state = state,
            actions = BenefitsActions(
                onBack = { navController.popBackStack() },
                onCouponClick = { open(EcoRutaDestination.couponRedemption(it.id)) },
                onCategorySelected = { state = state.copy(category = it) },
                onMyCoupons = { navController.navigateToCitizenTab(CitizenTab.COUPONS) },
                onHistory = { open(EcoRutaDestination.ECOPOINTS_HISTORY) },
                onRetry = {},
                onTabClick = tabClick,
            ),
        )
    }

    composable(
        EcoRutaDestination.COUPON_REDEMPTION,
        arguments = listOf(navArgument(EcoRutaDestination.ARG_ID) { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val coupon = SampleCoupons.find { it.id == id } ?: SampleCoupons.first()
        var state by remember {
            mutableStateOf(
                CouponRedemptionUiState(
                    isLoading = false,
                    coupon = coupon,
                    balance = 1_250,
                    isSample = true,
                ),
            )
        }
        BackHandler(enabled = state.step == RedemptionStep.CONFIRM || state.step == RedemptionStep.INSUFFICIENT) {
            state = state.copy(step = RedemptionStep.DETAIL)
        }
        CouponRedemptionScreen(
            state = state,
            actions = CouponRedemptionActions(
                onBack = {
                    if (state.step == RedemptionStep.CONFIRM || state.step == RedemptionStep.INSUFFICIENT) {
                        state = state.copy(step = RedemptionStep.DETAIL)
                    } else {
                        navController.popBackStack()
                    }
                },
                onRedeem = {
                    val affordable = (state.balance ?: 0) >= coupon.pointsCost
                    state = state.copy(step = if (affordable) RedemptionStep.CONFIRM else RedemptionStep.INSUFFICIENT)
                },
                onConfirm = {
                    state = state.copy(
                        step = RedemptionStep.REDEEMED,
                        receipt = RedemptionReceipt(SampleRedeemedCoupons.first(), (state.balance ?: 1250) - coupon.pointsCost),
                        redeemedWithoutBackend = true,
                    )
                },
                onBackToDetail = { state = state.copy(step = RedemptionStep.DETAIL) },
                onMyCoupons = { navController.navigateToCitizenTab(CitizenTab.COUPONS) },
                onBackToBenefits = ::backToBenefits,
                onRetry = {},
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.ECOPOINTS_HISTORY) {
        val movements = listOf(
            PointsMovement("MOV-01", "Entrega de material reciclable", 1_760_100_000_000L, 80),
            PointsMovement("MOV-02", "Canje en Café del Río", 1_760_050_000_000L, -450),
        )
        PointsHistoryScreen(
            state = PointsHistoryUiState.Loaded(balance = 1_250, movements = movements, isSample = true),
            onBack = { navController.popBackStack() },
            onRetry = {},
            onTabClick = tabClick,
        )
    }

    listOf(EcoRutaDestination.MY_COUPONS to false, EcoRutaDestination.PROFILE_MY_COUPONS to true).forEach { (route, fromProfile) ->
        composable(route) {
            var state by remember {
                mutableStateOf(
                    MyCouponsUiState(
                        isLoading = false,
                        coupons = SampleRedeemedCoupons,
                        expandedId = SampleRedeemedCoupons.first().id,
                        isSample = true,
                    ),
                )
            }
            MyCouponsScreen(
                state = state,
                selectedTab = if (fromProfile) CitizenTab.PROFILE else CitizenTab.COUPONS,
                actions = MyCouponsActions(
                    onBack = { navController.popBackStack() },
                    onCategorySelected = { state = state.copy(category = it) },
                    onToggleExpanded = { id -> state = state.copy(expandedId = if (state.expandedId == id) null else id) },
                    onShowQr = { open(EcoRutaDestination.myCouponDetail(it.id, fromProfile)) },
                    onGoToBenefits = { navController.navigateToCitizenTab(CitizenTab.BENEFITS) },
                    onRetry = {},
                    onTabClick = tabClick,
                ),
            )
        }
    }

    composable(
        EcoRutaDestination.MY_COUPON_DETAIL,
        arguments = listOf(
            navArgument(EcoRutaDestination.ARG_ID) { type = NavType.StringType },
            navArgument(EcoRutaDestination.ARG_FROM_PROFILE) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) { entry ->
        val fromProfile = entry.arguments?.getBoolean(EcoRutaDestination.ARG_FROM_PROFILE) ?: false
        MyCouponDetailScreen(
            state = MyCouponUiState.Loaded(coupon = SampleRedeemedCoupons.first(), isSample = true),
            selectedTab = if (fromProfile) CitizenTab.PROFILE else CitizenTab.COUPONS,
            onBack = { navController.popBackStack() },
            onBackToMyCoupons = { navController.popBackStack() },
            onBackToBenefits = ::backToBenefits,
            onRetry = {},
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.GUEST_BENEFITS) {
        var state by remember {
            mutableStateOf(
                BenefitsUiState(
                    isLoading = false,
                    coupons = SampleCoupons,
                    isSample = true,
                ),
            )
        }
        GuestBenefitsScreen(
            state = state,
            onBack = { navController.popBackStack() },
            onLogin = { open(EcoRutaDestination.LOGIN) },
            onCategorySelected = { state = state.copy(category = it) },
            onTabClick = { tab -> navController.navigateToPublicTab(tab) },
        )
    }
}
