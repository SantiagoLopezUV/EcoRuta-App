package com.ecoruta.app.features.circular_economy.presentation.navigation

// Subgrafo de navegación para economía circular, reciclaje y entregas con QR
// Conecta la publicación de materiales, detalle, código de entrega y vista sin sesión

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ecoruta.app.R
import com.ecoruta.app.core.navigation.EcoRutaDestination
import com.ecoruta.app.core.navigation.navigateToCitizenTab
import com.ecoruta.app.core.navigation.navigateToPublicTab
import com.ecoruta.app.core.ui.components.GuestServiceActions
import com.ecoruta.app.features.circular_economy.presentation.publish.DeliveryCode
import com.ecoruta.app.features.circular_economy.presentation.publish.DeliveryCodeScreen
import com.ecoruta.app.features.circular_economy.presentation.publish.DeliveryCodeUiState
import com.ecoruta.app.features.circular_economy.presentation.publish.DeliveryCompletedScreen
import com.ecoruta.app.features.circular_economy.presentation.publish.ListingsViewModel
import com.ecoruta.app.features.circular_economy.presentation.publish.PublicationDetailActions
import com.ecoruta.app.features.circular_economy.presentation.publish.PublicationDetailScreen
import com.ecoruta.app.features.circular_economy.presentation.publish.PublicationStatus
import com.ecoruta.app.features.circular_economy.presentation.publish.PublicationUiState
import com.ecoruta.app.features.circular_economy.presentation.publish.PublishMaterialActions
import com.ecoruta.app.features.circular_economy.presentation.publish.PublishMaterialScreen
import com.ecoruta.app.features.circular_economy.presentation.publish.PublishMaterialUiState
import com.ecoruta.app.features.circular_economy.presentation.publish.RecyclingGuestScreen
import com.ecoruta.app.features.circular_economy.presentation.publish.RecyclingHomeActions
import com.ecoruta.app.features.circular_economy.presentation.publish.RecyclingHomeScreen
import com.ecoruta.app.features.circular_economy.presentation.publish.RecyclingHomeUiState
import com.ecoruta.app.features.circular_economy.presentation.publish.RecyclingPublication

