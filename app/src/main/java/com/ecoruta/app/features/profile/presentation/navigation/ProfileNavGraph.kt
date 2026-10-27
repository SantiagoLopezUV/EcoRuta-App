package com.ecoruta.app.features.profile.presentation.navigation

// Subgrafo de navegación para el perfil de usuario, cambio de rol y postulaciones
// Orquesta las vistas de perfil con y sin sesión, solicitud de comercio y reciclador

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.components.CitizenTab
import com.ecoruta.app.core.navigation.goToPublicHomeAfterSignOut
import com.ecoruta.app.core.navigation.navigateToCitizenTab
import com.ecoruta.app.core.navigation.navigateToPublicTab
import com.ecoruta.app.features.auth.presentation.first_login.ChangePasswordMode
import com.ecoruta.app.features.profile.presentation.Company
import com.ecoruta.app.features.profile.presentation.GuestProfileActions
import com.ecoruta.app.features.profile.presentation.GuestProfileScreen
import com.ecoruta.app.features.profile.presentation.ProfileActions
import com.ecoruta.app.features.profile.presentation.ProfileScreen
import com.ecoruta.app.features.profile.presentation.ProfileUiState
import com.ecoruta.app.features.profile.presentation.RecyclerApplicationUiState
import com.ecoruta.app.features.profile.presentation.RecyclerModality
import com.ecoruta.app.features.profile.presentation.StoreApplicationUiState
import com.ecoruta.app.features.profile.presentation.UserProfile
import com.ecoruta.app.features.profile.presentation.UserRole
import com.ecoruta.app.features.profile.presentation.components.ChangeProfileActions
import com.ecoruta.app.features.profile.presentation.components.ChangeProfileScreen
import com.ecoruta.app.features.profile.presentation.components.CompanyRequestSentScreen
import com.ecoruta.app.features.profile.presentation.components.CompanySearchScreen
import com.ecoruta.app.features.profile.presentation.components.RecyclerApplicationActions
import com.ecoruta.app.features.profile.presentation.components.RecyclerApplicationScreen
import com.ecoruta.app.features.profile.presentation.components.RecyclerApplicationSentScreen
import com.ecoruta.app.features.profile.presentation.components.SelectedCompanyScreen
import com.ecoruta.app.features.profile.presentation.components.StoreApplicationActions
import com.ecoruta.app.features.profile.presentation.components.StoreApplicationScreen
import com.ecoruta.app.features.profile.presentation.components.StoreRequestInReviewScreen

private val SampleUserProfile = UserProfile(
    fullName = "Camila Restrepo",
    role = UserRole.CIUDADANO,
    city = "Rionegro, Antioquia",
    email = "camila.restrepo@ecoruta.co",
    documentNumber = "1.036.948.210",
    ecoPoints = 1_250,
    approvedRoles = setOf(UserRole.CIUDADANO),
)

private val SampleCompanies = listOf(
    Company("EMP-01", "Asorecicladores del Oriente", "Asociación"),
    Company("EMP-02", "Cooperativa EcoVida", "Cooperativa"),
)

