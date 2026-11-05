package com.ecoruta.app.features.gamification.presentation.navigation

// Subgrafo de navegación del comercio aliado
// Conecta el panel con las pestañas de crear cupón, validar en caja, mis cupones y perfil

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.components.MerchantTab
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.core.ui.components.EcoSystemStateScreen
import com.ecoruta.app.features.gamification.presentation.cashier_validation.COUPON_CODE_LENGTH
import com.ecoruta.app.features.gamification.presentation.cashier_validation.COUPON_CODE_PREFIX
import com.ecoruta.app.features.gamification.presentation.cashier_validation.MerchantCashierActions
import com.ecoruta.app.features.gamification.presentation.cashier_validation.MerchantCashierScreen
import com.ecoruta.app.features.gamification.presentation.cashier_validation.MerchantCashierUiState
import com.ecoruta.app.features.gamification.presentation.cashier_validation.MerchantCouponValidatedScreen
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCouponFormActions
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCouponFormUiState
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCouponPublishedActions
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCouponPublishedScreen
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCouponPublishedUiState
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCouponsActions
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCouponsScreen
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCouponsUiState
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantCreateCouponScreen
import com.ecoruta.app.features.gamification.presentation.merchant_coupons.MerchantEditCouponScreen
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantDashboardActions
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantDashboardScreen
import com.ecoruta.app.features.gamification.presentation.merchant_dashboard.MerchantDashboardUiState
import com.ecoruta.app.features.profile.presentation.MerchantChangeProfileScreen
import com.ecoruta.app.features.profile.presentation.MerchantProfileActions
import com.ecoruta.app.features.profile.presentation.MerchantProfileScreen
import com.ecoruta.app.features.profile.presentation.MerchantProfileUiState
import com.ecoruta.app.features.profile.presentation.UserProfile
import com.ecoruta.app.features.profile.presentation.UserRole
import com.ecoruta.app.features.profile.presentation.components.ChangeProfileActions

