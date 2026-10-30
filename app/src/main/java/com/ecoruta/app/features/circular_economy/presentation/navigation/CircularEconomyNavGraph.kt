package com.ecoruta.app.features.circular_economy.presentation.navigation

// Subgrafo de navegación para economía circular, reciclaje y entregas con QR
// Conecta la publicación de materiales, detalle, código de entrega y vista sin sesión

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import com.ecoruta.app.features.circular_economy.presentation.publish.AssignedRecycler
import com.ecoruta.app.features.circular_economy.presentation.publish.DeliveryCode
import com.ecoruta.app.features.circular_economy.presentation.publish.DeliveryCodeScreen
import com.ecoruta.app.features.circular_economy.presentation.publish.DeliveryCodeUiState
import com.ecoruta.app.features.circular_economy.presentation.publish.DeliveryCompletedScreen
import com.ecoruta.app.features.circular_economy.presentation.publish.MaterialType
import com.ecoruta.app.features.circular_economy.presentation.publish.MaterialVolume
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

private val SamplePublications = listOf(
    RecyclingPublication(
        id = "PUB-01",
        code = "PUB-2026-01",
        material = MaterialType.PET,
        volume = MaterialVolume.LARGE_BAG,
        address = "Calle 14 #8-20, La Esperanza",
        status = PublicationStatus.EN_CAMINO,
        createdAtMillis = 1_760_100_000_000L,
        recycler = AssignedRecycler(name = "Carlos Gómez", affiliation = "Asorecicladores del Oriente", phone = "3112345678"),
        pointsEarned = 80,
    ),
)

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
        RecyclingHomeScreen(
            state = RecyclingHomeUiState.Loaded(ecoPoints = 1_250, publications = SamplePublications, isSample = true),
            actions = RecyclingHomeActions(
                onBack = { navController.popBackStack() },
                onPublish = { open(EcoRutaDestination.PUBLISH_MATERIAL) },
                onPublicationClick = { open(EcoRutaDestination.publicationDetail(it.id)) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.PUBLISH_MATERIAL) {
        var state by remember { mutableStateOf(PublishMaterialUiState()) }
        PublishMaterialScreen(
            state = state,
            actions = PublishMaterialActions(
                onBack = { navController.popBackStack() },
                onMaterialSelected = { state = state.copy(material = it) },
                onOtherMaterialChange = { state = state.copy(otherMaterial = it, otherMaterialError = null) },
                onVolumeSelected = { state = state.copy(volume = it, volumeError = null) },
                onAddressChange = { state = state.copy(address = it, addressError = null) },
                onPhotoSelected = { uri -> state = state.copy(photo = uri) },
                onPhotoRemoved = { state = state.copy(photo = null) },
                onPublish = { navController.popBackStack() },
            ),
        )
    }

    composable(EcoRutaDestination.PUBLICATION_DETAIL, arguments = idArgument) { entry ->
        val id = entry.arguments?.getString(EcoRutaDestination.ARG_ID).orEmpty()
        val pub = SamplePublications.find { it.id == id } ?: SamplePublications.first()
        val context = LocalContext.current
        PublicationDetailScreen(
            state = PublicationUiState.Loaded(publication = pub, isSample = true),
            isCancelling = false,
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
                onCancel = { navController.popBackStack() },
                onViewDelivery = { open(EcoRutaDestination.deliveryCompleted(id)) },
                onRetry = {},
                onTabClick = { tab -> navController.navigateToCitizenTab(tab) },
            ),
        )
    }

    composable(EcoRutaDestination.DELIVERY_CODE, arguments = idArgument) {
        val code = DeliveryCode(token = "ECO-4829", expiresAtMillis = System.currentTimeMillis() + DeliveryCode.VALIDITY_MS)
        DeliveryCodeScreen(
            state = DeliveryCodeUiState(isLoading = false, publication = SamplePublications.first(), code = code, isSample = true),
            onRenew = {},
            onBack = { navController.popBackStack() },
        )
    }

    composable(EcoRutaDestination.DELIVERY_COMPLETED, arguments = idArgument) {
        val delivered = SamplePublications.first().copy(status = PublicationStatus.ENTREGADA, deliveredAtMillis = 1_760_150_000_000L)
        DeliveryCompletedScreen(
            state = PublicationUiState.Loaded(publication = delivered, isSample = true),
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