// Registra las rutas del perfil de usuario, cambio de rol y postulaciones
fun NavGraphBuilder.profileGraph(
    navController: NavHostController,
    showNotice: (String) -> Unit,
) {
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }

    composable(EcoRutaDestination.PERFIL_SIN_SESION) {
        GuestProfileScreen(
            actions = GuestProfileActions(
                onBack = { navController.popBackStack() },
                onLogin = { open(EcoRutaDestination.INICIAR_SESION) },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.MI_PERFIL) {
        val context = LocalContext.current
        var localPhoto by remember { mutableStateOf<Uri?>(null) }
        ProfileScreen(
            state = ProfileUiState.Loaded(profile = SampleUserProfile, localPhoto = localPhoto),
            isSigningOut = false,
            isUpdatingPhoto = false,
            actions = ProfileActions(
                onBack = { navController.navigateToCitizenTab(CitizenTab.INICIO) },
                onPhotoSelected = { uri -> localPhoto = uri },
                onRemovePhoto = { localPhoto = null },
                onCameraUnavailable = { showNotice(context.getString(R.string.profile_camera_unavailable)) },
                onMyReports = { open(EcoRutaDestination.MIS_REPORTES) },
                onMyDebrisRequests = { open(EcoRutaDestination.MIS_SOLICITUDES_ESCOMBROS) },
                onMyCoupons = { open(EcoRutaDestination.MIS_CUPONES_PERFIL) },
                onChangeProfile = { open(EcoRutaDestination.CAMBIAR_PERFIL) },
                onChangePassword = { open(EcoRutaDestination.cambiarContrasena(ChangePasswordMode.PERFIL, SampleUserProfile.role)) },
                onSignOut = { navController.goToPublicHomeAfterSignOut() },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.CAMBIAR_PERFIL) {
        ChangeProfileScreen(
            state = ProfileUiState.Loaded(profile = SampleUserProfile),
            actions = ChangeProfileActions(
                onBack = { navController.popBackStack() },
                onSwitchRole = { role ->
                    navController.navigate(EcoRutaDestination.inicioDelRol(role)) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onRequestRole = { role ->
                    if (role == UserRole.COMERCIO) {
                        open(EcoRutaDestination.SOLICITUD_COMERCIO)
                    } else {
                        open(EcoRutaDestination.POSTULACION_RECICLADOR)
                    }
                },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    roleRequestDestinations(navController = navController)
}

private fun NavGraphBuilder.roleRequestDestinations(navController: NavHostController) {
    fun backToProfile() {
        if (!navController.popBackStack(EcoRutaDestination.MI_PERFIL, inclusive = false)) {
            navController.navigateToCitizenTab(CitizenTab.PERFIL)
        }
    }

    composable(EcoRutaDestination.SOLICITUD_COMERCIO) {
        var state by remember { mutableStateOf(StoreApplicationUiState()) }
        StoreApplicationScreen(
            state = state,
            actions = StoreApplicationActions(
                onBack = { navController.popBackStack() },
                onNameChange = { state = state.copy(name = it, nameError = null) },
                onAddressChange = { state = state.copy(address = it, addressError = null) },
                onPhoneChange = { state = state.copy(phone = it, phoneError = null) },
                onCategorySelected = { state = state.copy(category = it, categoryError = null) },
                onIdTypeSelected = { state = state.copy(idType = it) },
                onIdNumberChange = { state = state.copy(idNumber = it, idError = null) },
                onRutPicked = { doc -> state = state.copy(rut = doc, rutError = null) },
                onRutRemoved = { state = state.copy(rut = null) },
                onSubmit = {
                    navController.navigate(EcoRutaDestination.solicitudComercioRevision(true)) {
                        popUpTo(EcoRutaDestination.SOLICITUD_COMERCIO) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            ),
        )
    }

    composable(
        EcoRutaDestination.SOLICITUD_COMERCIO_REVISION,
        arguments = listOf(
            navArgument(EcoRutaDestination.ARG_SIN_BACKEND) {
                type = NavType.BoolType
                defaultValue = false
            },
        ),
    ) { entry ->
        val withoutBackend = entry.arguments?.getBoolean(EcoRutaDestination.ARG_SIN_BACKEND) ?: false
        BackHandler { backToProfile() }
        StoreRequestInReviewScreen(
            withoutBackend = withoutBackend,
            onBack = ::backToProfile,
            onGoHome = { navController.navigateToCitizenTab(CitizenTab.INICIO) },
            onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
        )
    }

    navigation(
        startDestination = EcoRutaDestination.POSTULACION_FORMULARIO,
        route = EcoRutaDestination.POSTULACION_RECICLADOR,
    ) {
        composable(EcoRutaDestination.POSTULACION_FORMULARIO) {
            var state by remember {
                mutableStateOf(
                    RecyclerApplicationUiState(
                        fullName = SampleUserProfile.fullName,
                        documentNumber = SampleUserProfile.documentNumber,
                        modality = RecyclerModality.INDEPENDIENTE,
                        results = SampleCompanies,
                    ),
                )
            }
            RecyclerApplicationScreen(
                state = state,
                actions = RecyclerApplicationActions(
                    onBack = { navController.popBackStack(EcoRutaDestination.POSTULACION_RECICLADOR, inclusive = true) },
                    onModalitySelected = { state = state.copy(modality = it) },
                    onPhoneChange = { state = state.copy(phone = it, phoneError = null) },
                    onDocumentPicked = { doc -> state = state.copy(idDocument = doc, documentError = null) },
                    onDocumentRemoved = { state = state.copy(idDocument = null) },
                    onContinue = {
                        if (state.modality == RecyclerModality.VINCULADO) {
                            navController.navigate(EcoRutaDestination.POSTULACION_BUSCAR_EMPRESA) { launchSingleTop = true }
                        } else {
                            navController.navigate(EcoRutaDestination.POSTULACION_ENVIADA) {
                                popUpTo(EcoRutaDestination.POSTULACION_FORMULARIO) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                ),
            )
        }

        composable(EcoRutaDestination.POSTULACION_BUSCAR_EMPRESA) {
            var state by remember {
                mutableStateOf(
                    RecyclerApplicationUiState(
                        modality = RecyclerModality.VINCULADO,
                        results = SampleCompanies,
                    ),
                )
            }
            CompanySearchScreen(
                state = state,
                onQueryChange = { state = state.copy(query = it) },
                onCompanySelected = { company ->
                    state = state.copy(company = company)
                    navController.navigate(EcoRutaDestination.POSTULACION_EMPRESA_SELECCIONADA) { launchSingleTop = true }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(EcoRutaDestination.POSTULACION_EMPRESA_SELECCIONADA) {
            val state = RecyclerApplicationUiState(
                modality = RecyclerModality.VINCULADO,
                company = SampleCompanies.first(),
            )
            SelectedCompanyScreen(
                state = state,
                onChangeCompany = { navController.popBackStack() },
                onComplete = {
                    navController.navigate(EcoRutaDestination.POSTULACION_EMPRESA_ENVIADA) {
                        popUpTo(EcoRutaDestination.POSTULACION_FORMULARIO) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(EcoRutaDestination.POSTULACION_ENVIADA) {
            BackHandler { backToProfile() }
            RecyclerApplicationSentScreen(
                state = RecyclerApplicationUiState(submittedWithoutBackend = true),
                onBack = ::backToProfile,
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.INICIO) },
            )
        }

        composable(EcoRutaDestination.POSTULACION_EMPRESA_ENVIADA) {
            BackHandler { backToProfile() }
            CompanyRequestSentScreen(
                state = RecyclerApplicationUiState(company = SampleCompanies.first(), submittedWithoutBackend = true),
                onGoHome = { navController.navigateToCitizenTab(CitizenTab.INICIO) },
            )
        }
    }
}