// Registra las rutas del módulo de reciclaje, publicación de material y código QR dinámico
fun NavGraphBuilder.circularEconomyGraph(
    navController: NavHostController,
    showNotice: (String) -> Unit,
) {
    val idArgument = listOf(navArgument(EcoRutaDestination.ARG_ID) { type = NavType.StringType })
    fun open(route: String) = navController.navigate(route) { launchSingleTop = true }
    fun backToRecycling() {
        if (!navController.popBackStack(EcoRutaDestination.RECYCLING, inclusive = false)) open(EcoRutaDestination.RECYCLING)
    }

    composable(EcoRutaDestination.RECYCLING) {
        val viewModel: ListingsViewModel = hiltViewModel()
        val state by viewModel.homeState.collectAsStateWithLifecycle()

        RecyclingHomeScreen(
            state = state,
            actions = RecyclingHomeActions(
                onBack = { navController.popBackStack() },
                onPublish = { open(EcoRutaDestination.PUBLISH_MATERIAL) },
                onPublicationClick = { open(EcoRutaDestination.publicationDetail(it.id)) },
                onRetry = viewModel::loadListings,
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.PUBLISH_MATERIAL) { backStackEntry ->
        val parentEntry = remember(backStackEntry) {
            try {
                navController.getBackStackEntry(EcoRutaDestination.RECYCLING)
            } catch (_: Exception) {
                backStackEntry
            }
        }
        val viewModel: ListingsViewModel = hiltViewModel(parentEntry)
        val state by viewModel.publishState.collectAsStateWithLifecycle()

        PublishMaterialScreen(
            state = state,
            actions = PublishMaterialActions(
                onBack = { navController.popBackStack() },
                onMaterialSelected = viewModel::onMaterialSelected,
                onOtherMaterialChange = viewModel::onOtherMaterialChange,
                onVolumeSelected = viewModel::onVolumeSelected,
                onAddressChange = viewModel::onAddressChange,
                onPhotoSelected = viewModel::onPhotoSelected,
                onPhotoRemoved = viewModel::onPhotoRemoved,
                onPublish = {
                    viewModel.publish(
                        onSuccess = { navController.popBackStack() },
                    )
                },
            ),
        )
    }

    composable(EcoRutaDestination.PUBLICATION_DETAIL, arguments = idArgument) { backStackEntry ->
        val id = backStackEntry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val parentEntry = remember(backStackEntry) {
            try {
                navController.getBackStackEntry(EcoRutaDestination.RECYCLING)
            } catch (_: Exception) {
                backStackEntry
            }
        }
        val viewModel: ListingsViewModel = hiltViewModel(parentEntry)
        val homeState by viewModel.homeState.collectAsStateWithLifecycle()
        val context = LocalContext.current

        var publication by remember(id) {
            mutableStateOf((homeState as? RecyclingHomeUiState.Loaded)?.publications?.find { it.id == id })
        }
        var isLoading by remember(id) { mutableStateOf(publication == null) }
        var isNotFound by remember(id) { mutableStateOf(false) }

        LaunchedEffect(id, homeState) {
            val fromHome = (homeState as? RecyclingHomeUiState.Loaded)?.publications?.find { it.id == id }
            if (fromHome != null) {
                publication = fromHome
                isLoading = false
            } else if (publication == null) {
                isLoading = true
                viewModel.getListingById(id)
                    .onSuccess { pub ->
                        publication = pub
                        isLoading = false
                        isNotFound = false
                    }
                    .onFailure {
                        isLoading = false
                        isNotFound = true
                    }
            }
        }

        val state = when {
            publication != null -> PublicationUiState.Loaded(publication = publication!!, isSample = false)
            isLoading || homeState is RecyclingHomeUiState.Loading -> PublicationUiState.Loading
            isNotFound -> PublicationUiState.NotFound
            else -> PublicationUiState.Error
        }

        val isCancelling by viewModel.isCancelling.collectAsStateWithLifecycle()

        PublicationDetailScreen(
            state = state,
            isCancelling = isCancelling,
            actions = PublicationDetailActions(
                onBack = { navController.popBackStack() },
                onShowQr = { open(EcoRutaDestination.deliveryCode(id)) },
                onCallRecycler = { phone ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() }))
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        showNotice(context.getString(R.string.recycling_phone, phone))
                    }
                },
                onCancel = {
                    viewModel.cancelPublication(
                        id = id,
                        onSuccess = {
                            showNotice(context.getString(R.string.recycling_cancelled))
                            navController.popBackStack()
                        },
                        onError = { errorMsg ->
                            showNotice(errorMsg)
                        },
                    )
                },
                onViewDelivery = { open(EcoRutaDestination.deliveryCompleted(id)) },
                onRetry = {
                    isLoading = true
                    viewModel.loadListings()
                },
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.DELIVERY_CODE, arguments = idArgument) { backStackEntry ->
        val id = backStackEntry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val parentEntry = remember(backStackEntry) {
            try {
                navController.getBackStackEntry(EcoRutaDestination.RECYCLING)
            } catch (_: Exception) {
                backStackEntry
            }
        }
        val viewModel: ListingsViewModel = hiltViewModel(parentEntry)
        val homeState by viewModel.homeState.collectAsStateWithLifecycle()
        var publication by remember(id) {
            mutableStateOf((homeState as? RecyclingHomeUiState.Loaded)?.publications?.find { it.id == id })
        }

        LaunchedEffect(id, homeState) {
            val fromHome = (homeState as? RecyclingHomeUiState.Loaded)?.publications?.find { it.id == id }
            if (fromHome != null) {
                publication = fromHome
            } else if (publication == null) {
                viewModel.getListingById(id).onSuccess { publication = it }
            }
        }

        val code = DeliveryCode(token = "ECO-4829", expiresAtMillis = System.currentTimeMillis() + DeliveryCode.VALIDITY_MS)
        DeliveryCodeScreen(
            state = DeliveryCodeUiState(
                isLoading = publication == null,
                publication = publication,
                code = code,
                isSample = false,
            ),
            onRenew = {},
            onBack = { navController.popBackStack() },
        )
    }

    composable(EcoRutaDestination.DELIVERY_COMPLETED, arguments = idArgument) { backStackEntry ->
        val id = backStackEntry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val parentEntry = remember(backStackEntry) {
            try {
                navController.getBackStackEntry(EcoRutaDestination.RECYCLING)
            } catch (_: Exception) {
                backStackEntry
            }
        }
        val viewModel: ListingsViewModel = hiltViewModel(parentEntry)
        val homeState by viewModel.homeState.collectAsStateWithLifecycle()
        var publication by remember(id) {
            mutableStateOf((homeState as? RecyclingHomeUiState.Loaded)?.publications?.find { it.id == id })
        }

        LaunchedEffect(id, homeState) {
            val fromHome = (homeState as? RecyclingHomeUiState.Loaded)?.publications?.find { it.id == id }
            if (fromHome != null) {
                publication = fromHome
            } else if (publication == null) {
                viewModel.getListingById(id).onSuccess { publication = it }
            }
        }

        val delivered = publication?.copy(status = PublicationStatus.COMPLETADO, deliveredAtMillis = System.currentTimeMillis())
        DeliveryCompletedScreen(
            state = if (delivered != null) {
                PublicationUiState.Loaded(publication = delivered, isSample = false)
            } else if (homeState is RecyclingHomeUiState.Loading) {
                PublicationUiState.Loading
            } else {
                PublicationUiState.NotFound
            },
            onBackToRecycling = ::backToRecycling,
            onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
        )
    }

    composable(EcoRutaDestination.GUEST_RECYCLING) {
        RecyclingGuestScreen(
            actions = GuestServiceActions(
                onBack = { navController.popBackStack() },
                onLogin = { open(EcoRutaDestination.LOGIN) },
                onTabClick = { tab -> navController.navigateToPublicTab(tab) },
            ),
        )
    }
}
