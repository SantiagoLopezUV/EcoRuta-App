package com.ecoruta.app.features.gamification.presentation.navigation

// Subgrafo de navegación para beneficios comerciales, EcoPuntos y billetera de cupones
// Conecta el catálogo de beneficios, flujo de canje, historial de puntos y mis cupones

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.navigateToCitizenTab
import com.ecoruta.app.core.navigation.navigateToPublicTab
import com.ecoruta.app.core.ui.components.GuestServiceActions
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsActions
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsScreen
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsUiState
import com.ecoruta.app.features.gamification.presentation.benefits.BenefitsViewModel
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
import com.ecoruta.app.features.gamification.presentation.my_coupons.GuestMyCouponsScreen
import com.ecoruta.app.features.gamification.presentation.my_coupons.MyCouponDetailScreen
import com.ecoruta.app.features.gamification.presentation.my_coupons.MyCouponsActions
import com.ecoruta.app.features.gamification.presentation.my_coupons.MyCouponsScreen

// Registra las rutas del catálogo de beneficios, canje de cupones e historial de puntos
fun NavGraphBuilder.gamificationGraph(navController: NavHostController) {
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }
    fun backToBenefits() {
        if (!navController.popBackStack(EcoRutaDestination.BENEFITS, inclusive = false)) {
            navController.navigateToCitizenTab(CitizenTab.BENEFITS)
        }
    }
    val tabClick: (CitizenTab) -> Unit = { tab -> navController.navigateToCitizenTab(tab) }

    composable(EcoRutaDestination.GUEST_MY_COUPONS) {
        GuestMyCouponsScreen(
            actions = GuestServiceActions(
                onBack = { navController.popBackStack() },
                onLogin = { open(EcoRutaDestination.LOGIN) },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.BENEFITS) {
        val viewModel: BenefitsViewModel = hiltViewModel()
        val state by viewModel.benefitsState.collectAsStateWithLifecycle()
        BenefitsScreen(
            state = state,
            actions = BenefitsActions(
                onBack = { navController.popBackStack() },
                onCouponClick = { open(EcoRutaDestination.couponRedemption(it.id)) },
                onCategorySelected = { viewModel.onCategorySelected(it) },
                onMyCoupons = { navController.navigateToCitizenTab(CitizenTab.COUPONS) },
                onHistory = { open(EcoRutaDestination.ECOPOINTS_HISTORY) },
                onRetry = { viewModel.loadBenefits() },
                onTabClick = tabClick,
            ),
        )
    }

    composable(
        EcoRutaDestination.COUPON_REDEMPTION,
        arguments = listOf(navArgument(EcoRutaDestination.ARG_ID) { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val viewModel: BenefitsViewModel = hiltViewModel()
        LaunchedEffect(id) {
            viewModel.loadCouponForRedemption(id)
        }
        val state by viewModel.redemptionState.collectAsStateWithLifecycle()
        BackHandler(enabled = state.step == RedemptionStep.CONFIRM || state.step == RedemptionStep.INSUFFICIENT) {
            viewModel.onBackToDetail()
        }
        CouponRedemptionScreen(
            state = state,
            actions = CouponRedemptionActions(
                onBack = {
                    if (state.step == RedemptionStep.CONFIRM || state.step == RedemptionStep.INSUFFICIENT) {
                        viewModel.onBackToDetail()
                    } else {
                        navController.popBackStack()
                    }
                },
                onRedeem = { viewModel.onRedeemRequested() },
                onConfirm = { viewModel.onConfirmRedeem() },
                onBackToDetail = { viewModel.onBackToDetail() },
                onMyCoupons = { navController.navigateToCitizenTab(CitizenTab.COUPONS) },
                onBackToBenefits = ::backToBenefits,
                onRetry = { viewModel.loadCouponForRedemption(id) },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.ECOPOINTS_HISTORY) {
        val viewModel: BenefitsViewModel = hiltViewModel()
        val state by viewModel.pointsHistoryState.collectAsStateWithLifecycle()
        PointsHistoryScreen(
            state = state,
            onBack = { navController.popBackStack() },
            onRetry = { viewModel.loadPointsHistory() },
            onTabClick = tabClick,
        )
    }

    listOf(EcoRutaDestination.MY_COUPONS to false, EcoRutaDestination.PROFILE_MY_COUPONS to true).forEach { (route, fromProfile) ->
        composable(route) {
            val viewModel: BenefitsViewModel = hiltViewModel()
            LaunchedEffect(Unit) {
                viewModel.loadMyCoupons()
            }
            val state by viewModel.myCouponsState.collectAsStateWithLifecycle()
            MyCouponsScreen(
                state = state,
                selectedTab = if (fromProfile) CitizenTab.PROFILE else CitizenTab.COUPONS,
                actions = MyCouponsActions(
                    onBack = { navController.popBackStack() },
                    onCategorySelected = { viewModel.onMyCouponsCategorySelected(it) },
                    onToggleExpanded = { id -> viewModel.onToggleMyCouponExpanded(id) },
                    onShowQr = { open(EcoRutaDestination.myCouponDetail(it.id, fromProfile)) },
                    onGoToBenefits = { navController.navigateToCitizenTab(CitizenTab.BENEFITS) },
                    onRetry = { viewModel.loadMyCoupons() },
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
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val fromProfile = entry.arguments?.getBoolean(EcoRutaDestination.ARG_FROM_PROFILE) ?: false
        val viewModel: BenefitsViewModel = hiltViewModel()
        LaunchedEffect(id) {
            viewModel.loadMyCouponDetail(id)
        }
        val detailState by viewModel.myCouponDetailState.collectAsStateWithLifecycle()
        MyCouponDetailScreen(
            state = detailState,
            selectedTab = if (fromProfile) CitizenTab.PROFILE else CitizenTab.COUPONS,
            onBack = { navController.popBackStack() },
            onBackToMyCoupons = { navController.popBackStack() },
            onBackToBenefits = ::backToBenefits,
            onRetry = { viewModel.loadMyCouponDetail(id) },
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.GUEST_BENEFITS) {
        val viewModel: BenefitsViewModel = hiltViewModel()
        val state by viewModel.benefitsState.collectAsStateWithLifecycle()
        GuestBenefitsScreen(
            state = state,
            onBack = { navController.popBackStack() },
            onLogin = { open(EcoRutaDestination.LOGIN) },
            onCategorySelected = { viewModel.onCategorySelected(it) },
            onTabClick = { tab -> navController.navigateToPublicTab(tab) },
        )
    }
}