// Registra las pantallas del comercio aliado con sesión iniciada
fun NavGraphBuilder.merchantGraph(navController: NavHostController) {
    val tabClick: (MerchantTab) -> Unit = { tab -> navController.navigateToMerchantTab(tab) }
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }

    composable(EcoRutaDestination.STORE_DASHBOARD) {
        MerchantDashboardScreen(
            state = MerchantDashboardUiState(),
            actions = MerchantDashboardActions(
                onEditCoupon = { open(EcoRutaDestination.merchantEditCoupon(it.id)) },
                onCreateCoupon = { tabClick(MerchantTab.CREATE_COUPON) },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.MERCHANT_CREATE_COUPON) {
        var state by remember { mutableStateOf(MerchantCouponFormUiState()) }
        MerchantCreateCouponScreen(
            state = state,
            actions = MerchantCouponFormActions(
                onTitleChange = { state = state.copy(title = it) },
                onDescriptionChange = { state = state.copy(description = it) },
                onPointsChange = { state = state.copy(pointsCost = it) },
                onQuantityChange = { state = state.copy(quantity = it) },
                onValidUntilChange = { state = state.copy(validUntil = it) },
                onSubmit = { open(EcoRutaDestination.MERCHANT_COUPON_PUBLISHED) },
            ),
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.MERCHANT_COUPON_PUBLISHED) {
        MerchantCouponPublishedScreen(
            state = MerchantCouponPublishedUiState(),
            actions = MerchantCouponPublishedActions(
                onViewMyCoupons = { tabClick(MerchantTab.MY_COUPONS) },
                onBackToDashboard = { tabClick(MerchantTab.DASHBOARD) },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.MERCHANT_COUPONS) {
        MerchantCouponsScreen(
            state = MerchantCouponsUiState(),
            actions = MerchantCouponsActions(
                onEditCoupon = { open(EcoRutaDestination.merchantEditCoupon(it.id)) },
                onCreateCoupon = { tabClick(MerchantTab.CREATE_COUPON) },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.MERCHANT_EDIT_COUPON) {
        // Sin datos del servidor el cupón elegido no se puede cargar y se muestra como inexistente
        var state by remember { mutableStateOf(MerchantCouponFormUiState(isCouponMissing = true)) }
        MerchantEditCouponScreen(
            state = state,
            actions = MerchantCouponFormActions(
                onTitleChange = { state = state.copy(title = it) },
                onDescriptionChange = { state = state.copy(description = it) },
                onPointsChange = { state = state.copy(pointsCost = it) },
                onQuantityChange = { state = state.copy(quantity = it) },
                onValidUntilChange = { state = state.copy(validUntil = it) },
                onActiveChange = { state = state.copy(isActive = it) },
                onSubmit = { tabClick(MerchantTab.MY_COUPONS) },
            ),
            onBackToMyCoupons = { tabClick(MerchantTab.MY_COUPONS) },
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.MERCHANT_VALIDATE) {
        val context = LocalContext.current
        var state by remember { mutableStateOf(MerchantCashierUiState()) }
        var showCameraPermission by remember { mutableStateOf(false) }
        // Al conceder el permiso se abre la cámara y al negarlo se vuelve a la pantalla de caja
        val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            showCameraPermission = false
            if (granted) state = state.copy(isScanning = true)
        }
        if (showCameraPermission) {
            // Explica el uso de la cámara en caja antes de pedir el permiso al sistema
            EcoSystemStateScreen(
                title = stringResource(R.string.permission_camera_title),
                subtitle = stringResource(R.string.merchant_cashier_camera_subtitle),
                illustrationRes = R.drawable.ill_state_camera,
                heading = stringResource(R.string.merchant_cashier_camera_heading),
                body = stringResource(R.string.merchant_cashier_camera_body),
                actionText = stringResource(R.string.permission_camera_action),
                onAction = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onBack = { showCameraPermission = false },
            )
        } else {
            MerchantCashierScreen(
                state = state,
                actions = MerchantCashierActions(
                    onStartScan = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        if (granted) state = state.copy(isScanning = true) else showCameraPermission = true
                    },
                    onStopScan = { state = state.copy(isScanning = false) },
                    // El QR trae el código completo y en el campo solo se escriben los caracteres después del prefijo
                    onQrDetected = { raw ->
                        val code = raw.trim().uppercase().removePrefix(COUPON_CODE_PREFIX).filter(Char::isLetterOrDigit).take(COUPON_CODE_LENGTH)
                        state = state.copy(code = code, isScanning = false, validationError = null)
                    },
                    onCodeChange = { state = state.copy(code = it, validationError = null) },
                    onVerify = { open(EcoRutaDestination.MERCHANT_COUPON_VALIDATED) },
                    onTabClick = tabClick,
                ),
            )
        }
    }

    composable(EcoRutaDestination.MERCHANT_COUPON_VALIDATED) {
        MerchantCouponValidatedScreen(
            coupon = null,
            onValidateAnother = { navController.popBackStack() },
            onTabClick = tabClick,
        )
    }

    composable(EcoRutaDestination.MERCHANT_PROFILE) {
        var localPhoto by remember { mutableStateOf<Uri?>(null) }
        MerchantProfileScreen(
            state = MerchantProfileUiState(localPhoto = localPhoto),
            actions = MerchantProfileActions(
                onPhotoSelected = { localPhoto = it },
                onRemovePhoto = { localPhoto = null },
                onChangeProfile = { open(EcoRutaDestination.MERCHANT_CHANGE_PROFILE) },
                onSignOut = { navController.goToPublicHomeAfterSignOut() },
                onTabClick = tabClick,
            ),
        )
    }

    composable(EcoRutaDestination.MERCHANT_CHANGE_PROFILE) {
        // Solo se conoce el rol activo mientras no lleguen los perfiles aprobados desde el servidor
        val currentProfile = UserProfile(fullName = "", role = UserRole.COMERCIO, city = "", email = "")
        MerchantChangeProfileScreen(
            profile = currentProfile,
            actions = ChangeProfileActions(
                onBack = { navController.popBackStack() },
                onSwitchRole = { role ->
                    navController.navigate(EcoRutaDestination.homeForRole(role)) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRequestRole = { role ->
                    val route = if (role == UserRole.COMERCIO) EcoRutaDestination.STORE_REQUEST else EcoRutaDestination.RECYCLER_APPLICATION
                    open(route)
                },
            ),
            onTabClick = tabClick,
        )
    }
}

// Navega entre las pestañas del comercio conservando el panel como base de la pila
fun NavHostController.navigateToMerchantTab(tab: MerchantTab) {
    val route = when (tab) {
        // El panel ya es la base de la pila, así que basta con volver a él
        MerchantTab.DASHBOARD -> {
            popBackStack(EcoRutaDestination.STORE_DASHBOARD, inclusive = false)
            return
        }
        MerchantTab.CREATE_COUPON -> EcoRutaDestination.MERCHANT_CREATE_COUPON
        MerchantTab.VALIDATE -> EcoRutaDestination.MERCHANT_VALIDATE
        MerchantTab.MY_COUPONS -> EcoRutaDestination.MERCHANT_COUPONS
        MerchantTab.PROFILE -> EcoRutaDestination.MERCHANT_PROFILE
    }
    navigate(route) {
        popUpTo(EcoRutaDestination.STORE_DASHBOARD)
        launchSingleTop = true
    }
}
